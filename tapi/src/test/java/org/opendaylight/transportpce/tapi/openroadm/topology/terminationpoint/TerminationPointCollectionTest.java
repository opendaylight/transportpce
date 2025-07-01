/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.TpId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPointBuilder;

class TerminationPointCollectionTest {

    @Test
    void add() {
        NodeTypeCollection terminationPointCollection = new TerminationPointCollection();

        terminationPointCollection.add(
                OpenroadmNodeType.DEGREE,
                "ROADM-A",
                Set.of(
                        new TerminationPointBuilder()
                                .setTpId(TpId.getDefaultInstance("TP-1"))
                                .build()
                )
        );

        terminationPointCollection.add(
                OpenroadmNodeType.DEGREE,
                "ROADM-A",
                Set.of(
                        new TerminationPointBuilder()
                                .setTpId(TpId.getDefaultInstance("TP-2"))
                                .build()
                )
        );

        assertEquals(2,
                terminationPointCollection.terminationPoints().get(OpenroadmNodeType.DEGREE).get("ROADM-A").size());
    }

}
