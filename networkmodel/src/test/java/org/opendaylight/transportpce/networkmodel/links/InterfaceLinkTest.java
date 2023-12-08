/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class InterfaceLinkTest {

    @Test
    void testEquals() {
        Link conn1 = new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG2");
        Link conn2 = new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG2");

        Assertions.assertEquals(conn1, conn2);

        Set<Link> set1 = new HashSet<>();
        set1.add(conn1);

        Set<Link> set2 = new HashSet<>();
        set2.add(conn2);

        Assertions.assertEquals(set1, set2);
    }

    @Test
    void testNotEquals() {
        Link conn1 = new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG1");
        Link conn2 = new InterfaceLink("ROADM-B-SRG10", "ROADM-B-DEG2");

        Assertions.assertNotEquals(conn1, conn2);

        Set<Link> set1 = new HashSet<>();
        set1.add(conn1);

        Set<Link> set2 = new HashSet<>();
        set2.add(conn2);

        Assertions.assertNotEquals(set1, set2);
    }
}
