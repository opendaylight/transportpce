/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import java.util.Map;

import org.jgrapht.GraphPath;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.networkanalyzer.PceNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAlgoPathValidator {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceGraph.class);

    // TODO hard-coded 96
    private static final int MAX_WAWELENGTH = 96;

    public PceResult checkPath(GraphPath<String, PceGraphEdge> path,
            Map<NodeId, PceNode> allPceNodes, PceResult pceResult) {

        if (path.getEdgeList().size() == 0) {
            pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
            return pceResult;
        }

        // choose wavelength available in all nodes of the path
        pceResult.setRC(ResponseCodes.RESPONSE_FAILED);
        Long waveL = chooseWavelength(path, allPceNodes);
        if (waveL > 0) {
            pceResult.setResultWavelength(waveL);
            pceResult.setRC(ResponseCodes.RESPONSE_OK);
            LOG.info("In PostAlgoPathValidator: chooseWavelength WL found {} {}", waveL, path.toString());
        }

        // TODO here other post algo validations can be added
        // more data can be sent to PceGraph module via PceResult structure if
        // required
        stubCheckOSNR(path);

        return pceResult;
    }

    private Long chooseWavelength(GraphPath<String, PceGraphEdge> path, Map<NodeId, PceNode> allPceNodes) {
        Long wavelength = -1L;

        for (long i = 1; i <= MAX_WAWELENGTH; i++) {
            boolean completed = true;
            LOG.debug("In chooseWavelength: {} {}", path.getLength(), path.toString());
            for (PceGraphEdge edge : path.getEdgeList()) {
                LOG.debug("In chooseWavelength: source {} ", edge.link().getSourceId().toString());
                PceNode pceNode = allPceNodes.get(edge.link().getSourceId());
                if (!pceNode.checkWL(i)) {
                    completed = false;
                    break;
                }
            }
            if (completed) {
                wavelength = i;
                break;
            }
        }
        return wavelength;
    }

    private boolean stubCheckOSNR(GraphPath<String, PceGraphEdge> path) {
        double localOsnr = 0L;
        for (PceGraphEdge edge : path.getEdgeList()) {
            localOsnr = localOsnr + edge.link().getosnr();
        }
        LOG.info("In OSNR Stub: {}", localOsnr);
        return true;
    }
}
