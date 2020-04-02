package com.gsww.ga.job;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.config.SubConfigManager;
import com.gsww.ga.exporter.DBExporter;
import com.gsww.ga.importer.ElasticsearchImporter;
import com.gsww.ga.importer.FileSysImporter;
import com.gsww.ga.importer.InnerDBSystemImporter;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b><code>ImportTask</code></b>
 * <p/>
 * ImportTask
 * <p/>
 * <b>Creation Time:</b> 2017/9/4 15:25.
 *
 * @author Elvis
 * @since date -import 0.0.1
 */
public class ImportTask implements Runnable {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImportTask.class);

    /**
     * The Config.
     */
    private Map<String, Object> config;

    /**
     * The Jdbc template.
     */
    private JdbcTemplate jdbcTemplate;

    /**
     * Instantiates a new Import task.
     *
     * @param config       the config
     * @param jdbcTemplate the jdbc template
     */
    public ImportTask(final Map<String, Object> config, final JdbcTemplate jdbcTemplate) {
        this.config = config;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        try {
            /* Get file name from the config */
            String fileName = SubConfigManager.getFileName(this.config);

            /* Get export config to construct a DBExporter */
            String driverClassName = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_DRIVER_CLASS_NAME);
            String url = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_URL);
            String username = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_USER_NAME);
            String password = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_PASSWORD);
            String queryCommand = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_QUERY_COMMAND);
            String firstQueryCommand = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_FIRST_QUERY_COMMAND);
            long updateInterval = (long) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TRIGGER_TIMER_UPDATE_INTERVAL);
            String msgID = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TRIGGER_MESSAGER_MSGID);
            String trimMode = (String) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TIME_TRIM_MODE);
            long trimMinute = (long) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TIME_TRIM_MINUTE);
            long delay = (long) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TIME_DELAY);
            long timeslot = (long) SubConfigManager.getConfigFromExporter(this.config, SubConfigManager.SOURCE_TIME_TIMESLOT);

            /* Calculate the sleeping time for the delay of program*/
            Map<String, Long> queryData = calWaitingTime(updateInterval, delay);

            DBExporter exporter = new DBExporter(fileName, driverClassName, url, username, password, queryCommand, firstQueryCommand, updateInterval, msgID, trimMode, trimMinute, delay, timeslot, queryData);
            /* Get Elasticsearch import config to construct a ElasticsearchImporter if exist */
            if (SubConfigManager.hasInputPinConfig(this.config, SubConfigManager.DESTINATION_TYPE_ELASTICSEARCH)
                    && !SubConfigManager.isIgnoreDatasource(this.config, SubConfigManager.DESTINATION_TYPE_ELASTICSEARCH)) {
                String indexPrefix = (String) SubConfigManager.getElasticsearchConfig(this.config, SubConfigManager.DESTINATION_ELASTICSEARCH_INDEX_ID_PREFIX);
                String indexIdColumn = (String) SubConfigManager.getElasticsearchConfig(this.config, SubConfigManager.DESTINATION_ELASTICSEARCH_INDEX_ID_ID_COLUMN);
                String modelName = (String) SubConfigManager.getElasticsearchConfig(this.config, SubConfigManager.DESTINATION_ELASTICSEARCH_MODEL_NAME);
                String groupId = (String) SubConfigManager.getElasticsearchConfig(this.config, SubConfigManager.DESTINATION_ELASTICSEARCH_GROUP_ID);
                Map<String, String> columnsMapper = (HashMap<String, String>) SubConfigManager.getElasticsearchConfig(this.config, SubConfigManager.DESTINATION_ELASTICSEARCH_COLUMNS_MAPPER);
                ElasticsearchImporter elasticInputPin = new ElasticsearchImporter(indexPrefix, indexIdColumn, groupId, modelName, columnsMapper);
                //LOG.info("Elasticsearch",elasticInputPin);
                exporter.bindPin(elasticInputPin);
            }
            /* Get mysql import config to construct a InnerDBImporter if exist */
            if (SubConfigManager.hasInputPinConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL)
                    && !SubConfigManager.isIgnoreDatasource(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL)) {
                String dbSystemDriverClassName = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DRIVER_CLASS_NAME);
                String dbSystemUrl = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_URL);
                String dbSystemUsername = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_USER_NAME);
                String dbSystemPassword = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_PASSWORD);
                List<InnerDBSystemImporter.Action> actions = SubConfigManager.getInnerDBSystemActions(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL);
                InnerDBSystemImporter innerDBSystemInputPin = new InnerDBSystemImporter(actions, dbSystemDriverClassName, dbSystemUrl, dbSystemUsername, dbSystemPassword);
                //LOG.info(innerDBInputPin.toString());
                exporter.bindPin(innerDBSystemInputPin);
                String dbSystemDatasource = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_MYSQL, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DATASOURCE);
                LOG.info(fileName + ",绑定mysql数据库:" + dbSystemDatasource);
            }
            /* Get sqlserver import config to construct a InnerDBImporter if exist */
            if (SubConfigManager.hasInputPinConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER)
                    && !SubConfigManager.isIgnoreDatasource(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER)) {
                String dbSystemDriverClassName = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DRIVER_CLASS_NAME);
                String dbSystemUrl = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_URL);
                String dbSystemUsername = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_USER_NAME);
                String dbSystemPassword = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_PASSWORD, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER);
                List<InnerDBSystemImporter.Action> actions = SubConfigManager.getInnerDBSystemActions(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER);
                InnerDBSystemImporter innerDBSystemInputPin = new InnerDBSystemImporter(actions, dbSystemDriverClassName, dbSystemUrl, dbSystemUsername, dbSystemPassword);
                //LOG.info(innerDBInputPin.toString());
                exporter.bindPin(innerDBSystemInputPin);
                String dbSystemDatasource = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_SQLSERVER, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DATASOURCE);
                LOG.info(fileName + ",绑定sqlserver数据库" + dbSystemDatasource);
            }
            /* Get postgres import config to construct a InnerDBImporter if exist */
            if (SubConfigManager.hasInputPinConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES)
                    && !SubConfigManager.isIgnoreDatasource(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES)) {
                String dbSystemDriverClassName = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DRIVER_CLASS_NAME);
                String dbSystemUrl = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_URL);
                String dbSystemUsername = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_USER_NAME);
                String dbSystemPassword = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_PASSWORD);
                List<InnerDBSystemImporter.Action> actions = SubConfigManager.getInnerDBSystemActions(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES);
                InnerDBSystemImporter innerDBSystemInputPin = new InnerDBSystemImporter(actions, dbSystemDriverClassName, dbSystemUrl, dbSystemUsername, dbSystemPassword);
                //LOG.info(innerDBInputPin.toString());
                exporter.bindPin(innerDBSystemInputPin);
                String dbSystemDatasource = (String) SubConfigManager.getDBSystemConfig(this.config, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_POSTGRES, SubConfigManager.DESTINATION_TYPE_DBSYSTEM_DATASOURCE);
                LOG.info(fileName + ",绑定postgres数据库" + dbSystemDatasource);
            }
            /* Get FileSystem import config to construct a FileSysImporter if exist */
            if (SubConfigManager.hasInputPinConfig(this.config, SubConfigManager.DESTINATION_TYPE_FILESYSTEM)
                    && !SubConfigManager.isIgnoreDatasource(this.config, SubConfigManager.DESTINATION_TYPE_FILESYSTEM)) {
                List<FileSysImporter.Action> actions = SubConfigManager.getFileSysActions(this.config);
                FileSysImporter FileSysInputPin = new FileSysImporter(actions, queryData);
                //LOG.info("FileSys",FileSysInputPin);
                exporter.bindPin(FileSysInputPin);
                LOG.info(fileName + ",绑定文件系统" + url);
            }
            //exporter.executeUpdate();
            LOG.info(fileName + ",任务执行前" + url);
            exporter.executeInsert();
            LOG.info(fileName + ",任务成功执行" + url);
        } catch (JSONException | SQLException | InterruptedException | IOException e) {
            LOG.error("Fail to do ImportTask of file " + SubConfigManager.getFileName(this.config), e);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("run exception", e);
            LOG.error(e.getCause() + "::::::" + e.getMessage());
        }
    }

    /**
     * 计算到查询的时间点为止，程序需要等待的时间
     */
    private Map<String, Long> calWaitingTime(long updateInterval, long delay) {
        long timestamp;
        Map<String, Long> queryData = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        LOG.warn("粒度：" + updateInterval);
        switch ((int) updateInterval) {
            case 15: {
                timestamp = System.currentTimeMillis() / 900000;
                timestamp = timestamp * 900000;
                timestamp = timestamp - (delay * 60000);// 空出5分钟用于程序运行
                //现场实际环境运行
                LOG.info("开始时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp - (updateInterval * 2 * 60000))));//开始时间
                LOG.info("结束时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp)));//结束时间
                queryData.put("startTime", timestamp - (updateInterval * 2 * 60000));
                queryData.put("endTime", timestamp);

                break;
            }
            case 60: {
                timestamp = ((long) ((System.currentTimeMillis() - 3600000) / 3600000)) * 3600000;// 空出5分钟用于程序运行
                //现场实际环境运行
                LOG.info("开始时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp)));//开始时间
                LOG.info("结束时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp + 3600000)));//结束时间
                queryData.put("startTime", timestamp);
                queryData.put("endTime", timestamp + 3600000);

                break;
            }
            case 1440: {
                timestamp = ((long) ((System.currentTimeMillis() - 86400000) / 86400000)) * 86400000 - 28800000;// 空出5分钟用于程序运行
                LOG.info("开始时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp)));//开始时间
                LOG.info("结束时间：(" + updateInterval + ")" + dateFormat.format(new Date(timestamp + 86400000)));//结束时间
                queryData.put("startTime", timestamp);
                queryData.put("endTime", timestamp + 86400000);
                break;
            }
            case 43200: {
//                Date curDate = new Date(System.currentTimeMillis());
                Date curDate = new Date();
                Date startDate = new Date(curDate.getYear(), curDate.getMonth() - 1, 1);
                Date endDate = new Date(curDate.getYear(), curDate.getMonth(), 1);
                LOG.info("开始时间：(" + updateInterval + ")" + dateFormat.format(startDate));
                LOG.info("结束时间：(" + updateInterval + ")" + dateFormat.format(endDate));
                queryData.put("startTime", startDate.getTime());
                queryData.put("endTime", endDate.getTime());
                break;
            }
        }
        //debug模式 固定时间设置
        LOG.warn(StartupConfig.getInstance().getQueryTime().toString());
        if (StartupConfig.getInstance().isDebug() && StartupConfig.getInstance().getQueryTime().get("isValid") == 1) {
            return StartupConfig.getInstance().getQueryTime();
        }
        return queryData;
    }

    /**
     * 为了测试用，固定查询日期为2018/3/7
     */
    private long fixDayforTest(long timestamp) {
        Date date = new Date(timestamp);
        date.setMonth(2);
        date.setDate(7);

        return date.getTime();
    }
}
