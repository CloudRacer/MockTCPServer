package io.cloudracer.properties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A {@link ConfigurationSettings#FILENAME resource file} is used as a default configuration file. If the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property} is set with a value of true, the {@link #FILENAME resource file} will be written to the {@link #DEFAULT_FILENAME default location}. This behaviour allow for self-configuration and the ability to "reset to factory settings" if the operator deletes the {@link #getFileName() Configuration File} and restarts MockTCPServer.
 *
 * @author John McDonnell
 */
public class ConfigurationSettings extends AbstractConfiguration {

    private Logger logger;

    /**
     * A System Property that, when set with a value of "true", will result in the <b>default</b> configuration file (stored as a {@link #FILENAME resource file}) being written to disk i.e self-initialised (an existing file will not be overwritten). Once on disk, the configuration file can be modified as required.
     */
    public final static String CONFIGURATION_INITIALISATION_ENABLED = "mocktcpserver.configuration.initialisation.enabled";
    /**
     * The name of the resource file that is the default configuration file. If the file cannot be located and the {@link #CONFIGURATION_INITIALISATION_ENABLED System Property} is true, this file can be written to the {@link #DEFAULT_FILENAME default location} on file system to initialise the configuration.
     */
    public final static String FILENAME = "mocktcpserver.xml";
    /**
     * The location of the default {@link #FILENAME resource file}. This folder name is relative to the runtime working folder.
     */
    public final static String FILENAME_PATH = "configuration";
    /**
     * The default location, and name, of the configuration file on the file system.
     * <p>
     * The default configuration file is held as a {@link #FILENAME resource file}.
     */
    public final static String DEFAULT_FILENAME = String.format("%s%s%s", FILENAME_PATH, File.separatorChar, FILENAME);
    private final static String PORT_PROPERTY_NAME = "server.port";

    private URL propertiesFile;
    private FileBasedConfigurationBuilder<XMLConfiguration> configurationBuilder;

    /**
     * The port the MockTCPServer will listen to, as read from the {@link #getFileName() configuration file} (from the property name defined as {@link #PORT_PROPERTY_NAME}).
     *
     * @return the port the MockTCPServer will listen to.
     * @throws ConfigurationException see source documentation.
     * @see #isConfigurationInitialisationEnabled()
     */
    public int getPort() throws ConfigurationException {
        return Integer.parseInt(this.getConfigurationBuilder().getConfiguration().getProperty(PORT_PROPERTY_NAME).toString());
    }

    /**
     * Set the port the MockTCPServer will listen to, the port number will be written to the {@link #getFileName() configuration file} (to the property name defined as {@link #PORT_PROPERTY_NAME}).
     *
     * @param port the port the MockTCPServer will listen to.
     * @throws ConfigurationException see source documentation.
     * @see #isConfigurationInitialisationEnabled()
     */
    public void setPort(int port) throws ConfigurationException {
        this.getConfigurationBuilder().getConfiguration().setProperty(PORT_PROPERTY_NAME, port);
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
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected void clearPropertyDirect(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean containsKeyInternal(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected Iterator<String> getKeysInternal() {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected Object getPropertyInternal(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    /**
     * <b>NOT IMPLEMENTED</b>
     * <p>
     * {@inheritDoc}
     */
    @Override
    protected boolean isEmptyInternal() {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
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
                // Reinitialise in order to pick up the newly created file;
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
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(this.getDefaultFile());
                try {
                    this.getConfigurationBuilder().getConfiguration().write(fileWriter);
                } catch (final ConfigurationException e) {
                    this.getLog().error(e.getMessage(), e);
                }
            } catch (final IOException e) {
                this.getLog().error(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(fileWriter);
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