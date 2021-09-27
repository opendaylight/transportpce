/*
 * Copyright © 2021 Nokia.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.cp.to.degree.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev170929.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Protocols;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.Protocols1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.NbrList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.lldp.container.lldp.nbr.list.IfName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.RestorationPolicy;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.NodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.link.ResilienceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.risk.parameter.pac.RiskCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.cost.pac.CostCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristic;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.transfer.timing.pac.LatencyCharacteristicBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanism;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.validation.pac.ValidationMechanismBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class R2RTapiLinkDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(R2RTapiLinkDiscovery.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private static final String PHTNC_MEDIA = "PHOTONIC_MEDIA";

    public R2RTapiLinkDiscovery(final DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public Map<LinkKey, Link> readLLDP(NodeId nodeId, int nodeVersion, Uuid tapiTopoUuid) {
        LOG.info("Tapi R2R Link Node version = {}", nodeVersion);
        // TODO -> waiting for device 7.1 in network model to change this to a switch statement and include
        //  support for 7.1 devices
        switch (nodeVersion) {
            case 1:
                // 1.2.1
                InstanceIdentifier<Protocols> protocols121IID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(Protocols.class);
                Optional<Protocols> protocol121Object = this.deviceTransactionManager.getDataFromDevice(
                    nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols121IID, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (!protocol121Object.isPresent()
                        || (protocol121Object.get().augmentation(Protocols1.class) == null)) {
                    LOG.warn("LLDP subtree is missing : isolated openroadm device");
                    return new HashMap<>();
                }
                // get neighbor list
                NbrList nbr121List = protocol121Object.get().augmentation(Protocols1.class).getLldp().getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr121List.getIfName().size());
                // try to create rdm2rdm link
                return rdm2rdmLinkCreatev121(nodeId, tapiTopoUuid, nbr121List);
            case 2:
                // 2.2.1
                InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                    .container.org.openroadm.device.Protocols> protocols221IID =
                        InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http
                            .org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                            .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                                .org.openroadm.device.container.org.openroadm.device.Protocols.class);
                Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device
                    .container.org.openroadm.device.Protocols> protocol221Object = this.deviceTransactionManager
                    .getDataFromDevice(nodeId.getValue(), LogicalDatastoreType.OPERATIONAL, protocols221IID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (!protocol221Object.isPresent() || (protocol221Object.get().augmentation(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.Protocols1.class) == null)) {
                    LOG.warn("LLDP subtree is missing : isolated openroadm device");
                    return new HashMap<>();
                }
                org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.@Nullable NbrList
                    nbr221List = protocol221Object.get().augmentation(org.opendaylight.yang.gen.v1.http
                        .org.openroadm.lldp.rev181019.Protocols1.class).getLldp().getNbrList();
                LOG.info("LLDP subtree is present. Device has {} neighbours", nbr221List.getIfName().size());
                return rdm2rdmLinkCreatev221(nodeId, tapiTopoUuid, nbr221List);
            case 3:
                // 7.1.0
                LOG.info("Not yet supported?");
                return new HashMap<>();
            default:
                LOG.error("Unable to read LLDP data for unmanaged openroadm device version");
                return new HashMap<>();
        }
    }

    private Map<LinkKey, Link> rdm2rdmLinkCreatev221(NodeId nodeId, Uuid tapiTopoUuid,
            org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.NbrList nbrList) {
        Map<LinkKey, Link> linkMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.lldp.container.lldp.nbr.list.IfName
                ifName : nbrList.nonnullIfName().values()) {
            if (ifName.getRemoteSysName() == null) {
                LOG.warn("Tapi R2R Link LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                    nodeId.getValue(),ifName.getIfName());
                continue;
            }
            Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName.getRemoteSysName());
            if (!mps.isPresent()) {
                LOG.warn("Tapi R2R Link Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                // The controller raises a warning rather than an error because the first node to
                // mount cannot see its neighbors yet. The link will be detected when processing
                // the neighbor node.
                continue;
            }
            Link omsLink = createR2RTapiLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                ifName.getRemotePortId(), tapiTopoUuid);
            if (omsLink != null) {
                linkMap.put(omsLink.key(), omsLink);
            } else {
                LOG.error("Link was not created");
            }
        }
        return linkMap;
    }

    private Map<LinkKey, Link> rdm2rdmLinkCreatev121(NodeId nodeId, Uuid tapiTopoUuid, NbrList nbrList) {
        Map<LinkKey, Link> linkMap = new HashMap<>();
        for (IfName ifName : nbrList.nonnullIfName().values()) {
            if (ifName.getRemoteSysName() == null) {
                LOG.warn("Tapi R2R Link LLDP subtree neighbour is empty for nodeId: {}, ifName: {}",
                    nodeId.getValue(),ifName.getIfName());
                continue;
            }
            Optional<MountPoint> mps = this.deviceTransactionManager.getDeviceMountPoint(ifName
                .getRemoteSysName());
            if (!mps.isPresent()) {
                LOG.warn("Tapi R2R Link Neighbouring nodeId: {} is not mounted yet", ifName.getRemoteSysName());
                // The controller raises a warning rather than an error because the first node to
                // mount cannot see its neighbors yet. The link will be detected when processing
                // the neighbor node.
                continue;
            }
            Link omsLink = createR2RTapiLink(nodeId, ifName.getIfName(), ifName.getRemoteSysName(),
                ifName.getRemotePortId(), tapiTopoUuid);
            if (omsLink != null) {
                linkMap.put(omsLink.key(), omsLink);
            } else {
                LOG.error("Link was not created");
            }
        }
        return linkMap;
    }

    public Link createR2RTapiLink(NodeId nodeId, String interfaceName, String remoteSystemName,
                                 String remoteInterfaceName, Uuid tapiTopoUuid) {
        String srcTpTx = null;
        String srcTpRx = null;
        String destTpTx = null;
        String destTpRx = null;
        // Find which degree is associated with ethernet interface
        Integer srcDegId = getDegFromInterface(nodeId, interfaceName);
        if (srcDegId == null) {
            LOG.error("Tapi R2R Link Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return null;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        Direction sourceDirection = getDegreeDirection(srcDegId, nodeId);
        if (Direction.NotApplicable == sourceDirection) {
            LOG.error("Tapi R2R Link Couldnt find degree direction for nodeId: {} and degree: {}", nodeId, srcDegId);
            return null;
        } else if (Direction.Bidirectional == sourceDirection) {
            srcTpTx = "DEG" + srcDegId + "-TTP-TXRX";
            srcTpRx = "DEG" + srcDegId + "-TTP-TXRX";
        } else {
            srcTpTx = "DEG" + srcDegId + "-TTP-TX";
            srcTpRx = "DEG" + srcDegId + "-TTP-RX";
        }
        LOG.debug("Tapi R2R Link SrcTPTx {}, SrcTPRx {}", srcTpTx, srcTpRx);
        // Find degree for which Ethernet interface is created on other end
        NodeId destNodeId = new NodeId(remoteSystemName);
        Integer destDegId = getDegFromInterface(destNodeId, remoteInterfaceName);
        if (destDegId == null) {
            LOG.error("Tapi R2R Link Couldnt find degree connected to Ethernet interface for nodeId: {}", nodeId);
            return null;
        }
        // Check whether degree is Unidirectional or Bidirectional by counting
        // number of
        // circuit-packs under degree subtree
        Direction destinationDirection = getDegreeDirection(destDegId, destNodeId);
        if (Direction.NotApplicable == destinationDirection) {
            LOG.error("Tapi R2R Link Couldnt find degree direction for nodeId: {} and degree: {}",
                destNodeId, destDegId);
            return null;
        } else if (Direction.Bidirectional == destinationDirection) {
            destTpTx = "DEG" + destDegId + "-TTP-TXRX";
            destTpRx = "DEG" + destDegId + "-TTP-TXRX";
        } else {
            destTpTx = "DEG" + destDegId + "-TTP-TX";
            destTpRx = "DEG" + destDegId + "-TTP-RX";
        }
        // Todo -> only handling for the bidirectional case. I assume all tps are of the type bidirectional
        LOG.debug("Tapi R2R Link DstTPTx {}, DstTPRx {}", destTpTx, srcTpRx);

        // Create OMS Tapi Link
        LOG.info("Tapi R2R Link Found a neighbor SrcNodeId: {} , SrcDegId: {} , SrcTPId: {}, DestNodeId:{} , "
            + "DestDegId: {}, DestTPId: {}", nodeId.getValue(), srcDegId, srcTpTx, destNodeId, destDegId, destTpRx);
        Link omsLink = createTapiLink(nodeId.getValue(), srcTpTx, destNodeId.getValue(), destTpRx, tapiTopoUuid);
        LOG.info("Tapi R2R Link OMS link created = {}", omsLink);
        return omsLink;
    }

    private Link createTapiLink(String sourceNode, String sourceTp, String destNode, String destTp, Uuid tapiTopoUuid) {
        Map<NodeEdgePointKey, NodeEdgePoint> nepList = new HashMap<>();
        Uuid sourceUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode,
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid sourceUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", sourceNode, PHTNC_MEDIA, sourceTp))
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidNode = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode,
            PHTNC_MEDIA)).getBytes(Charset.forName("UTF-8"))).toString());
        Uuid destUuidTp = new Uuid(UUID.nameUUIDFromBytes((String.join("+", destNode, PHTNC_MEDIA, destTp))
            .getBytes(Charset.forName("UTF-8"))).toString());
        NodeEdgePoint sourceNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(sourceUuidNode)
            .setNodeEdgePointUuid(sourceUuidTp)
            .build();
        nepList.put(sourceNep.key(), sourceNep);
        NodeEdgePoint destNep = new NodeEdgePointBuilder()
            .setTopologyUuid(tapiTopoUuid)
            .setNodeUuid(destUuidNode)
            .setNodeEdgePointUuid(destUuidTp)
            .build();
        nepList.put(destNep.key(), destNep);
        OperationalState sourceOperState = getOperState(tapiTopoUuid, sourceUuidTp, sourceUuidNode);
        OperationalState destOperState = getOperState(tapiTopoUuid, destUuidTp, destUuidNode);
        if (sourceOperState == null || destOperState == null) {
            LOG.error("No link can be created, as the operational state was not found in the TAPI topology");
            return null;
        }
        AdministrativeState sourceAdminState = getAdminState(tapiTopoUuid, sourceUuidTp, sourceUuidNode);
        AdministrativeState destAdminState = getAdminState(tapiTopoUuid, destUuidTp, destUuidNode);
        if (sourceAdminState == null || destAdminState == null) {
            LOG.error("No link can be created, as the administrative state was not found in the TAPI topology");
            return null;
        }
        OperationalState operState = (OperationalState.ENABLED.equals(sourceOperState)
                && OperationalState.ENABLED.equals(destOperState))
                ? OperationalState.ENABLED : OperationalState.DISABLED;
        AdministrativeState adminState = (AdministrativeState.UNLOCKED.equals(sourceAdminState)
                && AdministrativeState.UNLOCKED.equals(destAdminState))
                ? AdministrativeState.UNLOCKED : AdministrativeState.LOCKED;
        String linkNameValue = String.join("-", sourceNode, sourceTp.split("-")[0], sourceTp)
            + "to" + String.join("-", destNode, destTp.split("-")[0], destTp);
        Name linkName = new NameBuilder().setValueName("OMS link name")
            .setValue(linkNameValue)
            .build();
        CostCharacteristic costCharacteristic = new CostCharacteristicBuilder()
            .setCostAlgorithm("Restricted Shortest Path - RSP")
            .setCostName("HOP_COUNT")
            .setCostValue("12345678")
            .build();
        LatencyCharacteristic latencyCharacteristic = new LatencyCharacteristicBuilder()
            .setFixedLatencyCharacteristic("12345678")
            .setQueingLatencyCharacteristic("12345678")
            .setJitterCharacteristic("12345678")
            .setWanderCharacteristic("12345678")
            .setTrafficPropertyName("FIXED_LATENCY")
            .build();
        RiskCharacteristic riskCharacteristic = new RiskCharacteristicBuilder()
            .setRiskCharacteristicName("risk characteristic")
            .setRiskIdentifierList(List.of("risk identifier1", "risk identifier2"))
            .build();
        ValidationMechanism validationMechanism = new ValidationMechanismBuilder()
            .setValidationMechanism("validation mechanism")
            .setValidationRobustness("validation robustness")
            .setLayerProtocolAdjacencyValidated("layer protocol adjacency")
            .build();
        return new LinkBuilder()
            .setUuid(new Uuid(
                UUID.nameUUIDFromBytes(linkNameValue.getBytes(Charset.forName("UTF-8")))
                    .toString()))
            .setName(Map.of(linkName.key(), linkName))
            .setLayerProtocolName(List.of(LayerProtocolName.PHOTONICMEDIA))
            .setTransitionedLayerProtocolName(new ArrayList<>())
            .setNodeEdgePoint(nepList)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setResilienceType(new ResilienceTypeBuilder().setProtectionType(ProtectionType.NOPROTECTON)
                .setRestorationPolicy(RestorationPolicy.NA)
                .build())
            .setAdministrativeState(adminState)
            .setOperationalState(operState)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                    .setValue(Uint64.valueOf(100)).build()).build())
            .setAvailableCapacity(new AvailableCapacityBuilder().setTotalSize(
                new TotalSizeBuilder().setUnit(CapacityUnit.MBPS)
                    .setValue(Uint64.valueOf(100)).build())
                .build())
            .setCostCharacteristic(Map.of(costCharacteristic.key(), costCharacteristic))
            .setLatencyCharacteristic(Map.of(latencyCharacteristic.key(), latencyCharacteristic))
            .setRiskCharacteristic(Map.of(riskCharacteristic.key(), riskCharacteristic))
            .setErrorCharacteristic("error")
            .setLossCharacteristic("loss")
            .setRepeatDeliveryCharacteristic("repeat delivery")
            .setDeliveryOrderCharacteristic("delivery order")
            .setUnavailableTimeCharacteristic("unavailable time")
            .setServerIntegrityProcessCharacteristic("server integrity process")
            .setValidationMechanism(Map.of(validationMechanism.key(), validationMechanism))
            .build();
    }

    private Integer getDegFromInterface(NodeId nodeId, String interfaceName) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue())).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();
            if (nodesObject.isEmpty() || (nodesObject.get().getCpToDegree() == null)) {
                LOG.warn("Could not find mapping for Interface {} for nodeId {}", interfaceName,
                    nodeId.getValue());
                return null;
            }
            Collection<CpToDegree> cpToDeg = nodesObject.get().nonnullCpToDegree().values();
            Stream<CpToDegree> cpToDegStream = cpToDeg.stream().filter(cp -> cp.getInterfaceName() != null)
                .filter(cp -> cp.getInterfaceName().equals(interfaceName));
            if (cpToDegStream != null) {
                @SuppressWarnings("unchecked") Optional<CpToDegree> firstCpToDegree = cpToDegStream.findFirst();
                if (firstCpToDegree.isEmpty() || (firstCpToDegree == null)) {
                    LOG.debug("Not found so returning nothing");
                    return null;
                }
                LOG.debug("Found and returning {}",firstCpToDegree.get().getDegreeNumber().intValue());
                return firstCpToDegree.get().getDegreeNumber().intValue();
            } else {
                LOG.warn("CircuitPack stream couldnt find anything for nodeId: {} and interfaceName: {}",
                    nodeId.getValue(),interfaceName);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for Interface : {} for nodeId {}", interfaceName, nodeId, ex);
        }
        return null;
    }

    public Direction getDegreeDirection(Integer degreeCounter, NodeId nodeId) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.builder(Network.class)
            .child(Nodes.class, new NodesKey(nodeId.getValue())).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodesObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodesIID).get();
            if (nodesObject.isPresent() && (nodesObject.get().getMapping() != null)) {
                Collection<Mapping> mappingList = nodesObject.get().nonnullMapping().values();
                mappingList = mappingList.stream().filter(mp -> mp.getLogicalConnectionPoint().contains("DEG"
                    + degreeCounter)).collect(Collectors.toList());
                if (mappingList.size() == 1) {
                    return Direction.Bidirectional;
                } else if (mappingList.size() > 1) {
                    return Direction.Tx;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
        }
        return Direction.NotApplicable;
    }

    private OperationalState getOperState(Uuid tapiTopoUuid, Uuid nepUuid, Uuid nodeUuid) {
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<OwnedNodeEdgePoint> optionalOnep = readTx.read(LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                return optionalOnep.get().getOperationalState();
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }

    private AdministrativeState getAdminState(Uuid tapiTopoUuid, Uuid nepUuid, Uuid nodeUuid) {
        InstanceIdentifier<OwnedNodeEdgePoint> onepIID = InstanceIdentifier.builder(Context.class)
                .augmentation(Context1.class).child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiTopoUuid)).child(Node.class, new NodeKey(nodeUuid))
                .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nepUuid))
                .build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<OwnedNodeEdgePoint> optionalOnep = readTx.read(LogicalDatastoreType.OPERATIONAL, onepIID).get();
            if (optionalOnep.isPresent()) {
                return optionalOnep.get().getAdministrativeState();
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed getting Mapping data from portMapping",e);
            return null;
        }
    }
}
