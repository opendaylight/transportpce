/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.spectrum;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.tapi.frequency.Factory;
import org.opendaylight.transportpce.tapi.frequency.Frequency;
import org.opendaylight.transportpce.tapi.frequency.TeraHertz;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPacBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.AvailableSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.OccupiedSpectrumKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrum;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.spectrum.capability.pac.SupportableSpectrumKey;

/**
 * Default implementation of {@link TapiSpectrumCapabilityPacFactory}.
 *
 * <p>Builds a {@link SpectrumCapabilityPac} and always sets {@code supportableSpectrum}.
 * If neither used nor available spectrum is provided, a default {@code availableSpectrum}
 * range is generated based on {@link GridConstant}.
 */
public class DefaultTapiSpectrumCapabilityPacFactory implements TapiSpectrumCapabilityPacFactory {

    private final Factory frequencyFactory;

    public DefaultTapiSpectrumCapabilityPacFactory(Factory frequencyFactory) {
        this.frequencyFactory = frequencyFactory;
    }

    @Override
    public SpectrumCapabilityPac create(
            Map<Frequency, Frequency> usedFreqMap,
            Map<Frequency, Frequency> availableFreqMap) {

        SpectrumCapabilityPacBuilder spectrumPac = new SpectrumCapabilityPacBuilder();
        // If neither used nor available is present -> set default available spectrum
        if ((usedFreqMap == null || usedFreqMap.isEmpty())
                && (availableFreqMap == null || availableFreqMap.isEmpty())) {

            AvailableSpectrum defaultAvailable = new AvailableSpectrumBuilder()
                    .setLowerFrequency(new TeraHertz(GridConstant.START_EDGE_FREQUENCY_THZ).hertz())
                    .setUpperFrequency(frequencyFactory.frequency(
                            GridConstant.START_EDGE_FREQUENCY_THZ,
                            GridConstant.GRANULARITY,
                            GridConstant.EFFECTIVE_BITS).hertz())
                    .build();

            spectrumPac.setAvailableSpectrum(Map.of(
                    new AvailableSpectrumKey(
                            defaultAvailable.getLowerFrequency(),
                            defaultAvailable.getUpperFrequency()),
                    defaultAvailable
            ));
        } else {
            if (availableFreqMap != null && !availableFreqMap.isEmpty()) {
                spectrumPac.setAvailableSpectrum(toAvailableSpectrumMap(availableFreqMap));
            }
            if (usedFreqMap != null && !usedFreqMap.isEmpty()) {
                spectrumPac.setOccupiedSpectrum(toOccupiedSpectrumMap(usedFreqMap));
            }
        }

        // Always set supportable spectrum (same as before)
        SupportableSpectrum supportable = new SupportableSpectrumBuilder()
                .setLowerFrequency(new TeraHertz(GridConstant.START_EDGE_FREQUENCY_THZ).hertz())
                .setUpperFrequency(frequencyFactory.frequency(
                        GridConstant.START_EDGE_FREQUENCY_THZ,
                        GridConstant.GRANULARITY,
                        GridConstant.EFFECTIVE_BITS).hertz())
                .build();

        spectrumPac.setSupportableSpectrum(Map.of(
                new SupportableSpectrumKey(
                        supportable.getLowerFrequency(),
                        supportable.getUpperFrequency()),
                supportable
        ));

        return spectrumPac.build();
    }

    private static Map<AvailableSpectrumKey, AvailableSpectrum> toAvailableSpectrumMap(
            Map<Frequency, Frequency> availableFreqMap) {

        Map<AvailableSpectrumKey, AvailableSpectrum> aspecMap = new HashMap<>();
        for (Map.Entry<Frequency, Frequency> e : availableFreqMap.entrySet()) {
            AvailableSpectrum aspec = new AvailableSpectrumBuilder()
                    .setLowerFrequency(e.getKey().hertz())
                    .setUpperFrequency(e.getValue().hertz())
                    .build();

            aspecMap.put(new AvailableSpectrumKey(aspec.getLowerFrequency(), aspec.getUpperFrequency()), aspec);
        }
        return aspecMap;
    }

    private static Map<OccupiedSpectrumKey, OccupiedSpectrum> toOccupiedSpectrumMap(
            Map<Frequency, Frequency> usedFreqMap) {

        Map<OccupiedSpectrumKey, OccupiedSpectrum> ospecMap = new HashMap<>();
        for (Map.Entry<Frequency, Frequency> e : usedFreqMap.entrySet()) {
            OccupiedSpectrum ospec = new OccupiedSpectrumBuilder()
                    .setLowerFrequency(e.getKey().hertz())
                    .setUpperFrequency(e.getValue().hertz())
                    .build();

            ospecMap.put(new OccupiedSpectrumKey(ospec.getLowerFrequency(), ospec.getUpperFrequency()), ospec);
        }
        return ospecMap;
    }
}
