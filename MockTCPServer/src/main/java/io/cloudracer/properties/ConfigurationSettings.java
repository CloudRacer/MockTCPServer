package io.cloudracer.properties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.CombinedLocationStrategy;
import org.apache.commons.configuration2.io.FileLocationStrategy;
import org.apache.commons.configuration2.io.FileLocator;
import org.apache.commons.configuration2.io.FileLocatorUtils;
import org.apache.commons.configuration2.io.FileSystemLocationStrategy;
import org.apache.commons.configuration2.io.ProvidedURLLocationStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.cloudracer.mocktcpserver.MockTCPServer;
import io.cloudracer.mocktcpserver.responses.ResponseDAO;
import io.cloudracer.mocktcpserver.responses.Responses;

/**
 * A {@link ConfigurationSettings#FILENAME resource file} is used as a default configuration file. If the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property} is set with a value of true, the {@link #FILENAME resource file} will be written to the {@link #DEFAULT_FILENAME default location}. This behaviour allow for self-configuration and the ability to "reset to factory settings" if the operator deletes the {@link #getFileName() Configuration File} and restarts MockTCPServer.
 *
 * @author John McDonnell
 */
public class ConfigurationSettings extends AbstractConfiguration {

    private static final String UNSUPPORTED = "Unsupported.";

    private Logger logger;

    /**
     * A System Property that, when set with a value of "true", will result in the <b>default</b> configuration file (stored as a {@link #FILENAME resource file}) being written to disk i.e self-initialised (an existing file will not be overwritten). Once on disk, the configuration file can be modified as required.
     */
    public static final String CONFIGURATION_INITIALISATION_ENABLED = "mocktcpserver.configuration.initialisation.enabled";
    /**
     * The name of the resource file that is the default configuration file. If the file cannot be located and the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property} is true, this file can be written to the {@link #DEFAULT_FILENAME default location} on file system to initialise the configuration.
     */
    public static final String FILENAME = "mocktcpserver.xml";
    /**
     * The location of the default {@link #FILENAME resource file}. This folder name is relative to the runtime working folder.
     */
    public static final String FILENAME_PATH = "configuration";
    /**
     * The default location, and name, of the configuration file on the file system.
     * <p>
     * The default configuration file is held as a {@link #FILENAME resource file}.
     */
    public static final String DEFAULT_FILENAME = String.format("%s%s%s", FILENAME_PATH, File.separatorChar, FILENAME);
    private static final String RESPONSES_ELEMENT_NAME = "responses";
    private static final String RESPONSE_ELEMENT_NAME = "response";
    private static final String MESSAGE_ELEMENT_NAME = "message";
    private static final String PORT_ATTRIBUTE_NAME = "port";
    private static final String SERVER_ELEMENT_NAME = "server";
    /**
     * The name of the attribute, in the configuration file, that specifies this servers port number.
     */
    public static final String PORT_PROPERTY_NAME = String.format("%s[@%s]", SERVER_ELEMENT_NAME, PORT_ATTRIBUTE_NAME);

    private URL propertiesFile;
    private FileBasedConfigurationBuilder<XMLConfiguration> configurationBuilder;

