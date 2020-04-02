package com.gsww.ga.config;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.importer.FileSysImporter;
import com.gsww.ga.importer.InnerDBSystemImporter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * <b><code>SubConfigManager</code></b>
 * <p/>
 * SubConfigManager
 * <p/>
 * <b>Creation Time:</b> 2017/8/31 17:33.
 *
 * @author Elvis
 * @since data -importer 0.0.1
 */
public class SubConfigManager {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(SubConfigManager.class);

    /* The constants. */
    private static final String FILE_NAME = "fileName";

    public static final String SOURCE_DRIVER_CLASS_NAME = "driverClassName";

    public static final String SOURCE_URL = "url";

    public static final String SOURCE_USER_NAME = "username";

    public static final String SOURCE_PASSWORD = "password";

    public static final String SOURCE_QUERY_COMMAND = "queryCommand";

    public static final String SOURCE_FIRST_QUERY_COMMAND = "firstQueryCommand";

    public static final String SOURCE_TRIGGER_TIMER_UPDATE_INTERVAL = "updateInterval";

    public static final String SOURCE_TRIGGER_MESSAGER_MSGID = "msgID";

    public static final String SOURCE_TIME_TRIM_MODE = "trimMode";

    public static final String SOURCE_TIME_TRIM_MINUTE = "trimMinute";

    public static final String SOURCE_TIME_DELAY = "delay";

    public static final String SOURCE_TIME_TIMESLOT = "timeslot";

    public static final String DESTINATION_TYPE_ELASTICSEARCH = "Elasticsearch";

    public static final String DESTINATION_TYPE_DBSYSTEM_MYSQL= "mysql";

    public static final String DESTINATION_TYPE_DBSYSTEM_SQLSERVER = "sqlserver";

    public static final String DESTINATION_TYPE_DBSYSTEM_POSTGRES = "postgres";

    public static final String DESTINATION_TYPE_DBSYSTEM_URL = "url";

    public static final String DESTINATION_TYPE_DBSYSTEM_DRIVER_CLASS_NAME = "driverClassName";

    public static final String DESTINATION_TYPE_DBSYSTEM_USER_NAME = "username";

    public static final String DESTINATION_TYPE_DBSYSTEM_PASSWORD = "password";

    public static final String DESTINATION_TYPE_DBSYSTEM_DATASOURCE = "dbDatasource";

    public static final String DESTINATION_TYPE_FILESYSTEM = "FileSystem";

    public static final String DESTINATION_ELASTICSEARCH_INDEX_ID_PREFIX = "indexPrefix";

    public static final String DESTINATION_ELASTICSEARCH_INDEX_ID_ID_COLUMN = "indexIdColumn";

    public static final String DESTINATION_ELASTICSEARCH_GROUP_ID = "groupId";

    public static final String DESTINATION_ELASTICSEARCH_MODEL_NAME = "modelName";

    public static final String DESTINATION_ELASTICSEARCH_COLUMNS_MAPPER = "columnsMapper";

    /**
     * Gets sub config files.
     *
     * @return the sub config files
     * @throws IOException the io exception
     */
    public static List<File> getSubConfigFiles() throws IOException {
        File[] files = new File(MainConfigManager.getConfigDirectory())
                .listFiles((dir, name) -> name.endsWith(".ei"));
        return Arrays.stream(files != null ? files : new File[0])
                .sorted(Comparator.comparing(File::getName)).collect(toList());
    }

    /**
     * Gets config from file.
     *
     * @param file the file
     * @return the config from file
     * @throws IOException the io exception
     */
    public static Map<String, Object> getConfigFromFile(File file)
            throws IOException {
        String fileName = file.getName();
        List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        Map<String, Object> config = new Gson().fromJson(
                StringUtils.join(lines, ""),
                new TypeToken<LinkedTreeMap<String, Object>>() {
                }.getType());
        config.put(FILE_NAME, fileName);
        return config;
    }

