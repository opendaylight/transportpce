/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

public interface IDCollection {

    /**
     * Add xponder client termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addXpdrClientTp(@NonEmpty String tp);

    /**
     * Get xponder client termination point list.
     */
    List<@NonEmpty String> xpdrClientTplist();

    /**
     * Add xponder network termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addXpdrNetworkTp(@NonEmpty String tp);

    /**
     * Get xponder network termination point list.
     */
    List<@NonEmpty String> xpdrNetworkTplist();

    /**
     * Add ROADM PP termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmAddDropTp(@NonEmpty String tp);

    /**
     * Get ROADM PP termination point list.
     */
    List<@NonEmpty String> rdmAddDropTplist();

    /**
     * Add ROADM TTP termination point.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmDegTp(@NonEmpty String tp);

    /**
     * Get ROADM TTP termination point list.
     */
    List<@NonEmpty String> rdmDegTplist();

    /**
     * Add ROADM node.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addRdmNode(@NonEmpty String tp);

    /**
     * Get ROADM node list.
     */
    List<@NonEmpty String> rdmNodelist();

    /**
     * Add xponder node.
     * @return true if and only if the TP was added.
     * @throws UnsupportedOperationException if object is immutable. The implementing class may choose to
     *                                       return false instead of throwing this exception.
     */
    boolean addXpdrNode(@NonEmpty String tp);

    /**
     * Get xponder node list.
     */
    List<@NonEmpty String> xpdrNodelist();

    /**
     * Log connectivity information contained in this object.
     */
    void log();
}
