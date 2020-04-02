package com.gsww.ga.importer;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.db.connections.ConnectionFactory;
import com.gsww.ga.filter.InputPin;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yangpy on 2017/8/29.
 */
public class InnerDBSystemImporter implements InputPin {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(InnerDBSystemImporter.class);

    /**
     * The Actions.
     */
    private List<Action> actions;

    /**
     * The Connection.
     */
    private Connection conn;

    /**
     * The Sql count.
     */
    private long sqlCount = 0;

    private String driverClassName;

    private String url;

    private String username;

    private String password;

    private boolean isInit;

    /*更新家宽映射表特殊功能*/
    private Map<String,String> areaMap;

    private Map<String,String> townMap;

    private Map<String,Integer> onuMap;

    private Map<String,Integer> oltMap;

    private Connection postgresConnection;
    /*-------------------*/

//    /**
//     * Constructor
//     *
//     * @param actions the actions
//     * @param conn    the connection
//     * @throws SQLException the sql exception
//     */
//    public InnerDBSystemImporter(List<Action> actions, Connection conn) throws SQLException {
//        this.actions = actions;
//        this.conn = conn;
//        this.conn.setAutoCommit(false);
//        for (Action action : this.actions) {
//            action.preparedStatement = this.conn.prepareStatement(action.getSqlCommand());
//        }
//    }

    /**
     * Instantiates a new Inner db system importer.
     *
     * @param actions         the actions
     * @param driverClassName the driver class name
     * @param url             the url
     * @param username        the username
     * @param password        the password
     */
    public InnerDBSystemImporter(List<Action> actions, String driverClassName, String url, String username, String password) {
        this.actions = actions;
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        isInit = true;
    }

