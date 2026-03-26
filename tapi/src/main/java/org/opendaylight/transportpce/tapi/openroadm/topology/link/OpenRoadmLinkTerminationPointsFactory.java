/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OpenRoadmTpTypeResolver;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.Destination;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.Source;


public class OpenRoadmLinkTerminationPointsFactory implements LinkTerminationPointsFactory {

    private final OpenRoadmTpTypeResolver openRoadmTpTypeResolver;

    public OpenRoadmLinkTerminationPointsFactory(OpenRoadmTpTypeResolver openRoadmTpTypeResolver) {
        this.openRoadmTpTypeResolver = openRoadmTpTypeResolver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkTerminationPoints fromLink(Link link, Network network) {

        Source linkSrc = link.getSource();
        String sourceNodeId = linkSrc.getSourceNode().getValue();

        Destination linkDst = link.getDestination();
        String destNodeId = linkDst.getDestNode().getValue();

        String srcTpId = linkSrc.getSourceTp().getValue();
        String dstTpId = linkDst.getDestTp().getValue();

        TerminationPointId sourceTp = openRoadmTpTypeResolver.terminationPointId(sourceNodeId, srcTpId, network);
        TerminationPointId destTp = openRoadmTpTypeResolver.terminationPointId(destNodeId, dstTpId, network);

        return new LinkTerminationPoints(sourceTp, destTp);
    }
}
