/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import java.util.Map;
import org.opendaylight.transportpce.common.StringConstants;

/**
 * Constant class common to fixed grid and flex grid.
 *
 */
public final class GridConstant {

    public static final String C_BAND = "cband";
    public static final int AVAILABLE_SLOT_VALUE = 255;
    public static final int USED_SLOT_VALUE = 0;
    public static final double GRANULARITY = 6.25;
    public static final int EFFECTIVE_BITS = 768;
    public static final double START_EDGE_FREQUENCY = 191.325;
    public static final int NB_OCTECTS = 96;
    public static final double CENTRAL_FREQUENCY = 193.1;
    public static final int NB_SLOTS_100G = 8;
    public static final int NB_SLOTS_400G = 14;
    public static final Map<String, Integer> SPECTRAL_WIDTH_SLOT_NUMBER_MAP = Map.of(
            StringConstants.SERVICE_TYPE_100GE, NB_SLOTS_100G,
            StringConstants.SERVICE_TYPE_400GE, NB_SLOTS_400G,
            StringConstants.SERVICE_TYPE_OTU4, NB_SLOTS_100G);

    private GridConstant() {
    }
}
