/*
 * Copyright © 2026 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Node1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation that listens to any data change on TAPI SBI topology and propagate changes on TP of Interest
 * to OpenROADM TAPI-SBI-ABS-NODE.
 */
public class SbiTapiTopoListener implements DataTreeChangeListener<Topology> {

    private static final Logger LOG = LoggerFactory.getLogger(SbiTapiTopoListener.class);
    private static final String TOPO_SBI_UUID = "a21e4756-4d70-3d40-95b6-f7f630b4a13b";
    private NetworkTransactionService networkTransactionService;
    private static final ImmutableMap<String, OpenroadmTpType> TP_OF_INTEREST =
        ImmutableMap.<String, OpenroadmTpType>builder()
            .put("eNodeEdgePoint_N", OpenroadmTpType.XPONDERCLIENT)
            .put("eNodeEdgePoint", OpenroadmTpType.XPONDERNETWORK)
            .put("NodeEdgePoint_C", OpenroadmTpType.XPONDERCLIENT)
            .put("PHOTONIC_MEDIA_OTSNodeEdgePoint+PP", OpenroadmTpType.SRGTXRXPP)
            .put("PHOTONIC_MEDIA_OTSNodeEdgePoint+TTP", OpenroadmTpType.DEGREETXRXTTP)
            .build();
    private static final ImmutableMap<String, String> INTERMEDIATE_TP =
        ImmutableMap.<String, String>builder()
            .put("PHOTONIC_MEDIA_OMSNodeEdgePoint", "ROADM-TTP-OMS")
            .put("iNodeEdgePoint_OTU", "Network-OTU")
            .put("PhotMedNodeEdgePoint", "Network-OTSiMC")
            .put("iNodeEdgePoint_N", "Network-I-ODU")
            .build();
    private static final ImmutableMap<OpenroadmTpType, List<String>> TP_TYPE_LAYERS =
        ImmutableMap.<OpenroadmTpType, List<String>>builder()
            .put(OpenroadmTpType.XPONDERCLIENT, List.of(StringConstants.OTN_NETWORK))
            .put(OpenroadmTpType.XPONDERNETWORK,
                List.of(StringConstants.OPENROADM_TOPOLOGY, StringConstants.OPENROADM_NETWORK))
            .put(OpenroadmTpType.SRGTXRXPP,
                List.of(StringConstants.OPENROADM_TOPOLOGY, StringConstants.OPENROADM_NETWORK))
            .put(OpenroadmTpType.DEGREETXRXTTP,
                List.of(StringConstants.OPENROADM_TOPOLOGY, StringConstants.OPENROADM_NETWORK))
            .build();
    /**
     * Instantiate the SbiTapiTopoListener.
     *      TapiTopoListener propagates tree changes observed in SBI Tapi Topology to the Abstracted TAPI-SBI-ABS-NODE
     *      of OpenROADM topology. Only TPs of interest are added/updated/deleted in this Node.
     *      TP of interest corresponds to :
     *         - ROADM PPs (for tunnel service provisioning)
     *         - ROADM TTPs (for inter-domain connections using inter-domain-links)
     *         - XPonders client ports (eODU/DSR)
     *              TPCE (will) just forward demand to the South Bound Controller if both endpoints are Xponders handled
     *              by this last)
     *         - XPonders network ports (OTS ports used in case of WDM service creation on Switchponders)
     *              TPCE (will) just forward demand to the South Bound Controller if both endpoints are Xponders handled
     *              by this last)
     *
     * @param networkTransactionService Service performing DATA STORE Read/write operations.
     */

