/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.common.ServiceRateConstant;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMaps;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.available.freq.map.AvailFreqMapsKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

public class GridUtilsTest {

    @Test
    void getWaveLengthIndexFromSpectrumAssigmentTest() {
        assertEquals(15, GridUtils.getWaveLengthIndexFromSpectrumAssigment(647), "Wavelength index should be 15");
    }

    @Test
    void getFrequencyFromIndexTest() {
        BigDecimal[] expectedFrequencies = new BigDecimal[768];
        BigDecimal frequency = BigDecimal.valueOf(191.325);
        for (int i = 0; i < expectedFrequencies.length; i++) {
            expectedFrequencies[i] = frequency;
            frequency = frequency.add(BigDecimal.valueOf(0.00625));
        }
        assertEquals(0, expectedFrequencies[0].compareTo(GridUtils.getStartFrequencyFromIndex(0)),
            "Frequency should be 191.325");
        assertEquals(0, expectedFrequencies[284].compareTo(GridUtils.getStartFrequencyFromIndex(284)),
            "Frequency should be 193.1");
        assertEquals(0, expectedFrequencies[767].compareTo(GridUtils.getStartFrequencyFromIndex(767)),
            "Frequency should be 196.1188");
    }

    @Test
    void initFreqMaps4FixedGrid2AvailableTest() {
        AvailFreqMapsKey key = new AvailFreqMapsKey(GridConstant.C_BAND);
        byte[] byteArray = new byte[GridConstant.NB_OCTECTS];
        Arrays.fill(byteArray, (byte) GridConstant.AVAILABLE_SLOT_VALUE);
        Map<AvailFreqMapsKey, AvailFreqMaps> availFreqMaps = GridUtils.initFreqMaps4FixedGrid2Available();
        assertEquals(1, availFreqMaps.size(), "Should contains 1 element");
        assertTrue(availFreqMaps.containsKey(key), "should contains cband key");
        assertTrue(Arrays.equals(byteArray, availFreqMaps.get(key).getFreqMap()), "Should have available freq map");
    }

    @Test
    void getIndexFromFrequencyTest() {
        assertEquals(693, GridUtils.getIndexFromFrequency(Decimal64.valueOf("195.65625")), "Index should be 693");
        assertEquals(0, GridUtils.getIndexFromFrequency(Decimal64.valueOf("191.325")), "Index should be 0");
        assertEquals(767, GridUtils.getIndexFromFrequency(Decimal64.valueOf("196.11875")), "Index should be 767");
        assertEquals(8, GridUtils.getIndexFromFrequency(Decimal64.valueOf("191.375")), "Index should be 8");
        assertEquals(15, GridUtils.getIndexFromFrequency(Decimal64.valueOf("191.41875")), "Index should be 15");
        assertEquals(768, GridUtils.getIndexFromFrequency(Decimal64.valueOf("196.125")), "Index should be 768");
    }

