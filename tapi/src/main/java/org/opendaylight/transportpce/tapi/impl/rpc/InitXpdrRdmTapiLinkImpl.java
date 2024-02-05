/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import org.opendaylight.transportpce.tapi.TapiStringConstants;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkUtilsImpl;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.InitXpdrRdmTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InitXpdrRdmTapiLinkImpl implements InitXpdrRdmTapiLink {
    private static final Logger LOG = LoggerFactory.getLogger(InitXpdrRdmTapiLinkImpl.class);

    private TapiLink tapiLink;
    private TapiNetworkUtilsImpl tapiNetworkUtilsImpl;

    public InitXpdrRdmTapiLinkImpl(TapiLink tapiLink, TapiNetworkUtilsImpl tapiNetworkUtilsImpl) {
        this.tapiLink = tapiLink;
        this.tapiNetworkUtilsImpl = tapiNetworkUtilsImpl;
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmTapiLinkOutput>> invoke(InitXpdrRdmTapiLinkInput input) {
        // TODO --> need to check if the nodes and neps exist in the topology
        String destNode = input.getRdmNode();
        String destTp = input.getAddDropTp();
        String sourceNode = input.getXpdrNode();
        String sourceTp = input.getNetworkTp();
        Link link = this.tapiLink.createTapiLink(sourceNode, sourceTp, destNode, destTp,
            TapiStringConstants.OMS_XPDR_RDM_LINK, TapiStringConstants.OTSI, TapiStringConstants.PHTNC_MEDIA,
            TapiStringConstants.PHTNC_MEDIA_OTS, TapiStringConstants.PHTNC_MEDIA_OTS,
            this.tapiLink.getAdminState(sourceNode, sourceTp, destNode, destTp),
            this.tapiLink.getOperState(sourceNode, sourceTp, destNode, destTp),
            Set.of(LayerProtocolName.PHOTONICMEDIA), Set.of(LayerProtocolName.PHOTONICMEDIA.getName()),
            tapiNetworkUtilsImpl.getTapiTopoUuid());
        InitXpdrRdmTapiLinkOutputBuilder output = new InitXpdrRdmTapiLinkOutputBuilder();
        if (link == null) {
            LOG.error("Error creating link object");
            return RpcResultBuilder.<InitXpdrRdmTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to create link in topology")
                .buildFuture();
        }
        if (tapiNetworkUtilsImpl.putLinkInTopology(link)) {
            output = new InitXpdrRdmTapiLinkOutputBuilder()
                .setResult("Link created in tapi topology. Link-uuid = " + link.getUuid());
        }
        return RpcResultBuilder.success(output.build()).buildFuture();
    }

}
