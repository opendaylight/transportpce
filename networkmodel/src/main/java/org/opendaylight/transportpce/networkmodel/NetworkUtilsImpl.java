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
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.DeleteLink;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.DeleteLinkInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.DeleteLinkOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.DeleteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitInterDomainLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRdmXpdrLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRdmXpdrLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRdmXpdrLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRdmXpdrLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodesOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitRoadmNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitXpdrRdmLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitXpdrRdmLinksInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitXpdrRdmLinksOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev240923.InitXpdrRdmLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that allows to register TransportPCE internal RPC to complement the different topologies.
 */
@Component
public class NetworkUtilsImpl {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);
    private final DataBroker dataBroker;
    private final Registration rpcReg;

    /**
     * Instantiate the NetworkUtilsImpl object.
     * @param dataBroker Provides access to the conceptual data tree store.
     * @param rpcProvider Service that allows registering Remote Procedure Call (RPC) implementations.
     */
    @Activate
    public NetworkUtilsImpl(@Reference DataBroker dataBroker, @Reference RpcProviderService rpcProvider) {
        this.dataBroker = dataBroker;
        rpcReg = rpcProvider.registerRpcImplementations(
                (DeleteLink) this::deleteLink,
                (InitRoadmNodes) this::initRoadmNodes,
                (InitXpdrRdmLinks) this::initXpdrRdmLinks,
                (InitRdmXpdrLinks) this::initRdmXpdrLinks,
                (InitInterDomainLinks) this::initInterDomainLinks);
        LOG.info("NetworkUtilsImpl instanciated");
    }

    /**
     * Unregister RPC used in this network module when closing the network model service.
     */
    @Deactivate
    public void close() {
        rpcReg.close();
        LOG.info("{} closed", getClass().getSimpleName());
    }

    private ListenableFuture<RpcResult<DeleteLinkOutput>> deleteLink(DeleteLinkInput input) {

        LinkId linkId = new LinkId(input.getLinkId());
        // Building link instance identifier
        DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY)))
            .augmentation(Network1.class).child(Link.class, new LinkKey(linkId))
            .build();


        //Check if link exists
        try {
            ReadTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
            Optional<Link> linkOptional = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, linkIID).get();
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
        writeTransaction.delete(LogicalDatastoreType.CONFIGURATION, linkIID);
        try {
            writeTransaction.commit().get();
            LOG.info("Link with linkId: {} deleted from {} layer.",
                input.getLinkId(), StringConstants.OPENROADM_TOPOLOGY);
            return RpcResultBuilder
                .success(new DeleteLinkOutputBuilder().setResult("Link {} deleted successfully").build())
                .buildFuture();
        } catch (InterruptedException | ExecutionException e) {
            return RpcResultBuilder.<DeleteLinkOutput>failed().buildFuture();
        }
    }

    private ListenableFuture<RpcResult<InitRoadmNodesOutput>> initRoadmNodes(InitRoadmNodesInput input) {
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


    private ListenableFuture<RpcResult<InitInterDomainLinksOutput>>
            initInterDomainLinks(InitInterDomainLinksInput input) {
        LOG.info("Roadm to Roadm inter-domain links rpc called");
        boolean createRdmLinks = OrdLink.createInterDomainLinks(input, this.dataBroker);
        if (createRdmLinks) {
            return RpcResultBuilder
                .success(new InitInterDomainLinksOutputBuilder()
                    .setResult("Unidirectional Roadm-to-Roadm Inter-Domain Link created successfully")
                    .build())
                .buildFuture();
        } else {
            LOG.error("init-inter-domain-links rpc failed due to a bad input parameter");
            return RpcResultBuilder.<InitInterDomainLinksOutput>failed().buildFuture();
        }
    }

    private ListenableFuture<RpcResult<InitXpdrRdmLinksOutput>> initXpdrRdmLinks(InitXpdrRdmLinksInput input) {
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

    private ListenableFuture<RpcResult<InitRdmXpdrLinksOutput>> initRdmXpdrLinks(InitRdmXpdrLinksInput input) {
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
