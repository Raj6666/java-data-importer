package com.gsww.ga.common.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.FileReader;
import java.util.*;

/**
 * <b><code>StartupConfig</code></b>
 * <p/>
 * StartupConfig
 * <p/>
 * <b>Creation Time:</b> 2017/10/14 15:03.
 *
 * @author Elvis
 * @since data -importer 0.0.1
 */
public class StartupConfig {

    /**
     * The constant LOG.
     */
    private final static Logger LOG = LoggerFactory.getLogger(StartupConfig.class);

    private boolean isDebug = false;
    /**
     * The Thread count.
     */
    private String threadCount = "3";

    /**
     * The Batch count.
     */
    private String batchCount = "20000";

    /**
     * The Index name.
     */
    private String indexName = "gsww";

    /**
     * The Elastic addresses.
     */
    private List<Address> elasticAddresses = new ArrayList<>();

    /**
     * The Ignore datasource.
     */
    private List<String> ignoreDatasources = new ArrayList<>();
    
    /**
     * The datasource Maps
     */
    private Map<String,Map<String,String>> datasourceMaps = new HashMap<>();

    private String cronExpresionMin="";
    private String cronExpresionHour="";
    private String cronExpresionDay="";
    private String cronExpresionMonth="";

    private Integer dataSaveDays=0;

    /**
     * Gets query time.
     *
     * @return the query time
     */
    public Map<String, Long> getQueryTime() {
        return queryTime;
    }

    private Map<String,Long> queryTime=new HashMap<>();

    /**
     * Gets datasource maps.
     *
     * @return the datasource maps
     */
    public Map<String, Map<String, String>> getDatasourceMaps() {
        return datasourceMaps;
    }

    /**
     * Gets ignore datasource.
     *
     * @return the ignore datasource
     */
    public List<String> getIgnoreDatasource() {
        return ignoreDatasources;
    }

    public boolean isDebug() {
        return isDebug;
    }
    /**
     * The constant instance.
     */
    private static volatile StartupConfig instance;

