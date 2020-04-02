package com.gsww.ga.job;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.config.SubConfigManager;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <b><code>Scheduler</code></b>
 * <p/>
 * Scheduler
 * <p/>
 * <b>Creation Time:</b> 2017/9/4 15:59.
 *
 * @author Elvis
 * @since data-importer 0.0.1
 */
@Component
public class QuartzScheduler {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(QuartzScheduler.class);

    /**
     * The Scheduler.
     */
    private final Scheduler scheduler;

    /**
     * The Jdbc template.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * Instantiates a new Quartz scheduler.
     *
     * @param scheduler the scheduler
     */
    @Autowired
    public QuartzScheduler(Scheduler scheduler, JdbcTemplate jdbcTemplate) {
        this.scheduler = scheduler;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Start.
     *
     * @throws IOException          the io exception
     * @throws InterruptedException the interrupted exception
     * @throws SchedulerException   the scheduler exception
     */
    public void start() throws IOException, InterruptedException, SchedulerException {
        LOG.info("Quartz Scheduler Start!");
        List<File> configFiles = SubConfigManager.getSubConfigFiles();
        for (File file : configFiles) {
            try {
                String jobId = UUID.randomUUID().toString().replaceAll("-", "") + "[" + file.getName() + "]";
                Map<String, Object> config = SubConfigManager.getConfigFromFile(file);
                long updateInterval = (long) SubConfigManager.getConfigFromExporter(config, SubConfigManager.SOURCE_TRIGGER_TIMER_UPDATE_INTERVAL);
                String cronExpression="";
                switch ((int) updateInterval){
                    case 15: {
                        if(CronExpression.isValidExpression(StartupConfig.getInstance().getCronExpresion(15))){
                            cronExpression=StartupConfig.getInstance().getCronExpresion(15);
                        }else {
                            cronExpression = "0 10,25,40,55 * * * ?";
                        }
                        break;
                    }
                    case 60: {
                        if(CronExpression.isValidExpression(StartupConfig.getInstance().getCronExpresion(60))){
                            cronExpression=StartupConfig.getInstance().getCronExpresion(60);
                        }else {
                            cronExpression = "0 40 * * * ?";
                        }
                        break;
                    }
                    case 1440:{
                        if(CronExpression.isValidExpression(StartupConfig.getInstance().getCronExpresion(1440))){
                            cronExpression=StartupConfig.getInstance().getCronExpresion(1440);
                        }else {
                            cronExpression = "0 15 1 * * ?";
                        }
                        break;
                    }
                    case 43200:{
                        if(CronExpression.isValidExpression(StartupConfig.getInstance().getCronExpresion(43200))){
                            cronExpression=StartupConfig.getInstance().getCronExpresion(43200);
                        }else {
                            cronExpression = "0 15 1 * * ?";
                        }
                        break;
                    }
                }
                LOG.info("trigger"+cronExpression);
                JobDataMap jobDataMap = new JobDataMap();
                jobDataMap.put("id", jobId);
                jobDataMap.put("config", config);
                jobDataMap.put("jdbcTemplate", jdbcTemplate);

                JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class)
                        .usingJobData(jobDataMap)
                        .withIdentity(jobId)
                        .build();
                Trigger triggerTest = TriggerBuilder.newTrigger()
                        .forJob(jobDetail)
                        .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever((int) updateInterval)
                                .withMisfireHandlingInstructionNextWithRemainingCount())
                        .withIdentity(jobId + "_trigger")
                        .build();
                Trigger trigger = TriggerBuilder.newTrigger()
                        .forJob(jobDetail)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                                .withMisfireHandlingInstructionDoNothing())
                        .withIdentity(jobId + "_trigger")
                        .build();
                if(StartupConfig.getInstance().isDebug()){
                    LOG.warn("----------debug-----------");
                    scheduler.scheduleJob(jobDetail, triggerTest);
                }else {
                    scheduler.scheduleJob(jobDetail, trigger);
                }
                Thread.sleep(1000);
            } catch (IOException e) {
                String error = "Cannot get config from file, file name :" + file.getName();
                LOG.error(error, e);
            }
        }
    }

    /**
     * Stop.
     */
    public void stop() throws SchedulerException {
        scheduler.shutdown();
    }

}
