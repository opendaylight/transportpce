/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev250110.Link1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;

class OpenRoadmLinkResolverTest {

    @Test
    @DisplayName("should resolve link when matching link exists in topology")
    void shouldResolveLinkWhenMatchingLinkExistsInTopology() {
        LinkId linkId = new LinkId("ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1");
        LinkKey linkKey = new LinkKey(linkId);
        Link expected = new LinkBuilder()
                .withKey(linkKey)
                .setLinkId(linkId)
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(new NodeId("ROADM-A1-SRG1"))
                                .setSourceTp(new TpId("SRG1-PP1-TXRX"))
                                .build())
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(new NodeId("XPDR-A1-XPDR1"))
                                .setDestTp(new TpId("XPDR1-NETWORK1"))
                                .build())
                .addAugmentation(
                        new Link1Builder()
                                .setLinkType(OpenroadmLinkType.XPONDERINPUT)
                                .setOppositeLink(
                                        new LinkId("XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX"))
                                .setOperationalState(State.InService)
                                .setAdministrativeState(AdminStates.InService)
                                .build()
                ).build();

        Network network = new NetworkBuilder()
                .setNetworkId(NetworkId.getDefaultInstance("openroadm-topology"))
                .addAugmentation(
                        new Network1Builder()
                                .setLink(Map.of(expected.key(), expected))
                                .build()
                ).build();

        LinkResolver linkResolver = new OpenRoadmLinkResolver();
        Link actual = linkResolver.resolveLink(
                "ROADM-A1-SRG1",
                "SRG1-PP1-TXRX",
                "XPDR-A1-XPDR1",
                "XPDR1-NETWORK1",
                network
        );

        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("should throw when topology link augmentation is missing")
    void shouldThrowWhenTopologyLinkAugmentationIsMissing() {
        Network network = new NetworkBuilder()
                .setNetworkId(NetworkId.getDefaultInstance("openroadm-topology"))
                .build();

        LinkResolver linkResolver = new OpenRoadmLinkResolver();
        LinkNotFoundException exception = assertThrows(
                LinkNotFoundException.class,
                () -> linkResolver.resolveLink(
                        "ROADM-A1-SRG1",
                        "SRG1-PP1-TXRX",
                        "XPDR-A1-XPDR1",
                        "XPDR1-NETWORK1",
                        network
                )
        );

        assertEquals("No topology link data available in network", exception.getMessage());
    }

    @Test
    @DisplayName("should throw when matching link does not exist in topology")
    void shouldThrowWhenMatchingLinkDoesNotExistInTopology() {
        Network network = new NetworkBuilder()
                .setNetworkId(NetworkId.getDefaultInstance("openroadm-topology"))
                .addAugmentation(new Network1Builder()
                        .setLink(Map.of())
                        .build())
                .build();

        LinkResolver linkResolver = new OpenRoadmLinkResolver();
        LinkNotFoundException exception = assertThrows(
                LinkNotFoundException.class,
                () -> linkResolver.resolveLink(
                        "ROADM-A1-SRG1",
                        "SRG1-PP1-TXRX",
                        "XPDR-A1-XPDR1",
                        "XPDR1-NETWORK1",
                        network
                )
        );

        assertEquals(
                "Link not found in network topology: "
                        + "ROADM-A1-SRG1/SRG1-PP1-TXRX -> XPDR-A1-XPDR1/XPDR1-NETWORK1 "
                        + "(linkId=ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1)",
                exception.getMessage()
        );
    }
}
