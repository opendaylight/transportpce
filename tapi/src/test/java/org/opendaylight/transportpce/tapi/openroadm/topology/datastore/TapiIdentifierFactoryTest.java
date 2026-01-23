/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.OwnedNodeEdgePointName;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

class TapiIdentifierFactoryTest {

    @DisplayName("nepIid() builds stable UUID keys for topology/node/nep")
    @Test
    void nepIid() {
        TapiIdentifierFactory factory = new TapiIdentifierFactory();

        DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> actual = factory.nepIid(
                "T0 - Full Multi-layer topology",
                new TerminationPointId(
                        "ROADM-C1",
                        "ROADM-C1-SRG1",
                        "SRG1-PP1-TXRX",
                        OpenroadmTpType.SRGTXRXPP
                ),
                OwnedNodeEdgePointName.create(
                        "ROADM-C1",
                        NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                        "SRG1-PP1-TXRX"
                )
        );

        assertEquals("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                actual.findFirstKeyOf(Topology.class).orElseThrow().getUuid().getValue());
        assertEquals("4986dca9-2d59-3d79-b306-e11802bcf1e6",
                actual.findFirstKeyOf(Node.class).orElseThrow().getUuid().getValue());
        assertEquals("abfc9b93-cfae-35a8-9ea9-7fb66b568927",
                actual.findFirstKeyOf(OwnedNodeEdgePoint.class).orElseThrow().getUuid().getValue());
    }

    @DisplayName("nepIdentifier() builds stable UUID keys for topology/node/nep")
    @Test
    void nepIdentifier() {
        TapiIdentifierFactory factory = new TapiIdentifierFactory();

        NepIdentifier nepIdentifier = factory.nepIdentifier(
                "T0 - Full Multi-layer topology",
                new TerminationPointId(
                        "ROADM-C1",
                        "ROADM-C1-SRG1",
                        "SRG1-PP1-TXRX",
                        OpenroadmTpType.SRGTXRXPP
                ),
                OwnedNodeEdgePointName.create(
                        "ROADM-C1",
                        NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                        "SRG1-PP1-TXRX"
                )
        );

        DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> actual = nepIdentifier.iid();

        assertEquals("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                actual.findFirstKeyOf(Topology.class).orElseThrow().getUuid().getValue());
        assertEquals("4986dca9-2d59-3d79-b306-e11802bcf1e6",
                actual.findFirstKeyOf(Node.class).orElseThrow().getUuid().getValue());
        assertEquals("abfc9b93-cfae-35a8-9ea9-7fb66b568927",
                actual.findFirstKeyOf(OwnedNodeEdgePoint.class).orElseThrow().getUuid().getValue());

    }

    @DisplayName("NepIdentifier.toLogString() produces a user friendly string")
    @Test
    void nepIdentifierToString() {
        TapiIdentifierFactory factory = new TapiIdentifierFactory();

        NepIdentifier nepIdentifier = factory.nepIdentifier(
                "T0 - Full Multi-layer topology",
                new TerminationPointId(
                        "ROADM-C1",
                        "ROADM-C1-SRG1",
                        "SRG1-PP1-TXRX",
                        OpenroadmTpType.SRGTXRXPP
                ),
                OwnedNodeEdgePointName.create(
                        "ROADM-C1",
                        NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                        "SRG1-PP1-TXRX"
                )
        );

        String expected = "NEP[Topology=T0 - Full Multi-layer topology, Node=ROADM-C1+PHOTONIC_MEDIA,"
                + " NEP=ROADM-C1+PHOTONIC_MEDIA_OTS+SRG1-PP1-TXRX]";
        assertEquals(expected, nepIdentifier.toLogString());
    }

}
