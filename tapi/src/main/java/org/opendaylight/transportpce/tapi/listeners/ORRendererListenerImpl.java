package org.opendaylight.transportpce.tapi.listeners;

import org.opendaylight.transportpce.tapi.topology.MongoDbDataStoreService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ORRendererListenerImpl implements TransportpceRendererListener {

    private MongoDbDataStoreService mongoDbDataStoreService;
    private static final Logger LOG = LoggerFactory.getLogger(ORRendererListenerImpl.class);
    public String serviceUuid = "";
    private ServiceRpcResultSp serviceRpcResultSp;

    public ORRendererListenerImpl(MongoDbDataStoreService mongoDbDataStoreService) {
        this.mongoDbDataStoreService = mongoDbDataStoreService;
    }

    @Override
    public void onServiceRpcResultSp(ServiceRpcResultSp notification) {
        LOG.info("New renderer notification: {}", notification);
        // todo --> update connectivity service based on result
        if (!compareServiceRpcResultSp(notification)) {
            serviceRpcResultSp = notification;
            int notifType = serviceRpcResultSp.getNotificationType().getIntValue();
            switch (notifType) {
                /* service-implementation-request. */
                case 3 :
                    // todo --> could be that the service doesnt exist because the rpc called was OR service create
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("Service implemented!");
                        // todo --> update mongo db status
                        this.mongoDbDataStoreService.updateServiceState(serviceUuid, LifecycleState.INSTALLED,
                                AdministrativeState.UNLOCKED, OperationalState.ENABLED);
                        setServiceUuid("");

                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("Renderer implementation failed !");
                        // todo --> delete service from mongo db
                        this.mongoDbDataStoreService.deleteService(serviceUuid);
                        setServiceUuid("");
                    }
                    break;
                /* service-delete. */
                case 4 :
                    if (serviceRpcResultSp.getStatus() == RpcStatusEx.Successful) {
                        // todo --> service delete. How to relate OR service with TAPI service??
                        setServiceUuid("");

                    } else if (serviceRpcResultSp.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("Renderer service delete failed !");
                        setServiceUuid("");
                    }
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServiceRpcResultSp already wired !");
        }

    }

    private Boolean compareServiceRpcResultSp(ServiceRpcResultSp notification) {
        Boolean result = true;
        if (serviceRpcResultSp == null) {
            result = false;
        } else {
            if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

    public void setServiceUuid(String uuid) {
        this.serviceUuid = uuid;
    }
}
