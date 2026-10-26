/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * One lock per node, so that operations on a node run one at a time while a node that does not answer does not hold
 * up operations on the other nodes.
 *
 * <p>Waiting for a lock can be interrupted, so a thread waiting behind a node that does not answer can still be
 * stopped, for example when its pool shuts down. A node's lock is removed once no thread holds it or waits for it,
 * so nodes that come and go do not leave locks behind.
 */
final class NodeLocks {

    /**
     * An operation run while holding a node's lock.
     */
    @FunctionalInterface
    interface NodeOperation {
        void run() throws OpenRoadmInterfaceException;
    }

    private final ConcurrentMap<String, NodeLock> locks = new ConcurrentHashMap<>();

    /**
     * Runs the operation while holding the node's lock.
     *
     * @param nodeId the node whose lock to hold
     * @param operation the operation to run
     * @throws OpenRoadmInterfaceException if the operation fails, or the thread is interrupted while waiting for the
     *     lock, in which case its interrupt status is kept
     */
    void runLocked(String nodeId, NodeOperation operation) throws OpenRoadmInterfaceException {
        NodeLock nodeLock = locks.compute(nodeId, (key, existing) -> {
            NodeLock lock = existing == null ? new NodeLock() : existing;
            lock.users++;
            return lock;
        });
        try {
            try {
                nodeLock.lock.lockInterruptibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new OpenRoadmInterfaceException("Interrupted while waiting for another operation on node "
                    + nodeId, e);
            }
            try {
                operation.run();
            } finally {
                nodeLock.lock.unlock();
            }
        } finally {
            locks.computeIfPresent(nodeId, (key, lock) -> --lock.users == 0 ? null : lock);
        }
    }

    /**
     * Returns the number of nodes that have a lock, for tests.
     */
    int size() {
        return locks.size();
    }
}