    @Test
    void getIndexFromFrequencyExceptionTest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            GridUtils.getIndexFromFrequency(Decimal64.valueOf("196.13125"));
        });
        assertEquals("Frequency not in range 196.13125", exception.getMessage());
    }

    @Test
    void getIndexFromFrequencyException2Test() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            GridUtils.getIndexFromFrequency(Decimal64.valueOf("191.31875"));
        });
        assertEquals("Frequency not in range 191.31875", exception.getMessage());
    }

    @Test
    void getWidthFromRateAndModulationFormatTest() {
        assertEquals(
            new FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_75)),
            GridUtils.getWidthFromRateAndModulationFormat(ServiceRateConstant.RATE_400, ModulationFormat.DpQam16),
            "Width should be 75");
    }

    @Test
    void getWidthFromRateAndModulationFormatNotFoundTest() {
        assertEquals(
            new FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)),
            GridUtils.getWidthFromRateAndModulationFormat(ServiceRateConstant.RATE_100, ModulationFormat.DpQam16),
            "As not found width should be 40");
    }

    @Test
    void getCentralFrequencyTest() {
        assertEquals(
            new FrequencyTHz(Decimal64.valueOf(BigDecimal.valueOf(191.35).setScale(3))),
            GridUtils.getCentralFrequency(BigDecimal.valueOf(191.325), BigDecimal.valueOf(191.375)),
            "Central frequency should be 191.350");
    }

    @Test
    void getCentralFrequencyWithPrecisionTest() {
        assertEquals(
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz(
                    Decimal64.valueOf(BigDecimal.valueOf(191.35).setScale(4))),
            GridUtils.getCentralFrequencyWithPrecision(BigDecimal.valueOf(191.325), BigDecimal.valueOf(191.375), 4),
            "Central frequency should be 191.3500");
    }

    @Test
    void getCentralFrequencyWithPrecisionAndRoundTest() {
        assertEquals(
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz(
                    Decimal64.valueOf("191.3499")),
            GridUtils.getCentralFrequencyWithPrecision(
                    BigDecimal.valueOf(191.3244445), BigDecimal.valueOf(191.3754457788), 4),
            "Central frequency should be 191.3499");
    }

    @Test
    void initSpectrumInformationFromServicePathInputTest() {
        ServicePathInput input = new ServicePathInputBuilder()
                .setWaveNumber(Uint32.valueOf(1))
                .setCenterFreq(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyTHz(Decimal64.valueOf("196.1")))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setMaxFreq(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyTHz(Decimal64.valueOf("196.125")))
                .setMinFreq(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyTHz(Decimal64.valueOf("196.075")))
                .setNmcWidth(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)))
                .build();
        SpectrumInformation spectrumInformation = GridUtils.initSpectrumInformationFromServicePathInput(input);
        assertEquals(BigDecimal.valueOf(40), spectrumInformation.getWidth(), "Width should be 40");
        assertEquals(Uint32.valueOf(1), spectrumInformation.getWaveLength(), "Wavelength should be 1");
        assertEquals(BigDecimal.valueOf(196.1).setScale(4), spectrumInformation.getCenterFrequency(),
            "Center freq should be 196.1");
        assertEquals(761, spectrumInformation.getLowerSpectralSlotNumber(), "Lower slot number should be 761");
        assertEquals(768, spectrumInformation.getHigherSpectralSlotNumber(), "Higher slot number should be 768");
        assertEquals(BigDecimal.valueOf(196.075).setScale(4), spectrumInformation.getMinFrequency(),
            "Min freq should be 196.075");
        assertEquals(BigDecimal.valueOf(196.125).setScale(4), spectrumInformation.getMaxFrequency(),
            "Max freq should be 196.125");
    }

    @Test
    void initSpectrumInformationFromServicePathInputNoCenterFreqTest() {
        ServicePathInput input = new ServicePathInputBuilder()
                .setWaveNumber(Uint32.valueOf(1))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setMaxFreq(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyTHz(Decimal64.valueOf("196.125")))
                .setMinFreq(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyTHz(Decimal64.valueOf("196.075")))
                .setNmcWidth(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)))
                .build();
        SpectrumInformation spectrumInformation = GridUtils.initSpectrumInformationFromServicePathInput(input);
        assertEquals(BigDecimal.valueOf(40), spectrumInformation.getWidth(), "Width should be 40");
        assertEquals(Uint32.valueOf(1), spectrumInformation.getWaveLength(), "Wavelength should be 1");
        assertEquals(BigDecimal.valueOf(196.1).setScale(4), spectrumInformation.getCenterFrequency(),
            "Center freq should be 196.1");
        assertEquals(761, spectrumInformation.getLowerSpectralSlotNumber(), "Lower slot number should be 761");
        assertEquals(768, spectrumInformation.getHigherSpectralSlotNumber(), "Higher slot number should be 768");
        assertEquals(BigDecimal.valueOf(196.075).setScale(4), spectrumInformation.getMinFrequency(),
            "Min freq should be 196.075");
        assertEquals(BigDecimal.valueOf(196.125).setScale(4), spectrumInformation.getMaxFrequency(),
            "Max freq should be 196.125");
    }

    @Test
    void initSpectrumInformationFromServicePathInputNoFreqTest() {
        ServicePathInput input = new ServicePathInputBuilder()
                .setWaveNumber(Uint32.valueOf(1))
                .setHigherSpectralSlotNumber(Uint32.valueOf(768))
                .setLowerSpectralSlotNumber(Uint32.valueOf(761))
                .setNmcWidth(new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019
                        .FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)))
                .build();
        SpectrumInformation spectrumInformation = GridUtils.initSpectrumInformationFromServicePathInput(input);
        assertEquals(BigDecimal.valueOf(40), spectrumInformation.getWidth(), "Width should be 40");
        assertEquals(Uint32.valueOf(1), spectrumInformation.getWaveLength(), "Wavelength should be 1");
        assertEquals(BigDecimal.valueOf(196.1).setScale(4), spectrumInformation.getCenterFrequency(),
            "Center freq should be 196.1");
        assertEquals(761, spectrumInformation.getLowerSpectralSlotNumber(), "Lower slot number should be 761");
        assertEquals(768, spectrumInformation.getHigherSpectralSlotNumber(), "Higher slot number should be 768");
        assertEquals(BigDecimal.valueOf(196.075).setScale(4), spectrumInformation.getMinFrequency(),
            "Min freq should be 196.075");
        assertEquals(BigDecimal.valueOf(196.125).setScale(4), spectrumInformation.getMaxFrequency(),
            "Max freq should be 196.125");
    }

    @Test
    void initSpectrumInformationFromServicePathInputNoSlotTest() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            GridUtils.initSpectrumInformationFromServicePathInput(new ServicePathInputBuilder().build());
        });
        assertEquals("low and higher spectral slot numbers cannot be null", exception.getMessage());
    }


    @Test
    void startFrequencyTest() {

        BigDecimal startFrequency = BigDecimal.valueOf(191.325);
        BigDecimal granularity = BigDecimal.valueOf(0.00625);

        for (int i = 0; i < 768; i++) {
            BigDecimal index = BigDecimal.valueOf(i);
            BigDecimal expected = startFrequency.add(granularity.multiply(index));
            BigDecimal found = GridUtils.getStartFrequencyFromIndex(i);
            Assert.assertTrue(
                    "Expected frequency " + expected + " but found " + found,
                    expected.compareTo(found) == 0
            );

        }

    }

    @Test
    void stopFrequencyTest() {

        BigDecimal startFrequency = BigDecimal.valueOf(191.325);
        BigDecimal granularity = BigDecimal.valueOf(0.00625);

        for (int i = 0; i < 768; i++) {
            BigDecimal index = BigDecimal.valueOf(i + 1);
            BigDecimal expected = startFrequency.add(granularity.multiply(index));
            BigDecimal found = GridUtils.getStopFrequencyFromIndex(i);
            Assert.assertTrue(
                    "Expected frequency " + expected + " but found " + found,
                    expected.compareTo(found) == 0
            );

        }

    }
}
