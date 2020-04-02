package com.gsww.ga.filter;

import org.json.JSONException;

import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by yangpy on 2017/8/29.
 */
public interface InputPin {

    /**
     * On input.
     *
     * @param resultSet the result set
     * @param metaData  the meta data
     * @throws SQLException         the sql exception
     * @throws JSONException        the json exception
     * @throws UnknownHostException the unknown host exception
     */
    void onInput(ResultSet resultSet, ResultSetMetaData metaData) throws SQLException, JSONException, UnknownHostException;


    /**
     * On close.
     *
     * @throws SQLException         the sql exception
     * @throws UnknownHostException the unknown host exception
     * @throws InterruptedException the interrupted exception
     */
    void onClose() throws SQLException, UnknownHostException, InterruptedException;

}
