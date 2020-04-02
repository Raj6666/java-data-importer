package com.gsww.ga.service.impl;

import com.gsww.ga.common.config.StartupConfig;
import com.gsww.ga.service.SearchIndexer;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by yangpy on 2017/9/2.
 */
public class ElasticsearchIndex implements SearchIndexer {

    public static final String INDEX_NAME = StartupConfig.getInstance().getIndexName();
    public static final String INDEX_TYPE = "gsgairs";

    /**
     * The constant instance.
     */
    private static volatile ElasticsearchIndex instance;

    /**
     * The Client.
     */
    private final TransportClient client;

    /**
     * The Bulk processor.
     */
    private final BulkProcessor bulkProcessor;

    /**
     * Instantiates a new Elasticsearch index.
     *
     * @throws UnknownHostException the unknown host exception
     */
    private ElasticsearchIndex() throws UnknownHostException {
        client = new PreBuiltTransportClient(Settings.builder().put("cluster.name", "cluster-elastic").build());
        for (StartupConfig.Address address : StartupConfig.getInstance().getElasticAddresses()) {
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(address.getIp()), address.getPort()));
        }
        BulkProcessor.Listener bulkListener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long l, BulkRequest bulkRequest) {
            }

            @Override
            public void afterBulk(long l, BulkRequest bulkRequest, BulkResponse bulkResponse) {
            }

            @Override
            public void afterBulk(long l, BulkRequest bulkRequest, Throwable throwable) {
            }
        };
        bulkProcessor = new BulkProcessor.Builder(client, bulkListener)
                .setBulkActions(10000)
                .setBulkSize(new ByteSizeValue(15, ByteSizeUnit.MB))
                .setFlushInterval(TimeValue.timeValueSeconds(5))
                .setConcurrentRequests(30)
                .build();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     * @throws UnknownHostException the unknown host exception
     */
    public static ElasticsearchIndex getInstance() throws UnknownHostException {
        if (instance == null) {
            synchronized (ElasticsearchIndex.class) {
                if (instance == null)
                    instance = new ElasticsearchIndex();
            }
        }
        return instance;
    }

    /**
     * Add index.
     *
     * @param index    the index
     * @param type     the type
     * @param id       the id
     * @param document the document
     */
    @Override
    public void addIndex(String index, String type, String id, String document) {
        bulkProcessor.add(new IndexRequest(index, type, id).source(document, XContentType.JSON));
    }

    /**
     * Flush.
     *
     * @throws InterruptedException the interrupted exception
     */
    @Override
    public void flush() throws InterruptedException {
        bulkProcessor.flush();
//        bulkProcessor.awaitClose(100, TimeUnit.SECONDS);
    }

}