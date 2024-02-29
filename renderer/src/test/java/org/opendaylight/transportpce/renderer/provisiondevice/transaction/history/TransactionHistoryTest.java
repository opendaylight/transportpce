/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.history;

import java.util.List;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.DeviceInterface;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.Transaction;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;

class TransactionHistoryTest {

    @Test
    void add() {
        Transaction transaction = Mockito.mock(Transaction.class);
        History history = new TransactionHistory();

        Assert.assertTrue(history.add(transaction));
    }

    @Test
    void testDuplicateTransactionIsIgnored() {

        Transaction t1 = new DeviceInterface("ROADM-A", "DEG1");
        Transaction t2 = new DeviceInterface("ROADM-A", "DEG1");

        History history = new TransactionHistory();

        history.add(t1);
        Assert.assertFalse(history.add(t2));
    }

    @Test
    void testAddCollectionOfUniqueTransactions() {
        Transaction t1 = new DeviceInterface("ROADM-A", "DEG1");
        Transaction t2 = new DeviceInterface("ROADM-A", "DEG2");

        List<Transaction> transactions = List.of(t1, t2);

        History history = new TransactionHistory();

        Assert.assertTrue(history.add(transactions));
    }

    @Test
    void testAddCollectionOfDuplicateTransactions() {
        Transaction t1 = new DeviceInterface("ROADM-A", "DEG1");
        Transaction t2 = new DeviceInterface("ROADM-A", "DEG1");

        List<Transaction> transactions = List.of(t1, t2);

        History history = new TransactionHistory();

        Assert.assertFalse(history.add(transactions));
    }

    @Test
    void testAddUniqueStringOfInterfaceIds() {
        String nodeId = "ROADM-A";
        String[] interfaces = new String[]{"DEG1", "DEG2"};

        History history = new TransactionHistory();

        Assert.assertTrue(history.addInterfaces(nodeId, interfaces));
    }

    @Test
    void testAddDuplicateStringOfInterfaceIds() {
        String nodeId = "ROADM-A";
        String[] interfaces = new String[]{"DEG1", "DEG1"};

        History history = new TransactionHistory();

        Assert.assertTrue(history.addInterfaces(nodeId, interfaces));

    }

    @Test
    void testAddDuplicateListOfInterfaceIds() {
        String nodeId = "ROADM-A";
        List<String> interfaces = List.of("DEG1", "DEG1");

        History history = new TransactionHistory();

        Assert.assertTrue(history.addInterfaces(nodeId, interfaces));

    }

    @Test
    void rollbackOneInterface() {

        String nodeId = "ROADM-A";
        List<String> interfaces = List.of("DEG1", "DEG1");

        History history = new TransactionHistory();
        history.addInterfaces(nodeId, interfaces);

        Delete delete = Mockito.mock(Delete.class);
        Mockito.when(delete.deleteInterface("ROADM-A", "DEG1")).thenReturn(true);

        Assert.assertTrue(history.rollback(delete));

        //Although the same interface was added twice, we only rollback once.
        Mockito.verify(delete, Mockito.times(1))
                .deleteInterface("ROADM-A", "DEG1");
    }

    @Test
    void rollbackTwoInterfacesInReverseOrderTheyWereAdded() {

        String nodeId = "ROADM-A";

        //Note DEG1 is added before DEG2
        List<String> interfaces = List.of("DEG1", "DEG2");

        History history = new TransactionHistory();
        history.addInterfaces(nodeId, interfaces);

        Delete delete = Mockito.mock(Delete.class);
        Mockito.when(delete.deleteInterface("ROADM-A", "DEG1")).thenReturn(true);
        Mockito.when(delete.deleteInterface("ROADM-A", "DEG2")).thenReturn(true);

        Assert.assertTrue(history.rollback(delete));

        //The rollback occurs in the reverse order.
        // i.e. DEG2 before DEG1.
        InOrder inOrder = Mockito.inOrder(delete);
        inOrder.verify(delete, Mockito.times(1))
                .deleteInterface("ROADM-A", "DEG2");
        inOrder.verify(delete, Mockito.times(1))
                .deleteInterface("ROADM-A", "DEG1");

    }
}