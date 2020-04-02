package com.gsww.ga.service;

/**
 * Created by yangpy on 2017/9/2.
 */
public interface SearchIndexer {

    /**
     * Add index.
     *
     * @param index    the index
     * @param type     the type
     * @param id       the id
     * @param document the document
     */
    void addIndex(String index, String type, String id, String document);

    /**
     * Flush.
     *
     * @throws InterruptedException the interrupted exception
     */
    void flush() throws InterruptedException;
}
