/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

/**
 * Constant class related to fixed grid.
 * @author Orange
 *
 */
public final class FixedGridConstant {

    private FixedGridConstant() {
    }

    public static final double GRANULARITY = 6.25;
    public static final int EFFECTIVE_BITS = 8;
    public static final double START_EDGE_FREQUENCY = 191.325;
    public static final int NB_CHANNELS = 96;

}
