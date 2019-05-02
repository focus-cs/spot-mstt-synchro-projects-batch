package com.schneider.mstt.synchro.projects;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComponentScan(basePackages = "com.schneider.mstt.synchro")
@Configuration
public class Runner {

    public static final String APP_INFO = "SPOT-MSTT-SYNCHRO-PROJECTS-SERVER v1.6 - 2019/05/01";

    protected static final Logger LOG = Logger.getLogger(Runner.class);

    private static final String PROPERTY_FILE = "../conf/log4j.properties";

    @Autowired
    private ProjectUpdate projectUpdate;

    /**
     * Void main to run the application from command line.
     *
     * @param args
     */
    public static void main(final String[] args) {
        // configure log4j logger
        PropertyConfigurator.configure(PROPERTY_FILE);

        ApplicationContext context = new AnnotationConfigApplicationContext(Runner.class);
        Runner api = context.getBean(Runner.class);

        api.start();

    }

    private void start() {
        // Display application info.
        LOG.info(APP_INFO);
        projectUpdate.process();
        
    }

}
