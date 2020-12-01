/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;

public class GridUtilsTest {

    @Test
    public void getWaveLengthIndexFromSpectrumAssigmentTest() {
        assertEquals("Wavelength index should be 15", 15, GridUtils.getWaveLengthIndexFromSpectrumAssigment(119));
    }

    @Test
    public void getFrequencyFromIndexTest() {
        BigDecimal[] expectedFrequencies = new BigDecimal[768];
        BigDecimal frequency = BigDecimal.valueOf(191.325);
        for (int i = 0; i < expectedFrequencies.length; i++) {
            expectedFrequencies[i] = frequency;
            frequency = frequency.add(BigDecimal.valueOf(0.00625));
        }
        assertEquals("Frequency should be 191.325", expectedFrequencies[0],
                GridUtils.getFrequencyFromIndex(0));
        assertEquals("Frequency should be 193.1", expectedFrequencies[284].setScale(1),
                GridUtils.getFrequencyFromIndex(284));
        assertEquals("Frequency should be 196.11875", expectedFrequencies[767],
                GridUtils.getFrequencyFromIndex(767));
    }

    @Test
    public void initFreqMaps4FixedGrid2AvailableTest() {
        AvailFreqMapsKey key = new AvailFreqMapsKey(GridConstant.C_BAND);
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMaps = GridUtils.initFreqMaps4FixedGrid2Available();
        assertEquals("Should contains 1 element", 1, availFreqMaps.size());
        assertTrue("should contains cband key", availFreqMaps.containsKey(key));
        assertTrue("Should have available freq map", Arrays.equals(byteArray, availFreqMaps.get(key).getFreqMap()));
    }

}
