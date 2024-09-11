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
     * The frequency in TeraHertz.
     *
     * @return e.g. 196.125(THz)
     */
    BigDecimal teraHertz();

    /**
     * The frequency in Hertz.
     *
     * @return e.g. 196125000000000(Hz)
     */
    Uint64 hertz();

}
