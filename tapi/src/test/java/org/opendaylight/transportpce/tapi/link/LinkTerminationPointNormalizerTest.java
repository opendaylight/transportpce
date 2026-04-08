/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.openroadm.topology.link.LinkTerminationPoints;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointId;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;

class LinkTerminationPointNormalizerTest {

    private final LinkTerminationPointNormalizer normalizer = new LinkTerminationPointNormalizer();

    @Test
    void shouldUseNodeIdForXpdrTerminationPoints() {
        LinkTerminationPoints input = new LinkTerminationPoints(
                new TerminationPointId(
                        "XPDR-C1",
                        "XPDR-C1-XPDR1",
                        "XPDR1-NETWORK2",
                        OpenroadmTpType.XPONDERNETWORK),
                new TerminationPointId(
                        "XPDR-C1",
                        "XPDR-C1-XPDR1",
                        "XPDR1-CLIENT1",
                        OpenroadmTpType.XPONDERCLIENT));

        LinkEndpoints result = normalizer.normalize(input);

        assertEquals("XPDR-C1-XPDR1", result.srcNodeId());
        assertEquals("XPDR1-NETWORK2", result.srcTpId());
        assertEquals("XPDR-C1-XPDR1", result.dstNodeId());
        assertEquals("XPDR1-CLIENT1", result.dstTpId());
    }

    @Test
    void shouldUseSupportingNodeIdForNonXpdrTerminationPoints() {
        LinkTerminationPoints input = new LinkTerminationPoints(
                new TerminationPointId(
                        "supporting-node-a",
                        "node-a",
                        "tp-a",
                        OpenroadmTpType.DEGREERXCTP),
                new TerminationPointId(
                        "supporting-node-b",
                        "node-b",
                        "tp-b",
                        OpenroadmTpType.SRGTXRXPP));

        LinkEndpoints result = normalizer.normalize(input);

        assertEquals("supporting-node-a", result.srcNodeId());
        assertEquals("tp-a", result.srcTpId());
        assertEquals("supporting-node-b", result.dstNodeId());
        assertEquals("tp-b", result.dstTpId());
    }

    @Test
    void shouldNormalizeMixedTerminationPointTypes() {
        LinkTerminationPoints input = new LinkTerminationPoints(
                new TerminationPointId(
                        "SPDR-SA1",
                        "SPDR-SA1-XPDR2",
                        "XPDR2-NETWORK3",
                        OpenroadmTpType.XPONDERNETWORK),
                new TerminationPointId(
                        "ROADM-A1",
                        "ROADM-A1-SRG1",
                        "SRG1-PP4-TXRX",
                        OpenroadmTpType.SRGTXRXPP));

        LinkEndpoints result = normalizer.normalize(input);

        assertEquals("SPDR-SA1-XPDR2", result.srcNodeId());
        assertEquals("XPDR2-NETWORK3", result.srcTpId());
        assertEquals("ROADM-A1", result.dstNodeId());
        assertEquals("SRG1-PP4-TXRX", result.dstTpId());
    }
}
