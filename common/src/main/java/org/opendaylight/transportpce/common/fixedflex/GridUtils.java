/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yangtools.yang.common.Uint16;

/**
 * Util class for grid.
 *
 */
public final class GridUtils {

    private GridUtils() {
    }

    public static Map<AvailFreqMapsKey, AvailFreqMaps> initFreqMaps4FixedGrid2Available() {
        byte[] byteArray = new byte[FixedGridConstant.NB_CHANNELS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        Map<AvailFreqMapsKey, AvailFreqMaps> waveMap = new HashMap<>();
        AvailFreqMaps availFreqMaps = new AvailFreqMapsBuilder().setMapName(GridConstant.C_BAND)
                .setFreqMapGranularity(new FrequencyGHz(BigDecimal.valueOf(FixedGridConstant.GRANULARITY)))
                .setStartEdgeFreq(new FrequencyTHz(BigDecimal.valueOf(FixedGridConstant.START_EDGE_FREQUENCY)))
                .setEffectiveBits(Uint16.valueOf(FixedGridConstant.EFFECTIVE_BITS))
                .setFreqMap(byteArray)
                .build();
        waveMap.put(availFreqMaps.key(), availFreqMaps);
        return waveMap;
    }

}
