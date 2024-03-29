/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackProcessor;

public class RollbackProcessorTest {

    @Test
    void rollbackIfNecessaryTest() throws Exception {
        int rolledBack = -1;
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        rollbackProcessor.addTask(new TestRollbackTask("task1", false));
        rollbackProcessor.addTask(new TestRollbackTask("task2", false));
        rolledBack = rollbackProcessor.rollbackAllIfNecessary();
        assertEquals(0, rolledBack);
        rollbackProcessor.addTask(new TestRollbackTask("task3", true));
        rollbackProcessor.addTask(new TestRollbackTask("task4", false));
        rolledBack = rollbackProcessor.rollbackAllIfNecessary();
        assertEquals(4, rolledBack);
        rolledBack = rollbackProcessor.rollbackAllIfNecessary();
        assertEquals(0, rolledBack);
    }

    @Test
    void rollbackAllTest() throws Exception {
        RollbackProcessor rollbackProcessor = new RollbackProcessor();
        rollbackProcessor.addTask(new TestRollbackTask("task1", false));
        rollbackProcessor.addTask(new TestRollbackTask("task2", false));
        rollbackProcessor.addTask(new TestRollbackTask("task3", false));
        rollbackProcessor.addTask(new TestRollbackTask("task4", false));
        int rolledBack = -1;
        rolledBack = rollbackProcessor.rollbackAll();
        assertEquals(4, rolledBack);
        rolledBack = rollbackProcessor.rollbackAll();
        assertEquals(0, rolledBack);
    }
}