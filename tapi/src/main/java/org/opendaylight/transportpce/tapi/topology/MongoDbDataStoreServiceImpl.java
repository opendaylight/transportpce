package org.opendaylight.transportpce.tapi.topology;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.SipBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.slf4j.LoggerFactory;

public class MongoDbDataStoreServiceImpl implements MongoDbDataStoreService {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(MongoDbDataStoreServiceImpl.class);

    private static com.mongodb.MongoClient mongoClient = null;
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 27017;
    public static final String DB_NAME = "tapi-context";
    public static final String CONNECTIVITY_DB_NAME = "connectivity-context";

    @Override
    public com.mongodb.MongoClient getMongoClient() {
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
        if (mongoClient == null) {
            mongoClient = new com.mongodb.MongoClient(HOST, PORT);
        }
        return mongoClient;
    }

    @Override
    public void initialize() {
        LOG.info("Preparing mongodb database");
        com.mongodb.MongoClient mongo = getMongoClient();
        MongoDatabase database = mongo.getDatabase(DB_NAME);
        if (!database.listCollectionNames().into(new ArrayList<String>()).contains("topology-context")) {
            database.createCollection("topology-context");
            Document name = new Document();
            name.append("value-name", "name");
            name.append("value", "OPTICAL-TOPOLOGY");
            BasicDBList nameList = new BasicDBList();
            nameList.add(name);
            Document topology = new Document();
            topology.append("uuid", new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()).toString());
            topology.append("name", nameList);
            topology.append("node", new BasicDBList());
            topology.append("link", new BasicDBList());
            BasicDBList topoList = new BasicDBList();
            topoList.add(topology);
            Document topologyList = new Document();
            topologyList.append("_id", new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString())
                    .toString());
            topologyList.append("topology", topoList);
            MongoCollection<Document> topo = database.getCollection("topology-context");
            topo.insertOne(topologyList);
        }
        if (!database.listCollectionNames().into(new ArrayList<String>()).contains("connectivity-context")) {
            database.createCollection("connectivity-context");
            Document connection = new Document();
            connection.append("_id", new Uuid(UUID.nameUUIDFromBytes("connectivity-context".getBytes())
                    .toString()).toString());
            // connection.append("connection", new BasicDBList());
            connection.append("connectivity-service", new BasicDBList());
            MongoCollection<Document> connectivity = database.getCollection("connectivity-context");
            connectivity.insertOne(connection);
        }
        if (!database.listCollectionNames().into(new ArrayList<String>()).contains("sip-context")) {
            database.createCollection("sip-context");
            Document sip = new Document();
            sip.append("_id", new Uuid(UUID.nameUUIDFromBytes("sip-list".getBytes())
                    .toString()).toString());
            sip.append("sip", new BasicDBList());
            MongoCollection<Document> connectivity = database.getCollection("sip-context");
            connectivity.insertOne(sip);
        }
        LOG.info("Finished configuration of DB");
    }

    @Override
    public void finish() {
        getMongoClient().close();
    }

    @Override
    public void addNode(Node tapiNode) {
        BasicDBObject node = new BasicDBObject();
        //uuid
        node.append("uuid", tapiNode.getUuid().getValue().toString());
        //nameList
        BasicDBList nameList = new BasicDBList();
        for (Name name1:tapiNode.getName()) {
            Document name = new Document();
            name.append("value-name", name1.getValueName());
            name.append("value", name1.getValue());
            nameList.add(name);
        }
        node.append("name", nameList);
        //layer-protocol-name
        BasicDBList layerProtocolNameList = new BasicDBList();
        for (LayerProtocolName layer1:tapiNode.getLayerProtocolName()) {
            layerProtocolNameList.add(layer1.getName());
        }
        node.append("layer-protocol-name", layerProtocolNameList);
        // owned-node-edge-point
        BasicDBList onepList = new BasicDBList();
        for (OwnedNodeEdgePoint onep:tapiNode.getOwnedNodeEdgePoint()) {
            Document one = new Document();
            //uuid
            one.append("uuid", onep.getUuid().getValue().toString());
            //nameList
            BasicDBList nameListonep = new BasicDBList();
            for (Name name1:onep.getName()) {
                Document name = new Document();
                name.append("value-name", name1.getValueName());
                name.append("value", name1.getValue());
                nameListonep.add(name);
            }
            one.append("name", nameListonep);
            //layer-protocol-name
            BasicDBList layerProtocolNameListonep = new BasicDBList();
            layerProtocolNameListonep.add(onep.getLayerProtocolName().getName());
            one.append("layer-protocol-name", layerProtocolNameListonep);
            //administrative-state
            one.append("administrative-state", onep.getAdministrativeState().getName());
            //operational-state
            one.append("operational-state", onep.getOperationalState().getName());
            //lifecycle-state
            one.append("lifecycle-state", onep.getLifecycleState().getName());
            //termination-state
            one.append("termination-state", onep.getTerminationState().getName());
            //termination-direction
            one.append("termination-direction", onep.getTerminationDirection().getName());
            //link-port-direction
            one.append("link-port-direction", onep.getLinkPortDirection().getName());
            //link-port-role
            one.append("link-port-role", onep.getLinkPortRole().getName());
            //mapped-sip
            if (onep.getMappedServiceInterfacePoint() != null) {
                BasicDBList mappedSIP = new BasicDBList();
                for (MappedServiceInterfacePoint sip:onep.getMappedServiceInterfacePoint()) {
                    Document msip = new Document();
                    msip.append("service-interface-point-uuid", sip.getServiceInterfacePointUuid().toString());
                    mappedSIP.add(msip);
                }
                one.append("mapped-service-interface-point", mappedSIP);
            } else {
                one.append("mapped-service-interface-point", null);
            }
            //supported-cep-layer-protocol-qualifier
            BasicDBList supportedCepLayerProtocolQualifier = new BasicDBList();
            supportedCepLayerProtocolQualifier.add("PHOTONIC_LAYER_QUALIFIER_OTSiA");
            one.append("supported-cep-layer-protocol-qualifier", supportedCepLayerProtocolQualifier);
            onepList.add(one);
        }
        node.append("owned-node-edge-point", onepList);
        //aggregated-node-edge-point
        node.append("aggregated-node-edge-point", new BasicDBList());
        //administrative-state
        node.append("administrative-state", tapiNode.getAdministrativeState().getName());
        //operational-state
        node.append("operational-state", tapiNode.getOperationalState().getName());
        //lifecycle-state
        node.append("lifecycle-state", tapiNode.getLifecycleState().getName());
        LOG.info("Node to be added: {}", node.toJson());
        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put("_id", new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString())
                .toString());
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put("topology.0.node" , node);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        com.mongodb.MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        topo.update(inQuery, valuesWithSet);
    }

    @Override
    public void deleteNode(String uuid) {
        com.mongodb.MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        BasicDBObject match = new BasicDBObject("topology.0.node.uuid", uuid); // to match your document
        BasicDBObject update = new BasicDBObject("topology.0.node", new BasicDBObject("uuid", uuid));
        topo.update(match, new BasicDBObject("$pull", update));
    }

    @Override
    public void addPort(String nodeId, OwnedNodeEdgePoint nodeEdgePoint) {
        BasicDBObject port = new BasicDBObject();
        //uuid
        port.append("uuid", nodeEdgePoint.getUuid().getValue().toString());
        //nameList
        BasicDBList nameList = new BasicDBList();
        for (Name name1:nodeEdgePoint.getName()) {
            Document name = new Document();
            name.append("value-name", name1.getValueName());
            name.append("value", name1.getValue());
            nameList.add(name);
        }
        port.append("name", nameList);
        //layer-protocol-name
        BasicDBList layerProtocolNameList = new BasicDBList();
        layerProtocolNameList.add(nodeEdgePoint.getLayerProtocolName().getName());
        port.append("layer-protocol-name", layerProtocolNameList);
        //administrative-state
        port.append("administrative-state", nodeEdgePoint.getAdministrativeState().getName());
        //operational-state
        port.append("operational-state", nodeEdgePoint.getOperationalState().getName());
        //lifecycle-state
        port.append("lifecycle-state", nodeEdgePoint.getLifecycleState().getName());
        //termination-state
        port.append("termination-state", nodeEdgePoint.getTerminationState().getName());
        //termination-direction
        port.append("termination-direction", nodeEdgePoint.getTerminationDirection().getName());
        //link-port-direction
        port.append("link-port-direction", nodeEdgePoint.getLinkPortDirection().getName());
        //link-port-role
        port.append("link-port-role", nodeEdgePoint.getLinkPortRole().getName());
        //mapped-sip
        if (nodeEdgePoint.getMappedServiceInterfacePoint() != null) {
            BasicDBList mappedSIP = new BasicDBList();
            for (MappedServiceInterfacePoint sip:nodeEdgePoint.getMappedServiceInterfacePoint()) {
                Document msip = new Document();
                msip.append("service-interface-point-uuid", sip.getServiceInterfacePointUuid().toString());
                mappedSIP.add(msip);
            }
            port.append("mapped-service-interface-point", mappedSIP);
        } else {
            port.append("mapped-service-interface-point", null);
        }
        //supported-cep-layer-protocol-qualifier
        BasicDBList supportedCepLayerProtocolQualifier = new BasicDBList();
        supportedCepLayerProtocolQualifier.add("PHOTONIC_LAYER_QUALIFIER_OTSiA");
        port.append("supported-cep-layer-protocol-qualifier", supportedCepLayerProtocolQualifier);
        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put("topology.0.node.uuid", new Uuid(UUID.nameUUIDFromBytes(nodeId.getBytes()).toString())
                .toString());
        // inQuery.put("topology.0.node.$.uuid","node-1");
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put("topology.0.node.$.owned-node-edge-point" , port);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        topo.update(inQuery, valuesWithSet);
    }

    @Override
    public void addLink(Link tapiLink) {
        BasicDBObject link = new BasicDBObject();
        //uuid
        link.append("uuid", tapiLink.getUuid().getValue().toString());
        //nameList
        BasicDBList nameList = new BasicDBList();
        for (Name name1:tapiLink.getName()) {
            Document name = new Document();
            name.append("value-name", name1.getValueName());
            name.append("value", name1.getValue());
            nameList.add(name);
        }
        link.append("name", nameList);
        //layer-protocol-name
        BasicDBList layerProtocolNameList = new BasicDBList();
        for (LayerProtocolName layer1:tapiLink.getLayerProtocolName()) {
            layerProtocolNameList.add(layer1.getName());
        }
        link.append("layer-protocol-name", layerProtocolNameList);
        //administrative-state
        link.append("administrative-state", tapiLink.getAdministrativeState().getName());
        //operational-state
        link.append("operational-state", tapiLink.getOperationalState().getName());
        //lifecycle-state
        link.append("lifecycle-state", tapiLink.getLifecycleState().getName());
        //termination-state
        link.append("direction", tapiLink.getDirection().getName());
        //resilience-type
        BasicDBObject resilienceType = new BasicDBObject();
        resilienceType.append("protection-type", tapiLink.getResilienceType().getProtectionType().getName());
        resilienceType.append("restoration-policy", tapiLink.getResilienceType().getRestorationPolicy().getName());
        link.append("resilience-type", resilienceType);
        //total-potential-capacity
        BasicDBObject totalPotentialCapacity = new BasicDBObject();
        BasicDBObject totalSize = new BasicDBObject();
        totalSize.append("value", "8000");
        totalSize.append("unit", "GHz");
        totalPotentialCapacity.append("total-size", totalSize);
        link.append("total-potential-capacity", totalPotentialCapacity);
        //available-capacity
        link.append("available-capacity", totalPotentialCapacity);
        //cost-characteristic
        BasicDBObject costCharacteristic = new BasicDBObject();
        costCharacteristic.append("cost-name", tapiLink.getCostCharacteristic().get(0).getCostName());
        costCharacteristic.append("cost-value", tapiLink.getCostCharacteristic().get(0).getCostValue());
        link.append("cost-characteristic", costCharacteristic);
        //latency-characteristic
        BasicDBObject latencyCharacteristic = new BasicDBObject();
        latencyCharacteristic.append("traffic-property-name", "FixedLatency");
        latencyCharacteristic.append("fixed-latency-characteristic", "1");
        link.append("latency-characteristic", latencyCharacteristic);


        BasicDBObject nodeEdgePoint1 = new BasicDBObject();
        nodeEdgePoint1.append("topology-uuid", UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString());
        nodeEdgePoint1.append("node-uuid", tapiLink.getNodeEdgePoint().get(0).getNodeUuid().getValue().toString());
        nodeEdgePoint1.append("node-edge-point-uuid", tapiLink.getNodeEdgePoint().get(0)
                .getNodeEdgePointUuid().getValue().toString());

        BasicDBObject nodeEdgePoint2 = new BasicDBObject();
        nodeEdgePoint2.append("topology-uuid", UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString());
        nodeEdgePoint2.append("node-uuid", tapiLink.getNodeEdgePoint().get(1).getNodeUuid().getValue().toString());
        nodeEdgePoint2.append("node-edge-point-uuid", tapiLink.getNodeEdgePoint().get(1)
                .getNodeEdgePointUuid().getValue().toString());
        BasicDBList nodeEdgePoint = new BasicDBList();
        nodeEdgePoint.add(nodeEdgePoint1);
        nodeEdgePoint.add(nodeEdgePoint2);
        link.append("node-edge-point", nodeEdgePoint);

        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put("_id", new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString())
                .toString());
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put("topology.0.link" , link);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        topo.update(inQuery, valuesWithSet);
    }

    @Override
    public void deleteLink(String linkId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        BasicDBObject match = new BasicDBObject("topology.0.link.uuid",linkId); // to match your document
        BasicDBObject update = new BasicDBObject("topology.0.link", new BasicDBObject("uuid", linkId));
        topo.update(match, new BasicDBObject("$pull", update));
    }

    @Override
    public BasicDBObject getNode(String nodeId) {
        BasicDBObject node = null;
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        String uuid = new Uuid(UUID.nameUUIDFromBytes(nodeId.getBytes()).toString())
                .toString();
        DBCursor myCursor = topo.find(new BasicDBObject("topology.0.node.uuid", uuid));
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("topology");
            BasicDBObject topology = (BasicDBObject) topologyList.get(0);
            BasicDBList nodes = (BasicDBList) topology.get("node");
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) nodes.get(i);
                if (nodetem.get("uuid").equals(uuid)) {
                    node = nodetem;
                }
            }
        }
        return node;
    }

    @Override
    public void addSip(ServiceInterfacePoint sip) {
        BasicDBObject serviceInterfacePoint = new BasicDBObject();
        //uuid
        serviceInterfacePoint.append("uuid", sip.getUuid().getValue().toString());
        //nameList
        BasicDBList nameList = new BasicDBList();
        for (Name name1:sip.getName()) {
            Document name = new Document();
            name.append("value-name", name1.getValueName());
            name.append("value", name1.getValue());
            nameList.add(name);
        }
        serviceInterfacePoint.append("name", nameList);
        //layer-protocol-name
        BasicDBList layerProtocolNameList = new BasicDBList();
        layerProtocolNameList.add(sip.getLayerProtocolName().getName());
        serviceInterfacePoint.append("layer-protocol-name", layerProtocolNameList);
        //administrative-state
        serviceInterfacePoint.append("administrative-state", sip.getAdministrativeState().getName());
        //operational-state
        serviceInterfacePoint.append("operational-state", sip.getOperationalState().getName());
        //lifecycle-state
        serviceInterfacePoint.append("lifecycle-state", sip.getLifecycleState().getName());
        //supported-cep-layer-protocol-qualifier
        BasicDBList supportedCepLayerProtocolQualifier = new BasicDBList();
        supportedCepLayerProtocolQualifier.add("PHOTONIC_LAYER_QUALIFIER_OTSiA");
        serviceInterfacePoint.append("supported-cep-layer-protocol-qualifier", supportedCepLayerProtocolQualifier);

        // In sip context
        BasicDBObject inQuery1 = new BasicDBObject();
        inQuery1.put("_id", new Uuid(UUID.nameUUIDFromBytes("sip-list".getBytes()).toString())
                .toString());
        BasicDBObject updateDocument1 = new BasicDBObject();
        updateDocument1.put("sip" , serviceInterfacePoint);
        DBObject valuesWithSet1 = new BasicDBObject();
        valuesWithSet1.put("$push", updateDocument1);
        MongoClient mongo1 = getMongoClient();
        DB database1 = mongo1.getDB(DB_NAME);
        DBCollection topo1 = database1.getCollection("sip-context");
        topo1.update(inQuery1, valuesWithSet1);
    }

    private void deleteSip(String sipId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("sip-context");
        BasicDBObject match = new BasicDBObject("sip-context.sip.uuid",sipId);
        BasicDBObject update = new BasicDBObject("sip-context.sip", new BasicDBObject("uuid", sipId));
        topo.update(match, new BasicDBObject("$pull", update));
    }

    @Override
    public void deleteSips(String nodeId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        DBCursor myCursor = topo.find(new BasicDBObject("topology.0.uuid", "optical-topo"));
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("topology");
            BasicDBObject topology = (BasicDBObject) topologyList.get(0);
            BasicDBList nodes = (BasicDBList) topology.get("service-interface-point");
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) nodes.get(i);
                if (nodetem.get("uuid").toString().contains(nodeId)) {
                    deleteSip(nodetem.get("uuid").toString());
                }
            }
        }
    }


    @Override
    public void deleteService(String serviceId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        BasicDBObject match = new BasicDBObject("connectivity-service.uuid",serviceId); // to match your document
        BasicDBObject update = new BasicDBObject("connectivity-service", new BasicDBObject("uuid", serviceId));
        topo.update(match, new BasicDBObject("$pull", update));
    }

    @Override
    public List<Node> getTapiNodes(String topoId) {
        List<Node> nodeList = new ArrayList<>();
        BasicDBList nodes = getNodes();
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject node = (BasicDBObject) nodes.get(i);
                NodeBuilder nodeBuilder = new NodeBuilder();
                //uuid
                nodeBuilder.setUuid(new Uuid(node.get("uuid").toString()));
                List<Name> names = new ArrayList<>();
                //name
                BasicDBList nameList = (BasicDBList) node.get("name");
                for (Object name:nameList) {
                    BasicDBObject nameAux = (BasicDBObject) name;
                    names.add(new NameBuilder().setValueName(nameAux.getString("value-name"))
                            .setValue(nameAux.getString("value")).build());
                }
                nodeBuilder.setName(names);
                /*
                names.add(new NameBuilder().setValueName("node-type")
                        .setValue(((BasicDBObject) ((BasicDBList) node.get("name"))
                                .get(0)).get("value").toString()).build());

                 */
                //layer protocol name
                List<LayerProtocolName> layerProtocolNames = new ArrayList<>();
                BasicDBList layerName = (BasicDBList) node.get("layer-protocol-name");
                for (Object layer:layerName) {
                    String layName = layer.toString();
                    if (layName.equals("PHOTONIC_MEDIA")) {
                        layerProtocolNames.add(LayerProtocolName.PHOTONICMEDIA);
                    } else if (layName.equals("ETH")) {
                        layerProtocolNames.add(LayerProtocolName.ETH);
                    }
                }
                nodeBuilder.setLayerProtocolName(layerProtocolNames);
                /*
                String layerProtocolName = (String) ((BasicDBList) node.get("layer-protocol-name")).get(0);
                if (layerProtocolName.equals("PHOTONIC_MEDIA")) {
                    layerProtocolNames.add(LayerProtocolName.PHOTONICMEDIA);
                } else if (layerProtocolName.equals("ETH")) {
                    layerProtocolNames.add(LayerProtocolName.ETH);
                }
                nodeBuilder.setLayerProtocolName(layerProtocolNames);
                */
                // Owned Node-edge points

                //OperationalState
                if ((node.get("operational-state")).toString().equals("ENABLED")) {
                    nodeBuilder.setOperationalState(OperationalState.ENABLED);
                } else {
                    nodeBuilder.setOperationalState(OperationalState.DISABLED);
                }
                //AdministrativeState
                nodeBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);

                //LifecycleState
                nodeBuilder.setLifecycleState(LifecycleState.INSTALLED);

                // Owned node edge point
                BasicDBList neps = (BasicDBList) node.get("owned-node-edge-point");
                List<OwnedNodeEdgePoint> ownedNodeEdgePoints =  new ArrayList<>();
                for (int j = 0; j < neps.size(); j++) {
                    BasicDBObject nep = (BasicDBObject) neps.get(j);
                    OwnedNodeEdgePointBuilder ownedNodeEdgePointBuilder = new OwnedNodeEdgePointBuilder();
                    ownedNodeEdgePointBuilder.setUuid(new Uuid(nep.get("uuid").toString()));
                    List<Name> nameNeps = new ArrayList<>();
                    BasicDBList namesList = (BasicDBList) nep.get("name");
                    for (Object name:namesList) {
                        BasicDBObject nameAux = (BasicDBObject) name;
                        nameNeps.add(new NameBuilder().setValueName(nameAux.getString("value-name"))
                                .setValue(nameAux.getString("value")).build());
                    }
                    ownedNodeEdgePointBuilder.setName(nameNeps);
                    //layer protocol name
                    BasicDBList protoName = (BasicDBList) nep.get("layer-protocol-name");
                    for (Object layerProtocolName:protoName) {
                        if (layerProtocolName.toString().equals("PHOTONIC_MEDIA")) {
                            ownedNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
                        } else if (layerProtocolName.toString().equals("ETH")) {
                            ownedNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.ETH);
                        }
                    }

                    // mapped sip
                    List<MappedServiceInterfacePoint> mappedServiceInterfacePointList = new ArrayList<>();
                    BasicDBList msip = (BasicDBList) nep.get("mapped-service-interface-point");
                    if (msip != null) {
                        for (Object msp:msip) {
                            BasicDBObject mspAux = (BasicDBObject) msp;
                            mappedServiceInterfacePointList.add(new MappedServiceInterfacePointBuilder()
                                    .setServiceInterfacePointUuid(new Uuid(mspAux
                                            .getString("service-interface-point-uuid"))).build());
                        }
                    }
                    ownedNodeEdgePointBuilder.setMappedServiceInterfacePoint(mappedServiceInterfacePointList);
                    //
                    ownedNodeEdgePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
                    if ((nep.get("operational-state")).toString().equals("ENABLED")) {
                        ownedNodeEdgePointBuilder.setOperationalState(OperationalState.ENABLED);
                    } else {
                        ownedNodeEdgePointBuilder.setOperationalState(OperationalState.DISABLED);
                    }
                    ownedNodeEdgePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
                    ownedNodeEdgePointBuilder.setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
                    //termination-direction
                    if ((nep.get("termination-direction")).toString().equals("BIDIRECTIONAL")) {
                        ownedNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.BIDIRECTIONAL);
                    } else if ((nep.get("termination-direction")).toString().equals("SINK")) {
                        ownedNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.SINK);
                    } else if ((nep.get("termination-direction")).toString().equals("SOURCE")) {
                        ownedNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.SOURCE);
                    } else {
                        ownedNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.UNDEFINEDORUNKNOWN);
                    }
                    // port direction
                    if ((nep.get("link-port-direction")).toString().equals("BIDIRECTIONAL")) {
                        ownedNodeEdgePointBuilder.setLinkPortDirection(PortDirection.BIDIRECTIONAL);
                    } else if ((nep.get("termination-direction")).toString().equals("INPUT")) {
                        ownedNodeEdgePointBuilder.setLinkPortDirection(PortDirection.INPUT);
                    } else if ((nep.get("termination-direction")).toString().equals("OUTPUT")) {
                        ownedNodeEdgePointBuilder.setLinkPortDirection(PortDirection.OUTPUT);
                    } else {
                        ownedNodeEdgePointBuilder.setLinkPortDirection(PortDirection.UNIDENTIFIEDORUNKNOWN);
                    }

                    ownedNodeEdgePointBuilder.setLinkPortRole(PortRole.SYMMETRIC);

                    ownedNodeEdgePoints.add(ownedNodeEdgePointBuilder.build());
                }
                nodeBuilder.setOwnedNodeEdgePoint(ownedNodeEdgePoints);

                nodeList.add(nodeBuilder.build());
            }
        } else {
            return null;
        }
        return nodeList;
    }

    @Override
    public List<Link> getTapiLinks(String topoId) {
        List<Link> linkList = new ArrayList<>();
        BasicDBList links = getLinks();
        if (links != null) {
            for (int i = 0; i < links.size(); i++) {
                BasicDBObject link = (BasicDBObject) links.get(i);
                LinkBuilder linkBuilder = new LinkBuilder();
                //uuid
                linkBuilder.setUuid(new Uuid(link.get("uuid").toString()));
                //name
                List<Name> names = new ArrayList<>();
                BasicDBList nameList = (BasicDBList) link.get("name");
                for (Object name:nameList) {
                    BasicDBObject nameAux = (BasicDBObject) name;
                    names.add(new NameBuilder().setValueName(nameAux.getString("value-name"))
                            .setValue(nameAux.getString("value")).build());
                }
                linkBuilder.setName(names);
                //
                linkBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);

                if ((link.get("operational-state")).toString().equals("ENABLED")) {
                    linkBuilder.setOperationalState(OperationalState.ENABLED);
                } else {
                    linkBuilder.setOperationalState(OperationalState.DISABLED);
                }

                List<LayerProtocolName> layerProtocolNames = new ArrayList<>();
                layerProtocolNames.add(LayerProtocolName.PHOTONICMEDIA);
                linkBuilder.setLayerProtocolName(layerProtocolNames);
                linkBuilder.setLifecycleState(LifecycleState.INSTALLED);
                linkBuilder.setDirection(ForwardingDirection.UNIDIRECTIONAL);
                ResilienceTypeBuilder resilienceType = new ResilienceTypeBuilder();
                resilienceType.setProtectionType(ProtectionType.NOPROTECTON);
                resilienceType.setRestorationPolicy(RestorationPolicy.NA);
                linkBuilder.setResilienceType(resilienceType.build());
                List<CostCharacteristic> costCharacteristics = new ArrayList<>();
                costCharacteristics.add(new CostCharacteristicBuilder().setCostName(((BasicDBObject)link
                        .get("cost-characteristic")).get("cost-name").toString())
                        .setCostValue(((BasicDBObject)link.get("cost-characteristic")).get("cost-value")
                                .toString()).build());
                linkBuilder.setCostCharacteristic(costCharacteristics);
                List<LatencyCharacteristic> latencyCharacteristics = new ArrayList<>();
                latencyCharacteristics.add(new LatencyCharacteristicBuilder().setTrafficPropertyName("FixedLatency")
                        .setFixedLatencyCharacteristic("1").build());
                linkBuilder.setLatencyCharacteristic(latencyCharacteristics);
                List<NodeEdgePoint> nodeEdgePoints = new ArrayList<>();

                nodeEdgePoints.add(0, new NodeEdgePointBuilder()
                        .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()))
                        .setNodeUuid(new Uuid(((BasicDBObject)((BasicDBList)link.get("node-edge-point"))
                                .get(0)).get("node-uuid").toString()))
                        .setNodeEdgePointUuid(new Uuid(((BasicDBObject)((BasicDBList)link.get("node-edge-point"))
                                .get(0)).get("node-edge-point-uuid").toString())).build());

                nodeEdgePoints.add(1, new NodeEdgePointBuilder()
                        .setTopologyUuid(new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()))
                        .setNodeUuid(new Uuid(((BasicDBObject)((BasicDBList)link.get("node-edge-point")).get(1))
                                .get("node-uuid").toString()))
                        .setNodeEdgePointUuid(new Uuid(((BasicDBObject)((BasicDBList)link.get("node-edge-point"))
                                .get(1)).get("node-edge-point-uuid").toString())).build());

                linkBuilder.setNodeEdgePoint(nodeEdgePoints);

                linkList.add(linkBuilder.build());
            }
        } else {
            return null;
        }
        return linkList;
    }

    private BasicDBList getNodes() {
        BasicDBList nodeList = new BasicDBList();
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        DBCursor myCursor = topo.find(new BasicDBObject("topology.0.uuid",
                new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()).toString()));
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("topology");
            BasicDBObject topology = (BasicDBObject) topologyList.get(0);
            BasicDBList nodes = (BasicDBList) topology.get("node");
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) nodes.get(i);
                nodeList.add(nodetem);
            }
        }
        return nodeList;
    }

    private BasicDBList getLinks() {
        BasicDBList linkList = new BasicDBList();
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        DBCursor myCursor = topo.find(new BasicDBObject("topology.0.uuid",
                new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()).toString()));
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("topology");
            BasicDBObject topology = (BasicDBObject) topologyList.get(0);
            BasicDBList nodes = (BasicDBList) topology.get("link");
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) nodes.get(i);
                linkList.add(nodetem);
            }
        }
        return linkList;
    }

    @Override
    public void addService(Service tapiService) {
        BasicDBObject service = new BasicDBObject();
        //uuid
        service.append("uuid", tapiService.getUuid().getValue().toString());
        //nameList
        Document sip1 = new Document();
        sip1.append("local-id", tapiService.getEndPoint().get(0).getLocalId());
        sip1.append("service-interface-point",new Document().append("service-interface-point-uuid", tapiService
                .getEndPoint().get(0).getServiceInterfacePoint().getServiceInterfacePointUuid().getValue().toString()));
        sip1.append("layer-protocol-name", tapiService.getEndPoint().get(0).getLayerProtocolName().getName());
        sip1.append("direction", tapiService.getEndPoint().get(0).getDirection().getName());
        sip1.append("role", tapiService.getEndPoint().get(0).getRole().getName());
        sip1.append("protection-role", tapiService.getEndPoint().get(0).getProtectionRole().getName());
        sip1.append("administrative-state", tapiService.getEndPoint().get(0).getAdministrativeState().getName());
        sip1.append("operational-state", tapiService.getEndPoint().get(0).getOperationalState().getName());

        Document sip2 = new Document();
        sip2.append("local-id", tapiService.getEndPoint().get(1).getLocalId());
        sip2.append("service-interface-point",new Document().append("service-interface-point-uuid",tapiService
                .getEndPoint().get(1).getServiceInterfacePoint().getServiceInterfacePointUuid().getValue().toString()));
        sip2.append("layer-protocol-name", tapiService.getEndPoint().get(1).getLayerProtocolName().getName());
        sip2.append("direction", tapiService.getEndPoint().get(1).getDirection().getName());
        sip2.append("role", tapiService.getEndPoint().get(1).getRole().getName());
        sip2.append("protection-role", tapiService.getEndPoint().get(1).getProtectionRole().getName());
        sip2.append("administrative-state", tapiService.getEndPoint().get(1).getAdministrativeState().getName());
        sip2.append("operational-state", tapiService.getEndPoint().get(1).getOperationalState().getName());
        BasicDBList enPointList = new BasicDBList();
        enPointList.add(sip1);
        enPointList.add(sip2);
        service.append("end-point", enPointList);
        service.append("service-layer", tapiService.getServiceLayer().getName());
        service.append("administrative-state",tapiService.getAdministrativeState().getName());
        service.append("operational-state",tapiService.getOperationalState().getName());
        service.append("lifecycle-state",tapiService.getLifecycleState().getName());
        service.append("connectivity-direction", tapiService.getConnectivityDirection().getName());

        //name List
        BasicDBList nameList = new BasicDBList();
        for (Name name1:tapiService.getName()) {
            Document name = new Document();
            name.append("value-name", name1.getValueName());
            name.append("value", name1.getValue());
            nameList.add(name);
        }
        service.append("name", nameList);
        //connection list
        BasicDBList connectionList = new BasicDBList();
        /*
        for (Connection connection:tapiService.getConnection()) {
            Document connect = new Document();
            connect.append("connection-uuid", connection.getConnectionUuid().getValue());
            connectionList.add(connect);
        }

         */
        service.append("connection",connectionList);

        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put("_id", new Uuid(UUID.nameUUIDFromBytes("connectivity-context".getBytes())
                .toString()).toString());
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put("connectivity-service" , service);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        topo.update(inQuery, valuesWithSet);

    }

    @Override
    public void addConnectiontoService(String serviceUuid, Connection connection) {
        BasicDBObject service = getService(serviceUuid);
        BasicDBList connectionList = (BasicDBList) service.get("connection");
        Document connect = new Document();
        connect.append("connection-uuid", connection.getConnectionUuid().getValue());
        connectionList.add(connect);
        BasicDBObject updateFields = new BasicDBObject();
        String pathObject1 = "connectivity-service.$.";
        updateFields.append(pathObject1 + "connection", connectionList);
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        String pathObject = "connectivity-service.uuid";
        BasicDBObject searchQuery = new BasicDBObject(pathObject, serviceUuid);
        topo.update(searchQuery, setQuery);
    }

    @Override
    public void updateServiceState(String serviceUuid, LifecycleState lifecycleState,
                                   AdministrativeState administrativeState, OperationalState operationalState) {
        BasicDBObject updateFields = new BasicDBObject();
        String pathObject1 = "connectivity-service.$.";
        updateFields.append(pathObject1 + "lifecycle-state", lifecycleState.getName());
        updateFields.append(pathObject1 + "administrative-state", administrativeState.getName());
        updateFields.append(pathObject1 + "operational-state", operationalState.getName());
        BasicDBObject setQuery = new BasicDBObject();
        setQuery.append("$set", updateFields);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        String pathObject = "connectivity-service.uuid";
        BasicDBObject searchQuery = new BasicDBObject(pathObject, serviceUuid);
        topo.update(searchQuery, setQuery);
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity
            .service.list.output.Service> getTapiServices(String topoId) {
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity
                .service.list.output.Service> serviceList =  new ArrayList<>();
        BasicDBList services = getServices();
        if (services != null) {
            for (int i = 0; i < services.size(); i++) {
                BasicDBObject serv = (BasicDBObject) services.get(i);
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity
                        .service.list.output.ServiceBuilder serviceBuilder = new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service.list.output
                        .ServiceBuilder();
                serviceBuilder.setUuid(new Uuid(serv.get("uuid").toString()));
                serviceBuilder.setConnectivityDirection(ForwardingDirection.forName(serv
                        .getString("connectivity-direction")).get());
                serviceBuilder.setLifecycleState(LifecycleState.forName(serv.getString("lifecycle-state")).get());
                serviceBuilder.setOperationalState(OperationalState.forName(serv.getString("operational-state"))
                        .get());
                serviceBuilder.setAdministrativeState(AdministrativeState.forName(serv
                        .getString("administrative-state")).get());
                //name
                List<Name> names = new ArrayList<>();
                BasicDBList nameList = (BasicDBList) serv.get("name");
                for (Object name:nameList) {
                    BasicDBObject nameAux = (BasicDBObject) name;
                    names.add(new NameBuilder().setValueName(nameAux.getString("value-name"))
                            .setValue(nameAux.getString("value")).build());
                }
                serviceBuilder.setName(names);
                //connections
                List<Connection> connections = new ArrayList<>();
                BasicDBList connList = (BasicDBList) serv.get("connection");
                if (connList != null) {
                    for (Object conn:connList) {
                        BasicDBObject connAux = (BasicDBObject) conn;
                        connections.add(new ConnectionBuilder().setConnectionUuid(new Uuid(connAux
                                .getString("connection-uuid"))).build());
                    }
                }
                serviceBuilder.setConnection(connections);
                //endpoints
                List<EndPoint> endPointList = new ArrayList<>();
                BasicDBList enPoList = (BasicDBList) serv.get("end-point");
                for (Object endpoint:enPoList) {
                    BasicDBObject endpointAux = (BasicDBObject) endpoint;
                    EndPointBuilder endPointBuilder = new EndPointBuilder();
                    endPointBuilder.setAdministrativeState(AdministrativeState.forName(endpointAux
                            .getString("administrative-state")).get());
                    endPointBuilder.setDirection(PortDirection.forName(endpointAux.getString("direction")).get());
                    endPointBuilder.setLayerProtocolName(LayerProtocolName.forName(endpointAux
                            .getString("layer-protocol-name")).get());
                    endPointBuilder.setLocalId(endpointAux.getString("local-id"));
                    endPointBuilder.setRole(PortRole.forName(endpointAux.getString("role")).get());
                    endPointBuilder.setProtectionRole(ProtectionRole.forName(endpointAux
                            .getString("protection-role")).get());
                    endPointBuilder.setOperationalState(OperationalState.forName(endpointAux
                            .getString("operational-state")).get());
                    BasicDBObject sip = (BasicDBObject) endpointAux.get("service-interface-point");
                    endPointBuilder.setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                            .setServiceInterfacePointUuid(new Uuid(sip.getString("service-interface-point-uuid")))
                            .build());
                    endPointList.add(endPointBuilder.build());
                }
                serviceBuilder.setEndPoint(endPointList);
                serviceBuilder.setServiceLayer(LayerProtocolName.forName(serv.getString("service-layer")).get());
                serviceList.add(serviceBuilder.build());
            }
        } else {
            return null;
        }
        return serviceList;
    }

    @Override
    public BasicDBObject getService(String serviceId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        DBCursor myCursor = topo.find();
        BasicDBObject service = new BasicDBObject();
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("connectivity-service");
            for (int i = 0; i < topologyList.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) topologyList.get(i);
                if (nodetem.get("uuid").equals(serviceId)) {
                    service = nodetem;
                }
            }
        }
        return service;
    }

    private BasicDBList getServices() {
        BasicDBList servList = new BasicDBList();
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection services = database.getCollection("connectivity-context");
        DBCursor myCursor = services.find();
        while (myCursor.hasNext()) {
            BasicDBList listServices = (BasicDBList) myCursor.next().get("connectivity-service");
            for (int i = 0; i < listServices.size(); i++) {
                BasicDBObject serv = (BasicDBObject) listServices.get(i);
                servList.add(serv);
            }
        }
        return servList;
    }

    private BasicDBList getSips() {
        BasicDBList sipList = new BasicDBList();
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection sip = database.getCollection("sip-context");
        DBCursor myCursor = sip.find();
        while (myCursor.hasNext()) {
            BasicDBList listSip = (BasicDBList) myCursor.next().get("sip");
            for (int i = 0; i < listSip.size(); i++) {
                BasicDBObject sipNode = (BasicDBObject) listSip.get(i);
                sipList.add(sipNode);
            }
        }
        return sipList;
    }

    @Override
    public String getNepfromSip(String sipId) {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        DBCursor myCursor = topo.find(new BasicDBObject("topology.0.uuid",
                new Uuid(UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString()).toString()));
        BasicDBObject sip = new BasicDBObject();
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("topology");
            BasicDBObject topology = (BasicDBObject) topologyList.get(0);
            BasicDBList nodes = (BasicDBList) topology.get("service-interface-point");
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) nodes.get(i);
                if (((String) nodetem.get("uuid")).equals(sipId)) {
                    sip = nodetem;
                }
            }
        }
        String endPoint = ((BasicDBObject) ((BasicDBList) sip.get("name")).get(0)).get("value")
                .toString() + "_" + ((BasicDBObject) ((BasicDBList) sip.get("name")).get(1)).get("value").toString();
        return endPoint;
    }

    public List<Sip> getTapiSips() {
        List<Sip> sips =  new ArrayList<>();
        BasicDBList sipList = getSips();
        if (sipList != null) {
            for (int i = 0; i < sipList.size(); i++) {
                BasicDBObject sip = (BasicDBObject) sipList.get(i);
                SipBuilder sipBuilder = new SipBuilder();
                sipBuilder.setUuid(new Uuid(sip.get("uuid").toString()));
                //name
                List<Name> nameNeps = new ArrayList<>();
                BasicDBList namesList = (BasicDBList) sip.get("name");
                for (Object name:namesList) {
                    BasicDBObject nameAux = (BasicDBObject) name;
                    nameNeps.add(new NameBuilder().setValueName(nameAux.getString("value-name"))
                            .setValue(nameAux.getString("value")).build());
                }
                sipBuilder.setName(nameNeps);
                //layer protocol name
                BasicDBList protoName = (BasicDBList) sip.get("layer-protocol-name");
                for (Object layerProtocolName:protoName) {
                    if (layerProtocolName.toString().equals("PHOTONIC_MEDIA")) {
                        sipBuilder.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
                    } else if (layerProtocolName.toString().equals("ETH")) {
                        sipBuilder.setLayerProtocolName(LayerProtocolName.ETH);
                    }
                }

                sipBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
                if ((sip.get("operational-state")).toString().equals("ENABLED")) {
                    sipBuilder.setOperationalState(OperationalState.ENABLED);
                } else {
                    sipBuilder.setOperationalState(OperationalState.DISABLED);
                }
                sipBuilder.setLifecycleState(LifecycleState.INSTALLED);

                sips.add(sipBuilder.build());
            }
        } else {
            return null;
        }
        return sips;
    }

    @Override
    public Long allocateWavelengthforService() {
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        DBCursor myCursor = topo.find();
        Long wlengh = 4L;
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("connectivity-service");
            for (int i = 0; i < topologyList.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) topologyList.get(i);
                Long wavelength = Long.valueOf((String) (((BasicDBObject)((BasicDBList)nodetem
                        .get("resource")).get(0)).get("wavelength")));
                if (wavelength > wlengh) {
                    wlengh = wavelength;
                }
            }
        }
        return wlengh + 1L;
    }

    @Override
    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.details
            .output.Sip getSipDetails(String sipId) {
        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.details
                .output.SipBuilder sipDetail = new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common
                .rev181210.get.service._interface.point.details.output.SipBuilder();
        BasicDBList sipList = getSips();
        if (sipList != null) {
            for (int i = 0; i < sipList.size(); i++) {
                BasicDBObject sip = (BasicDBObject) sipList.get(i);
                if (sip.get("uuid").toString().equals(sipId)) {
                    sipDetail.setUuid(new Uuid(sip.get("uuid").toString()));
                    //name
                    List<Name> names = new ArrayList<>();
                    names.add(new NameBuilder().setValueName(((BasicDBObject) ((BasicDBList) sip.get("name"))
                            .get(0)).get("value-name").toString())
                            .setValue(((BasicDBObject) ((BasicDBList) sip.get("name")).get(0)).get("value")
                                    .toString()).build());
                    names.add(new NameBuilder().setValueName(((BasicDBObject) ((BasicDBList) sip.get("name"))
                            .get(1)).get("value-name").toString())
                            .setValue(((BasicDBObject) ((BasicDBList) sip.get("name")).get(1)).get("value")
                                    .toString()).build());
                    sipDetail.setName(names);

                    String layerProtocolName = (String) ((BasicDBList) sip.get("layer-protocol-name")).get(0);
                    if (layerProtocolName.equals("PHOTONIC_MEDIA")) {
                        sipDetail.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
                    } else if (layerProtocolName.equals("DSR")) {
                        sipDetail.setLayerProtocolName(LayerProtocolName.DSR);
                    }

                    sipDetail.setAdministrativeState(AdministrativeState.UNLOCKED);
                    if ((sip.get("operational-state")).toString().equals("ENABLED")) {
                        sipDetail.setOperationalState(OperationalState.ENABLED);
                    } else {
                        sipDetail.setOperationalState(OperationalState.DISABLED);
                    }
                    sipDetail.setLifecycleState(LifecycleState.INSTALLED);
                }
            }
        } else {
            return null;
        }

        return sipDetail.build();
    }

    public String getNepType(String nodeId, String nepId) {
        String nepType = null;
        BasicDBList nodes = getNodes();
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                BasicDBObject node = (BasicDBObject) nodes.get(i);
                if (node.get("uuid").toString().equals(nodeId)) {
                    BasicDBList neps = (BasicDBList) node.get("owned-node-edge-point");
                    for (int j = 0; j < neps.size(); j++) {
                        BasicDBObject nep = (BasicDBObject) neps.get(j);
                        if (nep.get("uuid").toString().equals(nepId)) {
                            String valueName =  ((BasicDBObject) ((BasicDBList) nep.get("name")).get(1))
                                    .get("value-name").toString();
                            String name =  ((BasicDBObject) ((BasicDBList) nep.get("name")).get(1)).get("value")
                                    .toString();
                            if (valueName.equals("supporting-port")) {
                                if (name.contains("TRANSCEIVER")) {
                                    nepType = "CLIENT";
                                } else if (name.contains("OCH")) {
                                    nepType = "LINE";
                                }
                            } else {
                                nepType = valueName;
                            }
                        }
                    }
                }
            }

        }
        return nepType;
    }

    private String getServiceIndex(String serviceId) {
        String serviceIndex = new String();
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        DBCursor myCursor = topo.find();
        while (myCursor.hasNext()) {
            BasicDBList topologyList = (BasicDBList) myCursor.next().get("connectivity-service");
            for (int i = 0; i < topologyList.size(); i++) {
                BasicDBObject nodetem = (BasicDBObject) topologyList.get(i);
                if (nodetem.get("uuid").equals(serviceId)) {
                    serviceIndex = String.valueOf(i);
                }
            }
        }
        return serviceIndex;
    }

    @Override
    public void addPathToService(DBObject path, String serviceId) {
        String pathObject = "connectivity-service." + getServiceIndex(serviceId) + ".uuid";
        String pathObject1 = "connectivity-service." + getServiceIndex(serviceId) + ".path";
        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put(pathObject,serviceId);
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put(pathObject1 , path);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        topo.update(inQuery, valuesWithSet);
    }

    @Override
    public void addResourceToService(String wavelengthNumber, String serviceId) {
        String pathObject = "connectivity-service." + getServiceIndex(serviceId) + ".uuid";
        String pathObject1 = "connectivity-service." + getServiceIndex(serviceId) + ".resource";
        BasicDBObject resource = new BasicDBObject();
        resource.append("wavelength", wavelengthNumber);
        BasicDBObject inQuery = new BasicDBObject();
        inQuery.put(pathObject,serviceId);
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put(pathObject1 , resource);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$push", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("connectivity-context");
        topo.update(inQuery, valuesWithSet);
    }


    @Override
    public void updateLinkOperationalState(String linkId, String state) {

        BasicDBObject match = new BasicDBObject("topology.0.link.uuid",linkId);
        BasicDBObject updateDocument = new BasicDBObject();
        updateDocument.put("topology.0.link.$.operational-state" , state);
        DBObject valuesWithSet = new BasicDBObject();
        valuesWithSet.put("$set", updateDocument);
        MongoClient mongo = getMongoClient();
        DB database = mongo.getDB(DB_NAME);
        DBCollection topo = database.getCollection("topology-context");
        topo.update(match, valuesWithSet);
    }

    @Override
    public void deleteLinksOfNode(String nodeId) {
        BasicDBList links = getLinks();
        if (links != null) {
            for (int i = 0; i < links.size(); i++) {
                BasicDBObject link = (BasicDBObject) links.get(i);
                if (link.get("uuid").toString().contains(nodeId)) {
                    deleteLink(link.get("uuid").toString());
                }
            }
        }
    }
}
