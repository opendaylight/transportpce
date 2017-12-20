/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice.tasks;

import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class collects tasks for later rollback.
 * This implementation is not thread safe, it must be called from single orchestration thread.
 * Rollback order is: last added task is rolled back first.
 * After rollback, each task is removed from rollback processor.
 * All rollback tasks are executed in single thread.
 */
public class RollbackProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(RollbackProcessor.class);

    private final Deque<RollbackTask> tasks;

    public RollbackProcessor() {
        this.tasks = new LinkedList<>();
    }

    /**
     * Add task to the rollback processor.
     * @param task the task to add
     */
    public void addTask(RollbackTask task) {
        this.tasks.add(task);
    }

    /**
     * Check if any previously added task requires rollback.
     * Rollback is necessary if just single task requires rollback.
     * @return
     *   true if any of added tasks requires rollback. false if none of added tasks requires rollback.
     */
    public boolean isRollbackNecessary() {
        for (RollbackTask task: this.tasks) {
            if (task.isRollbackNecessary()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rollback all tasks previously added to this processor.
     * It does not matter if any of the tasks requires rollback.
     * All previously added tasks will be rolled back and removed from this processor.
     * @return
     *   number of tasks rolled back
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    public int rollbackAll() {
        int rollbackCounter = 0;
        while (this.tasks.size() > 0) {
            RollbackTask task = this.tasks.pollLast();
            rollbackCounter++;
            try {
                LOG.info("rolling back: {}", task.getId());
                task.call();
            //this method prototype only uses the generic Exception but no specific and useable subclass
            } catch (Exception e) {
                LOG.error("ERROR: Rollback task {} has failed", task.getId(), e);
            }
        }
        return rollbackCounter;
    }

    /**
     * Rollback all tasks in case any task has failed.
     * If rollback is necessary, all previously added tasks will be rolled back and removed from this processor.
     * @return
     *   number of tasks rolled back
     */
    public int rollbackAllIfNecessary() {
        if (!isRollbackNecessary()) {
            return 0;
        }
        return rollbackAll();
    }

}
