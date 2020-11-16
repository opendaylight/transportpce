package org.opendaylight.transportpce.servicehandler.listeners;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TransportpceNetworkmodelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelListenerImpl implements TransportpceNetworkmodelListener {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelListenerImpl.class);
    private final NotificationPublishService notificationPublishService; // to be used for T-API notification
    private ServiceDataStoreOperations serviceDataStoreOperations;

    public NetworkModelListenerImpl(NotificationPublishService notificationPublishService,
                                    ServiceDataStoreOperations serviceDataStoreOperations) {
        this.notificationPublishService = notificationPublishService;
        this.serviceDataStoreOperations = serviceDataStoreOperations;
    }

    @Override
    public void onTopologyUpdateResult(TopologyUpdateResult notification) {
        LOG.info("Topology update notification: {}", notification.toString());
    }

    public void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData) {
        this.serviceDataStoreOperations = serviceData;
    }
}