    /**
     * Gets file name.
     *
     * @param config the config
     * @return the file name
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static String getFileName(Map<String, Object> config)
            throws IllegalArgumentException {
        String fileName = (String) config.get(FILE_NAME);
        if (StringUtils.isBlank(fileName)) {
            String error = "Cannot get file name in config!";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        return fileName;
    }

    /**
     * Gets config from exporter.
     *
     * @param config   the config
     * @param property the property
     * @return the config from exporter
     * @throws IllegalArgumentException the illegal argument exception
     */
    public static Object getConfigFromExporter(Map<String, Object> config,
            String property) throws IllegalArgumentException {
        Map<String, Object> sourceConfig = (LinkedTreeMap<String, Object>) config
                .get("source");
        Map<String, String> datasource = new HashMap<>();
        if (sourceConfig.isEmpty()) {
            String error = "The source not exist or empty in config file.";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        if (sourceConfig.get("datasource") != null) {
            datasource = StartupConfig.getInstance().getDatasourceMaps()
                    .get(sourceConfig.get("datasource"));
        }

        switch (property) {
        case SOURCE_DRIVER_CLASS_NAME:
            String driverClassName = "";
            if (datasource.get("driverClassName") != null) {
                driverClassName = datasource.get("driverClassName");
            }
            return driverClassName;
        case SOURCE_URL:
            String url = "";
            if (datasource.get("url") != null) {
                url = datasource.get("url");
            }
            return url;
        case SOURCE_USER_NAME:
            String username = "";
            if (datasource.get("username") != null) {
                username = datasource.get("username");
            }
            return username;
        case SOURCE_PASSWORD:
            String password = "";
            if (datasource.get("password") != null) {
                password = datasource.get("password");
            }
            return password;
        case SOURCE_QUERY_COMMAND:
            String queryCommand = "";
            if (sourceConfig.get("queryCommand") != null) {
                queryCommand = (String) sourceConfig.get("queryCommand");
            }
            return queryCommand;
        case SOURCE_FIRST_QUERY_COMMAND:
            String firstQueryCommand = "";
            if (sourceConfig.get("queryCommand") != null) {
                firstQueryCommand = (String) sourceConfig
                        .get("firstQueryCommand");
            }
            return firstQueryCommand;
        case SOURCE_TRIGGER_TIMER_UPDATE_INTERVAL:
            long updateInterval = 0;
            if (sourceConfig.get("trigger") != null) {
                Map<String, Object> trigger = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("trigger");
                if (trigger.get("timer") != null) {
                    Map<String, String> timer = (LinkedTreeMap<String, String>) trigger
                            .get("timer");
                    if (timer.get("updateInterval") != null) {
                        updateInterval = Long
                                .parseLong(timer.get("updateInterval"));
                    }
                }
            }
            return updateInterval;
        case SOURCE_TRIGGER_MESSAGER_MSGID:
            String msgID = "";
            if (sourceConfig.get("trigger") != null) {
                Map<String, Object> trigger = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("trigger");
                if (trigger.get("messager") != null) {
                    Map<String, String> messager = (LinkedTreeMap<String, String>) trigger
                            .get("messager");
                    if (messager.get("msgID") != null) {
                        msgID = messager.get("msgID");
                    }
                }
            }
            return msgID;
        case SOURCE_TIME_TRIM_MODE:
            String trimMode = "";
            if (sourceConfig.get("time") != null) {
                Map<String, Object> time = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("time");
                if (time.get("trim") != null) {
                    Map<String, String> trim = (LinkedTreeMap<String, String>) time
                            .get("trim");
                    if (trim.get("mode") != null) {
                        trimMode = trim.get("mode");
                    }
                }
            }
            return trimMode;
        case SOURCE_TIME_TRIM_MINUTE:
            long trimMinute = 0;
            if (sourceConfig.get("time") != null) {
                Map<String, Object> time = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("time");
                if (time.get("trim") != null) {
                    Map<String, String> trim = (LinkedTreeMap<String, String>) time
                            .get("trim");
                    if (trim.get("minute") != null) {
                        trimMinute = Long.parseLong(trim.get("minute"));
                    }
                }
            }
            return trimMinute;
        case SOURCE_TIME_DELAY:
            long delay = 0;
            if (sourceConfig.get("time") != null) {
                Map<String, Object> time = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("time");
                if (time.get("delay") != null) {
                    delay = Long.parseLong((String) time.get("delay"));
                }
            }
            return delay;
        case SOURCE_TIME_TIMESLOT:
            long timeslot = 0;
            if (sourceConfig.get("time") != null) {
                Map<String, Object> time = (LinkedTreeMap<String, Object>) sourceConfig
                        .get("time");
                if (time.get("timeslot") != null) {
                    timeslot = Long.parseLong((String) time.get("timeslot"));
                }
            }
            return timeslot;
        default:
            String error = "Parameter not match in config: property[" + property
                    + "]";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Has output pin config boolean.输出
     *
     * @param config the config
     * @param type the type
     * @return the boolean
     */
    public static boolean hasInputPinConfig(Map<String, Object> config,
            String type) {
        boolean hasPin = false;
        List<LinkedTreeMap<String, Object>> destinationConfigs = (List<LinkedTreeMap<String, Object>>) config
                .get("dest");
        for (Map<String, Object> destinationConfig : destinationConfigs) {
            String thisType = (String) destinationConfig.get("type");
            if (type.equals(thisType)) {
                hasPin = true;
            }
        }
        return hasPin;
    }

    public static boolean isIgnoreDatasource(Map<String, Object> config,String type){
         return  StartupConfig.getInstance().getIgnoreDatasource().contains(type);
    }

    /**
     * Gets elasticsearch config.
     *
     * @param config the config
     * @param property the property
     * @return the elasticsearch config
     */
    public static Object getElasticsearchConfig(Map<String, Object> config,
            String property) throws IllegalArgumentException {
        List<LinkedTreeMap<String, Object>> destinationConfigs = (List<LinkedTreeMap<String, Object>>) config
                .get("dest");
        Map<String, Object> elasticsearchConfig = new LinkedTreeMap<>();
        for (Map<String, Object> destinationConfig : destinationConfigs) {
            String thisType = (String) destinationConfig.get("type");
            if (DESTINATION_TYPE_ELASTICSEARCH.equals(thisType)) {
                elasticsearchConfig = destinationConfig;
                break;
            }
        }
        if (elasticsearchConfig.isEmpty()) {
            String error = "The dest[Elasticsearch] not exist or empty in config file.";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        switch (property) {
        case DESTINATION_ELASTICSEARCH_INDEX_ID_PREFIX:
            String indexPrefix = "";
            if (elasticsearchConfig.get("indexId") != null) {
                Map<String, String> indexId = (LinkedTreeMap<String, String>) elasticsearchConfig
                        .get("indexId");
                if (indexId.get("prefix") != null) {
                    indexPrefix = indexId.get("prefix");
                }
            }
            return indexPrefix;
        case DESTINATION_ELASTICSEARCH_INDEX_ID_ID_COLUMN:
            String indexIdColumn = "";
            if (elasticsearchConfig.get("indexId") != null) {
                Map<String, String> indexId = (LinkedTreeMap<String, String>) elasticsearchConfig
                        .get("indexId");
                if (indexId.get("idColumn") != null) {
                    indexIdColumn = indexId.get("idColumn");
                }
            }
            return indexIdColumn;
        case DESTINATION_ELASTICSEARCH_MODEL_NAME:
            String modelName = "";
            if (elasticsearchConfig.get("modelName") != null) {
                modelName = (String) elasticsearchConfig.get("modelName");
            }
            return modelName;
        case DESTINATION_ELASTICSEARCH_GROUP_ID:
            String groupId = "";
            if (elasticsearchConfig.get("groupId") != null) {
                groupId = (String) elasticsearchConfig.get("groupId");
            }
            return groupId;
        case DESTINATION_ELASTICSEARCH_COLUMNS_MAPPER:
            Map<String, String> columnsMapper = new HashMap<>();
            if (elasticsearchConfig.get("columnsMapper") != null) {
                Map<String, String> columnsTreeMap = (LinkedTreeMap<String, String>) elasticsearchConfig
                        .get("columnsMapper");
                for (String key : columnsTreeMap.keySet()) {
                    columnsMapper.put(key, columnsTreeMap.get(key));
                }
            }
            return columnsMapper;
        default:
            String error = "Parameter not match in config: property[" + property
                    + "]";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
    }

    /**
     * Gets db sys config.
     *
     * @param config the config
     * @param property the property
     * @return the db sys config
     */
    public static Object getDBSystemConfig(Map<String, Object> config,
            String DBtype,String property) throws IllegalArgumentException {
        List<LinkedTreeMap<String, Object>> destinationConfigs = (List<LinkedTreeMap<String, Object>>) config.get("dest");
        Map<String, Object> dbSystemConfig = new LinkedTreeMap<>();
        Map<String, String> datasource = new HashMap<>();
        for (Map<String, Object> destinationConfig : destinationConfigs) {
            String thisType = (String) destinationConfig.get("type");
            if (DBtype.equals(thisType)) {
                dbSystemConfig = destinationConfig;
                break;
            }
        }
        if (dbSystemConfig.isEmpty()) {
            String error = "The dest[Postgres] not exist or empty in config file.";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        if (dbSystemConfig.get("datasource") != null) {
            datasource = StartupConfig.getInstance().getDatasourceMaps()
                    .get(dbSystemConfig.get("datasource"));
        }
        switch (property) {
        case DESTINATION_TYPE_DBSYSTEM_URL:
            String dbSystemUrl = "";
            if (datasource.get("url") != null) {
                dbSystemUrl = datasource.get("url");
            }
            return dbSystemUrl;
        case DESTINATION_TYPE_DBSYSTEM_DRIVER_CLASS_NAME:
            String dbSystemClassName = "";
            if (datasource.get("driverClassName") != null) {
                dbSystemClassName = datasource.get("driverClassName");
            }
            return dbSystemClassName;
        case DESTINATION_TYPE_DBSYSTEM_USER_NAME:
            String dbSystemUserName = "";
            if (datasource.get("username") != null) {
                dbSystemUserName = (String) datasource.get("username");
            }
            return dbSystemUserName;
        case DESTINATION_TYPE_DBSYSTEM_PASSWORD:
            String dbSystemPassWord = "";
            if (datasource.get("password") != null) {
                dbSystemPassWord = (String) datasource.get("password");
            }
            return dbSystemPassWord;
        case DESTINATION_TYPE_DBSYSTEM_DATASOURCE:
            String dbDatasource = "";
            if (dbSystemConfig.get("datasource") != null){
                dbDatasource = (String) dbSystemConfig.get("datasource");
            }
            return dbDatasource;
        default:
            String error = "Parameter not match in config: property[" + property
                    + "]";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
    }


    /**
     * Gets inner db system actions.
     *
     * @return the inner db actions
     */
    public static List<InnerDBSystemImporter.Action> getInnerDBSystemActions(
            Map<String, Object> config,String DBType) throws IllegalArgumentException {
        List<LinkedTreeMap<String, Object>> destinationConfigs = (List<LinkedTreeMap<String, Object>>) config
                .get("dest");
        Map<String, Object> dbSystemConfig = new LinkedTreeMap<>();
        for (Map<String, Object> destinationConfig : destinationConfigs) {
            String thisType = (String) destinationConfig.get("type");
            if (DBType.equals(thisType)) {
                dbSystemConfig = destinationConfig;
                break;
            }
        }
        if (dbSystemConfig.isEmpty()) {
            String error = "The dest[SysDB] not exist or empty in config file.";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        List<LinkedTreeMap<String, Object>> actionMapList = (List<LinkedTreeMap<String, Object>>) dbSystemConfig
                .get("actions");
        List<InnerDBSystemImporter.Action> actions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(actionMapList)) {
            for (Map<String, Object> actionMap : actionMapList) {
                InnerDBSystemImporter.Action action = new InnerDBSystemImporter.Action();
                action.setInitSqlCommand((String) actionMap.get("initSqlCommand"));
                action.setSqlCommand((String) actionMap.get("sqlCommand"));
                List<String> columns = new ArrayList<>();
                if (actionMap.get("columns") != null) {
                    columns = (List<String>) actionMap.get("columns");
                }
                action.setParamColumns(columns);
                actions.add(action);
            }
        }
        return actions;
    }

    /**
     * Gets FileSystem actions.
     *
     * @return the FileSystem actions
     */
    public static List<FileSysImporter.Action> getFileSysActions(
            Map<String, Object> config) throws IllegalArgumentException {
        List<LinkedTreeMap<String, Object>> destinationConfigs = (List<LinkedTreeMap<String, Object>>) config
                .get("dest");
        Map<String, Object> fileSysConfig = new LinkedTreeMap<>();
        for (Map<String, Object> destinationConfig : destinationConfigs) {
            String thisType = (String) destinationConfig.get("type");
            if (DESTINATION_TYPE_FILESYSTEM.equals(thisType)) {
                fileSysConfig = destinationConfig;
                break;
            }
        }
        if (fileSysConfig.isEmpty()) {
            String error = "The dest[fileSystem] not exist or empty in config file.";
            LOG.error(error);
            throw new IllegalArgumentException(error);
        }
        List<LinkedTreeMap<String, String>> actionMapList = (List<LinkedTreeMap<String, String>>) fileSysConfig
                .get("actions");
        List<FileSysImporter.Action> actions = new ArrayList<>();
        if (!CollectionUtils.isEmpty(actionMapList)) {
            for (Map<String, String> actionMap : actionMapList) {
                FileSysImporter.Action action = new FileSysImporter.Action();
                Map<String, String> datasource = new HashMap<>();
                if(actionMap.get("datasource")!=null){
                    datasource=StartupConfig.getInstance().getDatasourceMaps().get(actionMap.get("datasource"));
                    action.setDataSource(actionMap.get("datasource"));
                }
                if (datasource.get("path") != null) {
                    action.setPath(datasource.get("path"));
                    if (datasource.get("username") != null) {
                        action.setUsername(datasource.get("username"));
                    }
                    if (datasource.get("password") != null) {
                        action.setPassword(datasource.get("password"));
                    }
                    if (actionMap.get("filename") != null) {
                        action.setFilename(actionMap.get("filename"));
                    }
                    if (actionMap.get("finishedFilename") != null) {
                        action.setFinshedFilename(
                                actionMap.get("finishedFilename"));
                    }
                    if (actionMap.get("compressor") != null) {
                        action.setCompressor(actionMap.get("compressor"));
                    }
                    if (actionMap.get("delimiter") != null) {
                        action.setDelimiter(actionMap.get("delimiter"));
                    }
                }
                actions.add(action);
            }
        }
        return actions;
    }
}
