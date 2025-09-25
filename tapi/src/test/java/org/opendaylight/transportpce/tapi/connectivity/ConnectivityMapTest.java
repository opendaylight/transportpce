/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;

class ConnectivityMapTest {

    @Test
    void addXpdrClientTp() {
        Connectivity connectivityMap = new ConnectivityMap();
        String tp = "SPDR-SA1-XPDR1+XPDR1-CLIENT1";

        assertTrue(connectivityMap.addXpdrClientTp(tp), "adding first time should return true");
        assertEquals(1, connectivityMap.xpdrClientTplist().size(), "size should be 1 after adding first TP");
        assertFalse(connectivityMap.addXpdrClientTp(tp), "adding duplicate should return false");
        assertEquals(1, connectivityMap.xpdrClientTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, connectivityMap.xpdrClientTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addXpdrNetworkTp() {
        Connectivity connectivityMap = new ConnectivityMap();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(connectivityMap.addXpdrNetworkTp(tp), "adding first time should return true");
        assertEquals(1, connectivityMap.xpdrNetworkTplist().size(), "size should be 1 after adding first TP");
        assertFalse(connectivityMap.addXpdrNetworkTp(tp), "adding duplicate should return false");
        assertEquals(1, connectivityMap.xpdrNetworkTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, connectivityMap.xpdrNetworkTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmAddDropTp() {
        Connectivity connectivityMap = new ConnectivityMap();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(connectivityMap.addRdmAddDropTp(tp), "adding first time should return true");
        assertEquals(1, connectivityMap.rdmAddDropTplist().size(), "size should be 1 after adding first TP");
        assertFalse(connectivityMap.addRdmAddDropTp(tp), "adding duplicate should return false");
        assertEquals(1, connectivityMap.rdmAddDropTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, connectivityMap.rdmAddDropTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmDegTp() {
        Connectivity connectivityMap = new ConnectivityMap();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(connectivityMap.addRdmDegTp(tp), "adding first time should return true");
        assertEquals(1, connectivityMap.rdmDegTplist().size(), "size should be 1 after adding first TP");
        assertFalse(connectivityMap.addRdmDegTp(tp), "adding duplicate should return false");
        assertEquals(1, connectivityMap.rdmDegTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, connectivityMap.rdmDegTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmNode() {
        Connectivity connectivityMap = new ConnectivityMap();
        String node = "ROADM-1";

        assertTrue(connectivityMap.addRdmNode(node), "adding first time should return true");
        assertEquals(1, connectivityMap.rdmNodelist().size(), "size should be 1 after adding first node");
        assertFalse(connectivityMap.addRdmNode(node), "adding duplicate should return false");
        assertEquals(1, connectivityMap.rdmNodelist().size(), "size should remain 1 after adding duplicate");
        assertEquals(node, connectivityMap.rdmNodelist().getFirst(), "the node should match the one added");

    }

    @Test
    void addXpdrNode() {
        Connectivity connectivityMap = new ConnectivityMap();
        String node = "XPDR1";

        assertTrue(connectivityMap.addXpdrNode(node), "adding first time should return true");
        assertEquals(1, connectivityMap.xpdrNodelist().size(), "size should be 1 after adding first node");
        assertFalse(connectivityMap.addXpdrNode(node), "adding duplicate should return false");
        assertEquals(1, connectivityMap.xpdrNodelist().size(), "size should remain 1 after adding duplicate");
        assertEquals(node, connectivityMap.xpdrNodelist().getFirst(), "the node should match the one added");
    }


    @Test
    void mutabilityXpdrNodeList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addXpdrNode("XPDR1");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.xpdrNodelist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrNode("XPDR2"),
                "addXpdrNode() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR2"),
                "xpdrNodelist() should return a mutable list");
    }

    @Test
    void mutabilityXpdrClientList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addXpdrClientTp("XPDR1-CLIENT1");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.xpdrClientTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrClientTp("XPDR1-CLIENT2"),
                "addXpdrClientTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR1-CLIENT2"),
                "xpdrClientTplist() should return a mutable list");
    }

    @Test
    void mutabilityXpdrNetworkList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addXpdrNetworkTp("XPDR1-NETWORK1");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.xpdrNetworkTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrNetworkTp("XPDR1-NETWORK2"),
                "addXpdrNetworkTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR1-NETWORK2"),
                "xpdrNetworkTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmAddDropList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addRdmAddDropTp("ROADM-A1-SRG1-PP1-TXRX");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.rdmAddDropTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmAddDropTp("ROADM-A1-SRG1-PP2-TXRX"),
                "addRdmAddDropTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-A1-SRG1-PP2-TXRX"),
                "rdmAddDropTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmDegreeList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addRdmDegTp("ROADM-A1-DEG1-TTP-TXRX");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.rdmDegTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmDegTp("ROADM-A1-SRG1-PP2-TXRX"),
                "addRdmDegTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-A1-DEG2-TTP-TXRX"),
                "rdmDegTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmNodeList() {
        Connectivity mutable = new ConnectivityMap();
        mutable.addRdmNode("ROADM-A1");

        Connectivity immutable = new ConnectivityMap(mutable);
        List<@NonNull String> list = immutable.rdmNodelist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmNode("ROADM-C1"),
                "addRdmNode() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-C1"),
                "rdmNodelist() should return a mutable list");
    }
}
