/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.config;

import org.opendaylight.transportpce.common.time.Timeout;


public interface Config {

    /**
     * While reading from the device, TPCE waits this amount of time for the device to respond.
     *
     * @return the device read timeout
     */
    Timeout deviceReadTimeout();

    /**
     * While writing to the device, TPCE waits this amount of time for the transaction to complete.
     *
     * @return the device write timeout
     */
    Timeout deviceWriteTimeout();

}
