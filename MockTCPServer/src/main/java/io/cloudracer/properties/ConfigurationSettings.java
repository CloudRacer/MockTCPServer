package io.cloudracer.properties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.apache.commons.configuration2.AbstractConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileLocator;
import org.apache.commons.configuration2.io.FileLocatorUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigurationSettings extends AbstractConfiguration {

    private Logger logger;

    /**
     * The name of the resource file that is the default configuration file. This file is written to the file system to initialise the configuration, if the file cannot be located.
     */
    public final static String FILENAME = "mocktcpserver.xml";
    /**
     * The location of the default {@link ConfigurationSettings#FILENAME resource file}. This folder name is relative to the runtime working folder.
     */
    public final static String FILENAME_PATH = "configuration";
    /**
     * The default location, and name, of the configuration file on the file system.
     * <p>
     * The default configuration file is held as a {@link ConfigurationSettings#FILENAME resource file}.
     */
    public final static String DEFAULT_FILENAME = String.format("%s%s%s", FILENAME_PATH, File.separatorChar, FILENAME);
    private final static String PORT_PROPERTY_NAME = "server.port";

    private URL propertiesFile;
    private FileBasedConfigurationBuilder<XMLConfiguration> configurationBuilder;

    public int getPort() throws ConfigurationException {
        return this.getConfigurationBuilder().getConfiguration().getInt(PORT_PROPERTY_NAME);
    }

    public void setPort(int port) throws ConfigurationException {
        this.getConfigurationBuilder().getConfiguration().setProperty(PORT_PROPERTY_NAME, port);
    }

    /**
     * The file {@link URL} can be absolute or relative to the working folder. The configuration file is located using a {@link FileLocatorUtils#DEFAULT_LOCATION_STRATEGY strategy} that uses a number of techniques to determine the file location.
     *
     * @return the configuration file {@link URL}.
     */
    public URL getFileName() {
        if (this.propertiesFile == null) {
            final FileLocator fileLocator = FileLocatorUtils.fileLocator()
                    .basePath(this.getDefaultFile().getParent())
                    .fileName(this.getDefaultFile().getName())
                    .create();

            this.propertiesFile = FileLocatorUtils.locate(fileLocator);
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

    @Override
    protected void clearPropertyDirect(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    @Override
    protected boolean containsKeyInternal(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    @Override
    protected Iterator<String> getKeysInternal() {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

    @Override
    protected Object getPropertyInternal(String arg0) {
        // TODO Auto-generated method stub
        throw new NotImplementedException("TODO", new UnsupportedOperationException());
    }

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
                            .setFileName(this.getFileName().getFile())
                            .setValidating(false));
            this.configurationBuilder.setAutoSave(true);

            // If the file has not yet been written to the disk, as this is the first execution or the file has been deleted (deletion is a legitimate way to "reset to factory settings"), write it now.
            if (!this.getDefaultFile().exists()) {
                // Reinitialise in order to pick up the newly created file;
                this.propertiesFile = null;
                this.save();
                this.configurationBuilder = null;
                this.getConfigurationBuilder();
            }
        }

        return this.configurationBuilder;
    }

    private void save() {
        try {
            // Create the destination folder, if it does not already exist.
            FileUtils.forceMkdir(this.getDefaultFile());
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