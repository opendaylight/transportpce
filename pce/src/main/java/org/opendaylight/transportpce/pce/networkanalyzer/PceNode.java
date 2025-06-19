/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev230526.networks.network.node.termination.point.XpdrNetworkAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yangtools.yang.common.Uint16;

public interface PceNode {

    /**
     * For Graph PCE nodes, returns the type of node.
     * @return  "optical" for Pce Optical Nodes, "otn" for Pce Otn Nodes.
     */
    String getPceNodeType();

    /**
     * For any kind of node, the Id of the supporting network Node.
     * @return  A string that corresponds to the Id of the node (a Uuid for Tapi Nodes)
     */
    String getSupNetworkNodeId();

    /**
     * For any kind of node, the Id of the supporting Node in the CLLI layer (Buildings).
     * @return  A string that corresponds to the Id of the node (a Uuid for Tapi Nodes)
     */
    String getSupClliNodeId();

    /**
     * Adds to the PceNode's List of outgoing links, a Pce Link.
     * @param outLink     The PceLink connected to the PceNode
     */
    void addOutgoingLink(PceORLink outLink);

    /**
     * For ROADM SRG (Add/drop) Nodes, provides the Id of a PP termination point connected to input Tp.
     * @param tp The CP for which we search a connected client PP.
     * @param direction     A string which corresponds to the direction of the path currently analyzed : aToZ/zToA.
     * @return  Id of the first PP found as a candidate
     */
    String getRdmSrgClient(String tp, String direction);

    /**
     * Provides the Id of a NW Port in visibility of a client Tp.
     * @return String  The Id of the port
     */
    String getXpdrNWfromClient(String tp);

    /**
     * Checks whether a Network tp is available (no channel provisioned on the port) or not.
     * @return  Boolean     True if the port is already used, false in the other case.
     */
    boolean checkTP(String tp);

    /**
     * For any kind of PceNodes return the list of connected links.
     * @return  List of PceLink
     */
    List<PceORLink> getOutgoingLinks();

    /**
     * Provides Administrative state for nodes in OpenROADM topology.
     * @return Administrative state for nodes in OpenROADM topology, null for nodes in T-API topology.
     */
    AdminStates getAdminStates();

    /**
     * Provides Administrative state for nodes in T-API topology topology.
     * @return Administrative state for nodes in T-API topology, null for nodes in OpenROADM topology.
     */
    AdministrativeState getAdminState();

    /**
     * Provides Operational state for nodes in OpenROADM topology.
     * @return Operational state for nodes in OpenROADM topology, null for nodes in T-API topology.
     */
    State getState();

    /**
     * Provides Operational state for nodes in T-API topology topology.
     * @return Operational state for nodes in T-API topology, null for nodes in OpenROADM topology.
     */
    OperationalState getOperationalState();

    /**
     * Provides the NodeId (ietf-topology nodeId) of the PceNode.
     * @return  ietf-topology nodeId
     */
    NodeId getNodeId();

    /**
     * Provides the OpenROADM nodType of the PceNode.
     * @return  OpenROADM nodeType (ROADM, DEGREE, SRG, ILA, XPONDER, EXT-PLUGGABLE, TPDR, MUXPDR, REGEN, REGEN-UNI,
     *          SWITCH)
     */
    OpenroadmNodeType getORNodeType();

    /**
     * Provides Operational Mode of ROADM SRG (WR Specification) and Degree (MW-MW Specification) for OpenROADM Nodes.
     * @return  String that corresponds to the Node Operational mode as in the OpenROADM specification Catalog for
     *          OpenROADM nodes, Null for others.
     */
    String getOperationalMode();

    /**
     * Provides Operational Mode of XPONDER (W Specification or specific-operational-mode) for Nodes in OpenROADM Topo.
     * @return  String that corresponds to the Node Operational mode as in the OpenROADM specification Catalog for
     *          OpenROADM nodes, or in specific-operational-mode Catalog (provided that a user imported it),
     *          Null for others (nodes that are not handled in OpenROADM topology).
     */
    String getXponderOperationalMode(XpdrNetworkAttributes tp);

    /**
     * Provides Operational Mode of XPONDER for XPONDERs in the T-API topology.
     * @return  String that corresponds to the Node Operational mode as in the specific-operational-mode Catalog for
     *          Xponders in the T-API topology, null for others.
     */
    String getXpdrOperationalMode(Uuid nepUuid);

    /**
     * Provides the Uuid of a Pcenode of the T-API topology.
     * @return Uuid of the node in T-API topology, null for others.
     */
    Uuid getNodeUuid();

    /**
     * Provides available trib-ports for an OTN termination point of an OpenROADM XPONDER.
     * @return  For OTN OpenROADM Nodes, a Map of List of integers (each integer corresponding to the number of an
     *          available tributary-port), with a String as the key, corresponding to the port name.
     *          Null for any other PceNode. For T-API we make the assumption that as far as a port is available for OTN
     *          service provisioning, the switching-matrix will have the available bandwidth so that the connection can
     *          be established by the SouthBound Controller. This last will have the responsibility for selecting the
     *          right tributary ports, from the request exercised by transportPCE to create the service.This must be
     *          considered as a first step of the implementation, assuming that the probability that the matrix does not
     *          allow for a connection to be established (leading to a crank-back) is very limited.
     */
    Map<String, List<Uint16>> getAvailableTribPorts();

    /**
     * Provides available trib-slots for an OTN termination point of an OpenROADM XPONDER.
     * @return  For OTN OpenROADM Nodes, a Map of List of integers (each integer corresponding to the number of an
     *          available tributary-slot), with a String as the key, corresponding to the port name.
     *          Null for any other PceNode. For T-API we make the assumption that as far as a port is available for OTN
     *          service provisioning, the switching-matrix will have the available bandwidth so that the connection can
     *          be established by the SouthBound Controller. This last will have the responsibility for selecting the
     *          right tributary ports, from the request exercised by transportPCE to create the service.This must be
     *          considered as a first step of the implementation, assuming that the probability that the matrix does not
     *          allow for a connection to be established (leading to a crank-back) is very limited.
     */
    Map<String, List<Uint16>> getAvailableTribSlots();

    /**
     * Provides a List of the NEPs that have been created for a Node in the T-API topology in TapiOpticalNode.
     * @return List of relevant NEP for a PceTapiOpticalNode or a PceTapiOtnNode, null for other PceNodes.
     */
    List<BasePceNep> getListOfNep();

    /**
     * Get the version of node.
     * @return the OpenROADM yang release supported by the node.
     */
    String getVersion();

    /**
     * For optical node, the spectrum occupation of the node.
     * @return BitSet.
     */
    BitSet getBitSetData();

    /**
     * For optical node the slot width granularity from mc capabilities.
     * @return Decimal64.
     */
    BigDecimal getSlotWidthGranularity();

    /**
     * For optical node the min-slots from mc capabilities.
     * Minimum number of slots permitted to be joined together to form a media channel.
     * Must be less than or equal to the max-slots.
     * @return int.
     */
    int getMinSlots();

    /**
     * For optical node the max-slots from mc capabilities.
     * Maximum number of slots permitted to be joined together to form a media channel.
     * Must be greater than or equal to the min-slots.
     * @return int.
     */
    int getMaxSlots();

    /**
     * For optical node the central-frequency granularity from mc capabilities.
     * @return Decimal64.
     */
    BigDecimal getCentralFreqGranularity();
}
