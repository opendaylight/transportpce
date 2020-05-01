package org.opendaylight.transportpce.tapi.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.opendaylight.transportpce.tapi.topology.MongoDbDataStoreService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ORPceListenerImpl implements TransportpcePceListener {

    private MongoDbDataStoreService mongoDbDataStoreService;
    private static final String TERMINATION_POINT = "TerminationPoint";
    public String serviceUuid = "";
    private ServicePathRpcResult servicePathRpcResult;
    private static final Logger LOG = LoggerFactory.getLogger(ORPceListenerImpl.class);

    public ORPceListenerImpl(MongoDbDataStoreService mongoDbDataStoreService) {
        this.mongoDbDataStoreService = mongoDbDataStoreService;
    }

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        // todo --> update path of the service + connections??
        if (!compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            PathDescription pathDescription = null;
            switch (servicePathRpcResult.getNotificationType().getIntValue()) {
                /* path-computation-request. */
                case 1:
                    LOG.info("PCE '{}' Notification received : {}",servicePathRpcResult.getNotificationType().getName(),
                            notification);
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("PCE calculation done OK!");
                        if (servicePathRpcResult.getPathDescription() != null) {
                            pathDescription = new PathDescriptionBuilder()
                                    .setAToZDirection(servicePathRpcResult.getPathDescription().getAToZDirection())
                                    .setZToADirection(servicePathRpcResult.getPathDescription().getZToADirection())
                                    .build();
                            LOG.info("PathDescription gets : {}", pathDescription);
                            // Todo --> check path and create the connections in locked state,
                            //  and when the renderer is done, update the state of all connections
                            // serviceUuid; // service uuid
                            String resourceType;
                            List<String> xpdrClientTplist = new ArrayList<>();
                            List<String> xpdrNetworkTplist = new ArrayList<>();
                            List<String> rdmAddDropTplist = new ArrayList<>();
                            for (AToZ elem:pathDescription.getAToZDirection().getAToZ()) {
                                // todo -> get tip list for xpdr and roadm
                                resourceType = elem.getResource().getResource().implementedInterface().getSimpleName();
                                if (TERMINATION_POINT.equals(resourceType)) {
                                    TerminationPoint tp = (TerminationPoint) elem.getResource().getResource();
                                    String tpID = tp.getTpId();
                                    String tpNode = tp.getTpNodeId();
                                    if (tpID.contains("CLIENT")) {
                                        xpdrClientTplist.add(tpNode + "-" + tpID);
                                    }
                                    if (tpID.contains("NETWORK")) {
                                        xpdrNetworkTplist.add(tpNode + "-" + tpID);
                                    }
                                    if (tpID.contains("PP")) {
                                        rdmAddDropTplist.add(tpNode + "-" + tpID);
                                    }
                                }
                            }
                            createTapiConnections(xpdrClientTplist, xpdrNetworkTplist, rdmAddDropTplist, serviceUuid);
                            setServiceUuid("");
                        } else {
                            LOG.error("'PathDescription' parameter is null ");


                            setServiceUuid("");
                        }
                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.error("PCE path computation failed !");
                    }
                    break;
                /* cancel-resource-reserve. */
                case 2:
                    if (servicePathRpcResult.getStatus() == RpcStatusEx.Successful) {
                        LOG.info("PCE cancel resource done OK !");



                        setServiceUuid("");

                    } else if (servicePathRpcResult.getStatus() == RpcStatusEx.Failed) {
                        LOG.info("PCE cancel resource failed !");



                        setServiceUuid("");
                    }
                    break;
                default:
                    break;
            }
        } else {
            LOG.warn("ServicePathRpcResult already wired !");
        }
    }

    private void createTapiConnections(List<String> xpdrClientTplist, List<String> xpdrNetworkTplist,
                                       List<String> rdmAddDropTplist, String uuid) {
        // top connection DSR/ETH
        for (int i = 0; i < xpdrClientTplist.size() - 1; i++) {
            List<String> node1Parts = Arrays.asList(xpdrClientTplist.get(i).split("-"));
            List<String> node2Parts = Arrays.asList(xpdrClientTplist.get(i + 1).split("-"));
            String node1id = node1Parts.get(0);
            String node2id = node2Parts.get(0);
            // todo --> get LCP from the list
            String node1lcp = "";
            String node2lcp = "";
        }

        // top connection OTSi

        // top connection NMC

        // XC connection
    }

    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        Boolean result = true;
        if (servicePathRpcResult == null) {
            result = false;
        } else {
            if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (servicePathRpcResult.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

    public void setServiceUuid(String uuid) {
        this.serviceUuid = uuid;
    }
}
