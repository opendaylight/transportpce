/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.history;

import java.util.List;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.Transaction;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Goldfish implementation of the History interface.
 *
 * <p>This implementation simply doesn't track anything.
 * Most useful for backwards compatibility reasons.
 */
public class NonStickHistoryMemory implements History {

    private static final Logger LOG = LoggerFactory.getLogger(NonStickHistoryMemory.class);

    @Override
    public boolean add(Transaction transaction) {
        LOG.warn("Transaction history disabled. Ignoring '{}'.", transaction.description());
        return false;
    }

    @Override
    public boolean add(List<Transaction> transactions) {
        LOG.warn("Transaction history disabled. No rollback executed.");
        return false;
    }

    @Override
    public boolean addInterfaces(String nodeId, String interfaceId) {
        LOG.warn("Transaction history disabled.");
        return false;
    }

    @Override
    public boolean addInterfaces(String nodeId, String[] interfaceIds) {
        LOG.warn("Transaction history disabled.");
        return false;
    }

    @Override
    public boolean addInterfaces(String nodeId, List<String> interfaceIds) {
        LOG.warn("Transaction history disabled.");
        return false;
    }

    @Override
    public boolean rollback(Delete delete) {
        LOG.warn("Transaction history disabled. No rollback executed.");
        return false;
    }
}