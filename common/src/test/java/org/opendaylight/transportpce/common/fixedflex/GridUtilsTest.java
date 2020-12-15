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
import org.opendaylight.transportpce.common.ServiceRateConstant;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.available.freq.map.AvailFreqMapsKey;

public class GridUtilsTest {

    @Test
    public void getWaveLengthIndexFromSpectrumAssigmentTest() {
        assertEquals("Wavelength index should be 15", 15, GridUtils.getWaveLengthIndexFromSpectrumAssigment(647));
    }

    @Test
    public void getFrequencyFromIndexTest() {
        BigDecimal[] expectedFrequencies = new BigDecimal[768];
        BigDecimal frequency = BigDecimal.valueOf(191.325);
        for (int i = 0; i < expectedFrequencies.length; i++) {
            expectedFrequencies[i] = frequency;
            frequency = frequency.add(BigDecimal.valueOf(0.00625));
        }
        assertEquals("Frequency should be 191.325", 0, expectedFrequencies[0]
                .compareTo(GridUtils.getStartFrequencyFromIndex(0)));
        assertEquals("Frequency should be 193.1", 0, expectedFrequencies[284]
                .compareTo(GridUtils.getStartFrequencyFromIndex(284)));
        assertEquals("Frequency should be 196.1188", 0, expectedFrequencies[767]
                .compareTo(GridUtils.getStartFrequencyFromIndex(767)));
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

    @Test
    public void getIndexFromFrequencyTest() {
        assertEquals("Index should be 693", 693, GridUtils.getIndexFromFrequency(BigDecimal.valueOf(195.65625)));
        assertEquals("Index should be 0", 0, GridUtils.getIndexFromFrequency(BigDecimal.valueOf(191.325)));
        assertEquals("Index should be 767", 767, GridUtils.getIndexFromFrequency(BigDecimal.valueOf(196.11875)));
        assertEquals("Index should be 8", 8, GridUtils.getIndexFromFrequency(BigDecimal.valueOf(191.375)));
        assertEquals("Index should be 15", 15, GridUtils.getIndexFromFrequency(BigDecimal.valueOf(191.41875)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIndexFromFrequencyExceptionTest() {
        GridUtils.getIndexFromFrequency(BigDecimal.valueOf(196.13125));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getIndexFromFrequencyException2Test() {
        GridUtils.getIndexFromFrequency(BigDecimal.valueOf(191.31875));
    }

    @Test
    public void getWidthFromRateAndModulationFormatTest() {
        assertEquals("Width should be 80", FrequencyGHz.getDefaultInstance("80"),
                GridUtils.getWidthFromRateAndModulationFormat(ServiceRateConstant.RATE_400,
                        ModulationFormat.DpQam16));
    }

    @Test
    public void getWidthFromRateAndModulationFormatNotFoundTest() {
        assertEquals("As not found width should be 40", FrequencyGHz.getDefaultInstance("40"),
                GridUtils.getWidthFromRateAndModulationFormat(ServiceRateConstant.RATE_100,
                        ModulationFormat.DpQam16));
    }

    @Test
    public void getCentralFrequencyTest() {
        assertEquals("Central frequency should be 191.350",
                new FrequencyTHz(BigDecimal.valueOf(191.35).setScale(3)),
                GridUtils.getCentralFrequency(BigDecimal.valueOf(191.325), BigDecimal.valueOf(191.375)));
    }

    @Test
    public void getCentralFrequencyWithPrecisionTest() {
        assertEquals("Central frequency should be 191.3500",
                new FrequencyTHz(BigDecimal.valueOf(191.35).setScale(4)),
                GridUtils.getCentralFrequencyWithPrecision(BigDecimal.valueOf(191.325),
                        BigDecimal.valueOf(191.375), 4));
    }
}