    /**
     * OnInput pin of InnerPostgres.
     *
     * @param resultSet the result set
     * @param metaData  the meta data
     * @throws SQLException the sql exception
     */
    @Override
    public void onInput(ResultSet resultSet, ResultSetMetaData metaData) throws SQLException {
        /*验证传输过来的信息*/
//        LOG.info("传输过来InnerPostgres的信息");
//        LOG.info( resultSet.getString(1)+" | "+resultSet.getString(2));
        if (null == conn){
            this.conn = ConnectionFactory.getConnection(driverClassName,url,username,password);
            this.conn.setAutoCommit(false);
            for (Action action : this.actions) {
                action.preparedStatement = this.conn.prepareStatement(action.getSqlCommand());
            }
        }

        for (Action action : actions) {

            /*执行初始化sql语句*/
            if(isInit){
                LOG.info(action.initSqlCommand);
                Statement initializeStatement = null;
                initializeStatement = this.conn.createStatement();
                LOG.info(String.valueOf(initializeStatement.executeUpdate(action.initSqlCommand))+ " 行受影响");
                isInit = false;

                /*更新家宽映射表特殊功能,初始化pg数据库连接,
                  然后把area和town映射表的数据放到对应的map中*/
                try {
                    Class.forName("org.postgresql.Driver");
                } catch (ClassNotFoundException e) {
                    LOG.error("load driver class:'{}' failed","org.postgresql.Driver",e);
                }
                /*现网*/
                postgresConnection = DriverManager.getConnection("jdbc:postgresql://172.16.1.21:5432/gemstack_data",
                        "gemstack",
                        "gemstack");
                /*本地*/
//                postgresConnection = DriverManager.getConnection("jdbc:postgresql://192.168.6.97:5432/gemstack_data",
//                        "postgres",
//                        "postgres");

                /*areaMap*/
                areaMap = new HashMap<String, String>();
                Statement areaMapGetStatement = null;
                ResultSet areaMapResultSet = null;
                areaMapGetStatement = postgresConnection.createStatement();
                areaMapResultSet = areaMapGetStatement.executeQuery("SELECT name,area from gemstack_areas");
                while (areaMapResultSet.next()) {
                    areaMap.put(areaMapResultSet.getString(1),areaMapResultSet.getString(2));
                }
                /*townMap*/
                townMap = new HashMap<String, String>();
                Statement townMapGetStatement = null;
                ResultSet townMapResultSet = null;
                townMapGetStatement = postgresConnection.createStatement();
                townMapResultSet = townMapGetStatement.executeQuery("SELECT name,community from gemstack_communities");
                while (townMapResultSet.next()) {
                    townMap.put(townMapResultSet.getString(1),townMapResultSet.getString(2));
                }
                /*onuMap*/
                onuMap = new HashMap<String, Integer>();
                Statement onuMapGetStatement = null;
                ResultSet onuMapResultSet = null;
                onuMapGetStatement = postgresConnection.createStatement();
                onuMapResultSet = onuMapGetStatement.executeQuery("select name,id from gemstack_devices where device_type = 5");
                while (onuMapResultSet.next()) {
                    onuMap.put(onuMapResultSet.getString(1),Integer.valueOf(onuMapResultSet.getString(2)));
                }
                /*oltMap*/
                oltMap = new HashMap<String, Integer>();
                Statement oltMapGetStatement = null;
                ResultSet oltMapResultSet = null;
                oltMapGetStatement = postgresConnection.createStatement();
                oltMapResultSet = oltMapGetStatement.executeQuery("select name,id from gemstack_devices where device_type = 4");
                while (oltMapResultSet.next()) {
                    oltMap.put(oltMapResultSet.getString(1),Integer.valueOf(oltMapResultSet.getString(2)));
                }
                /*完成映射Map后关闭所有链接*/
                areaMapGetStatement.close();
                areaMapResultSet.close();
                townMapGetStatement.close();
                townMapResultSet.close();
                onuMapGetStatement.close();
                onuMapResultSet.close();
                oltMapGetStatement.close();
                oltMapResultSet.close();
                postgresConnection.close();
                /*-------------------*/
            }

            List<Object> arguments = new ArrayList<>();
            List<Integer> argumentTypes = new ArrayList<>();
            for (String targetColumn : action.getParamColumns()) {
                /*更新家宽映射表特殊功能*/
                String area = "";
                String town = "";
                String onu_ch_name = "";
                String olt_ch_name = "";
                Integer areaType = null;
                Integer townType = null;
                Integer integerArgumentType = 4;
                /*-------------------*/
                for (int i = 1; i <= metaData.getColumnCount(); i++) {
                    String columnName = metaData.getColumnName(i);
                    if (columnName.contains(".")) {
                        String[] tableColumn = StringUtils.split(columnName, ".");
                        columnName = tableColumn[1];
                    }
                    if (columnName.equalsIgnoreCase(targetColumn)) {
                        arguments.add(resultSet.getObject(i));
                        argumentTypes.add(metaData.getColumnType(i));
                        break;
                    }

                    /*更新家宽映射表特殊功能,吧area和town字段和字段类型保存下来*/
                    if(columnName.equalsIgnoreCase("area")){
                        area = (String) resultSet.getObject(i);
                        areaType = metaData.getColumnType(i);
                    }else if(columnName.equalsIgnoreCase("town")){
                        town = (String) resultSet.getObject(i);
                        townType = metaData.getColumnType(i);
                    }else if(columnName.equalsIgnoreCase("onu_ch_name")){
                        onu_ch_name = (String) resultSet.getObject(i);
                    }else if(columnName.equalsIgnoreCase("olt_ch_name")){
                        olt_ch_name = (String) resultSet.getObject(i);
                    }
                    /*-------------------*/
                }

                /*更新家宽映射表特殊功能，根据映射把area和town的id映射后插入参数数组*/
                if(targetColumn.equalsIgnoreCase("area_id")){
                    arguments.add(areaMap.get(area));
                    argumentTypes.add(areaType);
                }else if(targetColumn.equalsIgnoreCase("town_id")){
                    arguments.add(townMap.get(town));
                    argumentTypes.add(townType);
                }else if(targetColumn.equalsIgnoreCase("onu_id")){
                    arguments.add(onuMap.get(onu_ch_name));
                    argumentTypes.add(integerArgumentType);
                }else if(targetColumn.equalsIgnoreCase("olt_id")){
                    arguments.add(oltMap.get(olt_ch_name));
                    argumentTypes.add(integerArgumentType);
                }
                /*-------------------*/
            }

            try {
                for (int i = 0; i < arguments.size(); i++) {
                    action.preparedStatement.setObject(i + 1, arguments.get(i), argumentTypes.get(i));
                }
                action.preparedStatement.addBatch();
                sqlCount++;
            } catch (SQLIntegrityConstraintViolationException | DataIntegrityViolationException e) {
                // SQLIntegrityConstraintViolationException(primary key duplicate) and DataIntegrityViolationException(not null)
                LOG.error("Add batch data failed",e);
            }
            if (sqlCount % Integer.parseInt(StartupConfig.getInstance().getBatchCount()) == 0) {
                SimpleDateFormat sdfLOG = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
                LOG.info("批次推送目标:"+conn.getMetaData().getDatabaseProductName()+","+action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+"批次插入开始"+ sdfLOG.format(System.currentTimeMillis()));
                int[] affectedRecords = action.preparedStatement.executeBatch();
                LOG.info("批次推送目标:"+conn.getMetaData().getDatabaseProductName()+","+action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+"批次插入结束,受影响的行"+ affectedRecords.length +" ,"+ sdfLOG.format(System.currentTimeMillis()));
                conn.commit();
                LOG.info(action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+":批次中连接提交");
            }
        }
    }

