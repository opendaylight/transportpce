/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.spectrum.assignment;

/**
 * A range of indexes. Typically, in relation to a 768 bit grid.
 */
public interface Range {

    /**
     * The lower index (inclusive).
     */
    int lower();

    /**
     * The upper index (inclusive).
     */
    int upper();

}
