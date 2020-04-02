package com.gsww.ga.exporter;

import com.gsww.ga.config.MainConfigManager;
import com.gsww.ga.db.connections.ConnectionFactory;
import com.gsww.ga.filter.InputPin;
import com.gsww.ga.filter.OutputPin;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yangpy on 2017/8/29.
 */
public class DBExporter implements OutputPin {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DBExporter.class);

    private String fileName;  // 主配置文件名称，用于查找更新时间
    private String driverClassName; // DataSource Driver
    private String url; // DataSource url
    private String username; // DataSource 用户名
    private String password; // DataSource 密码
    private String queryCommand; // 数据查询语句
    private String firstQueryCommand; //首次执行导出任务时使用此语句
    private long updateInterval; // 数据更新间隔（分钟）
    private String msgID; //消息ID
    private String trimMode; //时间取整（NONE：不取整；MINUTE:分钟取整；HOUR：小时取整）
    private long trimMinute; //取整分钟数，如5分钟取整填5
    private long delay; //延迟
    private long timeslot; //间隔时间
    private final ArrayList<InputPin> nextPins; // 后续处理节点
    private long timestamp; //查询的时间点
    private Map<String,Long> queryData; //保存开始时间，结束时间的映射


    /**
     * Instantiates a new Db exporter.
     *
     * @param fileName          the file name
     * @param driverClassName   the driver class name
     * @param url               the url
     * @param username          the username
     * @param password          the password
     * @param queryCommand      the query command
     * @param firstQueryCommand the first query command
     * @param updateInterval    the update interval
     * @param msgID             the msg id
     * @param trimMode          the trim mode
     * @param trimMinute        the trim minute
     * @param delay             the delay
     * @param timeslot          the timeslot
     * @param queryData         the query data
     */
    public DBExporter(String fileName, String driverClassName, String url, String username, String password,
                      String queryCommand, String firstQueryCommand, long updateInterval, String msgID, String trimMode, long trimMinute, long delay, long timeslot, Map<String,Long> queryData) {
        this.fileName = fileName;
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
        this.queryCommand = queryCommand;
        this.firstQueryCommand = firstQueryCommand; //新属性，未使用
        this.updateInterval = updateInterval;
        this.msgID = msgID; //新属性，未使用
        this.trimMode = trimMode; //新属性，未使用
        this.trimMinute = trimMinute; //新属性，未使用
        this.delay = delay;
        this.timeslot = timeslot; //新属性，未使用
        this.queryData = queryData;
        nextPins = new ArrayList<>();
    }

    /**
     * Execute insert.
     *
     * @throws SQLException         the sql exception
     * @throws IOException          the io exception
     * @throws JSONException        the json exception
     * @throws InterruptedException the interrupted exception
     */
    public void executeInsert() throws SQLException, IOException, JSONException, InterruptedException {
        long thisModifiedTime = System.currentTimeMillis();
        LOG.info(fileName+",当前时间："+thisModifiedTime);
        //LOG.info("查询命令："+queryCommand+"["+queryData.get("startTime")+","+queryData.get("endTime")+"]");
        try (Connection connection = ConnectionFactory.getConnection(driverClassName, url, username, password);
             PreparedStatement ps = connection.prepareStatement(queryCommand)) {
            LOG.info(fileName+",数据库链接::"+url+"::是否有效"+connection.isValid(2000));
            LOG.info(fileName+",数据库链接::"+url+"::是否关闭"+connection.isClosed());
            ResultSet resultSet;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            int parmCount= StringUtils.countMatches(queryCommand,"?");
            for(int i=1;i<=parmCount;i++){
                if(i%2!=0){
                    LOG.info(fileName+",slicetime开始时间"+i+":"+ String.valueOf(Long.parseLong(sdf.format(queryData.get("startTime") ))));
                    ps.setObject(i, Long.parseLong(sdf.format(queryData.get("startTime") )));
                }else {
                    LOG.info(fileName+",slicetime结束时间"+i+":"+ String.valueOf(Long.parseLong(sdf.format(queryData.get("endTime") ))));
                    ps.setObject(i, Long.parseLong(sdf.format(queryData.get("endTime") )));
                }
            }
            try {
                SimpleDateFormat sdfLOG = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
                LOG.info(fileName+":开始查询" + sdfLOG.format(System.currentTimeMillis()));
                resultSet = ps.executeQuery();
                LOG.info(fileName+":查询结束" + sdfLOG.format(System.currentTimeMillis()));
                /*验证impala查询信息*/
//                LOG.info ("impala查询结果");
//                while (resultSet.next()) {
//                    LOG.info (resultSet.getString(1)+" | "+resultSet.getString(2)+" | "+resultSet.getString(3)+" | "+resultSet.getString(4));
//                }
            } catch (SQLException e) {
                String error = "The request[" + queryCommand + "] might be killed by hive/yarn, retrying...";
                LOG.error(error);
                LOG.error(e.toString());
                resultSet = ps.executeQuery();
            }
            ResultSetMetaData metaData = resultSet.getMetaData();
            while (resultSet.next()) {
                String tmp = resultSet.getString(1)+" | "+resultSet.getString(2)+" | "+resultSet.getString(3)+" | "+resultSet.getString(4);
                for (InputPin pin : nextPins) {
                    pin.onInput(resultSet, metaData);
                }
            }
            for (InputPin pin : nextPins) {
                pin.onClose();
            }
            resultSet.close();
        }
        MainConfigManager.updateModifiedTime(fileName, thisModifiedTime);
    }

    /**
     * Get data from database for update.
     *
     * @throws SQLException  the sql exception
     * @throws JSONException the json exception
     * @throws IOException   the io exception
     */
//    public void executeUpdate() throws SQLException, JSONException, IOException, InterruptedException {
//        long lastModifiedTime = MainConfigManager.getUpdatedTime(fileName);
//        long thisModifiedTime = System.currentTimeMillis();
//        if (lastModifiedTime == 0) {
//            executeInsert();
//        } else if (lastModifiedTime + updateInterval * 60 * 1000 <= thisModifiedTime) {
//            try (Connection connection = ConnectionFactory.getConnection(driverClassName, url, username, password);
//                 PreparedStatement ps = connection.prepareStatement(updateQueryCommand)) {
//                ps.setObject(1, DateFormatUtils.format(lastModifiedTime, "yyyyMMddHHmmss"));
//                ResultSet resultSet;
//                try {
//                    resultSet = ps.executeQuery();
//                } catch (SQLException e) {
//                    String error = "The request[" + updateQueryCommand + "]["
//                            + DateFormatUtils.format(lastModifiedTime, "yyyyMMddHHmmss")
//                            + "] might be killed by hive/yarn, retrying...";
//                    LOG.error(error);
//                    resultSet = ps.executeQuery();
//                }
//                ResultSetMetaData metaData = resultSet.getMetaData();
//                while (resultSet.next()) {
//                    for (InputPin pin : nextPins) {
//                        pin.onInput(resultSet, metaData);
//                    }
//                }
//                for (InputPin pin : nextPins) {
//                    pin.onClose();
//                }
//                resultSet.close();
//            }
//            MainConfigManager.updateModifiedTime(fileName, thisModifiedTime);
//        }
//    }

    /**
     * Bind pin.
     *
     * @param pin the pin
     */
    @Override
    public void bindPin(InputPin pin) {
        nextPins.add(pin);
    }

    /**
     * Remove pin.
     *
     * @param pin the pin
     */
    @Override
    public void removePin(InputPin pin) {
        nextPins.remove(pin);
    }

}
