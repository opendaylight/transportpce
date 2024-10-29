/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.history;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.DeviceInterface;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.Transaction;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class keeping track of transaction history.
 *
 * <p>A transaction can be something like an interface or a roadm connection, that may need to be
 * rolled back in the future.
 */
public class TransactionHistory implements History {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionHistory.class);
    Set<Transaction> transactionHistory = Collections.synchronizedSet(new LinkedHashSet<>());

    @Override
    public boolean add(Transaction transaction) {

        boolean result = transactionHistory.add(transaction);

        if (result) {
            LOG.info("Adding {}", transaction.description());
        } else {
            LOG.warn("Transaction {} not added.", transaction.description());
        }

        return result;
    }

    @Override
    public boolean add(List<Transaction> transactions) {
        Set<Boolean> results = new HashSet<>(transactions.size());

        for (Transaction transaction : transactions) {
            results.add(add(transaction));
        }

        return results.stream().allMatch(i -> (i.equals(Boolean.TRUE)));
    }

    @Override
    public boolean addInterfaces(String nodeId, String interfaceId) {
        return addInterfaces(nodeId, Collections.singletonList(interfaceId));
    }

    @Override
    public boolean addInterfaces(String nodeId, String[] interfaceIds) {

        return addInterfaces(nodeId, Arrays.asList(interfaceIds));

    }

    @Override
    public boolean addInterfaces(String nodeId, List<String> interfaceIds) {

        Set<Boolean> results = new HashSet<>();
        Set<String> unique = new LinkedHashSet<>();

        for (String interfaceId : interfaceIds) {
            if (interfaceId != null && !interfaceId.trim().isEmpty()) {
                unique.add(interfaceId.trim());
            }
        }

        for (String interfaceId : unique) {
            results.add(this.add(new DeviceInterface(nodeId, interfaceId)));
        }

        return results.stream().allMatch(i -> (i.equals(Boolean.TRUE)));

    }

    @Override
    public boolean rollback(Delete delete) {

        LOG.info("History contains {} items. Rolling them back in reverse order.",
                transactionHistory.size());

        List<Transaction> reverse = new ArrayList<>(transactionHistory);

        Collections.reverse(reverse);

        boolean success = true;

        for (Transaction transaction : reverse) {
            LOG.info("Rolling back {}", transaction.description());
            if (!transaction.rollback(delete)) {
                success = false;
            }
        }

        return success;

    }
}