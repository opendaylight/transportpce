/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.link;

import org.opendaylight.transportpce.tapi.openroadm.topology.link.LinkTerminationPoints;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;

public class LinkTerminationPointNormalizer implements LinkEndpointNormalizer {

    @Override
    public LinkEndpoints normalize(LinkTerminationPoints linkTerminationPoints) {
        Endpoint source = normalize(linkTerminationPoints.source());
        Endpoint destination = normalize(linkTerminationPoints.destination());

        return new LinkEndpoints(
                source.nodeId(),
                source.tpId(),
                destination.nodeId(),
                destination.tpId());
    }

    private static Endpoint normalize(TerminationPointId tp) {
        return switch (tp.openRoadmTpType()) {
            case XPONDERNETWORK,
                 XPONDERCLIENT,
                 XPONDERPORT -> new Endpoint(tp.nodeId(), tp.tpId());

            default -> new Endpoint(tp.supportingNodeId(), tp.tpId());
        };
    }

    private record Endpoint(String nodeId, String tpId) {
    }
}
