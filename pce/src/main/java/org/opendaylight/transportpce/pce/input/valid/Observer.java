/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.input.valid;

/**
 * Any class wishing to observe validation processing should implement this interface.
 */
public interface Observer {

    /**
     * Send an error message to this observer.
     */
    void error(String errorMessage);

}