    /**
     * Instantiates a new Startup config.
     *
     * @param classpath the classpath
     */
    private StartupConfig(String classpath) throws Exception {
        try {
            if (StringUtils.isBlank(classpath))
                classpath = "conf/application-dev.properties";
            Properties properties = new Properties();
            properties.load(new FileReader(classpath));
            // Read Datasource
            Map<String,Map<String,String>> datasourceMaps= new HashMap<>();
            List datasorceNames=new ArrayList();
            for(String key:properties.stringPropertyNames()){
                String[] str=key.split("\\.");
                if(str.length>2&&"datasource".equals(str[0])){
                    datasorceNames.add(str[1]);
                }
            }
            datasorceNames=new ArrayList<String>(new HashSet(datasorceNames));
            for (Object key:datasorceNames){
                Map<String,String> datasorce=new HashMap<>();
                if(key.toString().indexOf("fileSys")!=-1){
                    datasorce.put("path",(String) properties.get("datasource."+key+".path"));
                    datasorce.put("username",(String) properties.get("datasource."+key+".username"));
                    datasorce.put("password",(String) properties.get("datasource."+key+".password"));
                }else {
                    datasorce.put("url",(String) properties.get("datasource."+key+".url"));
                    datasorce.put("username",(String) properties.get("datasource."+key+".username"));
                    datasorce.put("password",(String) properties.get("datasource."+key+".password"));
                    datasorce.put("driverClassName",(String) properties.get("datasource."+key+".driver-class-name"));
                }
                datasourceMaps.put(key.toString(),datasorce);
            }
            if(datasourceMaps !=null && datasourceMaps.size()>0){
                this.datasourceMaps=datasourceMaps;
            }
            // Read debug query time
            String debugTime=(String)properties.getProperty("debug.querytime.start.end");
            if(debugTime!=null && !debugTime.isEmpty()){
                LOG.warn(debugTime);
                String [] dateArrays=debugTime.split(",");
                Long startTime;
                Long endTime;
                if(dateArrays.length>=2){
                    startTime=Long.parseLong(dateArrays[0]);
                    endTime=Long.parseLong(dateArrays[1]);
                    LOG.warn(startTime+"-"+endTime);
                    if(endTime - startTime>0){
                        queryTime.put("startTime",startTime);
                        queryTime.put("endTime",endTime);
                        queryTime.put("isValid",Long.parseLong("1"));
                    }else {
                        queryTime.put("isValid",Long.parseLong("0"));
                    }
                }else {
                    queryTime.put("isValid",Long.parseLong("0"));
                }
            }else {
                queryTime.put("isValid",Long.parseLong("0"));
            }
            //Read data save days
            String dataSaveDays= (String) properties.get("importer.data.save.days");
            if(dataSaveDays!=null && !dataSaveDays.isEmpty()){
                this.dataSaveDays=Integer.parseInt(dataSaveDays);
            }
            // Read CronExpression day interval
            String cronExpressionDay =(String)properties.get("cronExpression.day");
            if(cronExpressionDay!=null && !cronExpressionDay.isEmpty()){
                this.cronExpresionDay= cronExpressionDay.replace("_"," ");
            }
            // Read CronExpression Hour interval
            String cronExpressionHour =(String)properties.get("cronExpression.hour");
            if(cronExpressionHour!=null && !cronExpressionHour.isEmpty()){
                this.cronExpresionHour= cronExpressionHour.replace("_"," ");
            }
            // Read CronExpression minute interval
            String cronExpressionMin =(String)properties.get("cronExpression.minute");
            if(cronExpressionMin!=null && !cronExpressionMin.isEmpty()){
                this.cronExpresionMin= cronExpressionMin.replace("_"," ");
            }
            // Read CronExpression month interval
            String cronExpressionMonth =(String)properties.get("cronExpression.month");
            if(cronExpressionMonth!=null && !cronExpressionMonth.isEmpty()){
                this.cronExpresionMonth= cronExpressionMonth.replace("_"," ");
            }
            // Read is debug
            String isDebug = (String)properties.get("debug");
            if(isDebug !=null && !isDebug.isEmpty()){
                this.isDebug=new Boolean(isDebug).booleanValue();
            }

            // Read ignore datasource
            String ignoreDatasources = (String)properties.get("ignore.datasource");
            if(ignoreDatasources !=null && !ignoreDatasources.isEmpty()){
                this.ignoreDatasources=Arrays.asList(ignoreDatasources.split(","));
            }
            // Read thread count
            String threadCount = (String) properties.get("importer.thread.count");
            if (threadCount != null && Integer.parseInt(threadCount) >= 0)
                this.threadCount = threadCount;
            // Read thread count
            String batchCount = (String) properties.get("importer.batch.count");
            if (batchCount != null && Integer.parseInt(batchCount) >= 0)
                this.batchCount = batchCount;
            // Read elastic index name
            String indexName = (String) properties.get("elastic.index.name");
            if (StringUtils.isNotBlank(indexName))
                this.indexName = indexName;
            // Read address
            String addressesStr = (String) properties.get("elastic.address");
            List<Address> addresses = new ArrayList<>();
            if (StringUtils.isNotBlank(addressesStr)) {
                String[] addressArray = addressesStr.split(",");
                for (String addressStr : addressArray) {
                    addressStr = addressStr.trim();
                    String[] addressGroup = addressStr.split(":");
                    addresses.add(new Address(addressGroup[0], Integer.parseInt(addressGroup[1])));
                }
            }
            if (CollectionUtils.isEmpty(addresses)) {
                this.elasticAddresses.add(new Address("95.1.121.225", 9300));
                this.elasticAddresses.add(new Address("95.1.121.226", 9300));
                this.elasticAddresses.add(new Address("95.1.121.227", 9300));
                this.elasticAddresses.add(new Address("95.1.121.228", 9300));
                this.elasticAddresses.add(new Address("95.1.121.229", 9300));
                this.elasticAddresses.add(new Address("95.1.121.210", 9300));
            } else {
                this.elasticAddresses = addresses;
            }
        } catch (Exception e) {
            LOG.error("Failed to read properties from file!", e);
            throw e;
        }
    }

    /**
     * Gets instance.
     *
     * @param classpath the classpath
     */
    public static void generateInstance(String classpath) throws Exception {
        if (instance == null) {
            synchronized (StartupConfig.class) {
                if (instance == null) {
                    instance = new StartupConfig(classpath);
                }
            }
        }
    }

    /**
     * Gets data save days.
     *
     * @return the data save days
     */
    public Integer getDataSaveDays() {
        return dataSaveDays;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static StartupConfig getInstance() {
        return instance;
    }

    /**
     * Gets thread count.
     *
     * @return the thread count
     */
    public String getThreadCount() {
        return threadCount;
    }

    public String getCronExpresion(int interval){
        switch (interval){
            case 15:return cronExpresionMin;
            case 60:return cronExpresionHour;
            case 1440:return cronExpresionDay;
            case 43200:return cronExpresionMonth;
            default:return "";
        }
    }

    public String getBatchCount() {
        return batchCount;
    }

    public void setBatchCount(String batchCount) {
        this.batchCount = batchCount;
    }
    /**
     * Gets index name.
     *
     * @return the index name
     */
    public String getIndexName() {
        return indexName;
    }

    /**
     * Gets elastic addresses.
     *
     * @return the elastic addresses
     */
    public List<Address> getElasticAddresses() {
        return elasticAddresses;
    }

    /**
     * Address.
     */
    public class Address {

        /**
         * The Ip.
         */
        private String ip;

        /**
         * The Port.
         */
        private Integer port;

        /**
         * Instantiates a new Address.
         *
         * @param ip   the ip
         * @param port the port
         */
        Address(String ip, Integer port) {
            this.ip = ip;
            this.port = port;
        }

        /**
         * Gets ip.
         *
         * @return the ip
         */
        public String getIp() {
            return ip;
        }

        /**
         * Gets port.
         *
         * @return the port
         */
        public Integer getPort() {
            return port;
        }

    }

}
