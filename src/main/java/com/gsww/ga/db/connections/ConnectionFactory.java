package com.gsww.ga.db.connections;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangpy on 2017/8/29.
 */
public class ConnectionFactory {

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionFactory.class);

    /**
     * The instance.
     */
    private static volatile ConnectionFactory instance;

    /**
     * The data source map.
     */
    private static volatile Map<String,BasicDataSource> dataSourceMap = new HashMap<>();

    /**
     * The Connection pools.
     */
    private ConcurrentHashMap<String, DriverManagerDataSource> connectionPools = new ConcurrentHashMap<>();

    /**
     * Constructor
     */
    private ConnectionFactory() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static ConnectionFactory getInstance() {
        if (instance == null) {
            synchronized (ConnectionFactory.class) {
                if (instance == null) {
                    instance = new ConnectionFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Gets connection.
     *
     * @param driverClassName the driver class name
     * @param url             the url
     * @param username        the username
     * @param password        the password
     * @return the connection
     * @throws SQLException the sql exception
     */
    public static Connection getConnection(String driverClassName, String url, String username, String password) throws SQLException {
        ConnectionFactory instance = getInstance();
        return instance._getConnection(driverClassName, url, username, password);
    }

    /**
     * 获取JDBC连接
     *
     * @param driverClassName the driver class name
     * @param url             the url
     * @param username        the username
     * @param password        the password
     * @return the connection
     * @throws SQLException the sql exception
     */
    private synchronized Connection _getConnection(String driverClassName, String url, String username, String password) throws SQLException {
        // 检查缓存中是否存在对应连接池
//        DriverManagerDataSource dataSource = null;
//        if(null != dataSource && !dataSource.getConnection().isClosed()){
//            LOG.info("datasource是否关掉：",dataSource.getConnection().isClosed());
//            dataSource = null;
//        }
//        if (null == dataSource) {
//            dataSource = new DriverManagerDataSource(url, username, password);
//            dataSource.setDriverClassName(driverClassName);
//            connectionPools.put(url, dataSource);
//        }
        try {
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            LOG.error("load driver class:'{}' failed",driverClassName,e);
        }
        return DriverManager.getConnection(url,username,password);

//        BasicDataSource dataSource = dataSourceMap.get(url);
//        if (null == dataSource){
//            dataSource = new BasicDataSource();
//            dataSource.setUrl(url);
//            dataSource.setUsername(username);
//            dataSource.setPassword(password);
//            dataSource.setDriverClassName(driverClassName);
//
//            dataSource.setMaxActive(100);
//            dataSource.setMinIdle(20);
//            dataSource.setMaxIdle(20);
//            dataSource.setInitialSize(30);
//            dataSource.setMaxWait(60000);
//            dataSource.setTestWhileIdle(true);
//            dataSource.setTestOnBorrow(true);
//            dataSource.setTestOnReturn(false);
//            dataSource.setValidationQuery("select 1");
//            dataSource.setTimeBetweenEvictionRunsMillis(30000);
//            dataSource.setNumTestsPerEvictionRun(20);
//            dataSource.setValidationQueryTimeout(30000);
//
//            dataSourceMap.put(url,dataSource);
//        }

//        return dataSource.getConnection();
    }

}
