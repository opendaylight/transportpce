/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Map;
import org.opendaylight.transportpce.common.ServiceRateConstant;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yangtools.yang.common.Uint32;

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
    /**
     * Map for associate service type with nb slots.
     */
    public static final Map<String, Integer> SPECTRAL_WIDTH_SLOT_NUMBER_MAP = Map.of(
            StringConstants.SERVICE_TYPE_100GE, NB_SLOTS_100G,
            StringConstants.SERVICE_TYPE_400GE, NB_SLOTS_400G,
            StringConstants.SERVICE_TYPE_OTU4, NB_SLOTS_100G);
    /**
     * Map to associate service rate to modulation format.
     */
    public static final Map<Uint32, ModulationFormat> RATE_MODULATION_FORMAT_MAP = Map.of(
            ServiceRateConstant.RATE_100, ModulationFormat.DpQpsk,
            ServiceRateConstant.RATE_200, ModulationFormat.DpQpsk,
            ServiceRateConstant.RATE_300, ModulationFormat.DpQam8,
            ServiceRateConstant.RATE_400, ModulationFormat.DpQam16);
    /**
     * Map to associate service rate and modulation format to frequency width.
     */
    public static final Table<Uint32, ModulationFormat, String> FREQUENCY_WIDTH_TABLE = initFrequencyWidthTable();

    public static final long IRRELEVANT_WAVELENGTH_NUMBER = 0;

    private GridConstant() {
    }

    private static Table<Uint32, ModulationFormat, String> initFrequencyWidthTable() {
        Table<Uint32, ModulationFormat, String> frequencyWidthTable = HashBasedTable.create();
        frequencyWidthTable.put(ServiceRateConstant.RATE_100, ModulationFormat.DpQpsk, "40");
        frequencyWidthTable.put(ServiceRateConstant.RATE_200, ModulationFormat.DpQpsk, "80");
        frequencyWidthTable.put(ServiceRateConstant.RATE_300, ModulationFormat.DpQam8, "80");
        frequencyWidthTable.put(ServiceRateConstant.RATE_400, ModulationFormat.DpQam16, "80");
        return frequencyWidthTable;
    }
}
