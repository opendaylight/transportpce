/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.PathValidator;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev211210.OpenroadmLinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InAlgoPathValidator implements PathValidator<String, PceGraphEdge> {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(InAlgoPathValidator.class);
    private static final String IN_CHECK_PATH_DROPPED_MSG = "in checkPath dropped {} {} ";

    public InAlgoPathValidator() {
        super();
    }

    @Override
    public boolean isValidPath(GraphPath<String, PceGraphEdge> partialPath, PceGraphEdge edge) {
        int size = partialPath.getEdgeList().size();
        if (size == 0) {
            return true;
        }
        LOG.debug("InAlgoPathValidator: partialPath size: {} prev edge {} new edge {}",
            size, edge.link().getlinkType(), partialPath.getEdgeList().get(size - 1).link().getlinkType());

        return (checkTurn(partialPath.getEdgeList().get(size - 1).link().getlinkType(), edge.link().getlinkType()));
    }

    @SuppressWarnings("java:S1541")
    //sonar issue The Cyclomatic Complexity of this method "checkTurn" is 13 which is greater than 10 authorized.
    //here we have clear conditional, so for the moment no need to manage this issue
    private boolean checkTurn(OpenroadmLinkType prevType, OpenroadmLinkType nextType) {

        if (nextType == OpenroadmLinkType.ADDLINK && prevType != OpenroadmLinkType.XPONDEROUTPUT) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.EXPRESSLINK && prevType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.DROPLINK && prevType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        if (nextType == OpenroadmLinkType.XPONDERINPUT && prevType != OpenroadmLinkType.DROPLINK) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        if (prevType == OpenroadmLinkType.EXPRESSLINK && nextType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        if (prevType == OpenroadmLinkType.ADDLINK && nextType != OpenroadmLinkType.ROADMTOROADM) {
            LOG.debug(IN_CHECK_PATH_DROPPED_MSG, prevType, nextType);
            return false;
        }

        return true;
    }
}

