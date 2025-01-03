/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkUtilsImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.DeleteTapiLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.DeleteTapiLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.DeleteTapiLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728.DeleteTapiLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeleteTapiLinkImpl implements DeleteTapiLink {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteTapiLinkImpl.class);

    private TapiNetworkUtilsImpl tapiNetworkUtilsImpl;
    private NetworkTransactionService networkTransactionService;

    public DeleteTapiLinkImpl(TapiNetworkUtilsImpl tapiNetworkUtilsImpl,
            NetworkTransactionService networkTransactionService) {
        this.tapiNetworkUtilsImpl = tapiNetworkUtilsImpl;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteTapiLinkOutput>> invoke(DeleteTapiLinkInput input) {
        // TODO: check if this IID is correct
        // TODO --> need to check if the link exists in the topology
        try {
            DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Context.class)
                .augmentation(Context1.class)
                .child(TopologyContext.class)
                .child(Topology.class, new TopologyKey(tapiNetworkUtilsImpl.getTapiTopoUuid()))
                .child(Link.class, new LinkKey(input.getUuid()))
                .build();
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, linkIID);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI link deleted successfully.");
            return RpcResultBuilder.success(new DeleteTapiLinkOutputBuilder()
                .setResult("Link successfully deleted from tapi topology").build()).buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI link", e);
            return RpcResultBuilder.<DeleteTapiLinkOutput>failed()
                .withError(ErrorType.RPC, "Failed to delete link from topology")
                .buildFuture();
        }
    }

}
