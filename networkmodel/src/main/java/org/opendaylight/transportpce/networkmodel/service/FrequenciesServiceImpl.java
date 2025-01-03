/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.NodeIdPair;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.RxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrPortAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev230526.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FrequenciesServiceImpl implements FrequenciesService {

    private static final Logger LOG = LoggerFactory.getLogger(FrequenciesServiceImpl.class);
    private final DataBroker dataBroker;
    private final AvailFreqMapsKey availFreqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);

    @Activate
    public FrequenciesServiceImpl(@Reference DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void allocateFrequencies(AToZDirection atoZDirection, ZToADirection ztoADirection) {
        updateFrequencies(atoZDirection, ztoADirection, true);
    }


    @Override
    public void releaseFrequencies(AToZDirection atoZDirection, ZToADirection ztoADirection) {
        updateFrequencies(atoZDirection, ztoADirection, false);
    }

    /**
     * Update frequency map for nodes and tp in atozDirection and ztoadirection.
     * @param atoZDirection AToZDirection
     * @param ztoADirection ZToADirection
     * @param used used boolean true if frequencies are used, false otherwise.
     */
    private void updateFrequencies(AToZDirection atoZDirection, ZToADirection ztoADirection, boolean used) {
        if (atoZDirection != null && atoZDirection.getAToZMinFrequency() != null) {
            LOG.info("Update frequencies for a to z direction {}, used {}", atoZDirection, used);
            List<NodeIdPair> atozTpIds = getAToZTpList(atoZDirection);
            Decimal64 atozMinFrequency = atoZDirection.getAToZMinFrequency().getValue();
            Decimal64 atozMaxFrequency = atoZDirection.getAToZMaxFrequency().getValue();
            ModulationFormat modulationFormat = ModulationFormat.forName(atoZDirection.getModulationFormat());
            if (modulationFormat == null) {
                LOG.error("Unknown modulation format {} for a to z direction, frequencies not updated",
                        atoZDirection.getModulationFormat());
                return;
            }
            setFrequencies4Tps(
                    atozMinFrequency, atozMaxFrequency,
                    atoZDirection.getRate(), modulationFormat, atozTpIds,
                    used);
            setFrequencies4Nodes(
                    atozMinFrequency, atozMaxFrequency,
                    atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                    used);
        }
        if (ztoADirection != null && ztoADirection.getZToAMinFrequency() != null) {
            LOG.info("Update frequencies for z to a direction {}, used {}", ztoADirection, used);
            List<NodeIdPair> ztoaTpIds = getZToATpList(ztoADirection);
            Decimal64 ztoaMinFrequency = ztoADirection.getZToAMinFrequency().getValue();
            Decimal64 ztoaMaxFrequency = ztoADirection.getZToAMaxFrequency().getValue();
            ModulationFormat modulationFormat = ModulationFormat.forName(ztoADirection.getModulationFormat());
            if (modulationFormat == null) {
                LOG.error("Unknown modulation format {} for z to a direction, frequencies not updated",
                        ztoADirection.getModulationFormat());
                return;
            }
            setFrequencies4Tps(
                    ztoaMinFrequency, ztoaMaxFrequency,
                    ztoADirection.getRate(), modulationFormat, ztoaTpIds,
                    used);
            setFrequencies4Nodes(
                    ztoaMinFrequency, ztoaMaxFrequency,
                    ztoaTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                    used);
        }
    }

    /**
     * Get network node with nodeId from datastore.
     * @param nodeId String
     * @return Node1, null otherwise.
     */
    private Node1 getNetworkNodeFromDatastore(String nodeId) {
        DataObjectIdentifier<Node1> nodeIID = OpenRoadmTopology.createNetworkNodeIID(nodeId);
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Node1> optionalNode = nodeReadTx
                .read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalNode.isEmpty()) {
                LOG.warn("Unable to get network node for node id {} from topology {}",
                     nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
            return optionalNode.orElseThrow();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting network node for node id {} from {} topology",
                    nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting network node for node id {} from {} topology was interrupted",
                    nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Get common network node with nodeId from datastore.
     * @param nodeId String
     * @return Node1, null otherwise.
     */
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1
            getCommonNetworkNodeFromDatastore(String nodeId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1> nodeIID =
            OpenRoadmTopology.createCommonNetworkNodeIID(nodeId);
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1> optionalNode =
                nodeReadTx
                    .read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalNode.isEmpty()) {
                LOG.error("Unable to get common network node for node id {} from topology {}",
                        nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
            return optionalNode.orElseThrow();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting common network node for node id {} from {} topology",
                    nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting common network node for node id {} from {} topology was interrupted",
                    nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Set frequency map for nodes in nodeIds.
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param nodeIds List of node id
     * @param used boolean true if min and max frequencies are used, false otherwise.
     */
    private void setFrequencies4Nodes(Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency,
            List<String> nodeIds, boolean used) {
        updateFreqMaps4Nodes(nodeIds, atozMinFrequency, atozMaxFrequency, used);
    }


    /**
     * Get a network termination point for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return network termination point, null otherwise
     */
    private TerminationPoint1 getNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        DataObjectIdentifier<TerminationPoint1> tpIID = InstanceIdentifiers
                .createNetworkTerminationPoint1IIDBuilder(nodeId, tpId);
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<TerminationPoint1> optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            return optionalTerminationPoint.isEmpty() ? null : optionalTerminationPoint.orElseThrow();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting termination {} for node id {} point from {} topology was interrupted",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Get a common network termination point for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return common network termination point, null otherwise
     */
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1
            getCommonNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1>
            tpIID = OpenRoadmTopology.createCommonNetworkTerminationPointIIDBuilder(nodeId, tpId).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1>
                optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalTerminationPoint.isEmpty()) {
                LOG.error("Unable to get common-network termination point {} for node id {}from topology {}",
                        tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
            return optionalTerminationPoint.orElseThrow();
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting common-network termination {} for node id {} point from {} topology",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting common-network termination {} for node id {} point from {} topology was interrupted",
                    tpId, nodeId, NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Update availFreqMapsMap for min and max frequencies for termination point in tpIds.
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param tpIds List of NodeIdPair
     * @param sed boolean true if min and max frequencies are used, false otherwise.
     */
    private void setFrequencies4Tps(Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency, Uint32 rate,
            ModulationFormat modulationFormat, List<NodeIdPair> tpIds, boolean used) {
        String strTpIdsList = String.join(", ", tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList()));
        LOG.debug(
            "Update frequencies for termination points {}, rate {}, modulation format {},"
                + " min frequency {}, max frequency {}, used {}",
            strTpIdsList, rate, modulationFormat, atozMinFrequency, atozMaxFrequency, used);
        WriteTransaction updateFrequenciesTransaction = this.dataBroker.newWriteOnlyTransaction();
        for (NodeIdPair idPair : tpIds) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.TerminationPoint1
                    commonNetworkTerminationPoint =
                getCommonNetworkTerminationPointFromDatastore(idPair.getNodeID(), idPair.getTpID());
            if (commonNetworkTerminationPoint == null) {
                LOG.warn("Cannot update frequencies for termination point {}, node id {}",
                    idPair.getTpID(), idPair.getNodeID());
                continue;
            }
            TerminationPoint1 networkTerminationPoint =
                    getNetworkTerminationPointFromDatastore(idPair.getNodeID(), idPair.getTpID());
            TerminationPoint1Builder networkTerminationPointBuilder =
                networkTerminationPoint == null
                    ? new TerminationPoint1Builder()
                    : new TerminationPoint1Builder(networkTerminationPoint);
            switch (commonNetworkTerminationPoint.getTpType()) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    networkTerminationPointBuilder.setTxTtpAttributes(updateTxTtpAttributes(networkTerminationPoint,
                            atozMinFrequency,atozMaxFrequency,used));
                    break;
                case DEGREERXTTP:
                    networkTerminationPointBuilder.setRxTtpAttributes(updateRxTtpAttributes(networkTerminationPoint,
                            atozMinFrequency,atozMaxFrequency,used));
                    break;
                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    networkTerminationPointBuilder.setCtpAttributes(updateCtpAttributes(networkTerminationPoint,
                            atozMinFrequency,atozMaxFrequency,used));
                    break;
                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    networkTerminationPointBuilder.setCpAttributes(updateCpAttributes(networkTerminationPoint,
                            atozMinFrequency,atozMaxFrequency,used));
                    break;
                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    networkTerminationPointBuilder.setPpAttributes(updatePpAttributes(networkTerminationPoint,
                            atozMinFrequency,atozMaxFrequency,used));
                    break;
                case XPONDERNETWORK:
                    networkTerminationPointBuilder.setXpdrNetworkAttributes(
                            updateXpdrNetworkAttributes(networkTerminationPoint,
                                    atozMinFrequency, atozMaxFrequency, rate, modulationFormat, used)).build();
                    break;
                case XPONDERCLIENT:
                    break;
                case XPONDERPORT:
                    networkTerminationPointBuilder.setXpdrPortAttributes(
                            updateXpdrPortAttributes(networkTerminationPoint,
                                    atozMinFrequency, atozMaxFrequency, rate, modulationFormat, used)).build();
                    break;
                default:
                    LOG.warn("Termination point type {} not managed", commonNetworkTerminationPoint.getTpType());
                    return;
            }
            updateFrequenciesTransaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers
                    .createNetworkTerminationPoint1IIDBuilder(idPair.getNodeID(),
                            idPair.getTpID()), networkTerminationPointBuilder.build());
        }
        try {
            updateFrequenciesTransaction.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error(
                "Something went wrong for frequencies update (min frequency {}, max frequency {}, used {} for TPs {}",
                atozMinFrequency, atozMaxFrequency, used, strTpIdsList, e);
        } catch (InterruptedException e) {
            LOG.error("Frequencies update (min frequency {}, max frequency {}, used {} for TPs {} was interrupted",
                    atozMinFrequency, atozMaxFrequency, used,
                    strTpIdsList, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Update availFreqMapsMap for min and max frequencies for nodes in nodeIds.
     * @param nodeIds  List of node id
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     */
    private void updateFreqMaps4Nodes(List<String> nodeIds, Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency,
            boolean used) {
        String strNodesList = String.join(", ", nodeIds);
        LOG.debug("Update frequencies for nodes {}, min frequency {}, max frequency {}, used {}",
                strNodesList, atozMinFrequency, atozMaxFrequency, used);
        WriteTransaction updateFrequenciesTransaction = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Node1 networkNode = getNetworkNodeFromDatastore(nodeId);
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Node1 commonNetworkNode =
                    getCommonNetworkNodeFromDatastore(nodeId);
            if (networkNode == null || commonNetworkNode == null) {
                LOG.warn(
                        "From topology {} for node id {} -> Get common-network : {} "
                                + "Get network-topology : {}. Skipping frequencies update for this node.",
                        NetworkUtils.OVERLAY_NETWORK_ID, nodeId, commonNetworkNode, networkNode);
                continue;
            }
            Node1Builder networkNodeBuilder = new Node1Builder(networkNode);
            switch (commonNetworkNode.getNodeType()) {
                case DEGREE:
                    networkNodeBuilder.setDegreeAttributes(
                            updateDegreeAttributes(networkNode.getDegreeAttributes(), atozMinFrequency,
                                    atozMaxFrequency, used));
                    break;
                case SRG:
                    networkNodeBuilder.setSrgAttributes(updateSrgAttributes(
                            networkNode.getSrgAttributes(), atozMinFrequency, atozMaxFrequency, used));
                    break;
                default:
                    LOG.warn("Node type not managed {}", commonNetworkNode.getNodeType());
                    break;
            }
            updateFrequenciesTransaction.put(LogicalDatastoreType.CONFIGURATION,
                    OpenRoadmTopology.createNetworkNodeIID(nodeId), networkNodeBuilder.build());
        }
        try {
            updateFrequenciesTransaction.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Cannot update frequencies {} {} for nodes {}", atozMinFrequency, atozMaxFrequency,
                    strNodesList, e);
        } catch (InterruptedException e) {
            LOG.error("Update of frequencies {} {} for nodes {} was interrupted", atozMinFrequency, atozMaxFrequency,
                    strNodesList, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get list of NodeIdPair from atoZDirection.
     * @param atoZDirection AToZDirection
     * @return List of NodeIdPair
     */
    private List<NodeIdPair> getAToZTpList(AToZDirection atoZDirection) {
        return atoZDirection.nonnullAToZ().values().stream()
                .filter(aToZ -> {
                    if (aToZ.getResource() == null || aToZ.getResource().getResource() == null) {
                        LOG.warn("Resource of AToZ node {} is null! Skipping this node!", aToZ.getId());
                        return false;
                    }
                    return aToZ.getResource().getResource() instanceof TerminationPoint;
                }).map(aToZ -> {
                    TerminationPoint tp = (TerminationPoint) aToZ.getResource().getResource();
                    if (tp == null || tp.getTpNodeId() == null || tp.getTpId() == null || tp.getTpId().isEmpty()) {
                        LOG.warn("Termination point in AToZ node {} contains nulls! Skipping this node!", aToZ.getId());
                        return null;
                    }
                    return new NodeIdPair(tp.getTpNodeId(), tp.getTpId());
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get list of NodeIdPair from ztoADirection.
     * @param ztoADirection ZToADirection
     * @return List of NodeIdPair
     */
    private List<NodeIdPair> getZToATpList(ZToADirection ztoADirection) {
        return ztoADirection.nonnullZToA().values().stream()
                .filter(zToA -> {
                    if (zToA.getResource() == null || zToA.getResource().getResource() == null) {
                        LOG.warn("Resource of ZToA node {} is null! Skipping this node!", zToA.getId());
                        return false;
                    }
                    return zToA.getResource().getResource() instanceof TerminationPoint;
                }).map(zToA -> {
                    TerminationPoint tp = (TerminationPoint) zToA.getResource().getResource();
                    if (tp == null || tp.getTpNodeId() == null || tp.getTpId() == null || tp.getTpId().isEmpty()) {
                        LOG.warn("Termination point in ZToA node {} contains nulls! Skipping this node!", zToA.getId());
                        return null;
                    }
                    return new NodeIdPair(tp.getTpNodeId(), tp.getTpId());
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Update Wavelength for xpdr port attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return XpdrPortAttributes with Wavelength updated
     */
    private XpdrPortAttributes updateXpdrPortAttributes(TerminationPoint1 networkTerminationPoint,
            Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency, Uint32 rate, ModulationFormat modulationFormat,
            boolean used) {
        LOG.debug("Update xpdr node attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        XpdrPortAttributesBuilder xpdrPortAttributesBuilder =
            networkTerminationPoint == null
                ? new XpdrPortAttributesBuilder()
                : new XpdrPortAttributesBuilder(networkTerminationPoint.getXpdrPortAttributes());
        return xpdrPortAttributesBuilder
            .setWavelength(
                used
                    ? new WavelengthBuilder()
                        .setWidth(GridUtils.getWidthFromRateAndModulationFormat(rate, modulationFormat))
                        .setFrequency(
                            GridUtils.getCentralFrequency(
                                atozMinFrequency.decimalValue(),
                                atozMaxFrequency.decimalValue()))
                        .build()
                    : new WavelengthBuilder().build())
            .build();
    }

    /**
     * Update Wavelength for xpdr network attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return XpdrNetworkAttributes with Wavelength updated
     */
    private XpdrNetworkAttributes updateXpdrNetworkAttributes(TerminationPoint1 networkTerminationPoint,
            Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency, Uint32 rate, ModulationFormat modulationFormat,
            boolean used) {
        LOG.debug("Update xpdr node attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        XpdrNetworkAttributesBuilder xpdrNetworkAttributesBuilder =
            networkTerminationPoint == null
                ? new XpdrNetworkAttributesBuilder()
                : new XpdrNetworkAttributesBuilder(networkTerminationPoint.getXpdrNetworkAttributes());
        return xpdrNetworkAttributesBuilder
            .setWavelength(
                used
                    ? new WavelengthBuilder()
                        .setWidth(GridUtils.getWidthFromRateAndModulationFormat(rate, modulationFormat))
                        .setFrequency(
                            GridUtils.getCentralFrequency(
                                atozMinFrequency.decimalValue(),
                                atozMaxFrequency.decimalValue()))
                        .build()
                    : null)
            .build();

    }

    /**
     * Update freqMaps for pp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return PpAttributes with frequency map updated
     */
    private PpAttributes updatePpAttributes(TerminationPoint1 networkTerminationPoint, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        LOG.debug("Update pp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        PpAttributesBuilder ppAttributesBuilder =
            networkTerminationPoint == null
                ? new PpAttributesBuilder()
                : new PpAttributesBuilder(networkTerminationPoint.getPpAttributes());
        return ppAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    ppAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for cp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return CpAttributes with frequency map updated
     */
    private CpAttributes updateCpAttributes(TerminationPoint1 networkTerminationPoint, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        LOG.debug("Update cp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        CpAttributesBuilder cpAttributesBuilder =
            networkTerminationPoint == null
                ? new CpAttributesBuilder()
                : new CpAttributesBuilder(networkTerminationPoint.getCpAttributes());
        return cpAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    cpAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for ctp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return CtpAttributes with frequency map updated
     */
    private CtpAttributes updateCtpAttributes(TerminationPoint1 networkTerminationPoint, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        LOG.debug("Update ctp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        CtpAttributesBuilder ctpAttributesBuilder =
            networkTerminationPoint == null
                ?  new CtpAttributesBuilder()
                :  new CtpAttributesBuilder(networkTerminationPoint.getCtpAttributes());
        return ctpAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    ctpAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for rxtp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return RxTtpAttributes with frequency map updated
     */
    private RxTtpAttributes updateRxTtpAttributes(TerminationPoint1 networkTerminationPoint, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        LOG.debug("Update rx attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        RxTtpAttributesBuilder rxTtpAttributesBuilder =
            networkTerminationPoint == null
                ? new RxTtpAttributesBuilder()
                : new RxTtpAttributesBuilder(networkTerminationPoint.getRxTtpAttributes());
        return rxTtpAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    rxTtpAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for txtp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return TxTtpAttributes with frequency map updated
     */
    private TxTtpAttributes updateTxTtpAttributes(TerminationPoint1 networkTerminationPoint, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        LOG.debug("Update tx attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, atozMinFrequency, atozMaxFrequency, used);
        TxTtpAttributesBuilder txTtpAttributesBuilder =
            networkTerminationPoint == null
                ? new TxTtpAttributesBuilder()
                : new TxTtpAttributesBuilder(networkTerminationPoint.getTxTtpAttributes());
        return txTtpAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    txTtpAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for srg attributes of srgAttributes.
     * @param srgAttributes SrgAttributes
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return SrgAttributes with frequency map updated
     */
    private SrgAttributes updateSrgAttributes(SrgAttributes srgAttributes, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        SrgAttributesBuilder srgAttributesBuilder =
            srgAttributes == null
                ? new SrgAttributesBuilder()
                : new SrgAttributesBuilder(srgAttributes);
        return srgAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    srgAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update freqMaps for degree attributes of degreeAttributes.
     * @param degreeAttributes DegreeAttributes
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return DegreeAttributes with frequency map updated
     */
    private DegreeAttributes updateDegreeAttributes(DegreeAttributes degreeAttributes, Decimal64 atozMinFrequency,
            Decimal64 atozMaxFrequency, boolean used) {
        DegreeAttributesBuilder degreeAttributesBuilder =
            degreeAttributes == null
                ? new DegreeAttributesBuilder()
                : new DegreeAttributesBuilder(degreeAttributes);
        return degreeAttributesBuilder
            .setAvailFreqMaps(
                updateFreqMaps(
                    atozMinFrequency, atozMaxFrequency,
                    degreeAttributesBuilder.getAvailFreqMaps(),
                    used))
            .build();
    }

    /**
     * Update availFreqMapsMap for min and max frequencies for cband AvailFreqMaps.
     * @param atozMinFrequency BigDecimal
     * @param atozMaxFrequency BigDecimal
     * @param availFreqMapsMap Map
     * @param used boolean
     * @return updated Update availFreqMapsMap for min and max frequencies for cband AvailFreqMaps.
     */
    private Map<AvailFreqMapsKey, AvailFreqMaps> updateFreqMaps(Decimal64 atozMinFrequency, Decimal64 atozMaxFrequency,
            Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap, boolean used) {
        int beginIndex = GridUtils.getIndexFromFrequency(atozMinFrequency);
        int endIndex = GridUtils.getIndexFromFrequency(atozMaxFrequency);
        if (availFreqMapsMap == null) {
            availFreqMapsMap = GridUtils.initFreqMaps4FixedGrid2Available();
        }
        AvailFreqMaps availFreqMaps = availFreqMapsMap.get(availFreqMapKey);
        if (availFreqMaps == null || availFreqMaps.getFreqMap() == null) {
            return availFreqMapsMap;
        }
        BitSet bitSetFreq = BitSet.valueOf(availFreqMaps.getFreqMap());
        LOG.debug(
             "Update frequency map from index {}, to index {}, min frequency {}, max frequency {}, available {} {}",
             beginIndex, endIndex, atozMinFrequency, atozMaxFrequency, !used, bitSetFreq);
        //if used = true then bit must be set to false to indicate the slot is no more available
        bitSetFreq.set(beginIndex, endIndex, !used);
        LOG.debug(
            "Updated frequency map from index {}, to index {}, min frequency {}, max frequency {}, available {} {}",
            beginIndex, endIndex, atozMinFrequency, atozMaxFrequency, !used, bitSetFreq);
        AvailFreqMaps updatedAvailFreqMaps = new AvailFreqMapsBuilder(availFreqMaps)
                .setFreqMap(Arrays.copyOf(bitSetFreq.toByteArray(), GridConstant.NB_OCTECTS))
                .build();
        return new HashMap<>(Map.of(availFreqMaps.key(), updatedAvailFreqMaps));
    }
}
