/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import org.opendaylight.transportpce.tapi.openroadm.topology.link.format.DefaultLinkIdFormatter;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.format.LinkIdFormatter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;

public class OpenRoadmLinkResolver implements LinkResolver {

    private final LinkIdFormatter linkIdFormatter;

    public OpenRoadmLinkResolver(LinkIdFormatter linkIdFormatter) {
        this.linkIdFormatter = linkIdFormatter;
    }

    public OpenRoadmLinkResolver() {
        this.linkIdFormatter = new DefaultLinkIdFormatter();
    }

    @Override
    public Link resolveLink(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId,
            Network network) {

        if (network == null) {
            throw new LinkNotFoundException("No network topology supplied");
        }

        Network1 network1 = network.augmentationOrElseThrow(Network1.class, () -> new LinkNotFoundException(
                "No topology link data available in network")
        );

        LinkKey key = linkKey(
                srcOpenRoadmTopologyNodeId,
                srcOpenRoadmTopologyTerminationPointId,
                destOpenRoadmTopologyNodeId,
                destOpenRoadmTopologyTerminationPointId
        );

        Link link = network1
                .nonnullLink()
                .get(key);

        if (link == null) {
            throw new LinkNotFoundException(
                    srcOpenRoadmTopologyNodeId,
                    srcOpenRoadmTopologyTerminationPointId,
                    destOpenRoadmTopologyNodeId,
                    destOpenRoadmTopologyTerminationPointId,
                    key.getLinkId().getValue());
        }

        return link;
    }

    private LinkKey linkKey(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId) {

        return new LinkKey(LinkId.getDefaultInstance(linkId(
                srcOpenRoadmTopologyNodeId,
                srcOpenRoadmTopologyTerminationPointId,
                destOpenRoadmTopologyNodeId,
                destOpenRoadmTopologyTerminationPointId
        )));
    }

    private String linkId(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId) {

        return linkIdFormatter.linkId(
                srcOpenRoadmTopologyNodeId,
                srcOpenRoadmTopologyTerminationPointId,
                destOpenRoadmTopologyNodeId,
                destOpenRoadmTopologyTerminationPointId
        );
    }
}
