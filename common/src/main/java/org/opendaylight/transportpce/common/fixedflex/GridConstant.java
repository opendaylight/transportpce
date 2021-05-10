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
import java.math.BigDecimal;
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
    public static final BigDecimal WIDTH_80 = BigDecimal.valueOf(80);
    public static final BigDecimal WIDTH_75 = BigDecimal.valueOf(75);
    public static final BigDecimal WIDTH_40 = BigDecimal.valueOf(40);
    public static final BigDecimal SLOT_WIDTH_50 = BigDecimal.valueOf(50);
    public static final BigDecimal SLOT_WIDTH_87_5 = BigDecimal.valueOf(87.5);

    /**
     * Map for associate service type with nb slots.
     */
    public static final Map<String, Integer> SPECTRAL_WIDTH_SLOT_NUMBER_MAP = Map.of(
            StringConstants.SERVICE_TYPE_100GE_T, NB_SLOTS_100G,
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

    /**
     * Map to associate width format to slot width.
     */
    public static final Map<BigDecimal, BigDecimal> WIDTH_SLOT_WIDTH_MAP = Map.of(
            WIDTH_40, SLOT_WIDTH_50,
            WIDTH_75, SLOT_WIDTH_87_5,
            WIDTH_80, SLOT_WIDTH_87_5);

    public static final int FIXED_GRID_FREQUENCY_PRECISION = 4;

    public static final int FLEX_GRID_FREQUENCY_PRECISION = 5;

    /**
     * Map to associate service rate and modulation format to frequency slot width.
     */
    public static final Table<Uint32, ModulationFormat, BigDecimal> FREQUENCY_SLOT_WIDTH_TABLE =
            initFrequencySlotWidthTable();
    public static final String SPECTRAL_SLOT_SEPARATOR = ":";
    public static final String NAME_PARAMETERS_SEPARATOR = "-";

    /**
     * Map for associate service rate with nb slots.
     */
    public static final Map<Uint32, Integer> RATE_SPECTRAL_WIDTH_SLOT_NUMBER_MAP = Map.of(
            ServiceRateConstant.RATE_100, NB_SLOTS_100G,
            ServiceRateConstant.RATE_200, NB_SLOTS_100G,
            ServiceRateConstant.RATE_300, NB_SLOTS_100G,
            ServiceRateConstant.RATE_400, NB_SLOTS_400G);

    private GridConstant() {
    }

    private static Table<Uint32, ModulationFormat, String> initFrequencyWidthTable() {
        Table<Uint32, ModulationFormat, String> frequencyWidthTable = HashBasedTable.create();
        frequencyWidthTable.put(ServiceRateConstant.RATE_100, ModulationFormat.DpQpsk, String.valueOf(WIDTH_40));
        frequencyWidthTable.put(ServiceRateConstant.RATE_200, ModulationFormat.DpQpsk, String.valueOf(WIDTH_80));
        frequencyWidthTable.put(ServiceRateConstant.RATE_300, ModulationFormat.DpQam8, String.valueOf(WIDTH_80));
        frequencyWidthTable.put(ServiceRateConstant.RATE_400, ModulationFormat.DpQam16, String.valueOf(WIDTH_75));
        return frequencyWidthTable;
    }

    private static Table<Uint32, ModulationFormat, BigDecimal> initFrequencySlotWidthTable() {
        Table<Uint32, ModulationFormat, BigDecimal> frequencyWidthTable = HashBasedTable.create();
        frequencyWidthTable.put(ServiceRateConstant.RATE_100, ModulationFormat.DpQpsk, SLOT_WIDTH_50);
        frequencyWidthTable.put(ServiceRateConstant.RATE_200, ModulationFormat.DpQpsk, SLOT_WIDTH_87_5);
        frequencyWidthTable.put(ServiceRateConstant.RATE_300, ModulationFormat.DpQam8, SLOT_WIDTH_87_5);
        frequencyWidthTable.put(ServiceRateConstant.RATE_400, ModulationFormat.DpQam16, SLOT_WIDTH_87_5);
        return frequencyWidthTable;
    }
}
