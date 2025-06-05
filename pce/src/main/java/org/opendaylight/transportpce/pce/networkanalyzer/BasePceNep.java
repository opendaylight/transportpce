/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OmsConnectionEndPointSpec;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.OtsMediaConnectionEndPointSpec;

public class BasePceNep {

    private Uuid nepCepUuid;
    private Map<NameKey, Name> nepCepName;
    private DirectionType nepDirection;
    private BitSet frequenciesBitSet;
    private LayerProtocolName lpn;
    private LAYERPROTOCOLQUALIFIER lpq;
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private Map<Uuid, Name> connectedLink = new HashMap<>();
    private Uuid clientNep;
    private Uuid parentNep;
    private Uuid sipUuid;
    private OpenroadmTpType tpType;
    private Map<Uuid, Name> virtualNep = new HashMap<>();
    private List<Uuid> nodeRuleGroupUuid = new ArrayList<>();
    private List<Uuid> indirectNrgUuid = new ArrayList<>();
    private Uuid cepOtsUuid;
    private Uuid cepOmsUuid;
    private OtsMediaConnectionEndPointSpec otsSpec;
    private OmsConnectionEndPointSpec omsSpec;
    private List<Uuid> verticallyConnectedNep = new ArrayList<>();

    /**
     * Abstracted object used in various lists to describe Logical connection points, whether they are NEPs or CEPs.
     */
    public BasePceNep(Uuid uuid, Map<NameKey, Name> name) {
        this.nepCepUuid = uuid;
        this.nepCepName = name;
    }

    /**
     * Sets tpType according to OpenROADM defined enum.
     * @param orTpType OpenroadmTpType
     */
    public void setTpType(OpenroadmTpType orTpType) {
        this.tpType = orTpType;
    }

    /**
     * Fills the list of NEP and CEPs that are vertically connected to a specific BasePceNep.
     *  Vertically-connected-NEP param is used to propagate some of the information such as connectivity between
     *  logical connection points that could be initially defined through NRGs and IRGs only at a specific layer.
     *  It helps providing continuity through the different horizontal layers corresponding to layer-protocol-qualifiers
     *  and simplifies some of the process when information needed is not directly supported by the layer we currently
     *  work on.
     * @param uuidList  A list of Uuid that corresponds to the logical connection points vertically stacked and
     *                  associated to the same port.
     */
    public void setVerticallyConnectedNep(List<Uuid> uuidList) {
        this.verticallyConnectedNep.addAll(uuidList);
    }

    /**
     * In the context of ROADM disaggregation, allows setting the virtual NEP associated with a line/client NEP.
     *   Virtual NEPs correspond to unmodeled NEP in T-API topology representation of ROADMs such as CP for SRG
     *   and CTPs for Degrees; for which specific restrictions may apply to reflect contention.
     *   They are also used to reduce the number of internal links to be created when modeling internal ROADM
     *   connectivity.
     * @param virtNepId     A Map of Names using the Uuid of the virtual NEP as the key, and its Name as the value.
     *                      Thus, the map contains a single couple Uuid/Name.
     */
    public void setVirtualNep(Map<Uuid, Name> virtNepId) {
        this.virtualNep = virtNepId;
    }

    /**
     * Adds to the List of NRG Uuids in nodeRuleGroupUuid param of the BasePceNep a NRG's Uuid.
     *  Each BasePceNep owns a list of the NRG Uuid it is directly or indirectly associated to. Directly means here the
     *  Uuid of the bpn appears in the NRG. Indirectly means that the Uuid of the bpn does not appear directly in the
     *  list of NEPs hosted by the NRG, but at least one of the NEP vertically connected to the bpn appears in the list
     *  of NEPs of the NRG. This participates in propagating connectivity information to any layer, and being less
     *  adherent to OEMs implementation.
     * @param nrgUuid       Uuid of the NRG to be added to the list.
     */
    public void setNodeRuleGroupUuid(Uuid nrgUuid) {
        this.nodeRuleGroupUuid.add(nrgUuid);
    }