    /**
     * Retrieve a unmodifiable set of server port numbers, to listen on, that are specified in the {@link #getFileName() configuration file}.
     *
     * @return an unmodifiable set of server port numbers specified in the {@link #getFileName() configuration file}
     * @throws ConfigurationException
     */
    public Set<Integer> getPorts() throws ConfigurationException {
        try {
            final String expression = "/configuration/server";
            final XPath xPath = XPathFactory.newInstance().newXPath();

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(getFileName().toString());
            final NodeList portNodes = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
            final Set<Integer> ports = new HashSet<>();
            for (int i = 0; i < portNodes.getLength(); i++) {
                final Node server = portNodes.item(i);
                final String portValue = server.getAttributes().getNamedItem(PORT_ATTRIBUTE_NAME).getNodeValue();
                final Integer port = Integer.parseInt(portValue);

                ports.add(port);
            }

            return Collections.unmodifiableSet(ports);
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Returns all of the responses specified for the {@link MockTCPServer} configured on the specified port.
     *
     * @param port of the {@link MockTCPServer} in question.
     * @return The responses for the {@link MockTCPServer} running on the specified port.
     * @throws ConfigurationException
     */
    public Responses getResponses(int port) throws ConfigurationException {
        final Responses responses = new Responses();

        final NodeList incomingList = getIncomingMessages(port);
        for (int incomingIndex = 0; incomingIndex <= incomingList.getLength() - 1; ++incomingIndex) {
            final Node incoming = incomingList.item(incomingIndex);
            final String incomingMessage = getIncomingMessage(incoming);
            final Node responseList = getIncomingResponses(incoming);
            for (int responseIndex = 0; responseIndex <= responseList.getChildNodes().getLength() - 1; ++responseIndex) {
                final Node response = responseList.getChildNodes().item(responseIndex);
                if (response.getNodeName().equals(RESPONSE_ELEMENT_NAME)) {
                    responses.add(incomingMessage, createResponseDAO(response));
                }
            }
        }

        return responses;
    }

    private NodeList getIncomingMessages(final int port) throws ConfigurationException {
        try {
            final String expression = String.format("/configuration/server[@port='%d']/incoming", port);
            final XPath xPath = XPathFactory.newInstance().newXPath();

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(getFileName().toString());
            return (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new ConfigurationException(e);
        }

    }

    private String getIncomingMessage(final Node incoming) {
        String message = null;

        for (int i = 0; i <= incoming.getChildNodes().getLength() - 1; ++i) {
            final Node node = incoming.getChildNodes().item(i);
            if (node.getNodeName().equals(MESSAGE_ELEMENT_NAME)) {
                message = incoming.getChildNodes().item(i).getTextContent();

                break;
            }
        }

        return message;
    }

    private Node getIncomingResponses(final Node incoming) {
        Node responses = null;

        for (int i = 0; i <= incoming.getChildNodes().getLength() - 1; ++i) {
            final Node node = incoming.getChildNodes().item(i);
            if (node.getNodeName().equals(RESPONSES_ELEMENT_NAME)) {
                responses = incoming.getChildNodes().item(i);

                break;
            }
        }

        return responses;
    }

    private ResponseDAO createResponseDAO(final Node response) {
        final String machineName = response.getAttributes().getNamedItem("machine").getTextContent();
        final int machinePort = Integer.parseInt(response.getAttributes().getNamedItem(PORT_ATTRIBUTE_NAME).getTextContent());
        final String responseMessage = response.getAttributes().getNamedItem("message").getTextContent();

        return new ResponseDAO(machineName, machinePort, responseMessage);
    }

    /**
     * The file {@link URL} can be absolute or relative to the working folder. The configuration file is located using a {@link FileLocatorUtils#DEFAULT_LOCATION_STRATEGY strategy} that uses a number of techniques to determine the file location.
     * <p>
     * If no file is found, the {@link #FILENAME resource file} is used.
     *
     * @return the configuration file {@link URL}.
     */
    public URL getFileName() {
        if (this.propertiesFile == null) {
            final List<FileLocationStrategy> subs = Arrays.asList(
                    new ProvidedURLLocationStrategy(),
                    new FileSystemLocationStrategy(),
                    new ClasspathLocationStrategy());
            final FileLocationStrategy strategy = new CombinedLocationStrategy(subs);

            FileLocator fileLocator = FileLocatorUtils.fileLocator()
                    .basePath(this.getDefaultFile().getParent())
                    .fileName(this.getDefaultFile().getName())
                    .create();

            if (!new File(FileLocatorUtils.locate(fileLocator).getFile()).exists()) {
                fileLocator = FileLocatorUtils.fileLocator()
                        .fileName(this.getDefaultFile().getName())
                        .locationStrategy(strategy)
                        .create();
            }

            this.propertiesFile = FileLocatorUtils.locate(fileLocator);

            // Ensure that the ConfigurationBuilder is now initialised as that may force a re-initialisation of this FileLocator.
            this.getConfigurationBuilder();
        }

        return this.propertiesFile;
    }

    private File getDefaultFile() {
        return new File(DEFAULT_FILENAME);
    }

    @Override
    protected void addPropertyDirect(String arg0, Object arg1) {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void clearPropertyDirect(String arg0) {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean containsKeyInternal(String arg0) {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected Iterator<String> getKeysInternal() {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected Object getPropertyInternal(String arg0) {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean isEmptyInternal() {
        throw new NotImplementedException(UNSUPPORTED, new UnsupportedOperationException());
    }

    private FileBasedConfigurationBuilder<XMLConfiguration> getConfigurationBuilder() {
        if (this.configurationBuilder == null) {
            final Parameters params = new Parameters();
            this.configurationBuilder = new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
                    .configure(params.xml()
                            .setURL(this.getFileName())
                            .setValidating(false));
            this.configurationBuilder.setAutoSave(true);

            // If the file has not yet been written to the disk, as this is the first execution or the file has been deleted (deletion is a legitimate way to "reset to factory settings"), write it now.
            if (this.isConfigurationInitialisationEnabled() && !this.getDefaultFile().exists()) {
                // Reinitialise in order to pick up the newly created file.
                this.propertiesFile = null;
                this.save();
                this.configurationBuilder = null;
                this.getConfigurationBuilder();
            }
        }

        return this.configurationBuilder;
    }

    /**
     * Returns the value of the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property}.
     *
     * @return the value of the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property}. If the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property} is not found, false is returned.
     * @see #CONFIGURATION_INITIALISATION_ENABLED
     */
    public boolean isConfigurationInitialisationEnabled() {
        return BooleanUtils.toBoolean(System.getProperties().getProperty(CONFIGURATION_INITIALISATION_ENABLED, BooleanUtils.toStringTrueFalse(Boolean.FALSE)));
    }

    private void save() {
        try {
            // Create the destination folder, if it does not already exist.
            FileUtils.forceMkdir(this.getDefaultFile().getParentFile());
        } catch (final IOException e) {
            this.getLog().error(e.getMessage(), e);
        } finally {
            try (final FileWriter fileWriter = new FileWriter(this.getDefaultFile())) {
                this.getConfigurationBuilder().getConfiguration().write(fileWriter);
            } catch (final IOException | ConfigurationException e) {
                this.getLog().error(e.getMessage(), e);
            }
        }
    }

    private Logger getLog() {
        if (this.logger == null) {
            this.logger = LogManager.getFormatterLogger();
        }

        return this.logger;
    }
}