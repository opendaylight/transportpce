/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction;

import org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete.Delete;

/**
 * Any class wishing to keep track of transactions
 * may implement this interface.
 */
public interface Transaction {

    /**
     * Rollback this transaction.
     */
    boolean rollback(Delete delete);

    String description();

    int hashCode();

    boolean equals(Object object);
}