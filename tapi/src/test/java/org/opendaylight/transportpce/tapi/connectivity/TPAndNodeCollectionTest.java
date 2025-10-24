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

import java.util.Arrays;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.Test;

class TPAndNodeCollectionTest {

    @Test
    void testConstructorIgnoreNullAndEmptyString() {
        IDCollection idCollection = new TPAndNodeCollection(
                Arrays.asList("", null),
                Arrays.asList("", null),
                Arrays.asList("", null),
                Arrays.asList("", null),
                Arrays.asList("", null),
                Arrays.asList("", null)
        );

        // when & then: all lists should be empty
        assertTrue(idCollection.xpdrClientTplist().isEmpty(), "xpdrClientTplist should be empty");
        assertTrue(idCollection.xpdrNetworkTplist().isEmpty(), "xpdrNetworkTplist should be empty");
        assertTrue(idCollection.rdmAddDropTplist().isEmpty(), "rdmAddDropTplist should be empty");
        assertTrue(idCollection.rdmDegTplist().isEmpty(), "rdmDegTplist should be empty");
        assertTrue(idCollection.rdmNodelist().isEmpty(), "rdmNodelist should be empty");
        assertTrue(idCollection.xpdrNodelist().isEmpty(), "xpdrNodelist should be empty");
    }

    @Test
    void testAddMethodsIgnoreNullAndEmptyStrings() {
        TPAndNodeCollection collection = new TPAndNodeCollection();

        // when: try adding null and empty string to each list type
        Arrays.asList("", null).forEach(value -> {
            collection.addXpdrClientTp(value);
            collection.addXpdrNetworkTp(value);
            collection.addRdmAddDropTp(value);
            collection.addRdmDegTp(value);
            collection.addRdmNode(value);
            collection.addXpdrNode(value);
        });

        assertTrue(collection.xpdrClientTplist().isEmpty(), "xpdrClientTplist should be empty");
        assertTrue(collection.xpdrNetworkTplist().isEmpty(), "xpdrNetworkTplist should be empty");
        assertTrue(collection.rdmAddDropTplist().isEmpty(), "rdmAddDropTplist should be empty");
        assertTrue(collection.rdmDegTplist().isEmpty(), "rdmDegTplist should be empty");
        assertTrue(collection.rdmNodelist().isEmpty(), "rdmNodelist should be empty");
        assertTrue(collection.xpdrNodelist().isEmpty(), "xpdrNodelist should be empty");
    }

