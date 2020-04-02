package com.gsww.ga.common.config;

import com.gsww.ga.job.JobFactory;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Properties;

/**
 * <b><code>QuartzConfig</code></b>
 * <p/>
 * QuartzConfig
 * <p/>
 * <b>Creation Time:</b> 2017/9/5 15:36.
 *
 * @author Elvis
 * @since data-importer 0.0.1
 */
@Configuration
public class QuartzConfig {

    /**
     * The Job factory.
     */
    private final JobFactory jobFactory;

    /**
     * Instantiates a new Quartz config.
     *
     * @param jobFactory the job factory
     */
    @Autowired
    public QuartzConfig(JobFactory jobFactory) {
        this.jobFactory = jobFactory;
    }

    /**
     * Quartz properties properties.
     *
     * @return the properties
     */
    @Bean
    public Properties quartzProperties() {
        Properties quartzProperties = new Properties();

        quartzProperties.setProperty("org.quartz.scheduler.instanceName", "DefaultQuartzScheduler");
        quartzProperties.setProperty("org.quartz.scheduler.rmi.export", "false");
        quartzProperties.setProperty("org.quartz.scheduler.rmi.proxy", "false");
        quartzProperties.setProperty("org.quartz.scheduler.wrapJobExecutionInUserTransaction", "false");

        quartzProperties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        quartzProperties.setProperty("org.quartz.threadPool.threadCount", StartupConfig.getInstance().getThreadCount());
        quartzProperties.setProperty("org.quartz.threadPool.threadPriority", "5");
        quartzProperties.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");

        quartzProperties.setProperty("org.quartz.jobStore.misfireThreshold", "864000000");
        quartzProperties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");

        return quartzProperties;
    }

    /**
     * Scheduler factory bean scheduler factory bean.
     *
     * @return the scheduler factory bean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setJobFactory(jobFactory);
        schedulerFactoryBean.setQuartzProperties(quartzProperties());
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(false);
        return schedulerFactoryBean;
    }

    /**
     * Scheduler scheduler.
     *
     * @return the scheduler
     */
    @Bean
    public Scheduler scheduler() {
        return schedulerFactoryBean().getScheduler();
    }

}
