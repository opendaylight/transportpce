/*
 * Copyright © 2023 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency;

import java.math.BigDecimal;
import java.util.BitSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.pce.frequency.interval.Collection;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyInterval;
import org.opendaylight.transportpce.pce.frequency.interval.FrequencyIntervalFactory;
import org.opendaylight.transportpce.pce.frequency.interval.Interval;
import org.opendaylight.transportpce.pce.frequency.service.ServiceFrequency;
import org.opendaylight.transportpce.pce.frequency.spectrum.FrequencySpectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.SpectrumIndex;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceAEnd1Builder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceZEnd1Builder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencyRange;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencyRangeBuilder;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencySlotBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yangtools.yang.common.Uint32;

class FrequencyIntervalFactoryTest {

    @Test
    void serviceAEndAndZEndInputSlots() {

        // Build a PathComputationRequestInput (i.e. API input)
        // where the client specified 191.33125 as the center frequency
        // and a two slot width (e.g. 2 x 6.5GHz = 12.5GHz)
        FrequencySlot frequencySlots1 = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(Uint32.ONE)
                .build();

        FrequencySlot frequencySlots2 = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.4"))
                .setSlotWidth(Uint32.ONE)
                .build();

        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlots1)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();

        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlots2)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();

        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceAEnd(serviceAEnd);
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        // Build a frequency range collection using the PathComputationRequestInput object.
        Collection frequencyWindow = frequencyIntervalFactory.frequencyRange(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        Collection flexGrid = frequencyIntervalFactory.frequencySlot(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        // The frequency collection should contain one frequency range
        Assertions.assertEquals(1, frequencyWindow.size());
        Assertions.assertEquals(1, flexGrid.size());

        // Turning the frequency interval into a BitSet object
        // should result in an object where two bits are true
        // at index locations 0 and 1.
        BitSet expectedFlexGrid = new BitSet(768);
        expectedFlexGrid.set(0, 2);
        Assertions.assertEquals(expectedFlexGrid, flexGrid.set());

        BitSet expectedFrequencyWindow = new BitSet(768);
        expectedFrequencyWindow.set(12, 28);
        Assertions.assertEquals(expectedFrequencyWindow, frequencyWindow.set());

    }



    @Test
    void serviceAEndInputSlotsAndFrequencyWindow() {

        // Build a PathComputationRequestInput (i.e. API input)
        // where the client specified 191.33125 as the center frequency
        // and a two slot width (e.g. 2 x 6.5GHz = 12.5GHz)
        FrequencySlot frequencySlots = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(Uint32.ONE)
                .build();

        FrequencyRange frequencyRange1 = new FrequencyRangeBuilder()
                .setMinFrequency(FrequencyTHz.getDefaultInstance("191.4"))
                .setMaxFrequency(FrequencyTHz.getDefaultInstance("191.5"))
                .build();

        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlots)
                .setFrequencyRange(frequencyRange1)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();

        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceAEnd(serviceAEnd);
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        // Build a frequency range collection using the PathComputationRequestInput object.
        Collection frequencyWindow = frequencyIntervalFactory.frequencyRange(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        Collection flexGrid = frequencyIntervalFactory.frequencySlot(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        // The frequency collection should contain one frequency range
        Assertions.assertEquals(1, frequencyWindow.size());
        Assertions.assertEquals(1, flexGrid.size());

        // Turning the frequency interval into a BitSet object
        // should result in an object where two bits are true
        // at index locations 0 and 1.
        BitSet expectedFlexGrid = new BitSet(768);
        expectedFlexGrid.set(0, 2);
        Assertions.assertEquals(expectedFlexGrid, flexGrid.set());

        BitSet expectedFrequencyWindow = new BitSet(768);
        expectedFrequencyWindow.set(12, 28);
        Assertions.assertEquals(expectedFrequencyWindow, frequencyWindow.set());

    }

    @Test
    void serviceZEndInputSlotsAndFrequencyWindow() {

        // Build a PathComputationRequestInput (i.e. API input)
        // where the client specified 191.33125 as the center frequency
        // and a two slot width (e.g. 2 x 6.5GHz = 12.5GHz)
        FrequencySlot frequencySlots = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(Uint32.ONE)
                .build();

        FrequencyRange frequencyRange1 = new FrequencyRangeBuilder()
                .setMinFrequency(FrequencyTHz.getDefaultInstance("191.4"))
                .setMaxFrequency(FrequencyTHz.getDefaultInstance("191.5"))
                .build();

        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlots)
                .setFrequencyRange(frequencyRange1)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();

        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        // Build a frequency range collection using the PathComputationRequestInput object.
        Collection frequencyWindow = frequencyIntervalFactory.frequencyRange(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        Collection flexGrid = frequencyIntervalFactory.frequencySlot(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        // The frequency collection should contain one frequency range
        Assertions.assertEquals(1, frequencyWindow.size());
        Assertions.assertEquals(1, flexGrid.size());

        // Turning the frequency interval into a BitSet object
        // should result in an object where two bits are true
        // at index locations 0 and 1.
        BitSet expectedFlexGrid = new BitSet(768);
        expectedFlexGrid.set(0, 2);
        Assertions.assertEquals(expectedFlexGrid, flexGrid.set());

        BitSet expectedFrequencyWindow = new BitSet(768);
        expectedFrequencyWindow.set(12, 28);
        Assertions.assertEquals(expectedFrequencyWindow, frequencyWindow.set());

    }

    @Test
    void serviceAEndCentralFrequencyFlexGrid() {

        // Build a PathComputationRequestInput (i.e. API input)
        // where the client specified 191.33125 as the center frequency
        // and a two slot width (e.g. 2 x 6.5GHz = 12.5GHz)
        FrequencySlot frequencySlots = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(Uint32.ONE)
                .build();

        ServiceAEnd1 serviceAEnd1 = new ServiceAEnd1Builder()
                .setFrequencySlot(frequencySlots)
                .build();

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
                .addAugmentation(serviceAEnd1)
                .build();

        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceAEnd(serviceAEnd);
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        // Build a frequency range collection using the PathComputationRequestInput object.
        Collection interval = frequencyIntervalFactory.frequencySlot(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        // The frequency collection should contain one frequency range
        Assertions.assertEquals(1, interval.size());

        BitSet expected = new BitSet(768);
        expected.set(0, 2);

        Assertions.assertEquals(expected, interval.set());
    }

    @Test
    void serviceZEndCentralFrequencyFlexGrid() {

        // Build a PathComputationRequestInput (i.e. API input)
        // where the client specified 191.33125 as the center frequency
        // and a two slot width (e.g. 2 x 6.5GHz = 12.5GHz)
        FrequencySlot frequencySlot = new FrequencySlotBuilder()
                .setCenterFrequency(FrequencyTHz.getDefaultInstance("191.33125"))
                .setSlotWidth(Uint32.ONE)
                .build();

        ServiceZEnd1 serviceZEnd1 = new ServiceZEnd1Builder()
                .setFrequencySlot(frequencySlot)
                .build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                .addAugmentation(serviceZEnd1)
                .build();
        PathComputationRequestInputBuilder pathComputationRequestInputBuilder =
                new PathComputationRequestInputBuilder();
        pathComputationRequestInputBuilder.setServiceZEnd(serviceZEnd);
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        // Build a frequency range collection using the PathComputationRequestInput object.
        Collection interval = frequencyIntervalFactory.frequencySlot(
                pathComputationRequestInputBuilder.build(),
                new FrequencySpectrum(
                        new SpectrumIndex(191.325, 6.25, 768),
                        768
                )
        );

        // The frequency collection should contain one frequency range
        Assertions.assertEquals(1, interval.size());

        // Turning the frequency interval into a BitSet object
        // should result in an object where two bits are true
        // at index locations 0 and 1.
        BitSet expected = new BitSet();
        expected.set(0, 2);
        Assertions.assertEquals(expected, interval.set());

    }

    @Test
    void frequencyWindowUsingCentralFrequency() {
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        Interval interval = frequencyIntervalFactory.interval(BigDecimal.valueOf(191.6), BigDecimal.valueOf(6.25), 2);

        Assertions.assertEquals(
                BigDecimal.valueOf(12.5),
                interval.end().subtract(interval.start())
                        .multiply(BigDecimal.valueOf(1000))
                        .stripTrailingZeros()
        );
    }

    @Test
    void frequencyWindowUsingCentralFrequencies() {
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        Interval interval = frequencyIntervalFactory.interval(
                BigDecimal.valueOf(191.33125),
                BigDecimal.valueOf(6.25),
                2
        );

        Interval expecterInterval = new FrequencyInterval(
                BigDecimal.valueOf(191.325),
                BigDecimal.valueOf(191.3375)
        );

        Assertions.assertEquals(
                expecterInterval,
                interval
        );

    }

    @Test
    void lowerFrequency() {
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        BigDecimal expectedLower = BigDecimal.valueOf(191.59375);

        Assertions.assertEquals(
                expectedLower,
                frequencyIntervalFactory.lowerFrequency(BigDecimal.valueOf(191.6),6.25, 2)
        );

    }

    @Test
    void upperFrequency() {
        FrequencyIntervalFactory frequencyIntervalFactory = new FrequencyIntervalFactory(
                new ServiceFrequency(),
                768,
                6.25
        );

        BigDecimal expectedLower = BigDecimal.valueOf(191.60625);

        Assertions.assertEquals(
                expectedLower,
                frequencyIntervalFactory.upperFrequency(BigDecimal.valueOf(191.6),6.25, 2)
        );

    }
}