    public SbiTapiTopoListener(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    /** {@inheritDoc} */
    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<Topology>> changes) {

        for (DataTreeModification<Topology> change : changes) {

            Topology updatedTopo = change.getRootNode().dataAfter();
            if (updatedTopo == null || updatedTopo.getNode() == null) {
                return;
            }
            List<NodeKey> newNodeUuidList = updatedTopo.getNode().entrySet().stream()
                .map(Map.Entry<NodeKey, Node>::getKey)
                .collect(Collectors.toList());

            List<NodeKey> oldNodeUuidList = new ArrayList<>();
            Topology oldTopo = change.getRootNode().dataBefore();
            String onepName;
            if (oldTopo != null && oldTopo.getNode() != null) {
                oldNodeUuidList = oldTopo.getNode().entrySet().stream()
                    .map(Map.Entry<NodeKey, Node>::getKey)
                    .collect(Collectors.toList());
                for (Map.Entry<NodeKey, Node> oldNode : oldTopo.getNode().entrySet()) {
                    // If node Uuid not present in newNodeUuidList, means the node is no more present and tp needs
                    // to be removed from TAPI-SBI-ABS-NODE
                    if (!newNodeUuidList.contains(oldNode.getKey())) {
                        for (OwnedNodeEdgePoint onep : oldNode.getValue().getOwnedNodeEdgePoint().values())  {
                            onepName = onep.getName().entrySet().iterator().next().getValue().getValue();
                            LOG.info("{} remove TP {} from TAPI-SBI-ABS-NODE",
                                removeTpFromTapiAbsSbiNode(new TpId(onepName), identifyTpType(onep)), onepName);
                        }
                    // Otherwise Node is present both before and after and this means tp needs to be updated, which will
                    // be done from dataBefore, if the TP has disappeared, and from dataAfter if not.
                    } else {
                        for (OwnedNodeEdgePoint onep : oldNode.getValue().getOwnedNodeEdgePoint().values())  {
                            if (!updatedTopo.getNode().get(oldNode.getKey()).getOwnedNodeEdgePoint().entrySet().stream()
                                    .map(Map.Entry<OwnedNodeEdgePointKey, OwnedNodeEdgePoint>::getKey)
                                    .collect(Collectors.toList()).contains(new OwnedNodeEdgePointKey(onep.getUuid()))) {
                                onepName = onep.getName().entrySet().iterator().next().getValue().getValue();
                                LOG.info("{} remove TP {} from TAPI-SBI-ABS-NODE",
                                    removeTpFromTapiAbsSbiNode(new TpId(onepName), identifyTpType(onep)), onepName);
                            }
                        }
                    }
                }
            }

            for (Map.Entry<NodeKey, Node> newNode : updatedTopo.getNode().entrySet()) {
                // If node Uuid not present in oldNodeUuidList, means the node is new, and all tps need
                // to be added to TAPI-SBI-ABS-NODE
                if (!oldNodeUuidList.contains(newNode.getKey())) {
                    for (OwnedNodeEdgePoint onep : newNode.getValue().getOwnedNodeEdgePoint().values())  {
                        onepName = onep.getName().entrySet().iterator().next().getValue().getValue();
                        LOG.info("{} add TP {} to TAPI-SBI-ABS-NODE", addTpToTapiAbsSbiNode(onep,
                            newNode.getKey().getUuid().getValue(),
                            newNode.getValue().getName().entrySet().stream()
                                .filter(name -> name.getKey().getValueName().equals("otsi node name")
                                    || name.getKey().getValueName().equals("roadm node name"))
                                .findAny().orElseThrow().getValue().getValue()),
                            onepName);
                    }
                // Otherwise Node is present both before and after and this means tp needs to be updated, which is
                // done from dataAfter.
                } else {
                    for (OwnedNodeEdgePoint onep : newNode.getValue().getOwnedNodeEdgePoint().values())  {
                        onepName = onep.getName().entrySet().iterator().next().getValue().getValue();
                        LOG.info("{} add TP {} to TAPI-SBI-ABS-NODE", updateTpToTapiAbsSbiNode(onep,
                            newNode.getKey().getUuid().getValue(),
                            newNode.getValue().getName().entrySet().stream()
                                .filter(name -> name.getKey().getValueName().equals("otsi node name")
                                    || name.getKey().getValueName().equals("roadm node name"))
                                .findAny().orElseThrow().getValue().getValue()),
                            onepName);
                    }
                }
            }
        }

    }

