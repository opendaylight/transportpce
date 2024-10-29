/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.util.Map;

public interface Numeric {

    /**
     * Return a map of ASSIGNED (i.e. used) frequency ranges.
     *
     * <p>The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> assignedFrequency(Available frequency);

    /**
     * Return a map of AVAILABLE frequency ranges.
     *
     * <p>The key is the lower frequency bound and the value is the upper frequency bound.
     */
    Map<Double, Double> availableFrequency(Available frequency);

}
