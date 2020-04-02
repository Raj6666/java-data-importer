package com.gsww.ga.job;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * <b><code>ScheduleListener</code></b>
 * <p/>
 * ScheduleListener
 * <p/>
 * <b>Creation Time:</b> 2017/9/4 15:59.
 *
 * @author Elvis
 * @since data-importer 0.0.1
 */
@Component
public class ScheduleListener implements ApplicationListener<ContextRefreshedEvent> {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleListener.class);

    /**
     * The Quartz scheduler.
     */
    private final QuartzScheduler quartzScheduler;

    /**
     * Instantiates a new Schedule listener.
     *
     * @param quartzScheduler the quartz scheduler
     */
    @Autowired
    public ScheduleListener(QuartzScheduler quartzScheduler) {
        this.quartzScheduler = quartzScheduler;
    }

    /**
     * On application event.
     *
     * @param contextRefreshedEvent the context refreshed event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            quartzScheduler.start();
        } catch (IOException e) {
            String error = "Cannot not get sub configs from the main config!";
            LOG.error(error, e);
        } catch (SchedulerException e) {
            String error = "Exception when set JobDetail and Trigger to Scheduler!";
            LOG.error(error, e);
        } catch (InterruptedException e) {
            String error = "Exception when doing thread sleeping!";
            LOG.error(error, e);
        }
    }

}
