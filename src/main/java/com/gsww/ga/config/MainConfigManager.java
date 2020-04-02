package com.gsww.ga.config;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b><code>MainConfigManager</code></b>
 * <p/>
 * MainConfigManager
 * <p/>
 * <b>Creation Time:</b> 2017/8/31 11:06.
 *
 * @author Elvis
 * @since data-importer 0.0.1
 */
public class MainConfigManager {

    /**
     * The directory of the sub config files.
     */
    private String importerConf = "";

    /**
     * The list of updated-time,
     * the key of the map is the file name of sub config file,
     * and the value is the last modified time.
     */
    private List<Map<String, String>> updatedTime = new ArrayList<>();

    /**
     * The file name with absolute path of main config .
     * You Must Set This Path Before Application Started!
     */
    private String mainConfigFileName = "./conf/config";

    /**
     * The Singleton instance.
     */
    private static volatile MainConfigManager instance;

    /**
     * Constructor.
     *
     * @throws IOException the io exception
     */
    private MainConfigManager() throws IOException {
        initConfigByReadingFile();
    }

    /**
     * Init MainConfigManager by reading file.
     * <p>
     * Config File example:
     * --------------------------------------------------------
     * [importer-conf]
     * conf-d: 数据同步配置文件目录（配置文件后缀为“.ei”）
     * [updated-time]
     * 配置文件名: 最后更新时间（1970-1-1 00:00:00开始的毫秒数）
     * --------------------------------------------------------
     * ps:
     * conf-d 必须跟在[importer-conf]下面，更新时间也必须跟在[updated-time]下面;
     * 一项配置必须写在同一行内;
     *
     * @throws IOException the io exception
     */
    private void initConfigByReadingFile() throws IOException {
        /* Read file. */
        List<String> configList = FileUtils.readLines(new File(mainConfigFileName), StandardCharsets.UTF_8);

        /* separate to [importer-conf] and [updated-time] */
        int type = 0;
        List<String> dirStrList = new ArrayList<>();
        List<String> timeStrList = new ArrayList<>();
        for (String thisLine : configList) {
            thisLine = thisLine.trim();
            if (type == 1 && !StringUtils.isBlank(thisLine)) {
                dirStrList.add(thisLine);
            } else if (type == 2 && !StringUtils.isBlank(thisLine)) {
                timeStrList.add(thisLine);
            }
            if (thisLine.contains("[importer-conf]")) {
                type = 1;
            } else if (thisLine.contains("[updated-time]")) {
                type = 2;
            }
        }

        /* set to importerConf and updatedTime */
        for (String thisLine : dirStrList) {
            if (thisLine.contains("conf-d")) {
                String[] result = StringUtils.split(thisLine, ":");
                if (result.length >= 2) {
                    this.importerConf = thisLine.substring(thisLine.indexOf(":") + 1).trim();
                }
                break;
            }
        }
        for (String thisLine : timeStrList) {
            String[] result = StringUtils.split(thisLine, ":");
            if (result.length == 2) {
                Map<String, String> thisUpdatedTime = new HashMap<>();
                thisUpdatedTime.put(result[0].trim(), result[1].trim());
                this.updatedTime.add(thisUpdatedTime);
            }
        }
    }

    /**
     * Gets instance.[double checked locking]
     *
     * @return the instance
     * @throws IOException the io exception
     */
    private static MainConfigManager getInstance() throws IOException {
        if (instance == null) {
            synchronized (MainConfigManager.class) {
                if (instance == null) {
                    instance = new MainConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get directory of sub config files.
     *
     * @return the config directory
     * @throws IOException the io exception
     */
    public static String getConfigDirectory() throws IOException {
        MainConfigManager config = MainConfigManager.getInstance();
        return config.importerConf;
    }

    /**
     * Get the lastModifiedTime of sub config file.
     *
     * @param fileName the file name
     * @return the updated time
     * @throws IOException the io exception
     */
    public static long getUpdatedTime(String fileName) throws IOException {
        MainConfigManager config = MainConfigManager.getInstance();
        long lastUpdatedTime = 0;
        for (Map<String, String> thisUpdateTime : config.updatedTime) {
            if (!StringUtils.isBlank(thisUpdateTime.get(fileName))) {
                lastUpdatedTime = Long.parseLong(thisUpdateTime.get(fileName).trim());
            }
        }
        return lastUpdatedTime;
    }

    /**
     * Update or insert lastModifiedTime of this file.
     *
     * @param fileName         the file name
     * @param thisModifiedTime the this modified time
     * @throws IOException the io exception
     */
    public static void updateModifiedTime(String fileName, long thisModifiedTime) throws IOException {
        MainConfigManager config = MainConfigManager.getInstance();
        boolean isExist = false;
        for (Map<String, String> lastUpdatedTime : config.updatedTime) {
            if (lastUpdatedTime.get(fileName) != null) {
                isExist = true;
                lastUpdatedTime.put(fileName, String.valueOf(thisModifiedTime));
            }
        }
        if (!isExist) {
            Map<String, String> thisUpdatedTime = new HashMap<>();
            thisUpdatedTime.put(fileName, String.valueOf(thisModifiedTime));
            config.updatedTime.add(thisUpdatedTime);
        }
        save();
    }

    /**
     * Save to main config file.
     *
     * @throws IOException the io exception
     */
    private static void save() throws IOException {
        MainConfigManager config = MainConfigManager.getInstance();
        List<String> linesToWrite = new ArrayList<>();
        linesToWrite.add("[importer-conf]");
        linesToWrite.add("conf-d: " + config.importerConf);
        linesToWrite.add("");
        linesToWrite.add("[updated-time]");
        for (Map<String, String> updatedTime : config.updatedTime) {
            for (String fileName : updatedTime.keySet()) {
                linesToWrite.add(fileName + ": " + updatedTime.get(fileName));
            }
        }
        FileUtils.writeLines(new File(config.mainConfigFileName), StandardCharsets.UTF_8.name(), linesToWrite);
    }

}