    /**
     * Adds to the List indirectNrgUuid of the BasePceNep, the Uuid of a NRG the bpn is associated with through an IRG.
     *  Each BasePceNep owns a list of NRG's Uuid, the bpn is not directly associated to, but that an IndirectRuleGroup
     *  connects to. The bpn is associated to NRGs (being part the NRGs'NEP list or vertically connected to a NEP that
     *  is part of it) that appear as an associated NRG in a IRG. The other associated NRGs of the IRG are added to the
     *  indirectNrgUuid List, as far as the IRG is based on must/may-forward forwarding rule.
     *  This participates in propagating connectivity information to any layer, and being less adherent to
     *  OEMs implementation
     * @param indNrgUuid       List of Uuids of the associated NRGs referenced in the IRG to be added to the
     *                         IndirectNrgUuid list of the BasePceNep.
     */
    public void setIndirectNRGUuid(List<Uuid> indNrgUuid) {
        this.indirectNrgUuid.addAll(indNrgUuid);
        this.indirectNrgUuid.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Sets the Client NEP Uuid.
     * @param clientNepId  Uuid of the client NEP.
     */
    public void setClientNep(Uuid clientNepId) {
        this.clientNep = clientNepId;
    }

    /**
     * Sets the Parent NEP Uuid.
     * @param parentNepId  Uuid of the client NEP.
     */
    public void setParentNep(Uuid parentNepId) {
        this.parentNep = parentNepId;
    }

    /**
     * Sets the Uuid of the OTS CEP the network NEP is vertically connected to.
     * @param uuid  Uuid of the OTS CEP.
     */
    public void setCepOtsUuid(Uuid uuid) {
        this.cepOtsUuid = uuid;
    }

    /**
     * Sets the Uuid of the OMS CEP the network NEP is vertically connected to.
     * @param uuid  Uuid of the OMS CEP.
     */
    public void setCepOmsUuid(Uuid uuid) {
        this.cepOmsUuid = uuid;
    }

    /**
     * Sets the OtsMediaConnectionEndPointSpec of the related OTS NEP/CEP.
     * @param otsCepSpec    OtsMediaConnectionEndPointSpec of the OTS CEP which contains impairment-route-entries:
     *                      information associated with the output line fiber (fiber type, pmd, length , total-loss...),
     *                      as well as the occupation and the power measured across the different spectrum portions.
     */
    public void setCepOtsSpec(OtsMediaConnectionEndPointSpec otsCepSpec) {
        this.otsSpec = otsCepSpec;
    }

    /**
     * Sets the OmsMediaConnectionEndPointSpec of the related OMS NEP/CEP.
     * @param omsCepSpec    OmsMediaConnectionEndPointSpec of the OMS CEP which contains the list of amplifier and their
     *                      settings on the OMS path of the output line fiber (actual-gain/tilt, input/output power...).
     *                      as well as the occupation and the power measured across the different spectrum portions.
     */
    public void setCepOmsSpec(OmsConnectionEndPointSpec omsCepSpec) {
        this.omsSpec = omsCepSpec;
    }

    /**
     * Sets the LayerProtocolName of the BasePceNep.
     * @param lpname  LayerProtocolName.
     */
    public void setLpn(LayerProtocolName lpname) {
        this.lpn = lpname;
    }

    /**
     * Sets the LayerProtocolQualifier of the BasePceNep.
     * @param lpqname   LayerProtocolQualifier. Used for CEP but also NEP bpn. For this last, shall correspond to the
     *                  lpq of the NEP's client CEP.
     */
    public void setLpq(LAYERPROTOCOLQUALIFIER lpqname) {
        this.lpq = lpqname;
    }

    /**
     * Sets the AdministrativeState (as defined in T-API topology) of the BasePceNep.
     * @param as   AdministrativeState.
     */
    public void setAdminState(AdministrativeState as) {
        this.adminState = as;
    }

    /**
     * Sets the OperationalState (as defined in T-API topology) of the BasePceNep.
     * @param os   OperationalState.
     */
    public void setOperationalState(OperationalState os) {
        this.operationalState = os;
    }

    /**
     * Sets the DirectionType (as defined in TransportPCE/TapiOpticalNode) of the BasePceNep.
     * @param direction   DirectionType { SINK, SOURCE, BIDIRECTIONAL, UNIDIRECTIONAL, UNDEFINED }.
     */
    public void setDirection(DirectionType direction) {
        this.nepDirection = direction;
    }

    /**
     * Sets the frequency Bitset coding spectrum use of the BasePceNep.
     * @param freqBitset Bitset (Binary of 96 Bytes) coding spectrum use of the BasePceNep (1 = available, 0 = used).
     */
    public void setFrequencyBitset(BitSet freqBitset) {
        this.frequenciesBitSet = freqBitset;
    }

    /**
     * Set the ConnectedInternalLinks parameter of the BasePceNep.
     * @param linkId    A map of internal links connected to the NEP using the Uuid of the link as a key, and its name
     *                  as a value: the map contains only one couple (Uuid, linkId), internal links being bidirectional)
     */
    public void setConnectedInternalLinks(Map<Uuid, Name> linkId) {
        this.connectedLink.putAll(linkId);
    }

    /**
     * Sets the Uuid of the SIP mapped to the BasePceNep NEP.
     * @param setSipUuid    Uuid of the SIP.
     */
    public void setSipUuid(Uuid setSipUuid) {
        this.sipUuid = setSipUuid;
    }

    /**
     * Retrieves the DirectionType (as defined in TransportPCE/TapiOpticalNode) of the BasePceNep.
     * @return DirectionType { SINK, SOURCE, BIDIRECTIONAL, UNIDIRECTIONAL, UNDEFINED }.
     */
    public DirectionType getDirection() {
        return this.nepDirection;
    }

    /**
     * Retrieves the Name of of the BasePceNep.
     * @return  A map of (NameKey, Name)
     */
    public Map<NameKey, Name> getName() {
        return this.nepCepName;
    }

    /**
     * Retrieves the Uuid of the BasePceNep.
     * @return  Uuid of the NEP/CEP
     */
    public final Uuid getNepCepUuid() {
        return this.nepCepUuid;
    }

    public BitSet getFrequenciesBitSet() {
        return this.frequenciesBitSet;
    }

    /**
     * Retrieves the Layer Protocol Name of the BasePceNep.
     * @return LayerProtocolName of the NEP/CEP {ETH, DSR, PHOTONIC_MEDIA, DIGITAL_OTN}.
     */
    public LayerProtocolName getLpn() {
        return this.lpn;
    }

    /**
     * Retrieves the Layer Protocol Qualifier of the BasePceNep.
     * @return LAYERPROTOCOLQUALIFIER of the NEP/CEP.
     */
    public LAYERPROTOCOLQUALIFIER getLpq() {
        return this.lpq;
    }

    /**
     * Retrieves the AdministrativeState (as defined in T-API topology) of the BasePceNep.
     * @return AdministrativeState of the NEP/CEP.
     */
    public AdministrativeState getAdminState() {
        return this.adminState;
    }

    /**
     * Retrieves the OperationalState (as defined in T-API topology) of the BasePceNep.
     * @return OperationalState of the NEP/CEP.
     */
    public OperationalState getOperationalState() {
        return this.operationalState;
    }

    /**
     * Retrieves the Uuid/Names of the links connected to the NEP.
     * @return      A map of (Uuid, Name) of the links connected to the NEP.
     */
    public Map<Uuid, Name> getConnectedLink() {
        return this.connectedLink;
    }

    /**
     * Retrieves the Client NEP of the BasePceNep.
     * @return Uuid of client NEP.
     */
    public Uuid getClientNep() {
        return clientNep;
    }

    /**
     * Retrieves the Parent NEP of the BasePceNep.
     * @return Uuid of the parent NEP.
     */
    public final Uuid getParentNep() {
        return this.parentNep;
    }

    /**
     * Retrieves the tp type (OpenROADM format) of the BasePceNep.
     * @return   OpenroadmTpType of the NEP/CEP.
     */
    public OpenroadmTpType getTpType() {
        return this.tpType;
    }

    /**
     * Retrieves vertically connected NEPs of the BasePceNep.
     *  Provide a list of NEPs/CEP stacked over the different layers that share the same port as the BasePceNep
     * @return       A list of Uuid of the NEP/CEP vertically connected to the NEP/CEP.
     */
    public List<Uuid> getVerticallyConnectedNep() {
        return this.verticallyConnectedNep;
    }

    /**
     * Retrieves the Virtual NEP associated with the BasePceNep.
     *  Resulting from the disaggregation of a ROADM, for SRG PPs and Degree TTPs , provides name and Uuid of the
     *  VirtualNEP associated with the BasePceNep tp (Either CTP for Degrees, or CP for SRGs).
     * @return      A map of (Uuid,Name) corresponding to the Virtual BasePceNep.
     */
    public final Map<Uuid, Name> getVirtualNep() {
        return this.virtualNep;
    }

    /**
     * Provides the List of NRGs' Uuids, the BasePceNep is, directly or indirectly, associated to.
     *  Each BasePceNep owns a list of the NRG Uuid it is directly or indirectly associated to. Directly means here the
     *  Uuid of the bpn appears in the NRG. Indirectly means that the Uuid of the bpn does not appear directly in the
     *  list of NEPs hosted by the NRG, but at least one of the NEP vertically connected to the bpn appears in the list
     *  of NEPs of the NRG. This participates in propagating connectivity information to any layer, and being less
     *  adherent to OEMs implementation.
     * @return       A list of Uuid of the different NRG the BasePceNep is associated to.
     */
    public List<Uuid> getNodeRuleGroupUuid() {
        return this.nodeRuleGroupUuid;
    }

    /**
     * Provides the List NRGs'Uuid, the BasePceNep is, directly or indirectly, associated to through an IRG.
     *  Each BasePceNep owns a list of NRG's Uuid, the bpn is not directly associated to, but that an IndirectRuleGroup
     *  connects to. The bpn is associated to NRGs (being part the NRGs'NEP list or vertically connected to a NEP that
     *  is part of it) that appear as an associated NRG in a IRG. The other associated NRGs of the IRG are added to the
     *  indirectNrgUuid List, as far as the IRG is based on must/may-forward forwarding rule.
     *  This participates in propagating connectivity information to any layer, and being less adherent to
     *  OEMs implementation
     * @return        A list of Uuids of associated NRGs referenced in IRGs, the BasePceNep is associated to
     *               (directly or indirectly).
     */
    public List<Uuid> getindirectNrgUuid() {
        return this.indirectNrgUuid;
    }

    /**
     * Retrieves the Uuid of the OTS CEP the network NEP is vertically connected to.
     * @return Uuid of the OTS CEP.
     */
    public Uuid getcepOtsUuid() {
        return this.cepOtsUuid;
    }

    /**
     * Retrieves the Uuid of the OMS CEP the network NEP is vertically connected to.
     * @return Uuid of the OMS CEP.
     */
    public Uuid getcepOmsUuid() {
        return this.cepOmsUuid;
    }

    /**
     * Retrieves the OtsMediaConnectionEndPointSpec of the BasePceNep related OTS CEP.
     * @return    OtsMediaConnectionEndPointSpec of the OTS CEP which contains impairment-route-entries:
     *            information associated with the output line fiber (fiber type, pmd, length , total-loss...),
     *            as well as the occupation and the power measured across the different spectrum portions.
     */
    public OtsMediaConnectionEndPointSpec getCepOtsSpec() {
        return this.otsSpec;
    }

    /**
     * Retrieves the OmsMediaConnectionEndPointSpec of the BasePceNep related OMS NEP/CEP.
     * @return   OmsMediaConnectionEndPointSpec of the OMS CEP which contains the list of amplifier and their
     *          settings on the OMS path of the output line fiber (actual-gain/tilt, input/output power...).
     *          as well as the occupation and the power measured across the different spectrum portions.
     */
    public OmsConnectionEndPointSpec getCepOmsSpec() {
        return this.omsSpec;
    }

    /**
     * Retrieves the Uuid of the SIP mapped to BasePceNep.
     * @return Uuid of the mapped SIP.
     */
    public Uuid getSipUuid() {
        return this.sipUuid;
    }

}
