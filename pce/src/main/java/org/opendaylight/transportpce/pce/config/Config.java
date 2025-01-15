/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.config;

public interface Config {

    /**
     * Whether PCE should search for an available frequency range
     * starting at high frequencies going down or vice versa.
     */
    String availableSpectrumIterationDirectionStrategy();

}
