/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OpenRoadmTpTypeResolver;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.link.SourceBuilder;

class OpenRoadmLinkTerminationPointsFactoryTest {

    @DisplayName("Should extract termination points and states when Link1 augmentation is present")
    @Test
    void shouldCreateOpenRoadmLinkFromLinkAndResolvedTerminationPoints() {
        Link link = new LinkBuilder()
                .setLinkId(LinkId.getDefaultInstance("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX"))
                .setSource(
                        new SourceBuilder()
                                .setSourceNode(NodeId.getDefaultInstance("ROADM-C1-DEG1"))
                                .setSourceTp(TpId.getDefaultInstance("DEG1-TTP-TXRX"))
                                .build()
                )
                .setDestination(
                        new DestinationBuilder()
                                .setDestNode(NodeId.getDefaultInstance("ROADM-A1-DEG2"))
                                .setDestTp(TpId.getDefaultInstance("DEG2-TTP-TXRX"))
                                .build()
                ).build();

        TerminationPointId sourceTp = new TerminationPointId(
                "ROADM-C1", "ROADM-C1-DEG1", "DEG1-TTP-TXRX", OpenroadmTpType.DEGREETXRXTTP);
        TerminationPointId destTp = new TerminationPointId(
                "ROADM-A1", "ROADM-A1-DEG2", "DEG2-TTP-TXRX", OpenroadmTpType.DEGREETXRXTTP);
        LinkTerminationPoints expected = new LinkTerminationPoints(sourceTp, destTp);

        Network networkMock = Mockito.mock(Network.class);
        OpenRoadmTpTypeResolver openRoadmTpTypeResolver = Mockito.mock(OpenRoadmTpTypeResolver.class);
        Mockito.when(openRoadmTpTypeResolver.terminationPointId("ROADM-C1-DEG1", "DEG1-TTP-TXRX", networkMock))
                .thenReturn(sourceTp);
        Mockito.when(openRoadmTpTypeResolver.terminationPointId("ROADM-A1-DEG2", "DEG2-TTP-TXRX", networkMock))
                .thenReturn(destTp);

        LinkTerminationPointsFactory linkFactory = new OpenRoadmLinkTerminationPointsFactory(openRoadmTpTypeResolver);

        assertEquals(expected, linkFactory.fromLink(link, networkMock));

        Mockito.verify(openRoadmTpTypeResolver)
                .terminationPointId("ROADM-C1-DEG1", "DEG1-TTP-TXRX", networkMock);
        Mockito.verify(openRoadmTpTypeResolver)
                .terminationPointId("ROADM-A1-DEG2", "DEG2-TTP-TXRX", networkMock);
    }
}
