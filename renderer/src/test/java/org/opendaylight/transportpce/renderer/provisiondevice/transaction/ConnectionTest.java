/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;

class ConnectionTest {

    @Test
    void rollback() {
        Delete delete = Mockito.mock(Delete.class);
        Mockito.when(delete.deleteCrossConnect("ROADM-A", "DEG1", false))
                .thenReturn(List.of("Interface1"));

        Connection n1 = new Connection("ROADM-A", "DEG1", false);

        Assert.assertTrue(n1.rollback(delete));

        Mockito.verify(delete, Mockito.times(1))
                .deleteCrossConnect("ROADM-A", "DEG1", false);
    }

    @Test
    void testTwoObjectsWithSameInformationIsEqual() {
        Connection n1 = new Connection("ROADM-A", "DEG1", false);
        Connection n2 = new Connection("ROADM-A", "DEG1", false);

        Assert.assertTrue(n1.equals(n2));
    }

    @Test
    void testTwoObjectsWithDifferentInformationIsNotEqual() {
        Connection n1 = new Connection("ROADM-A", "DEG1", true);
        Connection n2 = new Connection("ROADM-A", "DEG1", false);

        Assert.assertFalse(n1.equals(n2));
    }

    @Test
    void testTwoDifferentRoadmNodesAreNotEqual() {
        Connection n1 = new Connection("ROADM-A", "DEG1", false);
        Connection n2 = new Connection("ROADM-B", "DEG1", false);

        Assert.assertFalse(n1.equals(n2));
    }


    @Test
    void deleteReturnNull() {
        Delete delete = Mockito.mock(Delete.class);
        Mockito.when(delete.deleteCrossConnect("ROADM-A", "DEG1", false))
                .thenReturn(null);

        Connection n1 = new Connection("ROADM-A", "DEG1", false);

        Assert.assertFalse(n1.rollback(delete));

        Mockito.verify(delete, Mockito.times(1))
                .deleteCrossConnect("ROADM-A", "DEG1", false);
    }

    @Test
    void deleteReturnEmptyList() {
        Delete delete = Mockito.mock(Delete.class);
        Mockito.when(delete.deleteCrossConnect("ROADM-A", "DEG1", false))
                .thenReturn(new ArrayList<>());

        Connection n1 = new Connection("ROADM-A", "DEG1", false);

        Assert.assertFalse(n1.rollback(delete));

        Mockito.verify(delete, Mockito.times(1))
                .deleteCrossConnect("ROADM-A", "DEG1", false);
    }
}