/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;


public interface Factory {

    /**
     * Creates an instance of Assigned, treating
     * the byte array as an available frequency range.
     */
    Assigned fromAvailable(byte[] frequencyRange);

    /**
     * Creates an instance of Assigned, treating
     * the byte array as an assigned (used) frequency range.
     */
    Assigned fromAssigned(byte[] frequencyRange);

}