    /**
     * Removes a TP from TAPI-SBI-ABS-NODE if the corresponding NodeEdgePoint has disappeared.
     * Returns a message trunk to be included in an LOG.info.
     * @param onepTpId  Name of the NEP converted to a OpenROADM TPid.
     * @param tpType    OpenROADM tp type used to evaluate in which layers the tp is present.
     */
    private String removeTpFromTapiAbsSbiNode(TpId onepTpId, OpenroadmTpType tpType) {

        if (deleteTPfromORTopology(onepTpId, evaluateLayers(tpType))) {
            return "Succesfully";
        }
        return "Failed to";
    }

    /**
     * Adds a TP to TAPI-SBI-ABS-NODE.
     * Returns a message trunk to be included in an LOG.info.
     * @param onep      the OwnedNodeEdgePoint that was modified in TAPI SBI Topology.
     * @param nodeUuid  The Uuid of the Node the onep belongs to.
     * @param nodeName  The name (value) of the Node the onep belongs to.
     */
    private String addTpToTapiAbsSbiNode(OwnedNodeEdgePoint onep, String nodeUuid, String nodeName) {
        OpenroadmTpType tptype = identifyTpType(onep);
        if (tptype == null) {
            return "Ignore TP and did not";
        }
        var terminationPoint1 = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110
            .TerminationPoint1Builder()
                .setAdministrativeState(
                    onep.getAdministrativeState().equals(AdministrativeState.UNLOCKED)
                    ? AdminStates.InService : AdminStates.OutOfService)
                .setOperationalState(
                    onep.getOperationalState().equals(OperationalState.ENABLED)
                    ? State.InService : State.OutOfService)
                .setTpType(tptype)
            .build();
        var tpceAugmTp2 = new org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.or.network.augmentation.rev250902.TerminationPoint2Builder()
                .setSupportingNodeName(nodeName)
             .build();
        var tpceAugmTp1 = new org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.or.network.augmentation.rev250902.TerminationPoint1Builder()
                .setSupportingNodeTopologyUuid(TOPO_SBI_UUID)
                .setSupportingNodeUuid(nodeUuid)
                .setTpUuid(onep.getUuid().getValue())
             .build();

        TerminationPoint tp = new TerminationPointBuilder()
            .setTpId(new TpId(onep.getName().entrySet().iterator().next().getValue().getValue()))
            .addAugmentation(terminationPoint1)
            .addAugmentation(tpceAugmTp1)
            .addAugmentation(tpceAugmTp2)
            .build();
        if (mergeTPtoORTopology(tp, evaluateLayers(tptype))) {
            return "Succesfully";
        }
        return "Failed to";
    }

    /**
     * Identifies the OpenROADM equivalent type of the TP corresponding to an OwnedNodeEdgePoint.
     * Returns the OpenROADM Tp Type.
     * @param onep      the OwnedNodeEdgePoint that was modified in TAPI SBI Topology.
     */
    private OpenroadmTpType identifyTpType(OwnedNodeEdgePoint onep) {
        String tapiOnepTpType = onep.getName().entrySet().iterator().next().getValue().getValueName();
        String tapiOnepName = onep.getName().entrySet().iterator().next().getValue().getValue();
        tapiOnepTpType = tapiOnepName.contains("-TTP") ? String.join("+", tapiOnepTpType, "TTP") : tapiOnepTpType;
        tapiOnepTpType = tapiOnepName.contains("-PP") ? String.join("+", tapiOnepTpType, "PP") : tapiOnepTpType;
        return TP_OF_INTEREST.containsKey(tapiOnepTpType) ? TP_OF_INTEREST.get(tapiOnepTpType) : null;
    }

    /**
     * Evaluate the different OpenROADM topological layers the TP is associated to.
     * Returns a List of String that identify the corresponding topological layers.
     * @param tpType      OpenROADM equivalent type of the TP.
     */
    private List<String> evaluateLayers(OpenroadmTpType tpType) {
        List<String> layerList = new ArrayList<>();
        layerList.addAll(TP_TYPE_LAYERS.get(tpType));
        return layerList;
    }

