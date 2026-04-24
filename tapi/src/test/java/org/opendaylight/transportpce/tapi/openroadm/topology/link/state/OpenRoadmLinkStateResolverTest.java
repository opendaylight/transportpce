/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;

class OpenRoadmLinkStateResolverTest {

    private final OpenRoadmLinkStateResolver resolver =
            new OpenRoadmLinkStateResolver(new OpenRoadmLinkStateMapper());

    @Test
    void resolve_shouldUseSingleLinkStateWhenNoOppositeLinkIsDeclared() {
        Link link = link(
                "link-a",
                null,
                AdminStates.InService,
                State.InService);

        LinkStateAttributes result = resolver.resolve(link, null);

        assertEquals(AdministrativeState.UNLOCKED, result.administrativeState());
        assertEquals(OperationalState.ENABLED, result.operationalState());
    }

    @Test
    void resolve_shouldCombineCurrentAndOppositeLinkStates() {
        Link link = link(
                "link-a",
                "link-b",
                AdminStates.InService,
                State.InService);

        Link oppositeLink = link(
                "link-b",
                "link-a",
                AdminStates.OutOfService,
                State.OutOfService);

        LinkStateAttributes result = resolver.resolve(
                link,
                topology(oppositeLink));

        assertEquals(AdministrativeState.LOCKED, result.administrativeState());
        assertEquals(OperationalState.DISABLED, result.operationalState());
    }

    @Test
    void resolve_shouldReturnNullStatesWhenOppositeLinkIsMissing() {
        Link link = link(
                "link-a",
                "missing-link",
                AdminStates.InService,
                State.InService);

        LinkStateAttributes result = resolver.resolve(
                link,
                topology());

        assertNull(result.administrativeState());
        assertNull(result.operationalState());
    }

    @Test
    void resolve_shouldReturnNullStatesWhenTopologyIsMissing() {
        Link link = link(
                "link-a",
                "link-b",
                AdminStates.InService,
                State.InService);

        LinkStateAttributes result = resolver.resolve(link, null);

        assertNull(result.administrativeState());
        assertNull(result.operationalState());
    }

    @Test
    void resolve_shouldReturnNullStatesWhenOppositeLinkHasNoOpenRoadmAugmentation() {
        Link link = link(
                "link-a",
                "link-b",
                AdminStates.InService,
                State.InService);

        Link oppositeLinkWithoutAugmentation = new LinkBuilder()
                .withKey(new LinkKey(new LinkId("link-b")))
                .setLinkId(new LinkId("link-b"))
                .build();

        LinkStateAttributes result = resolver.resolve(
                link,
                topology(oppositeLinkWithoutAugmentation));

        assertNull(result.administrativeState());
        assertNull(result.operationalState());
    }

    @Test
    void resolve_shouldReturnNullStatesWhenCurrentLinkHasNoOpenRoadmAugmentation() {
        Link link = new LinkBuilder()
                .withKey(new LinkKey(new LinkId("link-a")))
                .setLinkId(new LinkId("link-a"))
                .build();

        LinkStateAttributes result = resolver.resolve(link, topology());

        assertNull(result.administrativeState());
        assertNull(result.operationalState());
    }

    private static Link link(
            String linkId,
            String oppositeLinkId,
            AdminStates adminState,
            State operState) {

        Link1Builder augmentation = new Link1Builder()
                .setAdministrativeState(adminState)
                .setOperationalState(operState);

        if (oppositeLinkId != null) {
            augmentation.setOppositeLink(new LinkId(oppositeLinkId));
        }

        return new LinkBuilder()
                .withKey(new LinkKey(new LinkId(linkId)))
                .setLinkId(new LinkId(linkId))
                .addAugmentation(augmentation.build())
                .build();
    }

    private static Network topology(Link... links) {
        Map<LinkKey, Link> linkMap = java.util.Arrays.stream(links)
                .collect(java.util.stream.Collectors.toMap(Link::key, link -> link));

        return new NetworkBuilder()
                .withKey(new NetworkKey(new NetworkId("openroadm-topology")))
                .setNetworkId(new NetworkId("openroadm-topology"))
                .addAugmentation(new Network1Builder()
                        .setLink(linkMap)
                        .build())
                .build();
    }
}
