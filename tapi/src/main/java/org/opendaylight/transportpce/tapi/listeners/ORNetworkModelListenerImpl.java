package org.opendaylight.transportpce.tapi.listeners;

import org.opendaylight.transportpce.tapi.topology.MongoDbDataStoreService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev200512.TransportpceNetworkmodelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ORNetworkModelListenerImpl implements TransportpceNetworkmodelListener {

    private MongoDbDataStoreService mongoDbDataStoreService;
    private static final Logger LOG = LoggerFactory.getLogger(ORNetworkModelListenerImpl.class);

    public ORNetworkModelListenerImpl(MongoDbDataStoreService mongoDbDataStoreService) {
        this.mongoDbDataStoreService = mongoDbDataStoreService;
    }

    @Override
    public void onTopologyUpdateResult(TopologyUpdateResult notification) {
        LOG.info("New topology notification: {}", notification);
        // todo --> update tapi topology
    }
}
