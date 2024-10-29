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

public interface History {

    /**
     * Add transaction.
     *
     * <p>Only accepts the transaction if this History
     * object doesn't already contain the object.
     *
     * @return true if the transaction was added.
     */
    boolean add(Transaction transaction);

    /**
     * A list of transactions.
     *
     * <p>Will only accept unique transactions.
     * @return true if all transactions was added. false if one or more transactions was rejected.
     */
    boolean add(List<Transaction> transactions);

    /**
     * Add an array of interface transactions.
     *
     * <p>Duplicate interface ids, null or empty strings
     * are silently ignored.
     * @return may return false
     */
    boolean addInterfaces(String nodeId, String interfaceId);

    /**
     * Add an array of interface transactions.
     *
     * <p>Duplicate interface ids, null or empty strings
     * are silently ignored.
     * @return may return false
     */
    boolean addInterfaces(String nodeId, String[] interfaceIds);

    /**
     * Add a list of interface transactions.
     *
     * <p>Duplicate interface ids, null or empty strings
     * are silently ignored.
     */
    boolean addInterfaces(String nodeId, List<String> interfaceIds);

    /**
     * Rollback all transactions.
     *
     * <p>Typically, the transactions are rolled back in reverse
     * order, but the implementing class may choose a different
     * logic.
     */
    boolean rollback(Delete delete);

}