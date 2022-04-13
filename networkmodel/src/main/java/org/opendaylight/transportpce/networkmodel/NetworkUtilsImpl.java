/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.DeleteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRdmXpdrLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRdmXpdrLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRdmXpdrLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitRoadmNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitXpdrRdmLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitXpdrRdmLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.InitXpdrRdmLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.TransportpceNetworkutilsService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtilsImpl implements TransportpceNetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);
    private final DataBroker dataBroker;

    public NetworkUtilsImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteLinkOutput>> deleteLink(DeleteLinkInput input) {

        LinkId linkId = new LinkId(input.getLinkId());
        // Building link instance identifier
        InstanceIdentifier.InstanceIdentifierBuilder<Link> linkIID = InstanceIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID)))
            .augmentation(Network1.class).child(Link.class, new LinkKey(linkId));


        //Check if link exists
        try {
            ReadTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            Optional<Link> linkOptional = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, linkIID.build())
                .get();
            if (!linkOptional.isPresent()) {
                LOG.info("Link not present");
                return RpcResultBuilder
                    .success(new DeleteLinkOutputBuilder().setResult("Fail").build())
                    .buildFuture();
            }
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("readMdSal: Error reading link {}", input.getLinkId());
            return RpcResultBuilder
                .success(new DeleteLinkOutputBuilder().setResult("Fail").build())
                .buildFuture();
        }

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, linkIID.build());
        try {
            writeTransaction.commit().get();
            LOG.info("Link with linkId: {} deleted from {} layer.",
                input.getLinkId(), NetworkUtils.OVERLAY_NETWORK_ID);
            return RpcResultBuilder
                .success(new DeleteLinkOutputBuilder().setResult("Link {} deleted successfully").build())
                .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            return RpcResultBuilder.<DeleteLinkOutput>failed().buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<InitRoadmNodesOutput>> initRoadmNodes(InitRoadmNodesInput input) {
        boolean createRdmLinks = OrdLink.createRdm2RdmLinks(input, this.dataBroker);
        if (createRdmLinks) {
            return RpcResultBuilder
                .success(new InitRoadmNodesOutputBuilder()
                    .setResult("Unidirectional Roadm-to-Roadm Link created successfully")
                    .build())
                .buildFuture();
        } else {
            return RpcResultBuilder.<InitRoadmNodesOutput>failed().buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<InitXpdrRdmLinksOutput>> initXpdrRdmLinks(InitXpdrRdmLinksInput input) {
        // Assigns user provided input in init-network-view RPC to nodeId
        LOG.info("Xpdr to Roadm links rpc called");
        boolean createXpdrRdmLinks = Rdm2XpdrLink.createXpdrRdmLinks(input.getLinksInput(), this.dataBroker);
        if (createXpdrRdmLinks) {
            return RpcResultBuilder
                .success(new InitXpdrRdmLinksOutputBuilder()
                    .setResult("Xponder Roadm Link created successfully")
                    .build())
                .buildFuture();
        } else {
            LOG.error("init-xpdr-rdm-links rpc failed due to a bad input parameter");
            return RpcResultBuilder.<InitXpdrRdmLinksOutput>failed().buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<InitRdmXpdrLinksOutput>> initRdmXpdrLinks(InitRdmXpdrLinksInput input) {
        LOG.info("Roadm to Xpdr links rpc called");
        boolean createRdmXpdrLinks = Rdm2XpdrLink.createRdmXpdrLinks(input.getLinksInput(), this.dataBroker);
        if (createRdmXpdrLinks) {
            return RpcResultBuilder
                .success(new InitRdmXpdrLinksOutputBuilder()
                    .setResult("Roadm Xponder links created successfully")
                    .build())
                .buildFuture();
        } else {
            LOG.error("init-rdm-xpdr-links rpc failed due to a bad input parameter");
            return RpcResultBuilder.<InitRdmXpdrLinksOutput>failed().buildFuture();
        }
    }
}
