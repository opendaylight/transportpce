/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.List;
import org.jspecify.annotations.NonNull;

public interface Connectivity {

    /**
     * Add xponder client termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable
     */
    boolean addXpdrClientTp(@NonNull String tp);

    /**
     * Get xponder client termination point list.
     */
    List<@NonNull String> xpdrClientTplist();

    /**
     * Add xponder network termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addXpdrNetworkTp(@NonNull String tp);

    /**
     * Get xponder network termination point list.
     */
    List<@NonNull String> xpdrNetworkTplist();

    /**
     * Add ROADM PP termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmAddDropTp(@NonNull String tp);

    List<@NonNull String> rdmAddDropTplist();

    /**
     * Add ROADM TTP termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmDegTp(@NonNull String tp);

    /**
     * Get ROADM TTP termination point list.
     */
    List<@NonNull String> rdmDegTplist();

    /**
     * Add ROADM node.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmNode(@NonNull String tp);

    /**
     * Get ROADM node list.
     */
    List<@NonNull String> rdmNodelist();

    /**
     * Add xponder node.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addXpdrNode(@NonNull String tp);

    /**
     * Get xponder node list.
     */
    List<@NonNull String> xpdrNodelist();

    /**
     * Log connectivity information contained in this object.
     */
    void log();
}
