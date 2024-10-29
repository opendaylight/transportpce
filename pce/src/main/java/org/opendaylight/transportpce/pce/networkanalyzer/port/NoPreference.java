/*
 * Copyright (c) 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

/**
 * This class represents a state where the client has no port preference.
 * In essence, all ports on all nodes will therefore be treated as 'preferred'
 * when queried.
 *
 * <p>Usage of this class is of sorts the 'backwards compatible' approach. Meaning,
 * intended to offer a path for the application to behave as it did
 * prior to implementing client port preference.
 */
public class NoPreference implements Preference {
    @Override
    public boolean isPreferredPort(String node, String portName) {
        return true;
    }
}
