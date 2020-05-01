package org.opendaylight.transportpce.tapi.topology;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;

public interface MongoDbDataStoreService {

    MongoClient getMongoClient();

    void initialize();

    void finish();

    void addNode(Node tapiNode);

    void deleteNode(String uuid);

    void addPort(String nodeId, OwnedNodeEdgePoint nodeEdgePoint);

    void addLink(Link tapiLink);

    void deleteLink(String linkId);

    void addSip(ServiceInterfacePoint sip);

    void deleteSips(String nodeId);

    BasicDBObject getNode(String nodeId);

    List<Node> getTapiNodes(String topoId);

    List<Link> getTapiLinks(String topoId);

    List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service
            .list.output.Service> getTapiServices(String topoId);

    List<Sip> getTapiSips();

    // List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get
    // .connectivity.service.list.output.Service> getServiceList();

    org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service
            ._interface.point.details.output.Sip getSipDetails(String sipId);

    String getNepfromSip(String sipId);

    void addService(Service service);

    void addConnectiontoService(String serviceUuid, Connection connection);

    void updateServiceState(String serviceUuid, LifecycleState lifecycleState,
                            AdministrativeState administrativeState, OperationalState operationalState);

    String getNepType(String nodeId, String nepId);

    BasicDBObject getService(String serviceId);

    void addPathToService(DBObject path, String serviceId);

    void addResourceToService(String wavelengthNumber, String serviceId);

    void updateLinkOperationalState(String linkId, String state);

    void deleteService(String serviceId);

    void deleteLinksOfNode(String nodeId);

    Long allocateWavelengthforService();

}
