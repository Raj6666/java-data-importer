package com.gsww.ga.importer;

import com.gsww.ga.filter.InputPin;
import com.gsww.ga.service.impl.ElasticsearchIndex;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

/**
 * Created by yangpy on 2017/8/29.
 */
public class ElasticsearchImporter implements InputPin {

    private String indexPrefix; // index id 前缀
    private String indexIdColumn; // index id 后缀，从数据里取该字段
    private String groupId;
    private String modelName;
    private Map<String, String> columnsMapper;

    /**
     * The constant LOG.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchImporter.class);

    /**
     * Constructor.
     *
     * @param indexPrefix   the index prefix
     * @param indexIdColumn the index id column
     * @param modelName     the model name
     * @param columnsMapper the columns mapper
     */
    public ElasticsearchImporter(String indexPrefix, String indexIdColumn, String groupId, String modelName, Map<String, String> columnsMapper) {
        this.indexPrefix = indexPrefix;
        this.indexIdColumn = indexIdColumn;
        this.groupId = groupId;
        this.modelName = modelName;
        this.columnsMapper = columnsMapper;
    }

    /**
     * On input.
     *
     * @param resultSet the result set
     * @param metaData  the meta data
     * @throws JSONException        the json exception
     * @throws SQLException         the sql exception
     * @throws UnknownHostException the unknown host exception
     */
    @Override
    public void onInput(ResultSet resultSet, ResultSetMetaData metaDseaMoveFirstcata) throws JSONException, SQLException, UnknownHostException {
        /*验证传输过来的信息*/
//        LOG.info ("传输过来Elasticsearch的信息");
//        while (resultSet.next()) {
//            LOG.info (resultSet.getString(1)+" | "+resultSet.getString(2)+" | "+resultSet.getString(3)+" | "+resultSet.getString(4));
//        }

//        JSONObject documentJson = new JSONObject();
//        documentJson.put("modelName", this.modelName);
//        documentJson.put("groupId", this.groupId);
//        String indexId = "";
//        for (int i = 1; i <= metaData.getColumnCount(); i++) {
//            String columnName = metaData.getColumnName(i);
//            if (columnName.contains(".")) {
//                String[] tableColumn = StringUtils.split(columnName, ".");
//                columnName = tableColumn[1];
//            }
//            String mapperName = "";
//            for (String key : columnsMapper.keySet()) {
//                if (key.equalsIgnoreCase(columnName) && !StringUtils.isBlank(columnsMapper.get(key))) {
//                    mapperName = columnsMapper.get(key);
//                    break;
//                }
//            }
//            if (StringUtils.isBlank(mapperName))
//                mapperName = columnName;
//            documentJson.put(mapperName, resultSet.getObject(i));
//            if (columnName.equalsIgnoreCase(this.indexIdColumn)) {
//                if (resultSet.getObject(i) != null) {
//                    indexId = this.indexPrefix + resultSet.getObject(i).toString();
//                } else {
//                    // if index id is null, generate an UID.
//                    indexId = this.indexPrefix + UUID.randomUUID().toString();
//                }
//            }
//        }
//        if (StringUtils.isEmpty(indexId)) {
//            LOG.error("The field idColumn[" + this.indexIdColumn + "] in indexId cannot be found in ResultSet!");
//        } else {
//            ElasticsearchIndex.getInstance().addIndex(ElasticsearchIndex.INDEX_NAME, ElasticsearchIndex.INDEX_TYPE, indexId, documentJson.toString());
//        }
    }

    /**
     * On close.
     *
     * @throws UnknownHostException the unknown host exception
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public void onClose() throws UnknownHostException, InterruptedException {
//        ElasticsearchIndex.getInstance().flush();
    }

    private Object column2Object(ResultSet rs, int index, int type) throws SQLException {
        if (type == java.sql.Types.ARRAY) {
            return rs.getArray(index);
        } else if (type == java.sql.Types.BIGINT) {
            return rs.getLong(index);
        } else if (type == java.sql.Types.BOOLEAN) {
            return rs.getBoolean(index);
        } else if (type == java.sql.Types.BLOB) {
            return rs.getBlob(index);
        } else if (type == java.sql.Types.DOUBLE) {
            return rs.getDouble(index);
        } else if (type == java.sql.Types.FLOAT) {
            return rs.getFloat(index);
        } else if (type == java.sql.Types.INTEGER) {
            return rs.getInt(index);
        } else if (type == java.sql.Types.NVARCHAR) {
            return rs.getNString(index);
        } else if (type == java.sql.Types.VARCHAR) {
            return rs.getString(index);
        } else if (type == java.sql.Types.TINYINT) {
            return rs.getInt(index);
        } else if (type == java.sql.Types.SMALLINT) {
            return rs.getInt(index);
        } else if (type == java.sql.Types.DATE) {
            return rs.getDate(index);
        } else if (type == java.sql.Types.TIMESTAMP) {
            return rs.getTimestamp(index);
        } else {
            return rs.getObject(index);
        }
    }

}
