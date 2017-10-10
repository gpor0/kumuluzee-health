/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
*/
package com.kumuluz.ee.health;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.logs.HealthCheckLogger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Initializes Health module.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
public class HealthInitiator {

    private static final Logger LOG = Logger.getLogger(HealthInitiator.class.getName());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void initialize(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOG.info("Initializing Health extension");

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        // initialize servlet mapping
        String servletMapping = configurationUtil.get("kumuluzee.health.servlet.mapping").orElse("/health");

        LOG.info("Registering health servlet on " + servletMapping);

        // register servlet
        ServletRegistration.Dynamic dynamicRegistration = ((ServletContext) init).addServlet("health", new
                HealthServlet());

        dynamicRegistration.addMapping(servletMapping);

        // initialize health logger
        if (configurationUtil.getBoolean("kumuluzee.metrics.logs.enabled").orElse(true)) {
            int period = configurationUtil.getInteger("kumuluzee.health.logs.period-s").orElse(60);
            String level = configurationUtil.get("kumuluzee.health.logs.level").orElse("FINE");

            LOG.log(Level.INFO, "Starting health logger to log health check results every {0} s", period);

            HealthCheckLogger logger = new HealthCheckLogger(level);
            scheduler.scheduleWithFixedDelay(logger, period, period, TimeUnit.SECONDS);
        }
    }
}