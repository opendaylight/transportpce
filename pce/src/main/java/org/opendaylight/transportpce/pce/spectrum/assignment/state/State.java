/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment.state;

import org.opendaylight.transportpce.pce.spectrum.assignment.Assign;

public interface State {

    /**
     * Return an instance of Assign based of a configurable value.
     */
    Assign configuredState(String configuration);

}