    /**
     * Updates a TP of the TAPI-SBI-ABS-NODE.
     * Returns a message trunk to be included in an LOG.info.
     * @param onep      the OwnedNodeEdgePoint that was modified in TAPI SBI Topology.
     * @param nodeUuid  The Uuid of the Node the onep belongs to.
     * @param nodeName  The name (value) of the Node the onep belongs to.
     */
    private String updateTpToTapiAbsSbiNode(OwnedNodeEdgePoint onep, String nodeUuid, String nodeName) {
        // TODO : Since parameters of tps for TAPI-SBI-ABS-NODE are somewhat limited update could rely on Add, but need
        // to check that it works
        return addTpToTapiAbsSbiNode(onep, nodeUuid, nodeName);
    }

    /**
     * Deletes a TP from the TAPI-SBI-ABS-NODE.
     * Returns true if the TP was successfully deleted, false otherwise.
     * @param onepTpId          The TpId of the TP to be deleted.
     * @param networkLayers     The list of topological layer where the TP appears.
     */
    private boolean deleteTPfromORTopology(TpId onepTpId, List<String> networkLayers) {
        boolean uncounteredIssue = false;
        for (String netLayer : networkLayers) {
            try {
                DataObjectIdentifier<TerminationPoint> orTopologyTpIID = buildTopoIID(netLayer, onepTpId);
                networkTransactionService.delete(LogicalDatastoreType.CONFIGURATION, orTopologyTpIID);
                networkTransactionService.commit().get(1, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Error trying to delete tp  {} from TAPI-SBI-ABS-NODE in Datastore Topology {}",
                    onepTpId, netLayer,e);
                uncounteredIssue = true;
            }
        }
        return uncounteredIssue ? false : true;
    }

    /**
     * Build a DataObjectIdentifier pointing to a Tp of in a specific openROADM topological layer.
     * Returns a DataObjectIdentifier<TerminationPoint> for a specific topological layer.
     * @param onepTpId          The TpId of the TP to be deleted.
     * @param networkLayer     The topological layer that the DataObjectIdentifier points to.
     */
    private DataObjectIdentifier<TerminationPoint> buildTopoIID(String networkLayer, TpId onepTpId) {
        String tapiSBInode = "TAPI-SBI-ABS-NODE";
        return DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(networkLayer)))
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.Node.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                    .networks.network.NodeKey(new NodeId(tapiSBInode)))
            .augmentation(Node1.class)
            .child(TerminationPoint.class, new TerminationPointKey(onepTpId))
            .build();
    }

    /**
     * Adds a TP to TAPI-SBI-ABS node through a write (merge) operation in the different specified topological layers.
     * Returns true if the TP was successfully added, false otherwise.
     * @param tp            The TerminationPoint to be added in TAPI-SBI-ABS-NODE.
     * @param networkLayers The list of topological layers in which the TP shall be added.
     */
    private boolean mergeTPtoORTopology(TerminationPoint tp, List<String> networkLayers) {
        TpId onepTpId = tp.getTpId();
        String tapiSBInode = "TAPI-SBI-ABS-NODE";
        LOG.info("updating TP {} in openroadm-topology", onepTpId);
        boolean uncounteredIssue = false;
        for (String netLayer : networkLayers) {
            try {
                DataObjectIdentifier<TerminationPoint> orTopologyTpIID = DataObjectIdentifier.builder(Networks.class)
                    .child(Network.class, new NetworkKey(new NetworkId(netLayer)))
                    .child(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.network.Node.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226
                            .networks.network.NodeKey(new NodeId(tapiSBInode)))
                    .augmentation(Node1.class)
                    .child(TerminationPoint.class, new TerminationPointKey(onepTpId))
                    .build();
                networkTransactionService.merge(LogicalDatastoreType.CONFIGURATION, orTopologyTpIID, tp);
                this.networkTransactionService.commit().get(1, TimeUnit.SECONDS);
                LOG.info("update tp {} in TAPI-SBI-ABS-NODE at {} layer! ", onepTpId, netLayer);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Error trying to update tp {} in TAPI-SBI-ABS-NODE at {} layer!", onepTpId, netLayer,e);
                uncounteredIssue = true;
            }
        }
        return uncounteredIssue ? false : true;
    }

}