/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.frequency;

import java.math.BigDecimal;
import org.opendaylight.yangtools.yang.common.Uint64;

public interface Frequency extends Comparable<Frequency> {

    /**
     * Get the frequency in TeraHertz.
     *
     * @return the frequency in TeraHertz (e.g. 196.125)
     */
    BigDecimal teraHertz();

    /**
     * Get the frequency in Hertz.
     *
     * @return the frequency in Hertz (e.g. 196125000000000)
     */
    Uint64 hertz();

}
