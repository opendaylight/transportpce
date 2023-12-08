/*
 * Copyright © 2023  AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

public interface ConnectionMap {

    /**
     * true if source/destination is present in the connection map.
     */
    boolean contains(String source, String destination);

    /**
     * Size of the connection map.
     */
    int size();

}
