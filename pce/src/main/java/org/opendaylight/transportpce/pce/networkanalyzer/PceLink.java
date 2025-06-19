/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;

public interface PceLink {

    /**
     * Sets the opposite linkId.
     * @param oppositeLinkId  Uuid of the opposite link.
     */
    void setOppositeLinkUuid(Uuid oppositeLinkId);

    /**
     * Provides the result of the validity check for any kind of PceLinks.
     * @return  Boolean set to true if the link is considered as valid, false otherwise.
     */
    boolean isValid();

    /**
     * Provides the Uuid of the opposite Link.
     * @return  Uuid of the opposite ink.
     */
    Uuid getOppositeLinkUuid();


    /**
     * Provides the LinkId of the opposite Link.
     * @return  LinkId of the opposite Link.
     */
    LinkId getOppositeLink();

    /**
     * Provides the administrative state of the T-API Pcelink.
     * @return AdministrativeState (T-API)
     */
    AdministrativeState getAdministrativeState();

    /**
     * Provides the operational state of the T-API Pcelink.
     * @return OperationalState (T-API)
     */
    OperationalState getOperationalState();

    /**
     * Provides the adminstate of the OR Pcelink.
     * @return AdministrativeState
     */
    AdminStates getAdminStates();

    /**
     * Provides the operational state of the OR Pcelink.
     * @return OperationalState
     */
    State getState();

    /**
     * Provides the OR sourceTpId.
     * @return  TpId of the source .
     */
    TpId getSourceTP();

    /**
     * Provides the T-API sourceTpId.
     * @return  Uuid of the source .
     */
    Uuid getSourceTPUuid();

    /**
     * Provides the OR destTpId.
     * @return  Uuid of the destination Tp.
     */
    TpId getDestTP();

    /**
     * Provides the T-API destTpId.
     * @return  Uuid of the destination Tp.
     */
    Uuid getDestTPUuid();

    /**
     * Provides the linkType as defined by the corresponding OpenROADM enumeration.
     * @return linkType as defined by the corresponding OpenROADM enumeration : EXPRESS-LINK, ADD-LINK, DROP-LINK,
     *         ROADM-TO-ROADM, XPONDER-INPUT, XPONDER-OUTPUT, OTN-LINK, REGEN-INPUT, REGEN-OUTPUT, TURNBACK-LINK.
     */
    OpenroadmLinkType getlinkType();

    /**
     * Provides the Uuid corresponding to the LinkId.
     * @return      Uuid of the link.
     */
    Uuid getLinkUuid();

    /**
     * Provides the LinkID corresponding to the OR link.
     * @return      LinkId of the link.
     */
    LinkId getLinkId();

    /**
     * Provides the NodeId corresponding to the Source Node of the link.
     * @return      NodeId of the source Tp.
     */
    NodeId getSourceId();

    /**
     * Provides the Uuid corresponding to the Source Node of the link.
     * @return      Uuid of the source Tp.
     */
    Uuid getSourceUuid();

    /**
     * Provides the NodeId corresponding to the destination Node of the link.
     * @return      NodeId of the destination Tp.
     */
    NodeId getDestId();

    /**
     * Provides the Uuid corresponding to the destination Node of the link.
     * @return      Uuid of the destination Tp.
     */
    Uuid getDestUuid();

    /**
     * Provides link length for physical optical links.
     * @return  Double which corresponds to the link length expressed in kms.
     */
    Double getLength();

    /**
     * Provides latency of the link (evaluated only for physical optical link at that time).
     * @return  Double for transformer of JUNG graph.
     */
    Double getLatency();

    /**
     * Provides available bandwidth associated with OTN links.
     * @return  Long corresponding to available bandwidth expressed in Gbit/s.
     */
    Long getAvailableBandwidth();

    /**
     * Provides used bandwidth associated with OTN links.
     * @return  Long corresponding to used bandwidth expressed in Gbit/s.
     */
    Long getUsedBandwidth();

    /**
     * Provides the link's weight.
     * @return  Double corresponding to link's weight
     */
    double getWeight();

    /**
     * Sets the link's weight.
     * @param  linkWeight corresponding to link's weight
     */
    void setWeight(double linkWeight);

    /**
     * Provides the Id of the source Network Supporting Node of any kind of links.
     * @return  String that corresponds to the supporting Node Id.
     */
    String getsourceNetworkSupNodeId();

    /**
     * Provides the Id of the destination Network Supporting Node of any kind of links.
     * @return  String that corresponds to the supporting Node Id.
     */
    String getdestNetworkSupNodeId();

    /**
     * Provides the SRG list of the link (not handled at that time for T-API links).
     * @return   A List of Long.
     */
    List<Long> getsrlgList();

    /**
     * Provides the span loss of an optical physical link.
     * @return  Double wich corresponds to the fiber loss , expressed in dB, 0 for OTN links.
     */
    Double getspanLoss();

    /**
     * Provides the Chromatic dispersion associated with the physical optical link.
     * @return  Double value of the Chromatic dispersion calculated for the link expressed in ps, 0 for OTN links.
     */
    Double getcd();

    /**
     * Provides the square value of the mean PMD calculated for the physical optical link.
     * @return  Double value of the square of the mean PMD value expressed in ps2/km, 0 for OTN links.
     */
    Double getpmd2();

    /**
     * Provides the correction (according to fiber type) to be applied on target power calculations for optical links.
     * @return  Double corresponding to the correction to be applied (added to calculated power) in dB.
     */
    Double getpowerCorrection();

    /**
     * Provides the name of the client tp associated with A end and corresponding to the Source/Destination of the link.
     * @return  String corresponding to the name of the Tp (a PP for an Add/Drop link, a NW port for a XPONDER-input/
     *          output Link) that is the source (Add or XPONDER-OUTPUT link) or the destination (Drop or XPONDER-INPUT
     *          link) of the link.
     */
    String getClientA();

    /**
     * sets the name of the client tp associated with Z end and corresponding to the Source/Destination of the link.
     * @param  clientTp corresponding to the name of the Tp (a PP for an Add/Drop link, a NW port for a XPONDER-input/
     *         output Link) that is the source (Add or XPONDER-OUTPUT link) or the destination (Drop or XPONDER-INPUT
     *         link) of the link.
     */
    void setClientA(String clientTp);

    /**
     * Provides the name of the client tp associated with A end and corresponding to the Source/Destination of the link.
     * @return  String corresponding to the name of the Tp (a PP for an Add/Drop link, a NW port for a XPONDER-input/
     *          output Link) that is the source (Add or XPONDER-OUTPUT link) or the destination (Drop or XPONDER-INPUT
     *          link) of the link.
     */
    String getClientZ();

    /**
     * sets the name of the client tp associated with Z end and corresponding to the Source/Destination of the link.
     * @param  clientTp corresponding to the name of the Tp (a PP for an Add/Drop link, a NW port for a XPONDER-input/
     *         output Link) that is the source (Add or XPONDER-OUTPUT link) or the destination (Drop or XPONDER-INPUT
     *         link) of the link.
     */
    void setClientZ(String clientTp);

    /**
     * Provides the Id of supporting CLLI Node for link Source Node.
     * @return  String corresponding to the Id of Source supporting CLLI Node.
     */
    String getsourceCLLI();

    /**
     * Provides the Id of supporting CLLI Node for link Destination Node.
     * @return  String corresponding to the Id of Destination supporting CLLI Node.
     */
    String getdestCLLI();

}
