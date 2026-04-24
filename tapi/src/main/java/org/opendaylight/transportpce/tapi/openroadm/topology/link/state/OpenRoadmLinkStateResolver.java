/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.state;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;

public class OpenRoadmLinkStateResolver implements LinkStateResolver {

    private final LinkStateMapper linkStateMapper;

    public OpenRoadmLinkStateResolver(LinkStateMapper linkStateMapper) {
        this.linkStateMapper = linkStateMapper;
    }

    @Override
    public LinkStateAttributes resolve(Link link, Network topology) {
        AdminStates linkAdmState = null;
        State linkOpState = null;

        Link1 linkAugmentation = link.augmentation(Link1.class);
        if (linkAugmentation != null) {
            linkAdmState = linkAugmentation.getAdministrativeState();
            linkOpState = linkAugmentation.getOperationalState();

            if (linkAugmentation.getOppositeLink() != null) {
                if (topology == null) {
                    return new LinkStateAttributes(null, null);
                }

                Network1 network1 = topology.augmentation(Network1.class);
                if (network1 == null) {
                    return new LinkStateAttributes(null, null);
                }

                Link oppositeLink = network1.nonnullLink().get(new LinkKey(linkAugmentation.getOppositeLink()));

                if (oppositeLink == null) {
                    return new LinkStateAttributes(null, null);
                }

                Link1 oppositeLinkAug = oppositeLink.augmentation(Link1.class);
                if (oppositeLinkAug == null) {
                    return new LinkStateAttributes(null, null);
                }

                return new LinkStateAttributes(
                        linkStateMapper.toTapiAdminState(
                                linkAdmState,
                                oppositeLinkAug.getAdministrativeState()),
                        linkStateMapper.toTapiOperationalState(
                                linkOpState,
                                oppositeLinkAug.getOperationalState()));
            }
        }

        return new LinkStateAttributes(
                linkStateMapper.toTapiAdminState(linkAdmState),
                linkStateMapper.toTapiOperationalState(linkOpState));
    }
}
