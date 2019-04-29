package com.schneider.mstt.synchro.projects;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class Runner {

    public static final String APP_INFO = "SPOT-MSTT-SYNCHRO-PROJECTS-SERVER v1.5 - 2016/05/02";

    protected static final Logger LOG = Logger.getLogger(Runner.class);

    private static final String PROPERTY_FILE = "../conf/log4j.properties";

    /**
     * Void main to run the application from command line.
     *
     * @param args
     */
    public static void main(final String[] args) {
        // configure log4j logger
        PropertyConfigurator.configure(PROPERTY_FILE);

        // Display application info.
        LOG.info(APP_INFO);

        if (args.length == 1) {
            final ProjectUpdate projectUpdate = new ProjectUpdate(args[0]);
            projectUpdate.process();
        } else {
            LOG.error("Use : Main psconnect.properties");
            System.exit(Common.ERROR_EXIST_CODE);
        }
        
    }
    
}