    @Test
    void addXpdrClientTp() {
        IDCollection idCollection = new TPAndNodeCollection();
        String tp = "SPDR-SA1-XPDR1+XPDR1-CLIENT1";

        assertTrue(idCollection.addXpdrClientTp(tp), "adding first time should return true");
        assertEquals(1, idCollection.xpdrClientTplist().size(), "size should be 1 after adding first TP");
        assertFalse(idCollection.addXpdrClientTp(tp), "adding duplicate should return false");
        assertEquals(1, idCollection.xpdrClientTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, idCollection.xpdrClientTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addXpdrNetworkTp() {
        IDCollection idCollection = new TPAndNodeCollection();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(idCollection.addXpdrNetworkTp(tp), "adding first time should return true");
        assertEquals(1, idCollection.xpdrNetworkTplist().size(), "size should be 1 after adding first TP");
        assertFalse(idCollection.addXpdrNetworkTp(tp), "adding duplicate should return false");
        assertEquals(1, idCollection.xpdrNetworkTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, idCollection.xpdrNetworkTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmAddDropTp() {
        IDCollection idCollection = new TPAndNodeCollection();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(idCollection.addRdmAddDropTp(tp), "adding first time should return true");
        assertEquals(1, idCollection.rdmAddDropTplist().size(), "size should be 1 after adding first TP");
        assertFalse(idCollection.addRdmAddDropTp(tp), "adding duplicate should return false");
        assertEquals(1, idCollection.rdmAddDropTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, idCollection.rdmAddDropTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmDegTp() {
        IDCollection idCollection = new TPAndNodeCollection();
        String tp = "SPDR-SA1-XPDR1+XPDR1-NETWORK1";

        assertTrue(idCollection.addRdmDegTp(tp), "adding first time should return true");
        assertEquals(1, idCollection.rdmDegTplist().size(), "size should be 1 after adding first TP");
        assertFalse(idCollection.addRdmDegTp(tp), "adding duplicate should return false");
        assertEquals(1, idCollection.rdmDegTplist().size(), "size should remain 1 after adding duplicate");
        assertEquals(tp, idCollection.rdmDegTplist().getFirst(), "the TP should match the one added");
    }

    @Test
    void addRdmNode() {
        IDCollection idCollection = new TPAndNodeCollection();
        String node = "ROADM-1";

        assertTrue(idCollection.addRdmNode(node), "adding first time should return true");
        assertEquals(1, idCollection.rdmNodelist().size(), "size should be 1 after adding first node");
        assertFalse(idCollection.addRdmNode(node), "adding duplicate should return false");
        assertEquals(1, idCollection.rdmNodelist().size(), "size should remain 1 after adding duplicate");
        assertEquals(node, idCollection.rdmNodelist().getFirst(), "the node should match the one added");

    }

    @Test
    void addXpdrNode() {
        IDCollection idCollection = new TPAndNodeCollection();
        String node = "XPDR1";

        assertTrue(idCollection.addXpdrNode(node), "adding first time should return true");
        assertEquals(1, idCollection.xpdrNodelist().size(), "size should be 1 after adding first node");
        assertFalse(idCollection.addXpdrNode(node), "adding duplicate should return false");
        assertEquals(1, idCollection.xpdrNodelist().size(), "size should remain 1 after adding duplicate");
        assertEquals(node, idCollection.xpdrNodelist().getFirst(), "the node should match the one added");
    }


    @Test
    void mutabilityXpdrNodeList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addXpdrNode("XPDR1");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.xpdrNodelist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrNode("XPDR2"),
                "addXpdrNode() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR2"),
                "xpdrNodelist() should return a mutable list");
    }

    @Test
    void mutabilityXpdrClientList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addXpdrClientTp("XPDR1-CLIENT1");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.xpdrClientTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrClientTp("XPDR1-CLIENT2"),
                "addXpdrClientTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR1-CLIENT2"),
                "xpdrClientTplist() should return a mutable list");
    }

    @Test
    void mutabilityXpdrNetworkList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addXpdrNetworkTp("XPDR1-NETWORK1");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.xpdrNetworkTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addXpdrNetworkTp("XPDR1-NETWORK2"),
                "addXpdrNetworkTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("XPDR1-NETWORK2"),
                "xpdrNetworkTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmAddDropList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addRdmAddDropTp("ROADM-A1-SRG1-PP1-TXRX");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.rdmAddDropTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmAddDropTp("ROADM-A1-SRG1-PP2-TXRX"),
                "addRdmAddDropTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-A1-SRG1-PP2-TXRX"),
                "rdmAddDropTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmDegreeList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addRdmDegTp("ROADM-A1-DEG1-TTP-TXRX");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.rdmDegTplist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmDegTp("ROADM-A1-SRG1-PP2-TXRX"),
                "addRdmDegTp() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-A1-DEG2-TTP-TXRX"),
                "rdmDegTplist() should return a mutable list");
    }

    @Test
    void mutabilityRdmNodeList() {
        IDCollection mutable = new TPAndNodeCollection();
        mutable.addRdmNode("ROADM-A1");

        IDCollection immutable = new TPAndNodeCollection(mutable);
        List<@NonNull String> list = immutable.rdmNodelist();

        assertThrows(UnsupportedOperationException.class,
                () -> immutable.addRdmNode("ROADM-C1"),
                "addRdmNode() should throw UnsupportedOperationException on immutable object");
        assertDoesNotThrow(() -> list.add("ROADM-C1"),
                "rdmNodelist() should return a mutable list");
    }
}
