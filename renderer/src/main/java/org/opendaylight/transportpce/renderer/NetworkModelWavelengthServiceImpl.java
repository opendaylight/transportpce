/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.math.BigDecimal;
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
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexImpl;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev181130.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev181130.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.used.wavelengths.UsedWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.RxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrClientAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrPortAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.pp.attributes.UsedWavelengthKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev181130.xpdr.port.connection.attributes.Wavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev181130.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelWavelengthServiceImpl implements NetworkModelWavelengthService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelWavelengthServiceImpl.class);
    private final DataBroker dataBroker;

    public NetworkModelWavelengthServiceImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void useWavelengths(PathDescription pathDescription) {

        List<NodeIdPair> atozTpIds = getAToZTpList(pathDescription);
        atozTpIds.removeIf(Objects::isNull);
        deleteAvailableWL(atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getAToZDirection().getAToZWavelengthNumber().toJava());
        List<NodeIdPair> ztoaTpIds = getZToATpList(pathDescription);
        ztoaTpIds.removeIf(Objects::isNull);
        deleteAvailableWL(ztoaTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getZToADirection().getZToAWavelengthNumber().toJava());
        addUsedWL(pathDescription.getAToZDirection().getAToZWavelengthNumber().toJava(), atozTpIds);
        addUsedWL(pathDescription.getZToADirection().getZToAWavelengthNumber().toJava(), ztoaTpIds);
    }

    @Override
    public void freeWavelengths(PathDescription pathDescription) {
        List<NodeIdPair> atozTpIds = getAToZTpList(pathDescription);
        List<NodeIdPair> ztoaTpIds = getZToATpList(pathDescription);
        atozTpIds.removeIf(Objects::isNull);
        ztoaTpIds.removeIf(Objects::isNull);
        deleteUsedWL(pathDescription.getAToZDirection().getAToZWavelengthNumber().toJava(), atozTpIds);
        deleteUsedWL(pathDescription.getZToADirection().getZToAWavelengthNumber().toJava(), ztoaTpIds);
        addAvailableWL(atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getAToZDirection().getAToZWavelengthNumber().toJava());
        addAvailableWL(ztoaTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getZToADirection().getZToAWavelengthNumber().toJava());
    }

    private List<NodeIdPair> getAToZTpList(PathDescription pathDescription) {
        Collection<AToZ> atozList = pathDescription.getAToZDirection().nonnullAToZ().values();
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
                }).collect(Collectors.toList());
    }

    private List<NodeIdPair> getZToATpList(PathDescription pathDescription) {
        Collection<ZToA> ztoaList = pathDescription.getZToADirection().nonnullZToA().values();
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
                }).collect(Collectors.toList());
    }

    private InstanceIdentifier<Node1> createNode1IID(String nodeId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId))).augmentation(Node1.class).build();
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
        .Node1> createNode2IID(String nodeId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId))).augmentation(org.opendaylight.yang.gen.v1.http.org
                .openroadm.common.network.rev181130.Node1.class).build();
    }

    private Optional<Node1> getNode1FromDatastore(String nodeId) {
        InstanceIdentifier<Node1>
                nodeIID = createNode1IID(nodeId);
        Optional<Node1> nodeOpt;
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            nodeOpt = nodeReadTx.read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting node from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID, e);
            nodeOpt = Optional.empty();
        }
        return nodeOpt;
    }

    private Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
        .Node1> getNode2FromDatastore(String nodeId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1>
                nodeIID = createNode2IID(nodeId);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1> nodeOpt;
        try (ReadTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            nodeOpt = nodeReadTx.read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting node from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID, e);
            nodeOpt = Optional.empty();
        }
        return nodeOpt;
    }

    private void addAvailableWL(List<String> nodeIds, Long wavelengthNumber) {
        WriteTransaction nodeWriteTx = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Optional<Node1> node1Opt = getNode1FromDatastore(nodeId);
            Node1 node1;
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1> node2Opt =
                getNode2FromDatastore(nodeId);
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 node2;
            if (node2Opt.isPresent()) {
                node2 = node2Opt.get();
            } else {
                LOG.error("Unable to get common-network node {} from topology {}! Skipping addition of available"
                    + "wavelength for this node.", nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            if (node1Opt.isPresent()) {
                node1 = node1Opt.get();
            } else {
                LOG.error("Unable to get network-topology node {} from topology {}! Skipping addition of available"
                    + "wavelength for this node.", nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }

            Node1Builder node1Builder = new Node1Builder(node1);
            switch (node2.getNodeType()) {
                case DEGREE:
                    DegreeAttributes degreeAttributes = node1.getDegreeAttributes();
                    DegreeAttributesBuilder degreeAttributesBuilder;
                    if (degreeAttributes == null) {
                        degreeAttributesBuilder = new DegreeAttributesBuilder();
                    } else {
                        degreeAttributesBuilder = new DegreeAttributesBuilder(degreeAttributes);
                    }
                    Map<org.opendaylight.yang.gen.v1.http.org.openroadm.degree
                        .rev181130.degree.node.attributes.AvailableWavelengthsKey,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.degree
                        .rev181130.degree.node.attributes.AvailableWavelengths> availableDegreeWLs =
                        Map.copyOf(degreeAttributesBuilder.getAvailableWavelengths());
                    AvailableWavelengths availableWaveLength = new org.opendaylight.yang.gen.v1.http.org.openroadm
                            .degree.rev181130.degree
                            .node.attributes.AvailableWavelengthsBuilder().setIndex(Uint32.valueOf(wavelengthNumber))
                            .build();
                    availableDegreeWLs.put(availableWaveLength.key(), availableWaveLength);
                    degreeAttributesBuilder.setAvailableWavelengths(availableDegreeWLs);
                    node1Builder.setDegreeAttributes(degreeAttributesBuilder.build());
                    break;
                case SRG:
                    SrgAttributes srgAttributes = node1.getSrgAttributes();
                    SrgAttributesBuilder srgAttributesBuilder;
                    if (srgAttributes == null) {
                        srgAttributesBuilder = new SrgAttributesBuilder();
                    } else {
                        srgAttributesBuilder = new SrgAttributesBuilder(srgAttributes);
                    }
                    Map<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                        .AvailableWavelengthsKey,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                        .AvailableWavelengths> availableSrgWLs = Map.copyOf(srgAttributesBuilder
                                .getAvailableWavelengths());
                    org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130
                        .srg.node.attributes.AvailableWavelengths aval =
                            new AvailableWavelengthsBuilder().setIndex(Uint32.valueOf(wavelengthNumber))
                                    .build();
                    availableSrgWLs.put(aval.key(),aval);
                    srgAttributesBuilder.setAvailableWavelengths(availableSrgWLs);
                    node1Builder.setSrgAttributes(srgAttributesBuilder.build());
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            nodeWriteTx.put(LogicalDatastoreType.CONFIGURATION, createNode1IID(nodeId), node1Builder.build());
        }
        try {
            nodeWriteTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to add available WL {} for nodes {}!", wavelengthNumber, String.join(", ", nodeIds), e);
        }
    }

    private void deleteAvailableWL(List<String> nodeIds, Long wavelengthNumber) {
        WriteTransaction nodeWriteTx = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1> nodeOpt =
                getNode2FromDatastore(nodeId);
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 node;
            if (nodeOpt.isPresent()) {
                node = nodeOpt.get();
            } else {
                LOG.error(
                    "Unable to get node {} from topology {}! Skipping addition of available wavelength for this node.",
                         nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }

            InstanceIdentifierBuilder<Node1> nodeIIDBuilder = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(Node.class, new NodeKey(new NodeId(nodeId))).augmentation(Node1.class);
            InstanceIdentifier availableWlIID;

            switch (node.getNodeType()) {
            //switch (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1) node)
            //        .getNodeType()) {
                case DEGREE:
                    availableWlIID = nodeIIDBuilder.child(DegreeAttributes.class)
                            .child(org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node
                                    .attributes.AvailableWavelengths.class,
                                    new AvailableWavelengthsKey(Uint32.valueOf(wavelengthNumber)))
                            .build();
                    break;
                case SRG:
                    availableWlIID = nodeIIDBuilder.child(SrgAttributes.class)
                            .child(org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                                            .AvailableWavelengths.class,
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node
                                            .attributes.AvailableWavelengthsKey(Uint32.valueOf(wavelengthNumber)))
                            .build();
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            nodeWriteTx.delete(LogicalDatastoreType.CONFIGURATION, availableWlIID);
        }
        try {
            nodeWriteTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to delete available WL {} for nodes {}!", wavelengthNumber, String.join(", ", nodeIds),
                    e);
        }
    }

    private InstanceIdentifierBuilder<TerminationPoint1> createTerminationPoint1IIDBuilder(String nodeId, String tpId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml
                .ns.yang.ietf.network.rev180226.networks.network.Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                .rev180226.networks.network.node.TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
                .augmentation(TerminationPoint1.class);
    }

    private InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
        .TerminationPoint1> createTerminationPoint2IIDBuilder(String nodeId, String tpId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml
                .ns.yang.ietf.network.rev180226.networks.network.Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                .rev180226.networks.network.node.TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                    .TerminationPoint1.class);
    }

    private Optional<TerminationPoint1> getTerminationPoint1FromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<TerminationPoint1> tpIID = createTerminationPoint1IIDBuilder(nodeId, tpId).build();
        Optional<TerminationPoint1> tpOpt;
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            tpOpt = readTx.read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination point from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID,
                    e);
            tpOpt = Optional.empty();
        }
        return tpOpt;
    }

    private Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
        .TerminationPoint1> getTerminationPoint2FromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1> tpIID = createTerminationPoint2IIDBuilder(nodeId, tpId).build();
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1> tpOpt;
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            tpOpt = readTx.read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination point from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID,
                    e);
            tpOpt = Optional.empty();
        }
        return tpOpt;
    }

    private void deleteUsedWL(long wavelengthIndex, List<NodeIdPair> tpIds) {
        WriteTransaction deleteUsedWlTx = this.dataBroker.newWriteOnlyTransaction();
        for (NodeIdPair idPair : tpIds) {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1> tp2Opt = getTerminationPoint2FromDatastore(idPair.getNodeID(), idPair.getTpID());

            OpenroadmTpType tpType;
            if (tp2Opt.isPresent()) {
                tpType = tp2Opt.get().getTpType();
                //    ((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1)
                //        tpOpt.get()).getTpType();
            } else {
                LOG.error("Unable to get termination point {} from topology {}! Skipping removal of used wavelength"
                        + " for this node.", idPair.getTpID(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            InstanceIdentifier.InstanceIdentifierBuilder<TerminationPoint1> usedWlIIDBuilder =
                    createTerminationPoint1IIDBuilder(idPair.getNodeID(), idPair.getTpID());
            InstanceIdentifier usedWlIID;
            switch (tpType) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    usedWlIID = usedWlIIDBuilder.child(TxTtpAttributes.class).child(UsedWavelengths.class,
                            new UsedWavelengthsKey(Uint32.valueOf(wavelengthIndex))).build();
                    break;

                case DEGREERXTTP:
                    usedWlIID = usedWlIIDBuilder.child(RxTtpAttributes.class).child(UsedWavelengths.class,
                            new UsedWavelengthsKey(Uint32.valueOf(wavelengthIndex))).build();
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    usedWlIID = usedWlIIDBuilder.child(CtpAttributes.class).child(UsedWavelengths.class,
                            new UsedWavelengthsKey(Uint32.valueOf(wavelengthIndex))).build();
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    usedWlIID = usedWlIIDBuilder.child(CpAttributes.class).child(org.opendaylight.yang.gen.v1.http.org
                        .openroadm.network.topology.rev181130.networks.network.node.termination.point.cp.attributes
                        .UsedWavelengths.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology
                        .rev181130.networks.network.node.termination.point.cp.attributes
                        .UsedWavelengthsKey(Uint32.valueOf(wavelengthIndex))).build();
                    break;

                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    usedWlIID = usedWlIIDBuilder.child(PpAttributes.class).child(UsedWavelength.class,
                            new UsedWavelengthKey(Uint32.valueOf(wavelengthIndex))).build();
                    break;

                case XPONDERNETWORK:
                    usedWlIID = usedWlIIDBuilder.child(XpdrNetworkAttributes.class).child(Wavelength.class).build();
                    break;
                case XPONDERCLIENT:
                    usedWlIID = usedWlIIDBuilder.child(XpdrClientAttributes.class).child(Wavelength.class).build();
                    break;
                case XPONDERPORT:
                    usedWlIID = usedWlIIDBuilder.child(XpdrPortAttributes.class).child(Wavelength.class).build();
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION, usedWlIID);
        }
        try {
            deleteUsedWlTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            List<String> tpIdsString = tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList());
            LOG.error("Unable to delete used WL {} from TPs {}!", wavelengthIndex, String.join(", ", tpIdsString), e);
        }
    }

    private void addUsedWL(long wavelengthIndex, List<NodeIdPair> tpIds) {
        WriteTransaction addUsedWlTx = this.dataBroker.newWriteOnlyTransaction();
        FixedFlexImpl fixedFlex = new FixedFlexImpl(wavelengthIndex);
        FrequencyTHz centralTHz = new FrequencyTHz(new BigDecimal(fixedFlex.getCenterFrequency()));
        for (NodeIdPair idPair : tpIds) {
            Optional<TerminationPoint1> tp1Opt =
                getTerminationPoint1FromDatastore(idPair.getNodeID(), idPair.getTpID());
            TerminationPoint1 tp1 = null;
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
                .TerminationPoint1> tp2Opt = getTerminationPoint2FromDatastore(idPair.getNodeID(), idPair.getTpID());
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 tp2;
            if (tp2Opt.isPresent()) {
                tp2 = tp2Opt.get();
            } else {
                LOG.error(
                    "Unable to get common-network termination point {} from topology {}! Skip removal of used"
                    + "wavelength for the node", idPair.getTpID(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            TerminationPoint1Builder tp1Builder;
            if (tp1Opt.isPresent()) {
                tp1 = tp1Opt.get();
                tp1Builder = new TerminationPoint1Builder(tp1);
            } else {
                tp1Builder = new TerminationPoint1Builder();
            }

            UsedWavelengths usedWaveLength = new UsedWavelengthsBuilder().setIndex(Uint32.valueOf(wavelengthIndex))
                .setFrequency(centralTHz).setWidth(FrequencyGHz.getDefaultInstance("40")).build();
            switch (tp2.getTpType()) {
            //switch (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1) tp)
            //        .getTpType()) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    TxTtpAttributes txTtpAttributes = null;
                    Map<UsedWavelengthsKey,UsedWavelengths> usedDegreeTxTtpWls;
                    if (tp1 != null) {
                        txTtpAttributes = tp1.getTxTtpAttributes();
                    }
                    TxTtpAttributesBuilder txTtpAttributesBuilder;
                    if (txTtpAttributes == null) {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder();
                        usedDegreeTxTtpWls = new HashMap<>();
                    } else {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder(txTtpAttributes);
                        usedDegreeTxTtpWls = Map.copyOf(txTtpAttributesBuilder.getUsedWavelengths());
                    }
                    usedDegreeTxTtpWls.put(usedWaveLength.key(),usedWaveLength);
                    txTtpAttributesBuilder.setUsedWavelengths(usedDegreeTxTtpWls);
                    tp1Builder.setTxTtpAttributes(txTtpAttributesBuilder.build());
                    break;

                case DEGREERXTTP:
                    RxTtpAttributes rxTtpAttributes = null;
                    Map<UsedWavelengthsKey,UsedWavelengths> usedDegreeRxTtpWls;
                    if (tp1 != null) {
                        rxTtpAttributes = tp1.getRxTtpAttributes();
                    }
                    RxTtpAttributesBuilder rxTtpAttributesBuilder;
                    if (rxTtpAttributes == null) {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder();
                        usedDegreeRxTtpWls = new HashMap<>();
                    } else {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder(rxTtpAttributes);
                        usedDegreeRxTtpWls = Map.copyOf(rxTtpAttributesBuilder.getUsedWavelengths());
                    }
                    usedDegreeRxTtpWls.put(usedWaveLength.key(),usedWaveLength);
                    rxTtpAttributesBuilder.setUsedWavelengths(usedDegreeRxTtpWls);
                    tp1Builder.setRxTtpAttributes(rxTtpAttributesBuilder.build());
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    CtpAttributes ctpAttributes = null;
                    Map<UsedWavelengthsKey,UsedWavelengths> usedDegreeCtpWls;
                    if (tp1 != null) {
                        ctpAttributes = tp1.getCtpAttributes();
                    }
                    CtpAttributesBuilder ctpAttributesBuilder;
                    if (ctpAttributes == null) {
                        ctpAttributesBuilder = new CtpAttributesBuilder();
                        usedDegreeCtpWls = new HashMap<>();
                    } else {
                        ctpAttributesBuilder = new CtpAttributesBuilder(ctpAttributes);
                        usedDegreeCtpWls = Map.copyOf(ctpAttributesBuilder.getUsedWavelengths());
                    }
                    usedDegreeCtpWls.put(usedWaveLength.key(),usedWaveLength);
                    ctpAttributesBuilder.setUsedWavelengths(usedDegreeCtpWls);
                    tp1Builder.setCtpAttributes(ctpAttributesBuilder.build());
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    CpAttributes cpAttributes = null;
                    Map<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network
                        .node.termination.point.cp.attributes.UsedWavelengthsKey,
                        org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network
                        .node.termination.point.cp.attributes.UsedWavelengths> usedDegreeCpWls;
                    if (tp1 != null) {
                        cpAttributes = tp1.getCpAttributes();
                    }
                    CpAttributesBuilder cpAttributesBuilder;
                    if (cpAttributes == null) {
                        cpAttributesBuilder = new CpAttributesBuilder();
                        usedDegreeCpWls = new HashMap<>();
                    } else {
                        cpAttributesBuilder = new CpAttributesBuilder(cpAttributes);
                        usedDegreeCpWls = Map.copyOf(cpAttributesBuilder.getUsedWavelengths());
                    }
                    org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks
                        .network.node.termination.point.cp.attributes.UsedWavelengths cpUsedWaveLength =
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130
                                .networks.network.node.termination.point.cp.attributes.UsedWavelengthsBuilder()
                                .setIndex(Uint32.valueOf(wavelengthIndex))
                                .setFrequency(centralTHz).setWidth(FrequencyGHz.getDefaultInstance("40")).build();
                    usedDegreeCpWls.put(cpUsedWaveLength.key(),cpUsedWaveLength);
                    cpAttributesBuilder.setUsedWavelengths(usedDegreeCpWls);
                    tp1Builder.setCpAttributes(cpAttributesBuilder.build());
                    break;

                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    PpAttributes ppAttributes = null;
                    Map<UsedWavelengthKey, UsedWavelength> usedDegreePpWls;
                    if (tp1 != null) {
                        ppAttributes = tp1.getPpAttributes();
                    }
                    PpAttributesBuilder ppAttributesBuilder;
                    if (ppAttributes == null) {
                        ppAttributesBuilder = new PpAttributesBuilder();
                        usedDegreePpWls = new HashMap<>();
                    } else {
                        ppAttributesBuilder = new PpAttributesBuilder(ppAttributes);
                        usedDegreePpWls = Map.copyOf(ppAttributesBuilder.getUsedWavelength());
                    }
                    UsedWavelength usedDegreeWaveLength = new UsedWavelengthBuilder()
                            .setIndex(Uint32.valueOf(wavelengthIndex))
                            .setFrequency(centralTHz).setWidth(FrequencyGHz.getDefaultInstance("40")).build();
                    usedDegreePpWls.put(usedDegreeWaveLength.key(),usedDegreeWaveLength);
                    ppAttributesBuilder.setUsedWavelength(usedDegreePpWls);
                    tp1Builder.setPpAttributes(ppAttributesBuilder.build());
                    break;

                case XPONDERNETWORK:
                    XpdrNetworkAttributes xpdrNetworkAttributes = null;
                    if (tp1 != null) {
                        xpdrNetworkAttributes = tp1.getXpdrNetworkAttributes();
                    }
                    XpdrNetworkAttributesBuilder xpdrNetworkAttributesBuilder;
                    if (xpdrNetworkAttributes == null) {
                        xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder();
                    } else {
                        xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder(xpdrNetworkAttributes);
                    }
                    Wavelength usedXpdrNetworkWl = new WavelengthBuilder()
                        .setWidth(FrequencyGHz.getDefaultInstance("40")).setFrequency(centralTHz).build();
                    tp1Builder.setXpdrNetworkAttributes(xpdrNetworkAttributesBuilder.setWavelength(usedXpdrNetworkWl)
                        .build());
                    break;
                case XPONDERCLIENT:
                    break;
                case XPONDERPORT:
                    XpdrPortAttributes xpdrPortAttributes = null;
                    if (tp1 != null) {
                        xpdrPortAttributes = tp1.getXpdrPortAttributes();
                    }
                    XpdrPortAttributesBuilder xpdrPortAttributesBuilder;
                    if (xpdrPortAttributes == null) {
                        xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder();
                    } else {
                        xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder(xpdrPortAttributes);
                    }
                    Wavelength usedXpdrPortWl = new WavelengthBuilder().setWidth(FrequencyGHz.getDefaultInstance("40"))
                        .setFrequency(centralTHz).build();
                    tp1Builder.setXpdrPortAttributes(xpdrPortAttributesBuilder.setWavelength(usedXpdrPortWl)
                        .build());
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            addUsedWlTx.put(LogicalDatastoreType.CONFIGURATION, createTerminationPoint1IIDBuilder(idPair.getNodeID(),
                    idPair.getTpID()).build(), tp1Builder.build());
        }
        try {
            addUsedWlTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            List<String> tpIdsString = tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList());
            LOG.error("Unable to add used WL {} for TPs {}!", wavelengthIndex, String.join(", ", tpIdsString), e);
        }
    }
}
