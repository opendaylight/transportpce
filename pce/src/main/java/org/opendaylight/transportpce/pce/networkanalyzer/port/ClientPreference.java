/*
 * Copyright (c) 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer.port;

import java.util.Map;
import java.util.Set;

public class ClientPreference implements Preference {

    Map<String, Set<String>> nodePortPreference;

    public ClientPreference(Map<String, Set<String>> nodePortPreference) {
        this.nodePortPreference = nodePortPreference;
    }

    @Override
    public boolean isPreferredPort(String node, String portName) {

        //If there is no preferred port registered for the node, it means
        //the client has no preference regarding the node.
        //Therefore, we'll treat the node as it was preferred to
        //prevent it from NOT being used.
        return !nodePortPreference.containsKey(node) || nodePortPreference.get(node).contains(portName);

    }

}
