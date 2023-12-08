/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

/**
 * Answers whether a given source/destination pair is a viable link, according
 * to some notion of a device's internal connection map. Implementations may
 * consult an actual device connection map, or assume all pairs are viable
 * (see {@link org.opendaylight.transportpce.networkmodel.links.strategy}).
 */
public interface ConnectionMap {

    /**
     * Checks whether the given source/destination pair is present in the connection map.
     *
     * @param source e.g. "ROADM-B-DEG1"
     * @param destination e.g. "ROADM-B-SRG3"
     * @return true if source/destination is present in the connection map
     */
    boolean contains(String source, String destination);

    /**
     * Returns the number of entries in the connection map.
     *
     * @return the size of the connection map
     */
    int size();

}
