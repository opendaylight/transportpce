/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.math.BigDecimal;
import java.util.Arrays;
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
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev200529.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev200529.degree.used.wavelengths.UsedWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev200529.degree.used.wavelengths.UsedWavelengthsKey;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrClientAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev200529.networks.network.node.termination.point.pp.attributes.UsedWavelengthKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.port.connection.attributes.Wavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev200529.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkModelWavelengthServiceImpl implements NetworkModelWavelengthService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkModelWavelengthServiceImpl.class);
    private final DataBroker dataBroker;
    private final AvailFreqMapsKey availFreqMapKey = new AvailFreqMapsKey(GridConstant.C_BAND);

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

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
        .Node1> createNode2IID(String nodeId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network
                .Node.class, new NodeKey(new NodeId(nodeId))).augmentation(org.opendaylight.yang.gen.v1.http.org
                .openroadm.common.network.rev200529.Node1.class).build();
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

    private Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
        .Node1> getNode2FromDatastore(String nodeId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1>
                nodeIID = createNode2IID(nodeId);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1> nodeOpt;
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
        updateFreqMaps4Nodes(nodeIds, wavelengthNumber, true);
    }





    private void deleteAvailableWL(List<String> nodeIds, Long wavelengthNumber) {
        updateFreqMaps4Nodes(nodeIds, wavelengthNumber, false);
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

    private InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
        .TerminationPoint1> createTerminationPoint2IIDBuilder(String nodeId, String tpId) {
        return InstanceIdentifier
                .builder(Networks.class).child(Network.class, new NetworkKey(
                new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID))).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml
                .ns.yang.ietf.network.rev180226.networks.network.Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226
                .Node1.class).child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
                .rev180226.networks.network.node.TerminationPoint.class, new TerminationPointKey(new TpId(tpId)))
                .augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
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

    private Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
        .TerminationPoint1> getTerminationPoint2FromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
            .TerminationPoint1> tpIID = createTerminationPoint2IIDBuilder(nodeId, tpId).build();
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1> tpOpt;
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
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1> tp2Opt = getTerminationPoint2FromDatastore(idPair.getNodeID(), idPair.getTpID());

            OpenroadmTpType tpType;
            if (tp2Opt.isPresent()) {
                tpType = tp2Opt.get().getTpType();
            } else {
                LOG.error("Unable to get termination point {} from topology {}! Skipping removal of used wavelength"
                        + " for this node.", idPair.getTpID(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            Optional<TerminationPoint1> tp1Opt = getTerminationPoint1FromDatastore(idPair.getNodeID(),
                    idPair.getTpID());
            InstanceIdentifier.InstanceIdentifierBuilder<TerminationPoint1> usedWlIIDBuilder =
                    createTerminationPoint1IIDBuilder(idPair.getNodeID(), idPair.getTpID());
            switch (tpType) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(TxTtpAttributes.class).child(UsedWavelengths.class,
                                    new UsedWavelengthsKey((int)wavelengthIndex)).build());
                    break;

                case DEGREERXTTP:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(RxTtpAttributes.class).child(UsedWavelengths.class,
                                    new UsedWavelengthsKey((int)wavelengthIndex)).build());
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    if (tp1Opt.isPresent()) {
                        TerminationPoint1 tp1 = tp1Opt.get();
                        TerminationPoint1Builder tp1Builder = new TerminationPoint1Builder(tp1);
                        CtpAttributesBuilder ctpAttributesBuilder;
                        if (tp1Builder.getCtpAttributes() != null) {
                            ctpAttributesBuilder = new CtpAttributesBuilder(tp1Builder.getCtpAttributes());
                            Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = tp1Builder.getCtpAttributes()
                                    .nonnullAvailFreqMaps();
                            ctpAttributesBuilder
                                    .setAvailFreqMaps(updateFreqMaps(wavelengthIndex, availFreqMapsMap, true));
                            deleteUsedWlTx.merge(LogicalDatastoreType.CONFIGURATION,
                                    createTerminationPoint1IIDBuilder(idPair.getNodeID(),
                                            idPair.getTpID()).build(), tp1Builder.build());
                        }
                    }
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    if (tp1Opt.isPresent()) {
                        TerminationPoint1 tp1 = tp1Opt.get();
                        TerminationPoint1Builder tp1Builder = new TerminationPoint1Builder(tp1);
                        CpAttributesBuilder cpAttributesBuilder;
                        if (tp1Builder.getCpAttributes() != null) {
                            cpAttributesBuilder = new CpAttributesBuilder(tp1Builder.getCpAttributes());
                            Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap = tp1Builder.getCpAttributes()
                                    .nonnullAvailFreqMaps();
                            cpAttributesBuilder
                                    .setAvailFreqMaps(updateFreqMaps(wavelengthIndex, availFreqMapsMap, true));
                            deleteUsedWlTx.merge(LogicalDatastoreType.CONFIGURATION,
                                    createTerminationPoint1IIDBuilder(idPair.getNodeID(),
                                            idPair.getTpID()).build(), tp1Builder.build());
                        }
                    }
                    break;

                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(PpAttributes.class).child(UsedWavelength.class,
                                    new UsedWavelengthKey((int)wavelengthIndex)).build());
                    break;

                case XPONDERNETWORK:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(XpdrNetworkAttributes.class).child(Wavelength.class).build());
                    break;
                case XPONDERCLIENT:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(XpdrClientAttributes.class).child(Wavelength.class).build());
                    break;
                case XPONDERPORT:
                    deleteUsedWlTx.delete(LogicalDatastoreType.CONFIGURATION,
                            usedWlIIDBuilder.child(XpdrPortAttributes.class).child(Wavelength.class).build());
                    break;

                default:
                    break;
            }
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
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap;
        for (NodeIdPair idPair : tpIds) {
            Optional<TerminationPoint1> tp1Opt =
                getTerminationPoint1FromDatastore(idPair.getNodeID(), idPair.getTpID());
            TerminationPoint1 tp1 = null;
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529
                .TerminationPoint1> tp2Opt = getTerminationPoint2FromDatastore(idPair.getNodeID(), idPair.getTpID());
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.TerminationPoint1 tp2;
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

            UsedWavelengths usedWaveLength = new UsedWavelengthsBuilder().setIndex((int)wavelengthIndex)
                .setFrequency(centralTHz).setWidth(FrequencyGHz.getDefaultInstance("40")).build();
            switch (tp2.getTpType()) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    TxTtpAttributes txTtpAttributes = null;
                    Map<UsedWavelengthsKey,UsedWavelengths> usedDegreeTxTtpWls;
                    if (tp1 != null) {
                        txTtpAttributes = tp1.getTxTtpAttributes();
                    }
                    TxTtpAttributesBuilder txTtpAttributesBuilder;
                    usedDegreeTxTtpWls = new HashMap<>();
                    if (txTtpAttributes == null) {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder();
                    } else {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder(txTtpAttributes);
                        usedDegreeTxTtpWls.putAll(txTtpAttributesBuilder.getUsedWavelengths());
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
                    usedDegreeRxTtpWls = new HashMap<>();
                    if (rxTtpAttributes == null) {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder();
                    } else {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder(rxTtpAttributes);
                        usedDegreeRxTtpWls.putAll(rxTtpAttributesBuilder.getUsedWavelengths());
                    }
                    usedDegreeRxTtpWls.put(usedWaveLength.key(),usedWaveLength);
                    rxTtpAttributesBuilder.setUsedWavelengths(usedDegreeRxTtpWls);
                    tp1Builder.setRxTtpAttributes(rxTtpAttributesBuilder.build());
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    CtpAttributes ctpAttributes = null;
                    if (tp1 != null) {
                        ctpAttributes = tp1.getCtpAttributes();
                    }
                    CtpAttributesBuilder ctpAttributesBuilder;
                    if (ctpAttributes == null) {
                        ctpAttributesBuilder = new CtpAttributesBuilder();
                    } else {
                        ctpAttributesBuilder = new CtpAttributesBuilder(ctpAttributes);
                    }
                    availFreqMapsMap = ctpAttributesBuilder.getAvailFreqMaps();
                    ctpAttributesBuilder.setAvailFreqMaps(updateFreqMaps(wavelengthIndex, availFreqMapsMap, false));
                    tp1Builder.setCtpAttributes(ctpAttributesBuilder.build());
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    CpAttributes cpAttributes = null;
                    if (tp1 != null) {
                        cpAttributes = tp1.getCpAttributes();
                    }
                    CpAttributesBuilder cpAttributesBuilder;
                    if (cpAttributes == null) {
                        cpAttributesBuilder = new CpAttributesBuilder();
                    } else {
                        cpAttributesBuilder = new CpAttributesBuilder(cpAttributes);
                    }
                    availFreqMapsMap = cpAttributesBuilder.getAvailFreqMaps();
                    cpAttributesBuilder.setAvailFreqMaps(updateFreqMaps(wavelengthIndex, availFreqMapsMap, false));
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
                    usedDegreePpWls = new HashMap<>();
                    if (ppAttributes == null) {
                        ppAttributesBuilder = new PpAttributesBuilder();
                    } else {
                        ppAttributesBuilder = new PpAttributesBuilder(ppAttributes);
                        usedDegreePpWls.putAll(ppAttributesBuilder.getUsedWavelength());
                    }
                    UsedWavelength usedDegreeWaveLength = new UsedWavelengthBuilder()
                            .setIndex((int)wavelengthIndex)
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

    /**
     * Update availFreqMapsMap for wavelengthNumber for nodes in nodeIds.
     * @param nodeIds List of node id.
     * @param wavelengthNumber Long
     * @param isSlotAvailable boolean true if waveLength is available, false otherwise.
     */
    //TODO: reduce its Cognitive Complexity from 21 to the 15
    private void updateFreqMaps4Nodes(List<String> nodeIds, Long wavelengthNumber, boolean isSlotAvailable) {
        WriteTransaction nodeWriteTx = this.dataBroker.newWriteOnlyTransaction();
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap;
        String action = isSlotAvailable ? "addition" : "deletion";
        for (String nodeId : nodeIds) {
            Optional<Node1> node1Opt = getNode1FromDatastore(nodeId);
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1> node2Opt =
                    getNode2FromDatastore(nodeId);
            if (node1Opt.isPresent() && node2Opt.isPresent()) {
                Node1 node1 = node1Opt.get();
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev200529.Node1 node2 = node2Opt.get();
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
                        availFreqMapsMap = degreeAttributesBuilder.getAvailFreqMaps();
                        degreeAttributesBuilder
                        .setAvailFreqMaps(updateFreqMaps(wavelengthNumber, availFreqMapsMap, isSlotAvailable));
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
                        availFreqMapsMap = srgAttributesBuilder.getAvailFreqMaps();
                        srgAttributesBuilder
                        .setAvailFreqMaps(updateFreqMaps(wavelengthNumber, availFreqMapsMap, isSlotAvailable));
                        node1Builder.setSrgAttributes(srgAttributesBuilder.build());
                        break;
                    default:
                        LOG.warn("Node type not managed {}", node2.getNodeType());
                        break;
                }
                nodeWriteTx.put(LogicalDatastoreType.CONFIGURATION, createNode1IID(nodeId), node1Builder.build());
            } else {
                LOG.error(
                        "From topology {} for node id {} -> Get common-network : {} ! "
                        + "Get network-topology : {} !Skipping {} of available"
                                + "wavelength for this node.",
                        NetworkUtils.OVERLAY_NETWORK_ID, nodeId, node1Opt.isPresent(), node2Opt.isPresent(), action);
            }
        }
        try {
            nodeWriteTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Cannot perform {} WL {} for nodes {}!", action, wavelengthNumber, String.join(", ", nodeIds), e);
        } catch (InterruptedException e) {
            LOG.error("{} interrupted  WL {} for nodes {}!", action, wavelengthNumber, String.join(", ", nodeIds), e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Update availFreqMapsMap for wavelengthNumber.
     * @param wavelengthNumber Long
     * @param availFreqMapsMap Map
     * @param isSlotAvailable boolean
     * @return availFreqMapsMap updated for wavelengthNumber.
     */
    private Map<AvailFreqMapsKey, AvailFreqMaps> updateFreqMaps(Long wavelengthNumber,
            Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMapsMap, boolean isSlotAvailable) {
        byte slotValue = (byte)GridConstant.USED_SLOT_VALUE;
        if (isSlotAvailable) {
            slotValue = (byte)GridConstant.AVAILABLE_SLOT_VALUE;
        }
        if (availFreqMapsMap == null) {
            availFreqMapsMap = GridUtils.initFreqMaps4FixedGrid2Available();
        }
        AvailFreqMaps availFreqMaps = availFreqMapsMap.get(availFreqMapKey);
        if (availFreqMaps != null && availFreqMaps.getFreqMap() != null) {
            int effectiveBits = availFreqMaps.getEffectiveBits().intValue();
            int intWlNumber = wavelengthNumber.intValue();
            if (intWlNumber * effectiveBits < availFreqMaps.getFreqMap().length) {
                Arrays.fill(availFreqMaps.getFreqMap(), (intWlNumber - 1) * effectiveBits, intWlNumber * effectiveBits,
                        slotValue);
            }
        }
        return availFreqMapsMap;
    }
}
