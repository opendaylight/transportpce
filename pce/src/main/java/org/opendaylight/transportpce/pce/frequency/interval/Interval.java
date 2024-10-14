/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.math.BigDecimal;

public interface Interval {

    /**
     * The first frequency in the range.
     */
    BigDecimal start();

    /**
     * The last frequency in the range.
     */
    BigDecimal end();

}
