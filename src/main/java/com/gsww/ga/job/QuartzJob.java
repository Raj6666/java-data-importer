package com.gsww.ga.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

/**
 * <b><code>QuartzJob</code></b>
 * <p/>
 * QuartzJob
 * <p/>
 * <b>Creation Time:</b> 2017/9/4 16:14.
 *
 * @author Elvis
 * @since data-importer 0.0.1
 */
@DisallowConcurrentExecution
public class QuartzJob implements Job {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);

    /**
     * Execute.
     *
     * @param jobExecutionContext the job execution context
     */
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        String id = (String) jobDataMap.get("id");
        JdbcTemplate jdbcTemplate = (JdbcTemplate) jobDataMap.get("jdbcTemplate");
        Map<String, Object> config = (Map<String, Object>) jobDataMap.get("config");

        LOG.info("Job with id[" + id + "] Start!");
        ImportTask task = new ImportTask(config, jdbcTemplate);
        task.run();
        LOG.info("Job with id[" + id + "] End!");
    }

}
