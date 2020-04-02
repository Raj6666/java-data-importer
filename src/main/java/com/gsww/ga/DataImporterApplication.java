package com.gsww.ga;

import com.gsww.ga.common.config.StartupConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DataImporterApplication {

    /**
     * The constant LOG.
     */
    private final static Logger LOG = LoggerFactory.getLogger(DataImporterApplication.class);

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        boolean hasConfigLocation = false;
        for (String arg : args) {
            String[] paramKeyValue = arg.split("=");
            if (paramKeyValue[0].equals("--spring.config.location")) {
                StartupConfig.generateInstance(paramKeyValue[1]);
                hasConfigLocation = true;
                break;
            }
        }
        if (!hasConfigLocation) {
            LOG.error("The parameter spring.config.location is null, failed to startup!");
            return;
        }
        SpringApplication.run(DataImporterApplication.class, args);
    }

}