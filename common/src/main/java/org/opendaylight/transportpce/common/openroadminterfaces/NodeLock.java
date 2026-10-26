/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A node's lock, and the number of threads holding or waiting for it.
 */
public final class NodeLock {
    final ReentrantLock lock = new ReentrantLock();
    // Threads holding or waiting for the lock; only changed inside the map's compute methods
    int users;
}
