/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRdmXpdrLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitXpdrRdmLinksOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.NetworkutilsService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtilsImpl implements NetworkutilsService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkUtilsImpl.class);
    private final DataBroker dataBroker;
    private final OpenRoadmTopology openRoadmTopology;

    public NetworkUtilsImpl(DataBroker dataBroker, OpenRoadmTopology openRoadmTopology) {
        this.dataBroker = dataBroker;
        this.openRoadmTopology = openRoadmTopology;
    }

    public Future<RpcResult<InitRoadmNodesOutput>> initRoadmNodes(InitRoadmNodesInput input) {
        boolean createRdmLinks = OrdLink.createRdm2RdmLinks(input,
                openRoadmTopology,dataBroker);
        if (createRdmLinks) {
            return RpcResultBuilder
                    .success(new InitRoadmNodesOutputBuilder().setResult(
                            "Unidirectional Roadm-to-Roadm Link created successfully"))
                    .buildFuture();
        } else {
            return RpcResultBuilder.<InitRoadmNodesOutput>failed().buildFuture();
        }
    }

    @Override
    public Future<RpcResult<InitXpdrRdmLinksOutput>> initXpdrRdmLinks(InitXpdrRdmLinksInput input) {
        // Assigns user provided input in init-network-view RPC to nodeId
        boolean createXpdrRdmLinks = Rdm2XpdrLink.createXpdrRdmLinks(input.getLinksInput(),
                openRoadmTopology,dataBroker);
        if (createXpdrRdmLinks) {
            return RpcResultBuilder
                    .success(new InitXpdrRdmLinksOutputBuilder().setResult("Xponder Roadm Link created successfully"))
                    .buildFuture();
        } else {
            return RpcResultBuilder.<InitXpdrRdmLinksOutput>failed().buildFuture();
        }
    }

    public Future<RpcResult<InitRdmXpdrLinksOutput>> initRdmXpdrLinks(InitRdmXpdrLinksInput input) {
        boolean createRdmXpdrLinks = Rdm2XpdrLink.createRdmXpdrLinks(input.getLinksInput(),
                openRoadmTopology,dataBroker);
        if (createRdmXpdrLinks) {
            return RpcResultBuilder
                    .success(new InitRdmXpdrLinksOutputBuilder().setResult("Roadm Xponder links created successfully"))
                    .buildFuture();
        } else {
            return RpcResultBuilder.<InitRdmXpdrLinksOutput>failed().buildFuture();
        }
    }
}