    /**
     * On Close.
     *
     * @throws SQLException the sql exception
     */
    @Override
    public void onClose() throws SQLException {
        String insertedTable = "";
        Long insertTime = null;
        SimpleDateFormat sdfLOG = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        if (conn != null) {
            try {
                for (Action action : this.actions) {
                    if(action.preparedStatement.isClosed()){
                        LOG.error(action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+" preparedStatement已关闭,无法插入");
                    }
                    if (action.preparedStatement != null) {
                        insertedTable = action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"));
                        LOG.info("最终推送目标:"+conn.getMetaData().getDatabaseProductName()+","+action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+"最终批次插入开始"+ sdfLOG.format(System.currentTimeMillis()));
                        insertTime = System.currentTimeMillis();
                        LOG.info("最终连接对象哈希值："+conn.hashCode()+","+action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES")));
                        int[] affectedRecords=action.preparedStatement.executeBatch();
                        LOG.info("最终推送目标:"+conn.getMetaData().getDatabaseProductName()+","+action.sqlCommand.substring(action.sqlCommand.indexOf("INTO"), action.sqlCommand.indexOf("VALUES"))+"最终批次插入结束,受影响的行"+ affectedRecords.length +" ,"+ sdfLOG.format(System.currentTimeMillis()));
                    }
                }
                conn.commit();
                LOG.info("推送完成："+insertedTable+":所有记录插入完成时间："+ sdfLOG.format(System.currentTimeMillis()) +",耗时："+ (System.currentTimeMillis() - insertTime)/1000);
                for (Action action : this.actions) {
                    if (action.preparedStatement != null) {
                        action.preparedStatement.close();
                    }
                }
            } catch (SQLException e) {
                LOG.error(e.getMessage(),e);
            } finally {
                try {
                    conn.close();
                } finally {
                    conn=null;
                }
            }
        }
    }

    /**
     * Inner Class Action.
     */
    public static class Action {

        /**
         * The Initialize Sql command.
         */
        private String initSqlCommand;

        /**
         * The Sql command.
         */
        private String sqlCommand;

        /**
         * The Param columns.
         */
        private List<String> paramColumns;

        /**
         * The Prepared statement.
         */
        private PreparedStatement preparedStatement;

        /**
         * Gets init sql command.
         *
         * @return the init sql command
         */
        public String getInitSqlCommand() {
            return initSqlCommand;
        }

        /**
         * Sets init sql command.
         *
         * @param initSqlCommand the init sql command
         */
        public void setInitSqlCommand(String initSqlCommand) {
            this.initSqlCommand = initSqlCommand;
        }

        /**
         * Gets sql command.
         *
         * @return the sql command
         */
        public String getSqlCommand() {
            return sqlCommand;
        }

        /**
         * Sets sql command.
         *
         * @param sqlCommand the sql command
         */
        public void setSqlCommand(String sqlCommand) {
            this.sqlCommand = sqlCommand;
        }

        /**
         * Gets param columns.
         *
         * @return the param columns
         */
        public List<String> getParamColumns() {
            return paramColumns;
        }

        /**
         * Sets param columns.
         *
         * @param paramColumns the param columns
         */
        public void setParamColumns(List<String> paramColumns) {
            this.paramColumns = paramColumns;
        }

    }

}
