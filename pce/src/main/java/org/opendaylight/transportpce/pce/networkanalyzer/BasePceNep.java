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
import org.opendaylight.transportpce.pce.networkanalyzer.TapiOpticalNode.DirectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
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
    private AdministrativeState adminState;
    private OperationalState operationalState;
    private Map<Uuid, Name> connectedLink = new HashMap<>();
    private Uuid clientNep;
    private Uuid parentNep;
    private OpenroadmTpType tpType;
    private Map<Uuid, Name> virtualNep = new HashMap<>();
    private List<Uuid> nodeRuleGroupUuid = new ArrayList<>();
    private Uuid cepOtsUuid;
    private Uuid cepOmsUuid;
    private OtsMediaConnectionEndPointSpec otsSpec;
    private OmsConnectionEndPointSpec omsSpec;
    // private Uuid subNodeUuid;
    /*
     * Basic NEP object used in various list to describe OMS and OTS Logical
     * connection points
     */

    public BasePceNep(Uuid uuid, Map<NameKey, Name> name) {
        this.nepCepUuid = uuid;
        this.nepCepName = name;
        // TODO: retrieve direction, available spectrum, op/adminStates &
        // connected link
        // from topology, when 2.4 models available
        // compute frequenciesBitset from util compileBitSetFromSpectrum

    }

    public void setTpType(OpenroadmTpType orTpType) {
        this.tpType = orTpType;
    }

    public void setVirtualNep(Map<Uuid, Name> virtNepId) {
        this.virtualNep = virtNepId;
    }

    public void setNodeRuleGroupUuid(Uuid ndrgUuid) {
        this.nodeRuleGroupUuid.add(ndrgUuid);
    }

    public void setClientNep(Uuid clientNepId) {
        this.clientNep = clientNepId;
    }

    public void setParentNep(Uuid parentNepId) {
        this.parentNep = parentNepId;
    }

    public void setCepOtsUuid(Uuid uuid) {
        this.cepOtsUuid = uuid;
    }

    public void setCepOmsUuid(Uuid uuid) {
        this.cepOmsUuid = uuid;
    }

    public void setCepOtsSpec(OtsMediaConnectionEndPointSpec otsCepSpec) {
        this.otsSpec = otsCepSpec;
    }

    public void setCepOmsSpec(OmsConnectionEndPointSpec oces) {
        this.omsSpec = oces;
    }

    public void setLpn(LayerProtocolName lpname) {
        this.lpn = lpname;
    }

    public void setAdminState(AdministrativeState as) {
        this.adminState = as;
    }

    public void setOperationalState(OperationalState os) {
        this.operationalState = os;
    }

    public void setDirection(DirectionType direction) {
        this.nepDirection = direction;
    }

    public void setFrequencyBitset(BitSet freqBitset) {
        this.frequenciesBitSet = freqBitset;
    }

    public void setConnectedInternalLinks(Map<Uuid, Name> linkId) {
        this.connectedLink.putAll(linkId);
    }

    public DirectionType getDirection() {
        return this.nepDirection;
    }

    public Map<NameKey, Name> getName() {
        return this.nepCepName;
    }

    public final Uuid getNepCepUuid() {
        return this.nepCepUuid;
    }

    public BitSet getFrequenciesBitSet() {
        return this.frequenciesBitSet;
    }

    public LayerProtocolName getLpn() {
        return this.lpn;
    }

    public AdministrativeState getAdminState() {
        return this.adminState;
    }

    public OperationalState getOperationalState() {
        return this.operationalState;
    }

    public Map<Uuid, Name> getConnectedLink() {
        return this.connectedLink;
    }

    public Uuid getClientNep() {
        return clientNep;
    }

    public final Uuid getParentNep() {
        return this.parentNep;
    }

    public OpenroadmTpType getTpType() {
        return this.tpType;
    }

    public final Map<Uuid, Name> getVirtualNep() {
        return this.virtualNep;
    }

    public List<Uuid> getNodeRuleGroupUuid() {
        return this.nodeRuleGroupUuid;
    }

    public Uuid getcepOtsUuid() {
        return this.cepOtsUuid;
    }

    public Uuid getcepOmsUuid() {
        return this.cepOmsUuid;
    }

    public OtsMediaConnectionEndPointSpec getCepOtsSpec() {
        return this.otsSpec;
    }

    public OmsConnectionEndPointSpec getCepOmsSpec() {
        return this.omsSpec;
    }

}
