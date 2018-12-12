/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.node.attributes.AvailableWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.used.wavelengths.UsedWavelengths;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.used.wavelengths.UsedWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.used.wavelengths.UsedWavelengthsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.SrgAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.PpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.RxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.TxTtpAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrClientAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrPortAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.pp.attributes.UsedWavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.pp.attributes.UsedWavelengthKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev171215.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev171215.srg.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev171215.xpdr.port.connection.attributes.Wavelength;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev171215.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
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
        List<NodeIdPair> ztoaTpIds = getZToATpList(pathDescription);

        deleteAvailableWL(atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getAToZDirection().getAToZWavelengthNumber());
        deleteAvailableWL(ztoaTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getZToADirection().getZToAWavelengthNumber());

        addUsedWL(pathDescription.getAToZDirection().getAToZWavelengthNumber(), atozTpIds);
        addUsedWL(pathDescription.getZToADirection().getZToAWavelengthNumber(), ztoaTpIds);
    }

    @Override
    public void freeWavelengths(PathDescription pathDescription) {
        List<NodeIdPair> atozTpIds = getAToZTpList(pathDescription);
        List<NodeIdPair> ztoaTpIds = getZToATpList(pathDescription);

        deleteUsedWL(pathDescription.getAToZDirection().getAToZWavelengthNumber(), atozTpIds);
        deleteUsedWL(pathDescription.getZToADirection().getZToAWavelengthNumber(), ztoaTpIds);

        addAvailableWL(atozTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getAToZDirection().getAToZWavelengthNumber());
        addAvailableWL(ztoaTpIds.stream().map(NodeIdPair::getNodeID).distinct().collect(Collectors.toList()),
                pathDescription.getZToADirection().getZToAWavelengthNumber());
    }

    private List<NodeIdPair> getAToZTpList(PathDescription pathDescription) {
        List<AToZ> atozList = pathDescription.getAToZDirection().getAToZ();
        return atozList.stream()
                .filter(aToZ -> {
                    if ((aToZ.getResource() == null) || (aToZ.getResource().getResource() == null)) {
                        LOG.warn("Resource of AToZ node {} is null! Skipping this node!", aToZ.getId());
                        return false;
                    }
                    return aToZ.getResource().getResource() instanceof TerminationPoint;
                }).map(aToZ -> {
                    TerminationPoint tp = (TerminationPoint) aToZ.getResource().getResource();
                    if ((tp == null) || (tp.getTpNodeId() == null) ||  (tp.getTpId() == null)) {
                        LOG.warn("Termination point in AToZ node {} contains nulls! Skipping this node!", aToZ.getId());
                        return null;
                    }
                    return new NodeIdPair(tp.getTpNodeId(), tp.getTpId());
                }).collect(Collectors.toList());
    }

    private List<NodeIdPair> getZToATpList(PathDescription pathDescription) {
        List<ZToA> ztoaList = pathDescription.getZToADirection().getZToA();
        return ztoaList.stream()
                .filter(zToA -> {
                    if ((zToA.getResource() == null) || (zToA.getResource().getResource() == null)) {
                        LOG.warn("Resource of ZToA node {} is null! Skipping this node!", zToA.getId());
                        return false;
                    }
                    return zToA.getResource().getResource() instanceof TerminationPoint;
                }).map(zToA -> {
                    TerminationPoint tp = (TerminationPoint) zToA.getResource().getResource();
                    if ((tp == null) || (tp.getTpNodeId() == null) ||  (tp.getTpId() == null)) {
                        LOG.warn("Termination point in ZToA node {} contains nulls! Skipping this node!", zToA.getId());
                        return null;
                    }
                    return new NodeIdPair(tp.getTpNodeId(), tp.getTpId());
                }).collect(Collectors.toList());
    }

    private InstanceIdentifier<Node1> createNode1IID(String nodeId) {
        return InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608
                        .network.Node.class, new NodeKey(new NodeId(nodeId)))
                .augmentation(Node1.class)
                .build();
    }

    private Optional<Node1> getNode1FromDatastore(String nodeId) {
        InstanceIdentifier<Node1>
                nodeIID = createNode1IID(nodeId);
        Optional<Node1> nodeOpt;
        try (ReadOnlyTransaction nodeReadTx = this.dataBroker.newReadOnlyTransaction()) {
            nodeOpt = nodeReadTx.read(LogicalDatastoreType.CONFIGURATION, nodeIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting node from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID, e);
            nodeOpt = Optional.absent();
        }
        return nodeOpt;
    }

    private void addAvailableWL(List<String> nodeIds, Long wavelengthNumber) {
        WriteTransaction nodeWriteTx = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Optional<Node1> nodeOpt =
                    getNode1FromDatastore(nodeId);
            Node1 node;
            if (nodeOpt.isPresent()) {
                node = nodeOpt.get();
            } else {
                LOG.error("Unable to get node {} from topology {}! Skipping addition of available wavelength for this"
                        + "node.", nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }

            Node1Builder node1Builder = new Node1Builder(node);

            switch (node.getNodeType()) {
                case DEGREE:
                    DegreeAttributes degreeAttributes = node.getDegreeAttributes();
                    DegreeAttributesBuilder degreeAttributesBuilder;
                    if (degreeAttributes == null) {
                        degreeAttributesBuilder = new DegreeAttributesBuilder();
                    } else {
                        degreeAttributesBuilder = new DegreeAttributesBuilder(degreeAttributes);
                    }
                    List<org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.node.attributes
                            .AvailableWavelengths> availableDegreeWLs =
                            degreeAttributesBuilder.getAvailableWavelengths();
                    if (availableDegreeWLs == null) {
                        availableDegreeWLs = new ArrayList<>();
                        degreeAttributesBuilder.setAvailableWavelengths(availableDegreeWLs);
                    }
                    availableDegreeWLs.add(new org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree
                            .node.attributes.AvailableWavelengthsBuilder().setIndex(wavelengthNumber).build());
                    node1Builder.setDegreeAttributes(degreeAttributesBuilder.build());
                    break;
                case SRG:
                    SrgAttributes srgAttributes = node.getSrgAttributes();
                    SrgAttributesBuilder srgAttributesBuilder;
                    if (srgAttributes == null) {
                        srgAttributesBuilder = new SrgAttributesBuilder();
                    } else {
                        srgAttributesBuilder = new SrgAttributesBuilder(srgAttributes);
                    }
                    List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev171215.srg.node.attributes
                            .AvailableWavelengths> availableSrgWLs = srgAttributesBuilder.getAvailableWavelengths();
                    if (availableSrgWLs == null) {
                        availableSrgWLs = new ArrayList<>();
                        srgAttributesBuilder.setAvailableWavelengths(availableSrgWLs);
                    }
                    availableSrgWLs.add(new AvailableWavelengthsBuilder().setIndex(wavelengthNumber).build());
                    node1Builder.setSrgAttributes(srgAttributesBuilder.build());
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            nodeWriteTx.put(LogicalDatastoreType.CONFIGURATION, createNode1IID(nodeId), node1Builder.build(), true);
        }
        try {
            nodeWriteTx.submit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to add available WL {} for nodes {}!", wavelengthNumber, String.join(", ", nodeIds), e);
        }
    }

    private void deleteAvailableWL(List<String> nodeIds, Long wavelengthNumber) {
        WriteTransaction nodeWriteTx = this.dataBroker.newWriteOnlyTransaction();
        for (String nodeId : nodeIds) {
            Optional<Node1> nodeOpt = getNode1FromDatastore(nodeId);
            Node1 node;
            if (nodeOpt.isPresent()) {
                node = nodeOpt.get();
            } else {
                LOG.error("Unable to get node {} from topology {}! Skipping addition of available wavelength for this"
                        + "node.", nodeId, NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }

            InstanceIdentifierBuilder<Node1> nodeIIDBuilder = InstanceIdentifier
                    .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                    .child(Node.class, new NodeKey(new NodeId(nodeId))).augmentation(Node1.class);
            InstanceIdentifier availableWlIID;

            switch (node.getNodeType()) {
                case DEGREE:
                    availableWlIID = nodeIIDBuilder.child(DegreeAttributes.class)
                            .child(org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.node
                                    .attributes.AvailableWavelengths.class,
                                    new AvailableWavelengthsKey(wavelengthNumber))
                            .build();
                    break;
                case SRG:
                    availableWlIID = nodeIIDBuilder.child(SrgAttributes.class)
                            .child(org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev171215.srg.node.attributes
                                            .AvailableWavelengths.class,
                                    new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev171215.srg.node
                                            .attributes.AvailableWavelengthsKey(wavelengthNumber))
                            .build();
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            nodeWriteTx.delete(LogicalDatastoreType.CONFIGURATION, availableWlIID);
        }
        try {
            nodeWriteTx.submit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Unable to delete available WL {} for nodes {}!", wavelengthNumber, String.join(", ", nodeIds),
                    e);
        }
    }

    private InstanceIdentifierBuilder<TerminationPoint1> createTerminationPoint1IIDBuilder(String nodeId, String tpId) {
        return InstanceIdentifier
                .builder(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network
                    .Node.class, new NodeKey(new NodeId(nodeId))).augmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608
                                .network.node.TerminationPoint.class,
                        new TerminationPointKey(new TpId(tpId))).augmentation(TerminationPoint1.class);
    }

    private Optional<TerminationPoint1> getTerminationPoint1FromDatastore(String nodeId, String tpId) {
        InstanceIdentifier<TerminationPoint1> tpIID = createTerminationPoint1IIDBuilder(nodeId, tpId).build();
        Optional<TerminationPoint1> tpOpt;
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            tpOpt = readTx.read(LogicalDatastoreType.CONFIGURATION, tpIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting termination point from {} topology!", NetworkUtils.OVERLAY_NETWORK_ID,
                    e);
            tpOpt = Optional.absent();
        }
        return tpOpt;
    }

    private void deleteUsedWL(long wavelengthIndex, List<NodeIdPair> tpIds) {
        WriteTransaction deleteUsedWlTx = this.dataBroker.newWriteOnlyTransaction();
        for (NodeIdPair idPair : tpIds) {
            Optional<TerminationPoint1> tpOpt = getTerminationPoint1FromDatastore(idPair.getNodeID(), idPair.getTpID());

            OpenroadmTpType tpType;
            if (tpOpt.isPresent()) {
                tpType = tpOpt.get().getTpType();
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
                            new UsedWavelengthsKey(wavelengthIndex)).build();
                    break;

                case DEGREERXTTP:
                    usedWlIID = usedWlIIDBuilder.child(RxTtpAttributes.class).child(UsedWavelengths.class,
                            new UsedWavelengthsKey(wavelengthIndex)).build();
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    usedWlIID = usedWlIIDBuilder.child(CtpAttributes.class).child(UsedWavelengths.class,
                            new UsedWavelengthsKey(wavelengthIndex)).build();
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    usedWlIID = usedWlIIDBuilder.child(CpAttributes.class).child(org.opendaylight.yang.gen.v1.http.org
                                    .openroadm.network.topology.rev171215.network.node.termination.point.cp.attributes
                                    .UsedWavelengths.class,
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node
                                    .termination.point.cp.attributes.UsedWavelengthsKey(
                                    wavelengthIndex)).build();
                    break;

                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    usedWlIID = usedWlIIDBuilder.child(PpAttributes.class).child(UsedWavelength.class,
                            new UsedWavelengthKey(wavelengthIndex)).build();
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
            deleteUsedWlTx.submit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            List<String> tpIdsString = tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList());
            LOG.error("Unable to delete used WL {} from TPs {}!", wavelengthIndex, String.join(", ", tpIdsString), e);
        }
    }

    private void addUsedWL(long wavelengthIndex, List<NodeIdPair> tpIds) {
        WriteTransaction addUsedWlTx = this.dataBroker.newWriteOnlyTransaction();
        for (NodeIdPair idPair : tpIds) {
            Optional<TerminationPoint1> tpOpt = getTerminationPoint1FromDatastore(idPair.getNodeID(), idPair.getTpID());

            TerminationPoint1 tp;
            if (tpOpt.isPresent()) {
                tp = tpOpt.get();
            } else {
                LOG.error("Unable to get termination point {} from topology {}! Skipping removal of used wavelength"
                        + " for this node.", idPair.getTpID(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }

            TerminationPoint1Builder tp1Builder = new TerminationPoint1Builder(tp);

            switch (tp.getTpType()) {
                case DEGREETXTTP:
                case DEGREETXRXTTP:
                    TxTtpAttributes txTtpAttributes = tp.getTxTtpAttributes();
                    TxTtpAttributesBuilder txTtpAttributesBuilder;
                    if (txTtpAttributes == null) {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder();
                    } else {
                        txTtpAttributesBuilder = new TxTtpAttributesBuilder(txTtpAttributes);
                    }
                    List<UsedWavelengths> usedDegreeTxTtpWls = txTtpAttributesBuilder.getUsedWavelengths();
                    if (usedDegreeTxTtpWls == null) {
                        usedDegreeTxTtpWls = new ArrayList<>();
                        txTtpAttributesBuilder.setUsedWavelengths(usedDegreeTxTtpWls);
                    }
                    usedDegreeTxTtpWls.add(new UsedWavelengthsBuilder().setIndex(wavelengthIndex).build());
                    tp1Builder.setTxTtpAttributes(txTtpAttributesBuilder.build());
                    break;

                case DEGREERXTTP:
                    RxTtpAttributes rxTtpAttributes = tp.getRxTtpAttributes();
                    RxTtpAttributesBuilder rxTtpAttributesBuilder;
                    if (rxTtpAttributes == null) {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder();
                    } else {
                        rxTtpAttributesBuilder = new RxTtpAttributesBuilder(rxTtpAttributes);
                    }
                    List<UsedWavelengths> usedDegreeRxTtpWls = rxTtpAttributesBuilder.getUsedWavelengths();
                    if (usedDegreeRxTtpWls == null) {
                        usedDegreeRxTtpWls = new ArrayList<>();
                        rxTtpAttributesBuilder.setUsedWavelengths(usedDegreeRxTtpWls);
                    }
                    usedDegreeRxTtpWls.add(new UsedWavelengthsBuilder().setIndex(wavelengthIndex).build());
                    tp1Builder.setRxTtpAttributes(rxTtpAttributesBuilder.build());
                    break;

                case DEGREETXCTP:
                case DEGREERXCTP:
                case DEGREETXRXCTP:
                    CtpAttributes ctpAttributes = tp.getCtpAttributes();
                    CtpAttributesBuilder ctpAttributesBuilder;
                    if (ctpAttributes == null) {
                        ctpAttributesBuilder = new CtpAttributesBuilder();
                    } else {
                        ctpAttributesBuilder = new CtpAttributesBuilder(ctpAttributes);
                    }
                    List<UsedWavelengths> usedDegreeCtpWls = ctpAttributesBuilder.getUsedWavelengths();
                    if (usedDegreeCtpWls == null) {
                        usedDegreeCtpWls = new ArrayList<>();
                        ctpAttributesBuilder.setUsedWavelengths(usedDegreeCtpWls);
                    }
                    usedDegreeCtpWls.add(new UsedWavelengthsBuilder().setIndex(wavelengthIndex).build());
                    tp1Builder.setCtpAttributes(ctpAttributesBuilder.build());
                    break;

                case SRGTXCP:
                case SRGRXCP:
                case SRGTXRXCP:
                    CpAttributes cpAttributes = tp.getCpAttributes();
                    CpAttributesBuilder cpAttributesBuilder;
                    if (cpAttributes == null) {
                        cpAttributesBuilder = new CpAttributesBuilder();
                    } else {
                        cpAttributesBuilder = new CpAttributesBuilder(cpAttributes);
                    }
                    List<org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node
                            .termination.point.cp.attributes.UsedWavelengths> usedDegreeCpWls =
                            cpAttributesBuilder.getUsedWavelengths();
                    if (usedDegreeCpWls == null) {
                        usedDegreeCpWls = new ArrayList<>();
                        cpAttributesBuilder.setUsedWavelengths(usedDegreeCpWls);
                    }
                    usedDegreeCpWls.add(new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215
                            .network.node.termination.point.cp.attributes.UsedWavelengthsBuilder()
                            .setIndex(wavelengthIndex).build());
                    tp1Builder.setCpAttributes(cpAttributesBuilder.build());
                    break;

                case SRGTXRXPP:
                case SRGRXPP:
                case SRGTXPP:
                    PpAttributes ppAttributes = tp.getPpAttributes();
                    PpAttributesBuilder ppAttributesBuilder;
                    if (ppAttributes == null) {
                        ppAttributesBuilder = new PpAttributesBuilder();
                    } else {
                        ppAttributesBuilder = new PpAttributesBuilder(ppAttributes);
                    }
                    List<UsedWavelength> usedDegreePpWls = ppAttributesBuilder.getUsedWavelength();
                    if (usedDegreePpWls == null) {
                        usedDegreePpWls = new ArrayList<>();
                        ppAttributesBuilder.setUsedWavelength(usedDegreePpWls);
                    }
                    usedDegreePpWls.add(new UsedWavelengthBuilder().setIndex(wavelengthIndex).build());
                    tp1Builder.setPpAttributes(ppAttributesBuilder.build());
                    break;

                case XPONDERNETWORK:
                    XpdrNetworkAttributes xpdrNetworkAttributes = tp.getXpdrNetworkAttributes();
                    XpdrNetworkAttributesBuilder xpdrNetworkAttributesBuilder;
                    if (xpdrNetworkAttributes == null) {
                        xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder();
                    } else {
                        xpdrNetworkAttributesBuilder = new XpdrNetworkAttributesBuilder(xpdrNetworkAttributes);
                    }
                    Wavelength usedXpdrNetworkWl = new WavelengthBuilder().setIndex(wavelengthIndex).build();
                    tp1Builder.setXpdrNetworkAttributes(xpdrNetworkAttributesBuilder
                            .setWavelength(usedXpdrNetworkWl)
                            .build());
                    break;
                case XPONDERCLIENT:
                    XpdrClientAttributes xpdrClientAttributes = tp.getXpdrClientAttributes();
                    XpdrClientAttributesBuilder xpdrClientAttributesBuilder;
                    if (xpdrClientAttributes == null) {
                        xpdrClientAttributesBuilder = new XpdrClientAttributesBuilder();
                    } else {
                        xpdrClientAttributesBuilder = new XpdrClientAttributesBuilder(xpdrClientAttributes);
                    }
                    Wavelength usedXpdrClientWl = new WavelengthBuilder().setIndex(wavelengthIndex).build();
                    tp1Builder.setXpdrClientAttributes(xpdrClientAttributesBuilder
                            .setWavelength(usedXpdrClientWl)
                            .build());
                    break;
                case XPONDERPORT:
                    XpdrPortAttributes xpdrPortAttributes = tp.getXpdrPortAttributes();
                    XpdrPortAttributesBuilder xpdrPortAttributesBuilder;
                    if (xpdrPortAttributes == null) {
                        xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder();
                    } else {
                        xpdrPortAttributesBuilder = new XpdrPortAttributesBuilder(xpdrPortAttributes);
                    }
                    Wavelength usedXpdrPortWl = new WavelengthBuilder().setIndex(wavelengthIndex).build();
                    tp1Builder.setXpdrPortAttributes(xpdrPortAttributesBuilder
                            .setWavelength(usedXpdrPortWl)
                            .build());
                    break;

                default:
                    // TODO skip for now
                    continue;
            }
            addUsedWlTx.put(LogicalDatastoreType.CONFIGURATION, createTerminationPoint1IIDBuilder(idPair.getNodeID(),
                    idPair.getTpID()).build(), tp1Builder.build(), true);
        }
        try {
            addUsedWlTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            List<String> tpIdsString = tpIds.stream().map(NodeIdPair::toString).collect(Collectors.toList());
            LOG.error("Unable to add used WL {} for TPs {}!", wavelengthIndex, String.join(", ", tpIdsString), e);
        }
    }
}
