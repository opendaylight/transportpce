/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.frequency.interval;

import java.math.BigDecimal;
import org.opendaylight.transportpce.pce.frequency.service.Service;
import org.opendaylight.transportpce.pce.frequency.spectrum.Spectrum;
import org.opendaylight.transportpce.pce.frequency.spectrum.index.FrequencySpectrumSet;
import org.opendaylight.transportpce.pce.input.InvalidClientInputException;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceAEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.ServiceZEnd1;
import org.opendaylight.yang.gen.v1.http.com.smartoptics.openroadm.service.rev230907.spectrum.allocation.FrequencySlot;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.path.computation.request.input.ServiceZEnd;

public class FrequencyIntervalFactory implements IntervalFactory {

    private final Service service;

    private final int effectiveBits;

    private final Double frequencyGranularityGHz;

    public FrequencyIntervalFactory(Service service, int effectiveBits, Double frequencyGranularityGHz) {
        this.service = service;
        this.effectiveBits = effectiveBits;
        this.frequencyGranularityGHz = frequencyGranularityGHz;
    }

    @Override
    public Collection frequencyRange(PathComputationRequestInput input, Spectrum frequencySpectrum) {
        Collection intervalCollection = new IntervalCollection(
                new FrequencySpectrumSet(frequencySpectrum),
                effectiveBits
        );

        ServiceAEnd serviceAEnd = input.getServiceAEnd();
        if (serviceAEnd != null) {
            addAEndFrequencyRangeToCollection(intervalCollection, serviceAEnd.augmentation(ServiceAEnd1.class));
        }

        ServiceZEnd serviceZEnd = input.getServiceZEnd();
        if (serviceZEnd != null) {
            addZEndFrequencyRangeToCollection(intervalCollection, serviceZEnd.augmentation(ServiceZEnd1.class));
        }

        if (intervalCollection.size() < 1) {
            return new EntireSpectrum(effectiveBits);
        }

        return intervalCollection;
    }

    @Override
    public Interval interval(BigDecimal centerFrequencyTHz, BigDecimal slotWidthGHz, int nrOfSlots) {
        return new FrequencyInterval(
                lowerFrequency(centerFrequencyTHz, slotWidthGHz.doubleValue(), nrOfSlots),
                upperFrequency(centerFrequencyTHz, slotWidthGHz.doubleValue(), nrOfSlots)
        );
    }

    @Override
    public Collection frequencySlot(PathComputationRequestInput input, Spectrum frequencySpectrum) {
        Collection intervalCollection = new IntervalCollection(
                new FrequencySpectrumSet(frequencySpectrum),
                effectiveBits
        );

        ServiceAEnd serviceAEnd = input.getServiceAEnd();
        if (serviceAEnd != null) {
            addAEndFrequencySlotToCollection(intervalCollection, serviceAEnd.augmentation(ServiceAEnd1.class));
        }

        ServiceZEnd serviceZEnd = input.getServiceZEnd();
        if (serviceZEnd != null) {
            addZEndFrequencySlotToCollection(intervalCollection, serviceZEnd.augmentation(ServiceZEnd1.class));
        }

        if (intervalCollection.size() < 1) {
            return new EntireSpectrum(effectiveBits);
        }

        if (intervalCollection.size() > 1) {
            throw new InvalidClientInputException("Expecting no more than one frequency slot input.");
        }

        return intervalCollection;
    }

    public BigDecimal lowerFrequency(BigDecimal centerFrequencyTHz, Double slotWidthGHz, int nrOfSlots) {
        if (nrOfSlots < 2 || nrOfSlots % 2 != 0) {
            throw new InvalidIntervalException(
                    String.format(
                            "Cannot create a interval using slots (%s) on a "
                                    + "center frequency %s and slot frequency %s",
                            nrOfSlots,
                            centerFrequencyTHz.doubleValue(),
                            slotWidthGHz
                    )
            );
        }

        return centerFrequencyTHz
                .subtract(
                        BigDecimal.valueOf(slotWidthGHz)
                                .multiply(BigDecimal.valueOf(0.001))
                                .multiply(BigDecimal.valueOf(nrOfSlots).divide(BigDecimal.TWO))
                );
    }


    public BigDecimal upperFrequency(BigDecimal centerFrequencyTHz, Double slotWidthGHz, int nrOfSlots) {

        if (nrOfSlots < 2 || nrOfSlots % 2 != 0) {
            throw new InvalidIntervalException(
                    String.format(
                            "Cannot create a interval using slots (%s) on a "
                                    + "center frequency %s and slot frequency %s",
                            nrOfSlots,
                            centerFrequencyTHz.doubleValue(),
                            slotWidthGHz
                    )
            );
        }

        return centerFrequencyTHz
                .add(
                        BigDecimal.valueOf(slotWidthGHz)
                                .multiply(BigDecimal.valueOf(0.001))
                                .multiply(BigDecimal.valueOf(nrOfSlots).divide(BigDecimal.TWO))
                );
    }

    private void addAEndFrequencyRangeToCollection(Collection frequencyIntervalCollection, ServiceAEnd1 serviceAEnd1) {
        if (serviceAEnd1 != null && serviceAEnd1.getFrequencyRange() != null) {

            frequencyIntervalCollection.add(
                    new FrequencyInterval(
                            serviceAEnd1.getFrequencyRange()
                    )
            );
        }
    }

    private void addZEndFrequencyRangeToCollection(Collection frequencyIntervalCollection, ServiceZEnd1 serviceZEnd1) {
        if (serviceZEnd1 != null && serviceZEnd1.getFrequencyRange() != null) {

            frequencyIntervalCollection.add(
                    new FrequencyInterval(
                            serviceZEnd1.getFrequencyRange()
                    )
            );
        }
    }

    private void addAEndFrequencySlotToCollection(Collection frequencyIntervalCollection, ServiceAEnd1 serviceAEnd1) {
        if (serviceAEnd1 != null) {
            addFrequencySlotToCollection(frequencyIntervalCollection, serviceAEnd1.getFrequencySlot());
        }
    }

    private void addZEndFrequencySlotToCollection(Collection frequencyIntervalCollection, ServiceZEnd1 serviceZEnd1) {
        if (serviceZEnd1 != null) {
            addFrequencySlotToCollection(frequencyIntervalCollection, serviceZEnd1.getFrequencySlot());
        }
    }

    private void addFrequencySlotToCollection(Collection frequencyIntervalCollection, FrequencySlot frequencySlot) {

        if (frequencySlot != null
                && frequencySlot.getCenterFrequency() != null
                && frequencySlot.getSlotWidth() != null) {

            frequencyIntervalCollection.add(
                    interval(
                            frequencySlot.getCenterFrequency().getValue().decimalValue(),
                            BigDecimal.valueOf(frequencyGranularityGHz),
                            service.slotWidth(frequencySlot, frequencyGranularityGHz)
                    )
            );
        }
    }
}
