package org.opendaylight.transportpce.tapi.topology;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.DeleteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitInternalRoadmLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitInternalRoadmLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitInternalXpdrLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitInternalXpdrLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitInternalXpdrLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRdmXpdrLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRdmXpdrLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRdmXpdrLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRoadmRoadmLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRoadmRoadmLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitRoadmRoadmLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitXpdrRdmLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitXpdrRdmLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitXpdrRdmLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitXpdrXpdrLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.InitXpdrXpdrLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.tapi.networkutils.rev190223.TapiNetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetworkUtils implements TapiNetworkutilsService {

    public static final String TRANSPONDER = "TRANSPONDER";
    public static final String ROADM = "ROADM";
    MongoDbDataStoreService mongoDbDataStoreService;
    private static final Logger LOG = LoggerFactory.getLogger(TapiNetworkUtils.class);

    public TapiNetworkUtils(MongoDbDataStoreService mongoDbDataStoreService) {
        this.mongoDbDataStoreService = mongoDbDataStoreService;
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmRoadmLinkOutput>> initRoadmRoadmLink(InitRoadmRoadmLinkInput input) {
        LOG.info("Creating ROADM-TO-ROADM link {}", input.toString());

        String linkId = input.getRdmANode() + "-" + input.getDegANep() + "-TO-" + input.getRdmZNode() + "-" + input
                .getDegZNep();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes(linkId.getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("link-type").setValue("ROADM-TO-ROADM").build());
        names.add(new NameBuilder().setValueName("link-id").setValue(linkId).build());
        linkBuilder.setName(names);
        linkBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        linkBuilder.setOperationalState(OperationalState.ENABLED);
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
        costCharacteristics.add(new CostCharacteristicBuilder().setCostName("hop-count").setCostValue("1").build());
        linkBuilder.setCostCharacteristic(costCharacteristics);
        List<LatencyCharacteristic> latencyCharacteristics = new ArrayList<>();
        latencyCharacteristics.add(new LatencyCharacteristicBuilder()
                .setTrafficPropertyName("FixedLatency").setFixedLatencyCharacteristic("1").build());
        linkBuilder.setLatencyCharacteristic(latencyCharacteristics);
        List<NodeEdgePoint> nodeEdgePoints = new ArrayList<>();
        String topoUuid = UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString();
        String nodeaUuid = UUID.nameUUIDFromBytes(input.getRdmANode().getBytes()).toString();
        String nepaUuid = UUID.nameUUIDFromBytes((input.getRdmANode() + "-" + input.getDegANep())
                .getBytes()).toString();
        String nodezUuid = UUID.nameUUIDFromBytes(input.getRdmZNode().getBytes()).toString();
        String nepzUuid = UUID.nameUUIDFromBytes((input.getRdmZNode() + "-" + input.getDegZNep())
                .getBytes()).toString();
        nodeEdgePoints.add(0,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodeaUuid)).setNodeEdgePointUuid(new Uuid(nepaUuid))
                .build());
        nodeEdgePoints.add(1,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodezUuid)).setNodeEdgePointUuid(new Uuid(nepzUuid))
                .build());
        linkBuilder.setNodeEdgePoint(nodeEdgePoints);
        this.mongoDbDataStoreService.addLink(linkBuilder.build());

        return RpcResultBuilder.success(new InitRoadmRoadmLinkOutputBuilder().setResult("OK")).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitInternalRoadmLinkOutput>> initInternalRoadmLink(
            InitInternalRoadmLinkInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<InitInternalXpdrLinkOutput>> initInternalXpdrLink(
            InitInternalXpdrLinkInput input) {
        LOG.info("Creating XPDR internal link {}", input.toString());
        String linkId = input.getXpdrNode() + "-" + input.getLineNep() + "-TO-" + input.getXpdrNode() + "-" + input
                .getClientNep();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes(linkId.getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("link-type").setValue("TRANSPONDER-INTERNAL").build());
        names.add(new NameBuilder().setValueName("link-id").setValue(linkId).build());
        linkBuilder.setName(names);
        linkBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        linkBuilder.setOperationalState(OperationalState.DISABLED);
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
        costCharacteristics.add(new CostCharacteristicBuilder().setCostName("hop-count").setCostValue("1").build());
        linkBuilder.setCostCharacteristic(costCharacteristics);
        List<LatencyCharacteristic> latencyCharacteristics = new ArrayList<>();
        latencyCharacteristics.add(new LatencyCharacteristicBuilder().setTrafficPropertyName("FixedLatency")
                .setFixedLatencyCharacteristic("1").build());
        linkBuilder.setLatencyCharacteristic(latencyCharacteristics);
        List<NodeEdgePoint> nodeEdgePoints = new ArrayList<>();
        String topoUuid = UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString();
        String nodeaUuid = UUID.nameUUIDFromBytes(input.getXpdrNode().getBytes()).toString();
        String nepaUuid = UUID.nameUUIDFromBytes((input.getXpdrNode() + "-" + input.getLineNep())
                .getBytes()).toString();
        String nodezUuid = UUID.nameUUIDFromBytes(input.getXpdrNode().getBytes()).toString();
        String nepzUuid = UUID.nameUUIDFromBytes((input.getXpdrNode() + "-" + input.getClientNep())
                .getBytes()).toString();
        nodeEdgePoints.add(0,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodeaUuid)).setNodeEdgePointUuid(new Uuid(nepaUuid))
                .build());
        nodeEdgePoints.add(1,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodezUuid)).setNodeEdgePointUuid(new Uuid(nepzUuid))
                .build());
        linkBuilder.setNodeEdgePoint(nodeEdgePoints);
        this.mongoDbDataStoreService.addLink(linkBuilder.build());

        String linkId1 = input.getXpdrNode() + "-" + input.getClientNep() + "-TO-" + input.getXpdrNode() + "-" + input
                .getLineNep();
        LinkBuilder linkBuilder1 = new LinkBuilder();
        linkBuilder1.setUuid(new Uuid(UUID.nameUUIDFromBytes(linkId1.getBytes()).toString()));
        List<Name> names1 = new ArrayList<>();
        names1.add(new NameBuilder().setValueName("link-type").setValue("TRANSPONDER-INTERNAL").build());
        names1.add(new NameBuilder().setValueName("link-id").setValue(linkId1).build());
        linkBuilder1.setName(names1);
        linkBuilder1.setAdministrativeState(AdministrativeState.UNLOCKED);
        linkBuilder1.setOperationalState(OperationalState.DISABLED);
        List<LayerProtocolName> layerProtocolNames1 = new ArrayList<>();
        layerProtocolNames1.add(LayerProtocolName.PHOTONICMEDIA);
        linkBuilder1.setLayerProtocolName(layerProtocolNames);
        linkBuilder1.setLifecycleState(LifecycleState.INSTALLED);
        linkBuilder1.setDirection(ForwardingDirection.UNIDIRECTIONAL);
        ResilienceTypeBuilder resilienceType1 = new ResilienceTypeBuilder();
        resilienceType1.setProtectionType(ProtectionType.NOPROTECTON);
        resilienceType1.setRestorationPolicy(RestorationPolicy.NA);
        linkBuilder1.setResilienceType(resilienceType.build());
        List<CostCharacteristic> costCharacteristics1 = new ArrayList<>();
        costCharacteristics1.add(new CostCharacteristicBuilder().setCostName("hop-count").setCostValue("1").build());
        linkBuilder1.setCostCharacteristic(costCharacteristics1);
        List<LatencyCharacteristic> latencyCharacteristics1 = new ArrayList<>();
        latencyCharacteristics1.add(new LatencyCharacteristicBuilder().setTrafficPropertyName("FixedLatency")
                .setFixedLatencyCharacteristic("1").build());
        linkBuilder1.setLatencyCharacteristic(latencyCharacteristics1);
        List<NodeEdgePoint> nodeEdgePoints1 = new ArrayList<>();
        nodeEdgePoints1.add(0,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodezUuid)).setNodeEdgePointUuid(new Uuid(nepzUuid))
                .build());
        nodeEdgePoints1.add(1,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodeaUuid)).setNodeEdgePointUuid(new Uuid(nepaUuid))
                .build());
        linkBuilder1.setNodeEdgePoint(nodeEdgePoints1);
        this.mongoDbDataStoreService.addLink(linkBuilder1.build());

        return RpcResultBuilder.success(new InitInternalXpdrLinkOutputBuilder().setResult("OK")).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmLinkOutput>> initXpdrRdmLink(InitXpdrRdmLinkInput input) {
        LOG.info("Creating TRANSPONDER-TO-ROADM link {}", input.toString());

        String linkId = input.getXpdrNode() + "-" + input.getLineNep() + "-TO-" + input.getRdmNode() + "-" + input
                .getAddDropNep();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes(linkId.getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("link-type").setValue("TRANSPONDER-TO-ROADM").build());
        names.add(new NameBuilder().setValueName("link-id").setValue(linkId).build());
        linkBuilder.setName(names);
        linkBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        linkBuilder.setOperationalState(OperationalState.ENABLED);
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
        costCharacteristics.add(new CostCharacteristicBuilder().setCostName("hop-count").setCostValue("1").build());
        linkBuilder.setCostCharacteristic(costCharacteristics);
        List<LatencyCharacteristic> latencyCharacteristics = new ArrayList<>();
        latencyCharacteristics.add(new LatencyCharacteristicBuilder().setTrafficPropertyName("FixedLatency")
                .setFixedLatencyCharacteristic("1").build());
        linkBuilder.setLatencyCharacteristic(latencyCharacteristics);
        List<NodeEdgePoint> nodeEdgePoints = new ArrayList<>();
        String topoUuid = UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString();
        String nodeaUuid = UUID.nameUUIDFromBytes(input.getXpdrNode().getBytes()).toString();
        String nepaUuid = UUID.nameUUIDFromBytes((input.getXpdrNode() + "-" + input.getLineNep())
                .getBytes()).toString();
        String nodezUuid = UUID.nameUUIDFromBytes(input.getRdmNode().getBytes()).toString();
        String nepzUuid = UUID.nameUUIDFromBytes((input.getRdmNode() + "-" + input.getAddDropNep())
                .getBytes()).toString();
        nodeEdgePoints.add(0,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodeaUuid)).setNodeEdgePointUuid(new Uuid(nepaUuid))
                .build());
        nodeEdgePoints.add(1,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodezUuid)).setNodeEdgePointUuid(new Uuid(nepzUuid))
                .build());
        linkBuilder.setNodeEdgePoint(nodeEdgePoints);
        this.mongoDbDataStoreService.addLink(linkBuilder.build());

        return RpcResultBuilder.success(new InitXpdrRdmLinkOutputBuilder().setResult("OK")).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrXpdrLinkOutput>> initXpdrXpdrLink(InitXpdrXpdrLinkInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<InitRdmXpdrLinkOutput>> initRdmXpdrLink(InitRdmXpdrLinkInput input) {
        LOG.info("Creating ROADM-TO-TRANSPONDER link {}", input.toString());

        String linkId = input.getRdmNode() + "-" + input.getAddDropNep() + "-TO-" + input.getXpdrNode() + "-" + input
                .getLineNep();
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setUuid(new Uuid(UUID.nameUUIDFromBytes(linkId.getBytes()).toString()));
        List<Name> names = new ArrayList<>();
        names.add(new NameBuilder().setValueName("link-type").setValue("ROADM-TO-TRANSPONDER").build());
        names.add(new NameBuilder().setValueName("link-id").setValue(linkId).build());
        linkBuilder.setName(names);
        linkBuilder.setAdministrativeState(AdministrativeState.UNLOCKED);
        linkBuilder.setOperationalState(OperationalState.ENABLED);
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
        costCharacteristics.add(new CostCharacteristicBuilder().setCostName("hop-count").setCostValue("1").build());
        linkBuilder.setCostCharacteristic(costCharacteristics);
        List<LatencyCharacteristic> latencyCharacteristics = new ArrayList<>();
        latencyCharacteristics.add(new LatencyCharacteristicBuilder().setTrafficPropertyName("FixedLatency")
                .setFixedLatencyCharacteristic("1").build());
        linkBuilder.setLatencyCharacteristic(latencyCharacteristics);
        List<NodeEdgePoint> nodeEdgePoints = new ArrayList<>();
        String topoUuid = UUID.nameUUIDFromBytes("optical-topo".getBytes()).toString();
        String nodeaUuid = UUID.nameUUIDFromBytes(input.getRdmNode().getBytes()).toString();
        String nepaUuid = UUID.nameUUIDFromBytes((input.getRdmNode() + "-" + input.getAddDropNep())
                .getBytes()).toString();
        String nodezUuid = UUID.nameUUIDFromBytes(input.getXpdrNode().getBytes()).toString();
        String nepzUuid = UUID.nameUUIDFromBytes((input.getXpdrNode() + "-" + input.getLineNep())
                .getBytes()).toString();
        nodeEdgePoints.add(0,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodeaUuid)).setNodeEdgePointUuid(new Uuid(nepaUuid))
                .build());
        nodeEdgePoints.add(1,new NodeEdgePointBuilder().setTopologyUuid(new Uuid(topoUuid))
                .setNodeUuid(new Uuid(nodezUuid)).setNodeEdgePointUuid(new Uuid(nepzUuid))
                .build());
        linkBuilder.setNodeEdgePoint(nodeEdgePoints);
        this.mongoDbDataStoreService.addLink(linkBuilder.build());

        return RpcResultBuilder.success(new InitRdmXpdrLinkOutputBuilder().setResult("OK")).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteLinkOutput>> deleteLink(DeleteLinkInput input) {
        LOG.info("Deleting link {}", input.getLinkId());
        this.mongoDbDataStoreService.deleteLink(input.getLinkId());

        return RpcResultBuilder.success(new DeleteLinkOutputBuilder().setResult("OK")).buildFuture();
    }
}
