package org.opendaylight.transportpce.tapi.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200128.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200128.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200128.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TerminationState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.edge.point.MappedServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiTopoModelServiceImpl implements TapiTopoModelService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiTopoModelServiceImpl.class);
    private final DataBroker dataBroker;
    private final PortMapping portMapping;
    private final DeviceTransactionManager deviceTransactionManager;
    MongoDbDataStoreService mongoDbDataStoreService;

    public TapiTopoModelServiceImpl(DataBroker dataBroker, PortMapping portMapping,
                                    MongoDbDataStoreService mongoDbDataStoreService,
                                    DeviceTransactionManager deviceTransactionManager) {
        this.dataBroker = dataBroker;
        this.portMapping = portMapping;
        this.mongoDbDataStoreService = mongoDbDataStoreService;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    @Override
    public void createTapiNode(String nodeId, String nodeVersion) {
        LOG.info("createTAPINode: {} ", nodeId);
        while (!portMapping.isPortMappingDone(nodeId, nodeVersion)) {
            // Sleep --> so that the port mapping is created for the node
            LOG.warn("Port mapping of node {} still not created by network model service", nodeId);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error("Error: {}", e.toString());
            }
        }
        portMapping.setportMappingDone(false, nodeVersion);
        Nodes nodes = portMapping.getNode(nodeId);
        NodeInfo nodeInfo = portMapping.getNode(nodeId).getNodeInfo();
        if (NodeTypes.Rdm.getIntValue() == nodeInfo.getNodeType().getIntValue()) {
            LOG.info("ROADM detected... creating corresponding TAPI node");
            createRoadmNode(nodes, nodeId);
        } else if (NodeTypes.Xpdr.getIntValue() ==  nodeInfo.getNodeType().getIntValue()) {
            LOG.info("XPDR detected... creating corresponding TAPI node");
            createTransponderNode(nodes, nodeId);
        } else {
            LOG.error("Type of node {} not identified", nodeInfo.getNodeType().toString());
        }
    }

    private void createTransponderNode(Nodes nodes, String nodeId) {
        LOG.info("Mapping of xpdr node: {}", nodes.getMapping().toString());
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("node-type").setValue("XPDR").build());
        names.add(new NameBuilder().setValueName("OR-node-id").setValue(nodeId).build());
        List<LayerProtocolName> layerProtocolNames = new ArrayList<>();
        layerProtocolNames.add(LayerProtocolName.PHOTONICMEDIA);
        layerProtocolNames.add(LayerProtocolName.ETH);
        List<OwnedNodeEdgePoint> ownedNodeEdgePointList = getXpdrNEPList(nodes, nodeId);
        Node node = new NodeBuilder().setUuid(new Uuid(UUID.nameUUIDFromBytes(nodeId.getBytes()).toString()))
                .setName(names)
                .setLayerProtocolName(layerProtocolNames)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(ownedNodeEdgePointList)
                .build();

        this.mongoDbDataStoreService.addNode(node);
    }

    private void createRoadmNode(Nodes nodes, String nodeId) {
        LOG.info("Mapping of roadm node: {}", nodes.getMapping().toString());
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("node-type").setValue("ROADM").build());
        names.add(new NameBuilder().setValueName("OR-node-id").setValue(nodeId).build());
        List<LayerProtocolName> layerProtocolNames = new ArrayList<>();
        layerProtocolNames.add(LayerProtocolName.PHOTONICMEDIA);
        List<OwnedNodeEdgePoint> ownedNodeEdgePointList = getRoadmNEPList(nodes, nodeId);
        Node node = new NodeBuilder().setUuid(new Uuid(UUID.nameUUIDFromBytes(nodeId.getBytes()).toString()))
                .setName(names)
                .setLayerProtocolName(layerProtocolNames)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setOperationalState(OperationalState.ENABLED)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOwnedNodeEdgePoint(ownedNodeEdgePointList)
                .build();
        this.mongoDbDataStoreService.addNode(node);
        // Todo -> check LLDP to see neighbors and create rdmTOrdm links
    }

    private List<OwnedNodeEdgePoint> getXpdrNEPList(Nodes nodes, String nodeId) {
        List<OwnedNodeEdgePoint> ownedNodeEdgePointList = new ArrayList<>();
        for (Mapping mapping:nodes.getMapping()) {
            if (mapping.getLogicalConnectionPoint() != null) {
                String lcp = mapping.getLogicalConnectionPoint();
                if (lcp.contains("CLIENT")) {
                    LOG.info("CLIENT port found. Creating corresponding owned NEP");
                    ownedNodeEdgePointList.add(createClientONEP(mapping, nodeId));
                }
                if (lcp.contains("NETWORK")) {
                    LOG.info("NETWORK port found. Creating corresponding owned NEP");
                    ownedNodeEdgePointList.add(createNetworkONEP(mapping, nodeId));
                }
            }
        }
        // Todo -> check lcp mapping point between client and network ports to create corresponding internal
        //  xponder links
        return ownedNodeEdgePointList;
    }


    private List<OwnedNodeEdgePoint> getRoadmNEPList(Nodes nodes, String nodeId) {
        List<OwnedNodeEdgePoint> ownedNodeEdgePointList = new ArrayList<>();
        for (Mapping mapping:nodes.getMapping()) {
            if (mapping.getLogicalConnectionPoint() != null) {
                String lcp = mapping.getLogicalConnectionPoint();
                if (lcp.contains("DEG") && lcp.contains("TTP")) {
                    LOG.info("Degree line port found. Creating corresponding owned NEP");
                    ownedNodeEdgePointList.add(createDegONEP(mapping, nodeId));
                }
                if (lcp.contains("SRG") && lcp.contains("PP")) {
                    LOG.info("SRG add/drop port found. Creating corresponding owned NEP");
                    ownedNodeEdgePointList.add(createAddDropONEP(mapping, nodeId));
                }
            }
        }
        return ownedNodeEdgePointList;
    }

    private OwnedNodeEdgePoint createAddDropONEP(Mapping mapping, String nodeId) {
        // Need to create SIP for these ONEP
        OwnedNodeEdgePointBuilder ownedDegNodeEdgePointBuilder = new OwnedNodeEdgePointBuilder();
        ownedDegNodeEdgePointBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes((nodeId + "-" + mapping
                .getLogicalConnectionPoint()).getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("ADD/DROP").setValue(mapping.getLogicalConnectionPoint()).build());
        ownedDegNodeEdgePointBuilder.setName(names);
        ServiceInterfacePoint sip = createSip(nodeId, mapping, LayerProtocolName.PHOTONICMEDIA,
                OperationalState.ENABLED);
        this.mongoDbDataStoreService.addSip(sip);
        List<MappedServiceInterfacePoint> mappedServiceInterfacePoints = new ArrayList<>();
        mappedServiceInterfacePoints.add(new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sip.getUuid()).build());
        ownedDegNodeEdgePointBuilder.setMappedServiceInterfacePoint(mappedServiceInterfacePoints);
        ownedDegNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        ownedDegNodeEdgePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        ownedDegNodeEdgePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
        ownedDegNodeEdgePointBuilder.setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.BIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.ENABLED);
        ownedDegNodeEdgePointBuilder.setLinkPortRole(PortRole.UNKNOWN);
        ownedDegNodeEdgePointBuilder.setLinkPortDirection(PortDirection.BIDIRECTIONAL);
        return ownedDegNodeEdgePointBuilder.build();
    }

    private OwnedNodeEdgePoint createDegONEP(Mapping mapping, String nodeId) {
        OwnedNodeEdgePointBuilder ownedDegNodeEdgePointBuilder = new OwnedNodeEdgePointBuilder();
        ownedDegNodeEdgePointBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes((nodeId + "-" + mapping
                .getLogicalConnectionPoint()).getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("DEG").setValue(mapping.getLogicalConnectionPoint()).build());
        ownedDegNodeEdgePointBuilder.setName(names);
        ownedDegNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        ownedDegNodeEdgePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        ownedDegNodeEdgePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
        ownedDegNodeEdgePointBuilder.setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.BIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.ENABLED);
        ownedDegNodeEdgePointBuilder.setLinkPortRole(PortRole.UNKNOWN);
        ownedDegNodeEdgePointBuilder.setLinkPortDirection(PortDirection.BIDIRECTIONAL);
        return ownedDegNodeEdgePointBuilder.build();
    }

    private OwnedNodeEdgePoint createNetworkONEP(Mapping mapping, String nodeId) {
        // Need to create SIP for these ONEP
        OwnedNodeEdgePointBuilder ownedDegNodeEdgePointBuilder = new OwnedNodeEdgePointBuilder();
        if (mapping.getConnectionMapLcp() != null) {
            ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.ENABLED);
        } else {
            ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.DISABLED);
        }
        ownedDegNodeEdgePointBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes((nodeId + "-" + mapping
                .getLogicalConnectionPoint()).getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("NETWORK").setValue(mapping.getLogicalConnectionPoint()).build());
        names.add(new NameBuilder().setValueName("supporting-port").setValue(mapping.getSupportingPort()).build());
        ownedDegNodeEdgePointBuilder.setName(names);
        ServiceInterfacePoint sip = createSip(nodeId, mapping, LayerProtocolName.PHOTONICMEDIA,
                OperationalState.ENABLED);
        this.mongoDbDataStoreService.addSip(sip);
        List<MappedServiceInterfacePoint> mappedServiceInterfacePoints = new ArrayList<>();
        mappedServiceInterfacePoints.add(new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sip.getUuid()).build());
        ownedDegNodeEdgePointBuilder.setMappedServiceInterfacePoint(mappedServiceInterfacePoints);
        ownedDegNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.PHOTONICMEDIA);
        ownedDegNodeEdgePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        ownedDegNodeEdgePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
        ownedDegNodeEdgePointBuilder.setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.BIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setLinkPortRole(PortRole.SYMMETRIC);
        ownedDegNodeEdgePointBuilder.setLinkPortDirection(PortDirection.BIDIRECTIONAL);
        return ownedDegNodeEdgePointBuilder.build();
    }

    private OwnedNodeEdgePoint createClientONEP(Mapping mapping, String nodeId) {
        // Need to create SIP for these ONEP
        OwnedNodeEdgePointBuilder ownedDegNodeEdgePointBuilder = new OwnedNodeEdgePointBuilder();
        ServiceInterfacePoint sip;
        if (mapping.getConnectionMapLcp() != null) {
            ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.ENABLED);
            sip = createSip(nodeId, mapping, LayerProtocolName.ETH, OperationalState.ENABLED);
            this.mongoDbDataStoreService.addSip(sip);
        } else {
            ownedDegNodeEdgePointBuilder.setOperationalState(OperationalState.DISABLED);
            sip = createSip(nodeId, mapping, LayerProtocolName.ETH, OperationalState.DISABLED);
            this.mongoDbDataStoreService.addSip(sip);
        }
        ownedDegNodeEdgePointBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes((nodeId + "-" + mapping
                .getLogicalConnectionPoint()).getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("CLIENT").setValue(mapping.getLogicalConnectionPoint()).build());
        names.add(new NameBuilder().setValueName("supporting-port").setValue(mapping.getSupportingPort()).build());
        ownedDegNodeEdgePointBuilder.setName(names);
        List<MappedServiceInterfacePoint> mappedServiceInterfacePoints = new ArrayList<>();
        mappedServiceInterfacePoints.add(new MappedServiceInterfacePointBuilder()
                .setServiceInterfacePointUuid(sip.getUuid()).build());
        ownedDegNodeEdgePointBuilder.setMappedServiceInterfacePoint(mappedServiceInterfacePoints);
        ownedDegNodeEdgePointBuilder.setLayerProtocolName(LayerProtocolName.ETH);
        ownedDegNodeEdgePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        ownedDegNodeEdgePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
        ownedDegNodeEdgePointBuilder.setTerminationState(TerminationState.TERMINATEDBIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setTerminationDirection(TerminationDirection.BIDIRECTIONAL);
        ownedDegNodeEdgePointBuilder.setLinkPortRole(PortRole.SYMMETRIC);
        ownedDegNodeEdgePointBuilder.setLinkPortDirection(PortDirection.BIDIRECTIONAL);
        return ownedDegNodeEdgePointBuilder.build();
    }

    private ServiceInterfacePoint createSip(String nodeId, Mapping mapping, LayerProtocolName layerProtocolName,
                                            OperationalState operationalState) {

        ServiceInterfacePointBuilder serviceInterfacePointBuilder = new ServiceInterfacePointBuilder();
        serviceInterfacePointBuilder.setUuid(new Uuid(UUID
                .nameUUIDFromBytes((nodeId + "_SIP-" + mapping.getLogicalConnectionPoint()).getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("node").setValue(nodeId).build());
        names.add(new NameBuilder().setValueName("node-end-point").setValue(mapping.getLogicalConnectionPoint())
                .build());
        serviceInterfacePointBuilder.setName(names);
        serviceInterfacePointBuilder.setLayerProtocolName(layerProtocolName);
        serviceInterfacePointBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        serviceInterfacePointBuilder.setOperationalState(operationalState);
        serviceInterfacePointBuilder.setLifecycleState(LifecycleState.INSTALLED);
        return serviceInterfacePointBuilder.build();
    }

    @Override
    public void deleteTapinode(String nodeId) {
        // Todo --> delete tapi node from db
        this.mongoDbDataStoreService.deleteNode(UUID.nameUUIDFromBytes(nodeId.getBytes()).toString());
    }

    @Override
    public void deleteSips(String nodeId) {
        // Todo --> delete all sips of a node from db
    }

    @Override
    public void deleteLinks(String nodeId) {

    }

    @Override
    public void setTapiNodeStatus(String nodeId, NetconfNodeConnectionStatus.ConnectionStatus connectionStatus) {

    }
}
