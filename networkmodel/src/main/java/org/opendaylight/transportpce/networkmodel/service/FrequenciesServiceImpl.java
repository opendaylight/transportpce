/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
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
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.NodeIdPair;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.RxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrequenciesServiceImpl implements FrequenciesService {

    private static final Logger LOG = LoggerFactory.getLogger(FrequenciesServiceImpl.class);
    private final DataBroker dataBroker;
    private final AvailFreqMapsKey availFreqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);

    public FrequenciesServiceImpl(DataBroker dataBroker) {
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
            BigDecimal atozMinFrequency = atoZDirection.getAToZMinFrequency().getValue();
            BigDecimal atozMaxFrequency = atoZDirection.getAToZMaxFrequency().getValue();
            Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
                    .forName(atoZDirection.getModulationFormat());
            if (!optionalModulationFormat.isPresent()) {
                LOG.error("Unknown modulation format {} for a to z direction, frequencies not updated",
                        atoZDirection.getModulationFormat());
                return;
            }
            setFrequencies4Tps(atozMinFrequency, atozMaxFrequency, atoZDirection.getRate(),
                    optionalModulationFormat.get(), atozTpIds, used);
            setFrequencies4Nodes(atozMinFrequency,
                    atozMaxFrequency,
                    atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                    used);
        }
        if (ztoADirection != null && ztoADirection.getZToAMinFrequency() != null) {
            LOG.info("Update frequencies for z to a direction {}, used {}", ztoADirection, used);
            List<NodeIdPair> ztoaTpIds = getZToATpList(ztoADirection);
            BigDecimal ztoaMinFrequency = ztoADirection.getZToAMinFrequency().getValue();
            BigDecimal ztoaMaxFrequency = ztoADirection.getZToAMaxFrequency().getValue();
            Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
                    .forName(ztoADirection.getModulationFormat());
            if (!optionalModulationFormat.isPresent()) {
                LOG.error("Unknown modulation format {} for z to a direction, frequencies not updated",
                        ztoADirection.getModulationFormat());
                return;
            }
            setFrequencies4Tps(ztoaMinFrequency, ztoaMaxFrequency, ztoADirection.getRate(),
                    optionalModulationFormat.get(), ztoaTpIds, used);
            setFrequencies4Nodes(ztoaMinFrequency,
                    ztoaMaxFrequency,
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
        InstanceIdentifier<Node1> nodeIID = OpenRoadmTopology.createNetworkNodeIID(nodeId);
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Node1> optionalNode = nodeReadTx.read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalNode.isPresent()) {
                return optionalNode.get();
            } else {
                LOG.error("Unable to get network node for node id {} from topology {}", nodeId,
                        NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting network node for node id {} from {} topology", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting network node for node id {} from {} topology was interrupted", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Get common network node with nodeId from datastore.
     * @param nodeId String
     * @return Node1, null otherwise.
     */
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1
        getCommonNetworkNodeFromDatastore(String nodeId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1> nodeIID =
                OpenRoadmTopology.createCommonNetworkNodeIID(nodeId);
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1> optionalNode =
                    nodeReadTx
                    .read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalNode.isPresent()) {
                return optionalNode.get();
            } else {
                LOG.error("Unable to get common network node for node id {} from topology {}", nodeId,
                        NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting common network node for node id {} from {} topology", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting common network node for node id {} from {} topology was interrupted", nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Set frequency map for nodes in nodeIds.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param nodeIds List of node id
     * @param used boolean true if min and max frequencies are used, false otherwise.
     */
    private void setFrequencies4Nodes(BigDecimal minFrequency, BigDecimal maxFrequency,
            List<String> nodeIds, boolean used) {
        updateFreqMaps4Nodes(nodeIds, minFrequency, maxFrequency, used);
    }


    /**
     * Get a network termination point for nodeId and tpId.
     * @param nodeId String
     * @param tpId String
     * @return network termination point, null otherwise
     */
    private TerminationPoint1 getNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<TerminationPoint1> tpIID = OpenRoadmTopology
                .createNetworkTerminationPointIIDBuilder(nodeId, tpId).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<TerminationPoint1> optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalTerminationPoint.isPresent()) {
                return optionalTerminationPoint.get();
            } else {
                return null;
            }
        } catch (ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination {} for node id {} point from {} topology", tpId, nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
            return null;
        } catch (InterruptedException e) {
            LOG.warn("Getting termination {} for node id {} point from {} topology was interrupted", tpId, nodeId,
                    NetworkUtils.OVERLAY_NETWORK_ID, e);
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
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1
        getCommonNetworkTerminationPointFromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1>
            tpIID = OpenRoadmTopology.createCommonNetworkTerminationPointIIDBuilder(nodeId, tpId).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1>
                optionalTerminationPoint = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
            if (optionalTerminationPoint.isPresent()) {
                return optionalTerminationPoint.get();
            } else {
                LOG.error("Unable to get common-network termination point {} for node id {}from topology {}", tpId,
                        nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                return null;
            }

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
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param tpIds List of NodeIdPair
     * @param sed boolean true if min and max frequencies are used, false otherwise.
     */
    private void setFrequencies4Tps(BigDecimal minFrequency, BigDecimal maxFrequency, Uint32 rate,
            ModulationFormat modulationFormat, List<NodeIdPair> tpIds, boolean used) {
        String strTpIdsList = String.join(", ", tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList()));
        LOG.debug("Update frequencies for termination points {}, rate {}, modulation format {},"
                + "min frequency {}, max frequency {}, used {}", strTpIdsList, rate, modulationFormat,
                minFrequency, maxFrequency, used);
        WriteTransaction updateFrequenciesTransaction = this.dataBroker.newWriteOnlyTransaction();
        for (NodeIdPair idPair : tpIds) {
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1
                commonNetworkTerminationPoint = getCommonNetworkTerminationPointFromDatastore(idPair.getNodeID(),
                    idPair.getTpID());
            if (commonNetworkTerminationPoint == null) {
                LOG.warn("Cannot update frequencies for termination point {}, node id {}", idPair.getTpID(),
                        idPair.getNodeID());
                continue;
            }
            TerminationPoint1 networkTerminationPoint =
                    getNetworkTerminationPointFromDatastore(idPair.getNodeID(), idPair.getTpID());
            TerminationPoint1Builder networkTerminationPointBuilder;
            if (networkTerminationPoint != null) {
                networkTerminationPointBuilder = new TerminationPoint1Builder(networkTerminationPoint);
            } else {
                networkTerminationPointBuilder = new TerminationPoint1Builder();
            }
            switch (commonNetworkTerminationPoint.getTpType()) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    networkTerminationPointBuilder.setTxTtpAttributes(updateTxTtpAttributes(networkTerminationPoint,
                            minFrequency,maxFrequency,used));
                    break;
                case DEGREERXTTP:
                    networkTerminationPointBuilder.setRxTtpAttributes(updateRxTtpAttributes(networkTerminationPoint,
                            minFrequency,maxFrequency,used));
                    break;
                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    networkTerminationPointBuilder.setCtpAttributes(updateCtpAttributes(networkTerminationPoint,
                            minFrequency,maxFrequency,used));
                    break;
                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    networkTerminationPointBuilder.setCpAttributes(updateCpAttributes(networkTerminationPoint,
                            minFrequency,maxFrequency,used));
                    break;
                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    networkTerminationPointBuilder.setPpAttributes(updatePpAttributes(networkTerminationPoint,
                            minFrequency,maxFrequency,used));
                    break;
                case XPONDERNETWORK:
                    networkTerminationPointBuilder.setXpdrNetworkAttributes(
                            updateXpdrNetworkAttributes(networkTerminationPoint,
                                    minFrequency, maxFrequency, rate, modulationFormat, used)).build();
                    break;
                case XPONDERCLIENT:
                    break;
                case XPONDERPORT:
                    networkTerminationPointBuilder.setXpdrPortAttributes(
                            updateXpdrPortAttributes(networkTerminationPoint,
                                    minFrequency, maxFrequency, rate, modulationFormat, used)).build();
                    break;
                default:
                    LOG.warn("Termination point type {} not managed", commonNetworkTerminationPoint.getTpType());
                    return;
            }
            updateFrequenciesTransaction.put(LogicalDatastoreType.CONFIGURATION, OpenRoadmTopology
                    .createNetworkTerminationPointIIDBuilder(idPair.getNodeID(),
                            idPair.getTpID()).build(), networkTerminationPointBuilder.build());
        }
        try {
            updateFrequenciesTransaction.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error(
                "Something went wrong for frequencies update (min frequency {}, max frequency {}, used {} for TPs {}",
                minFrequency, maxFrequency, used, strTpIdsList, e);
        } catch (InterruptedException e) {
            LOG.error("Frequencies update (min frequency {}, max frequency {}, used {} for TPs {} was interrupted",
                    minFrequency, maxFrequency, used,
                    strTpIdsList, e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Update availFreqMapsMap for min and max frequencies for nodes in nodeIds.
     * @param nodeIds  List of node id
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     */
    private void updateFreqMaps4Nodes(List<String> nodeIds, BigDecimal minFrequency, BigDecimal maxFrequency,
            boolean used) {
        String strNodesList = String.join(", ", nodeIds);
        LOG.debug("Update frequencies for nodes {}, min frequency {}, max frequency {}, used {}",
                strNodesList, minFrequency, maxFrequency, used);
        WriteTransaction updateFrequenciesTransaction = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Node1 networkNode = getNetworkNodeFromDatastore(nodeId);
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 commonNetworkNode =
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
                            updateDegreeAttributes(networkNode.getDegreeAttributes(), minFrequency,
                                    maxFrequency, used));
                    break;
                case SRG:
                    networkNodeBuilder.setSrgAttributes(
                            updateSrgAttributes(networkNode.getSrgAttributes(), minFrequency, maxFrequency, used));
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
            LOG.error("Cannot update frequencies {} {} for nodes {}", minFrequency, maxFrequency,
                    strNodesList, e);
        } catch (InterruptedException e) {
            LOG.error("Update of frequencies {} {} for nodes {} was interrupted", minFrequency, maxFrequency,
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
        Collection<AToZ> atozList = atoZDirection.nonnullAToZ().values();
        return atozList.stream()
                .filter(aToZ -> {
                    if ((aToZ.getResource() == null) || (aToZ.getResource().getResource() == null)) {
                        LOG.warn("Resource of AToZ node {} is null! Skipping this node!", aToZ.getId());
                        return false;
                    }
                    return aToZ.getResource().getResource() instanceof TerminationPoint;
                }).map(aToZ -> {
                    TerminationPoint tp = (TerminationPoint) aToZ.getResource().getResource();
                    if ((tp == null) || (tp.getTpNodeId() == null) ||  (tp.getTpId() == null)
                        || tp.getTpId().isEmpty()) {
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
        Collection<ZToA> ztoaList = ztoADirection.nonnullZToA().values();
        return ztoaList.stream()
                .filter(zToA -> {
                    if ((zToA.getResource() == null) || (zToA.getResource().getResource() == null)) {
                        LOG.warn("Resource of ZToA node {} is null! Skipping this node!", zToA.getId());
                        return false;
                    }
                    return zToA.getResource().getResource() instanceof TerminationPoint;
                }).map(zToA -> {
                    TerminationPoint tp = (TerminationPoint) zToA.getResource().getResource();
                    if ((tp == null) || (tp.getTpNodeId() == null) ||  (tp.getTpId() == null)
                        || tp.getTpId().isEmpty()) {
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
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return XpdrPortAttributes with Wavelength updated
     */
    private XpdrPortAttributes updateXpdrPortAttributes(TerminationPoint1 networkTerminationPoint,
            BigDecimal minFrequency, BigDecimal maxFrequency, Uint32 rate, ModulationFormat modulationFormat,
            boolean used) {
        LOG.debug("Update xpdr node attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        XpdrPortAttributesBuilder xpdrPortAttributesBuilder;
        if (networkTerminationPoint != null) {
            xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder(networkTerminationPoint.getXpdrPortAttributes());
        } else {
            xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder();
        }
        WavelengthBuilder waveLengthBuilder = new WavelengthBuilder();
        if (used) {
            waveLengthBuilder.setWidth(GridUtils.getWidthFromRateAndModulationFormat(rate, modulationFormat))
                    .setFrequency(GridUtils.getCentralFrequency(minFrequency, maxFrequency));
        }
        return xpdrPortAttributesBuilder.setWavelength(waveLengthBuilder.build()).build();
    }

    /**
     * Update Wavelength for xpdr network attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param rate Uint32
     * @param modulationFormat ModulationFormat
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return XpdrNetworkAttributes with Wavelength updated
     */
    private XpdrNetworkAttributes updateXpdrNetworkAttributes(TerminationPoint1 networkTerminationPoint,
            BigDecimal minFrequency, BigDecimal maxFrequency, Uint32 rate, ModulationFormat modulationFormat,
            boolean used) {
        LOG.debug("Update xpdr node attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        XpdrNetworkAttributesBuilder xpdrNetworkAttributesBuilder;
        if (networkTerminationPoint != null) {
            xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder(
                    networkTerminationPoint.getXpdrNetworkAttributes());
        } else {
            xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder();
        }
        WavelengthBuilder waveLengthBuilder = new WavelengthBuilder();
        if (used) {
            waveLengthBuilder.setWidth(GridUtils.getWidthFromRateAndModulationFormat(rate, modulationFormat))
                    .setFrequency(GridUtils.getCentralFrequency(minFrequency, maxFrequency));
            xpdrNetworkAttributesBuilder.setWavelength(waveLengthBuilder.build());
        } else {
            xpdrNetworkAttributesBuilder.setWavelength(null);
        }
        return xpdrNetworkAttributesBuilder.build();
    }

    /**
     * Update freqMaps for pp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return PpAttributes with frequency map updated
     */
    private PpAttributes updatePpAttributes(TerminationPoint1 networkTerminationPoint, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        LOG.debug("Update pp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        PpAttributesBuilder ppAttributesBuilder;
        if (networkTerminationPoint != null) {
            ppAttributesBuilder = new PpAttributesBuilder(networkTerminationPoint.getPpAttributes());
        } else {
            ppAttributesBuilder = new PpAttributesBuilder();
        }
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = ppAttributesBuilder.getAvailFreqMaps();
        return ppAttributesBuilder.setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used))
                .build();
    }

    /**
     * Update freqMaps for cp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return CpAttributes with frequency map updated
     */
    private CpAttributes updateCpAttributes(TerminationPoint1 networkTerminationPoint, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        LOG.debug("Update cp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        CpAttributesBuilder cpAttributesBuilder;
        if (networkTerminationPoint != null) {
            cpAttributesBuilder = new CpAttributesBuilder(networkTerminationPoint.getCpAttributes());
        } else {
            cpAttributesBuilder = new CpAttributesBuilder();
        }
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = cpAttributesBuilder.getAvailFreqMaps();
        return cpAttributesBuilder.setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used))
                .build();
    }

    /**
     * Update freqMaps for ctp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return CtpAttributes with frequency map updated
     */
    private CtpAttributes updateCtpAttributes(TerminationPoint1 networkTerminationPoint, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        LOG.debug("Update ctp attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        CtpAttributesBuilder ctpAttributesBuilder;
        if (networkTerminationPoint != null) {
            ctpAttributesBuilder = new CtpAttributesBuilder(networkTerminationPoint.getCtpAttributes());
        } else {
            ctpAttributesBuilder = new CtpAttributesBuilder();
        }
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = ctpAttributesBuilder.getAvailFreqMaps();
        return ctpAttributesBuilder.setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used))
                .build();
    }

    /**
     * Update freqMaps for rxtp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return RxTtpAttributes with frequency map updated
     */
    private RxTtpAttributes updateRxTtpAttributes(TerminationPoint1 networkTerminationPoint, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        LOG.debug("Update rx attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        RxTtpAttributesBuilder rxTtpAttributesBuilder;
        if (networkTerminationPoint != null) {
            rxTtpAttributesBuilder = new RxTtpAttributesBuilder(networkTerminationPoint.getRxTtpAttributes());
        } else {
            rxTtpAttributesBuilder = new RxTtpAttributesBuilder();
        }
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = rxTtpAttributesBuilder.getAvailFreqMaps();
        return rxTtpAttributesBuilder
                .setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used)).build();
    }

    /**
     * Update freqMaps for txtp attributes.
     * @param networkTerminationPoint TerminationPoint1
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return TxTtpAttributes with frequency map updated
     */
    private TxTtpAttributes updateTxTtpAttributes(TerminationPoint1 networkTerminationPoint, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        LOG.debug("Update tx attributes for termination point {}, min frequency {}, max frequency {}, used {}",
                networkTerminationPoint, minFrequency, maxFrequency, used);
        TxTtpAttributesBuilder txTtpAttributesBuilder;
        if (networkTerminationPoint != null) {
            txTtpAttributesBuilder = new TxTtpAttributesBuilder(networkTerminationPoint.getTxTtpAttributes());
        } else {
            txTtpAttributesBuilder = new TxTtpAttributesBuilder();
        }
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = txTtpAttributesBuilder.getAvailFreqMaps();
        return txTtpAttributesBuilder
                .setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used)).build();
    }

    /**
     * Update freqMaps for srg attributes of srgAttributes.
     * @param srgAttributes SrgAttributes
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return SrgAttributes with frequency map updated
     */
    private SrgAttributes updateSrgAttributes(SrgAttributes srgAttributes, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap;
        SrgAttributesBuilder srgAttributesBuilder;
        if (srgAttributes == null) {
            srgAttributesBuilder = new SrgAttributesBuilder();
        } else {
            srgAttributesBuilder = new SrgAttributesBuilder(srgAttributes);
        }
        availFreqMapsMap = srgAttributesBuilder.getAvailFreqMaps();
        return srgAttributesBuilder.setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used))
                .build();
    }

    /**
     * Update freqMaps for degree attributes of degreeAttributes.
     * @param degreeAttributes DegreeAttributes
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param used boolean true if min and max frequencies are used, false otherwise.
     * @return DegreeAttributes with frequency map updated
     */
    private DegreeAttributes updateDegreeAttributes(DegreeAttributes degreeAttributes, BigDecimal minFrequency,
            BigDecimal maxFrequency, boolean used) {
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap;
        DegreeAttributesBuilder degreeAttributesBuilder;
        if (degreeAttributes == null) {
            degreeAttributesBuilder = new DegreeAttributesBuilder();
        } else {
            degreeAttributesBuilder = new DegreeAttributesBuilder(degreeAttributes);
        }
        availFreqMapsMap = degreeAttributesBuilder.getAvailFreqMaps();
        return degreeAttributesBuilder
                .setAvailFreqMaps(updateFreqMaps(minFrequency, maxFrequency, availFreqMapsMap, used)).build();
    }

    /**
     * Update availFreqMapsMap for min and max frequencies for cband AvailFreqMaps.
     * @param minFrequency BigDecimal
     * @param maxFrequency BigDecimal
     * @param availFreqMapsMap Map
     * @param used boolean
     * @return updated Update availFreqMapsMap for min and max frequencies for cband AvailFreqMaps.
     */
    private Map<AvailFreqMapsKey, AvailFreqMaps> updateFreqMaps(BigDecimal minFrequency, BigDecimal maxFrequency,
            Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap, boolean used) {
        int beginIndex = GridUtils.getIndexFromFrequency(minFrequency);
        int endIndex = GridUtils.getIndexFromFrequency(maxFrequency);
        if (availFreqMapsMap == null) {
            availFreqMapsMap = GridUtils.initFreqMaps4FixedGrid2Available();
        }
        AvailFreqMaps availFreqMaps = availFreqMapsMap.get(availFreqMapKey);
        if (availFreqMaps != null && availFreqMaps.getFreqMap() != null) {
            BitSet bitSetFreq = BitSet.valueOf(availFreqMaps.getFreqMap());
            LOG.debug(
                 "Update frequency map from index {}, to index {}, min frequency {}, max frequency {}, available {} {}",
                 beginIndex, endIndex, minFrequency, maxFrequency, !used, bitSetFreq);
            //if used = true then bit must be set to false to indicate the slot is no more available
            bitSetFreq.set(beginIndex, endIndex, !used);
            LOG.debug(
                "Updated frequency map from index {}, to index {}, min frequency {}, max frequency {}, available {} {}",
                beginIndex, endIndex, minFrequency, maxFrequency, !used, bitSetFreq);
            Map<AvailFreqMapsKey, AvailFreqMaps> updatedFreqMaps = new HashMap<>();
            byte[] frequenciesByteArray = bitSetFreq.toByteArray();
            AvailFreqMaps updatedAvailFreqMaps = new AvailFreqMapsBuilder(availFreqMaps)
                    .setFreqMap(Arrays.copyOf(frequenciesByteArray,GridConstant.NB_OCTECTS))
                    .build();
            updatedFreqMaps.put(availFreqMaps.key(), updatedAvailFreqMaps);
            return updatedFreqMaps;
        }
        return availFreqMapsMap;
    }
}