/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;

public interface TapiLink {

    Link createTapiLink(String srcNodeid, String srcTpId, String dstNodeId, String dstTpId, String linkType,
            String srcNodeQual, String dstNodeQual, String srcTpQual, String dstTpQual,
            String adminState, String operState, Set<LayerProtocolName> layerProtoNameList,
            Set<String> transLayerNameList, Uuid tapiTopoUuid);

    AdministrativeState setTapiAdminState(String adminState);

    AdministrativeState setTapiAdminState(AdminStates adminState1, AdminStates adminState2);

    OperationalState setTapiOperationalState(String operState);

    OperationalState setTapiOperationalState(State operState1, State operState2);

    String getOperState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);

    String getAdminState(String srcNodeId, String destNodeId, String sourceTpId, String destTpId);
}
