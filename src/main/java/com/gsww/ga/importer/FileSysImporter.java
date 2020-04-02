package com.gsww.ga.importer;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.filter.InputPin;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.rmi.runtime.Log;

import java.io.*;
import java.net.URI;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by husiyuan on 2017/8/29.
 */
public class FileSysImporter implements InputPin {
    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(FileSysImporter.class);

    /**
     * The Actions.
     */
    private List<Action> actions;

    /**
     * The Mapping of FilePath and output File.
     */
    private Map<String,File> fileMapping;

    /**
     * The counter of ResultSet's lines.
     */
    private int count;

    /**
     * The number of Files.
     */
    private int fileNumber;

    /**
     * The counter of uploaded lines.
     */
    private int uploadFtpCount;

    private Map<String,Long> queryTime;

    /*更新家宽映射表特殊功能*/
    private boolean isInit;

    private Map<String,String> areaMap;

    private Map<String,String> townMap;

    private Map<String,Integer> onuMap;

    private Map<String,Integer> oltMap;

    private Connection postgresConnection;
    /*-------------------*/
    /**
     * Constructor
     *
     * @param actions the actions
     */
    public FileSysImporter(List<Action> actions,Map<String,Long> queryTime) throws Exception {
        this.actions = actions;
        fileMapping = new HashMap<>();

        this.queryTime=queryTime;
        // count用于记录是不是表头
        for (Action action : this.actions) {
            if (URI.create(action.path).getScheme().equals("file")) {
                fileMapping.put(action.path, createEmptyFile((URI.create(action.path).getPath() + generateFilename(action.filename, true, queryTime))));
            } else {
                File fileParent = new File(action.dataSource);
                deletefile(fileParent.getPath());
                if(fileParent.mkdir()){
                    LOG.info("创建 "+fileParent.getName() +" 目录成功");
                    fileMapping.put(action.path, createEmptyFile(action.dataSource + "/" + generateFilename(action.filename, true, queryTime)));
                }
            }
        }
        //fileNumber用于计算有多少个文件需要生成
        fileNumber = fileMapping.size();
        LOG.info("建立配置："+fileNumber+" 个");
        //count用于记录是不是表头
        count = 0;
        //uploadFtpCount用于记录共上传至ftp多少行
        uploadFtpCount = 0;


        //初始化area，town映射关系
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
//        postgresConnection = DriverManager.getConnection("jdbc:postgresql://192.168.6.97:5432/gemstack_data",
//                "postgres",
//                "postgres");

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

    /**
     * OnInput pin of FileSystem.
     *
     * @param resultSet the result set
     * @param metaData the meta data
     * @throws SQLException the sql exception
     */
    @Override
    public void onInput(ResultSet resultSet, ResultSetMetaData metaData)
            throws SQLException {
        this.queryTime=queryTime;
        /* 验证传输过来的信息 */
//        LOG.info("传输过来FileSystem的信息");
//        String tmp = resultSet.getString(1)+" | "+resultSet.getString(2)+" | "+resultSet.getString(3);

        for (Action action : actions) {

            try {
                String delimiter = ",";
                if (action.getDelimiter() != null) {
                    delimiter = action.getDelimiter();
                }
                /* csv保存到本地时 */
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(fileMapping.get(action.path), true),
                                "UTF-8"));

                /*hdfs读取文件事不带BOM*/
                if(!URI.create(action.path).getScheme().equals("hdfs")){
                    writer.write(new String(
                            new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF }));
                }

                writeToFile(writer, resultSet, ++count, delimiter, URI.create(action.path).getScheme());
                writer.flush();
                writer.close();
                /*csv上传到ftp时*/
                URI ftpUri = URI.create(action.path);
                if (!ftpUri.getScheme().equals("file")) {
                    //uploadToFtp(ftpUri.getHost(), ftpUri.getPort(), action.username, action.password, ftpUri.getPath(), fileMapping.get(action.path), action.filename, true);
                    uploadFtpCount ++;
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("fileSys run Exception:",e);
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
        for (Action action : actions) {
            try {
                if (URI.create(action.path).getScheme().equals("file")) {
                    /*把数据文件保存到本地时*/
                    createEmptyFile((URI.create(action.path).getPath() + generateFilename(action.finshedFilename, false,queryTime)));
                } else if (URI.create(action.path).getScheme().equals("ftp")){
                    /*把数据文件上传到ftp时*/
                    LOG.info(generateFilename(action.filename, true,queryTime) +" 开始上传数据文件");
                    URI ftpUri = URI.create(action.path);
                    if (!ftpUri.getScheme().equals("file")) {
                        uploadToFtp(ftpUri.getHost(), ftpUri.getPort(), action.username, action.password, ftpUri.getPath(), fileMapping.get(action.path), action.filename, true);
                    }

                    File finishedFile = createEmptyFile(action.dataSource + "/" + generateFilename(action.finshedFilename, false,queryTime));
                    /*把结束文件上传到ftp*/
                    //URI ftpUri = URI.create(action.path);
                    uploadToFtp(ftpUri.getHost(), ftpUri.getPort(), action.username, action.password, ftpUri.getPath(), finishedFile, action.finshedFilename, false);

                    //判断数据文件是否成功上传
                    if(checkFileExistOnFtp(ftpUri.getHost(), ftpUri.getPort(), action.username, action.password, ftpUri.getPath(), action.filename, true)){
                        LOG.info(generateFilename(action.filename, true,queryTime) + ": 数据文件上传成功,共 " + uploadFtpCount + " 行");
                    }else{
                        LOG.info(generateFilename(action.filename, true,queryTime) + ": 数据文件上传失败！！！");
                    }
                    //判断结束文件是否成功上传
                    if(checkFileExistOnFtp(ftpUri.getHost(), ftpUri.getPort(), action.username, action.password, ftpUri.getPath(), action.finshedFilename, false)){
                        LOG.info(generateFilename(action.finshedFilename, false,queryTime) + ": 结束文件上传成功");
                    }else{
                        LOG.info(generateFilename(action.finshedFilename, false,queryTime) + ": 结束文件上传失败！！！");
                    }
                } else if (URI.create(action.path).getScheme().equals("hdfs")){
                    /*把数据文件上传到hdfs时*/
                    LOG.info(fileMapping.get(action.path).getAbsolutePath());
                    LOG.info(URI.create(action.path).getHost()+":"+URI.create(action.path).getPort());
                    Configuration conf = new Configuration();
                    /*现网*/
//                    conf.set("fs.defaultFS", "hdfs://hadoop01:10000");
                    /*本地*/
                    conf.set("fs.defaultFS", "hdfs://"+URI.create(action.path).getHost()+":"+URI.create(action.path).getPort());
                    String localDir = fileMapping.get(action.path).getAbsolutePath();
                    String hdfsDir = action.path;
                    try{
                        Path localPath = new Path(localDir);
                        Path hdfsPath = new Path(hdfsDir);
                        FileSystem hdfs = FileSystem.get(conf);
                        hdfs.delete(hdfsPath, true);
                        if(!hdfs.exists(hdfsPath)){
                            hdfs.mkdirs(hdfsPath);
                        }
                        hdfs.copyFromLocalFile(localPath, hdfsPath);
                        createEmptyFile(action.dataSource + "/" + generateFilename(action.finshedFilename, false,queryTime));
                    }catch(Exception e){
                        e.printStackTrace();
                    }

                }
                deleteHistoryData(action,StartupConfig.getInstance().getDataSaveDays());
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("fileSys close Exception:",e);
            }
        }
    }

    private void uploadToFtp(String host, int port, String username, String password, String directory, File file, String filename, Boolean isCSV) {
        FTPClient ftpClient = new FTPClient();
        FileInputStream fis = null;
        try {
            if (port == -1) {
                ftpClient.connect(host);
            } else {
                ftpClient.connect(host, port);
            }
            ftpClient.login(username, password);
            //File srcFile = new File("F:\images\460.jpg");
            fis = new FileInputStream(file);
            //设置上传目录(若无此路径则生成)
            //LOG.info(directory);
            StringTokenizer s = new StringTokenizer(directory, "/");
            String pathName = "";
            while (s.hasMoreElements()) {
                pathName = pathName + "/" + (String) s.nextElement();
                try {
                    while(!ftpClient.changeWorkingDirectory(pathName)){
                        ftpClient.makeDirectory(pathName);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ftpClient.changeWorkingDirectory(directory);
            ftpClient.setBufferSize(1024);
            ftpClient.setControlEncoding("UTF-8");
            //设置文件类型（二进制）
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.storeFile(generateFilename(filename, isCSV,queryTime), fis);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("FTP客户端出错！", e);
        } finally {
            IOUtils.closeQuietly(fis);
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("关闭FTP连接发生异常！", e);
            }
        }
    }

    // 检查指定文件是否存在指定ftp目录下
    private boolean checkFileExistOnFtp(String host, int port, String username, String password, String directory, String filename, Boolean isDataFile){
        FTPClient ftpClient = new FTPClient();
        try {
            if (port == -1) {
                ftpClient.connect(host);
            } else {
                ftpClient.connect(host, port);
            }
            ftpClient.login(username, password);
            //File srcFile = new File("F:\images\460.jpg");
            ftpClient.changeWorkingDirectory(directory);
            ftpClient.setBufferSize(1024);
            ftpClient.setControlEncoding("UTF-8");
            //设置文件类型（二进制）
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            //检验文件是否存在
            InputStream is = ftpClient.retrieveFileStream(generateFilename(filename, isDataFile, queryTime));
            if(is == null || ftpClient.getReplyCode() == FTPReply.FILE_UNAVAILABLE){
                return false;
            }

            if(is != null){
                is.close();
                ftpClient.completePendingCommand();
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("FTP客户端出错！", e);
        } finally {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("关闭FTP连接发生异常！", e);
            }
        }
    }

    // 将文件数据库记录写入文件
    private void writeToFile(BufferedWriter osw, ResultSet rs, int count,
            String delimiter, String destination) throws Exception {
        try {
            ResultSetMetaData rd = rs.getMetaData();
            int fields = rd.getColumnCount();
            if (rd.getColumnName(fields).equals("RN")) {
                fields--;
            }
            /*当上传文件至hdfs时不需要表头*/
            if (count > 0 && count <= fileNumber && !destination.equals("hdfs")) {
                for (int i = 1; i <= fields; i++) {
                    osw.write(rd.getColumnName(i));
                    if (i == fields)
                        osw.write("\n");
                    else
                        osw.write(delimiter);
                }
                osw.flush();
            }
            writeToFile(fields, osw, rs, delimiter);
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    // 将数据记录写入文件
    private void writeToFile(int fields, BufferedWriter osw, ResultSet rs,
            String delimiter) throws Exception {
        try {
            // rs.first();
            if (!rs.wasNull()) {
                String area = "";
                String town = "";
                String onu_ch_name = "";
                String olt_ch_name = "";
                for (int i = 1; i <= fields; i++) {
                    /*更新家宽映射表特殊功能*/
                    /*-------------------*/
                    String temp = rs.getString(i);
                    if (!rs.wasNull()) {
                        // 这里将记录里面的特殊符号进行替换， 假定数据中不包含替换后的特殊字串
                        temp = temp.replaceAll(",", "&%&");
                        temp = temp.replaceAll("\n\r|\r|\n|\r\n", "&#&");
                        osw.write(temp);

                         /*更新家宽映射表特殊功能,吧area和town字段,onu_ch_name和olt_ch_name字段保存下来*/
                         if(rs.getMetaData().getColumnName(i).equalsIgnoreCase("area")){
                             area = temp;
                         }else if(rs.getMetaData().getColumnName(i).equalsIgnoreCase("town")){
                             town = temp;
                         }else if(rs.getMetaData().getColumnName(i).equalsIgnoreCase("onu_ch_name")){
                             onu_ch_name = temp;
                         }else if(rs.getMetaData().getColumnName(i).equalsIgnoreCase("olt_ch_name")){
                             olt_ch_name = temp;
                         }
                         /*-------------------*/
                    }

                    osw.write(delimiter);
                }

                /*更新家宽映射表特殊功能,
                    * 已有的rs为所有fields写入后,则按顺序插入area_id数据与town_id数据,onu_id数据与olt_id数据
                    * 再进行换行*/
                /*onu_id写入*/
                osw.write(insertStringIfNotEmpty("onu",onu_ch_name));
                osw.write(delimiter);

                /*olt_id写入*/
                osw.write(insertStringIfNotEmpty("olt",olt_ch_name));
                osw.write(delimiter);

                /*area_id写入*/
                osw.write(insertStringIfNotEmpty("area",area));
                osw.write(delimiter);

                /*town_id写入*/
                osw.write(insertStringIfNotEmpty("town",town));

                /*一整条数据完整写入后换行*/
                osw.write("\r\n");
                /*-----------------------------------------------------------------*/
                osw.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
    }

    /*当插入需要Map映射的值时，判断插入的值，与映射后的值是否为空*/
    private String insertStringIfNotEmpty(String MapType, String input) throws Exception{
        if(input.equals("")){
            return "";
        }else{
            switch (MapType){
                case "onu":{
                    if(onuMap.get(input) != null)
                        return onuMap.get(input).toString();
                    else
                        return "";
                }
                case "olt":{
                    if(oltMap.get(input) != null)
                        return oltMap.get(input).toString();
                    else
                        return "";
                }
                case "area":{
                    if(areaMap.get(input) != null)
                        return areaMap.get(input);
                    else
                        return "";
                }
                case "town":{
                    if(townMap.get(input) != null)
                        return townMap.get(input);
                    else
                        return "";
                }
            }
        }
        return "";
    }

    // 创建一个空文件
    private File createEmptyFile(String filename) throws Exception {
        File file = new File(filename);
        try {
            if (file.exists()) {
                file.delete();
                file.createNewFile();
            } else {
                file.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        return file;
    }

    private String generateFilename(String actionFileName, Boolean isDataFile,
            Map<String, Long> curQueryTime) {
        String filename = "";
        String[] fileNameArrs = actionFileName.split("%");
        Map<String, String> tmpArrs = new HashMap<>();
        for (int i = 0; fileNameArrs.length>1 && i < fileNameArrs.length; i++) {
            if (fileNameArrs[i].contains("startTime")) {
                tmpArrs.put("startTime",
                        new SimpleDateFormat(fileNameArrs[i].split(":")[1])
                                .format(curQueryTime.get("startTime")));
            } else if (fileNameArrs[i].contains("endTime")) {
                tmpArrs.put("endTime",
                        new SimpleDateFormat(fileNameArrs[i].split(":")[1])
                                .format(curQueryTime.get("endTime")));
            } else if (i == 0) {
                tmpArrs.put("pre", fileNameArrs[i]);
            } else if (i == fileNameArrs.length - 1) {
                tmpArrs.put("end", fileNameArrs[i]);
            } else {
                tmpArrs.put("delimiter", fileNameArrs[i]);
            }
        }
        if(tmpArrs.size()>0){
            tmpArrs.putIfAbsent("pre", "");
            tmpArrs.putIfAbsent("startTime", "");
            tmpArrs.putIfAbsent("delimiter", "");
            tmpArrs.putIfAbsent("endTime", "");
            tmpArrs.putIfAbsent("end", "");
        }
        if (isDataFile) {
            if (tmpArrs.size() > 0) {
                filename = tmpArrs.get("pre") + tmpArrs.get("startTime")
                        + tmpArrs.get("delimiter")
                        + tmpArrs.get("endTime")
                        + tmpArrs.get("end");
            }else {
                filename = actionFileName;
            }
        } else {
            if (tmpArrs.size() > 0) {
                filename = tmpArrs.get("pre") + tmpArrs.get("startTime")
                        + tmpArrs.get("delimiter")
                        + tmpArrs.get("endTime");
                if(tmpArrs.get("end").contains(".")){
                    filename+=tmpArrs.get("end").split("\\.")[0]+".done";
                }else {
                    filename+=tmpArrs.get("end")+".done";
                }
            }else {
                filename = actionFileName;
                if(actionFileName.contains(".")){
                    filename=actionFileName.split("\\.")[0]+".done";
                }else {
                    filename+=".done";
                }
            }
        }
        return filename;
    }

    /**
     * 删除某个文件夹下的所有文件夹和文件
     *
     * @param delpath
     *            String
     * @throws FileNotFoundException
     * @throws IOException
     * @return boolean
     */
    private static boolean deletefile(String delpath) throws Exception {
        try {

            File file = new File(delpath);
            // 当且仅当此抽象路径名表示的文件存在且 是一个目录时，返回 true
            if (!file.isDirectory()) {
                file.delete();
            } else if (file.isDirectory()) {
                String[] filelist = file.list();
                for (int i = 0; i < (filelist != null ? filelist.length : 0); i++) {
                    File delfile = new File(delpath + "/" + filelist[i]);
                    if (!delfile.isDirectory()) {
                        if(delfile.delete()) {
                            LOG.info(delfile.getAbsolutePath() + "删除文件成功");
                        }
                    } else if (delfile.isDirectory()) {
                        deletefile(delpath + "/" + filelist[i]);
                    }
                }
                if(file.delete()){
                    LOG.info(file.getAbsolutePath() + "删除成功");
                }

            }

        } catch (FileNotFoundException e) {
            LOG.info("deletefile() Exception:" + e.getMessage());
        }
        return true;
    }

    private void deleteHistoryData(Action action,Integer saveDays){
        String datasourcePath= StartupConfig.getInstance().getDatasourceMaps().get(action.getDataSource()).get("path");
        LOG.warn(datasourcePath);
        if(StringUtils.isNotBlank(datasourcePath) && datasourcePath.contains("file:")){
            File fileDir=new File(URI.create(datasourcePath).getPath());
            if(fileDir.isDirectory() && saveDays>0){
                String[] fileNameList=fileDir.list();
                Map <String,Long> criticalQuerytime = new HashMap<>();
                criticalQuerytime.put("startTime",(queryTime.get("startTime")-saveDays*86400000));
                criticalQuerytime.put("endTime",(queryTime.get("endTime")-saveDays*86400000));
                String criticalFileName=generateFilename(action.filename,true,criticalQuerytime);
                String nameRegex=generateFilename(action.filename,true,queryTime).replaceAll("[0-9]",".");
                nameRegex="^"+nameRegex+".*$";
                Pattern pattern=Pattern.compile(nameRegex);
                int deleteFileCount=0;
                for(String fileName: fileNameList != null ? fileNameList : new String[0]){
                    Matcher matcher=pattern.matcher(fileName);
                    if(matcher.matches()){
                        File sameFile=new File(URI.create(datasourcePath).getPath()+fileName);
                        if(fileName.compareTo(criticalFileName)<0){
                            sameFile.delete();
                            deleteFileCount++;
                        }
                    }
                }
                LOG.warn("数据保存时间："+saveDays+"天，删除文件-"+criticalFileName+"-时间之前的数据文件（"+deleteFileCount+")个");
            }
        }
    }

    /**
     * Inner Class Action.
     */
    public static class Action {

        private String path;

        private String username;

        private String password;

        private String filename;

        private String finshedFilename;

        private String compressor;
        private String dataSource;

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        private String delimiter;

        public String getDelimiter() {
            return delimiter;
        }

        public void setDelimiter(String delimiter) {
            this.delimiter = delimiter;
        }

        /**
         * Gets path.
         *
         * @return the path
         */
        public String getPath() {
            return path;
        }

        /**
         * Sets path.
         *
         * @param path the path
         */
        public void setPath(String path) {
            this.path = path;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        /**
         * Gets filename.
         *
         * @return the filename
         */
        public String getFilename() {
            return filename;
        }

        /**
         * Sets filename.
         *
         * @param filename the filename
         */
        public void setFilename(String filename) {
            this.filename = filename;
        }

        /**
         * Gets finshed filename.
         *
         * @return the finshed filename
         */
        public String getFinshedFilename() {
            return finshedFilename;
        }

        /**
         * Sets finshed filename.
         *
         * @param finshedFilename the finshed filename
         */
        public void setFinshedFilename(String finshedFilename) {
            this.finshedFilename = finshedFilename;
        }

        /**
         * Gets compressor.
         *
         * @return the compressor
         */
        public String getCompressor() {
            return compressor;
        }

        /**
         * Sets compressor.
         *
         * @param compressor the compressor
         */
        public void setCompressor(String compressor) {
            this.compressor = compressor;
        }

    }

}
