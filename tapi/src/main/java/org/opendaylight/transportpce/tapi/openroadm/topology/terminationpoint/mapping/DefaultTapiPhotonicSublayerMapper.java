/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;

/**
 * Default implementation of {@link TapiPhotonicSublayerMapper} that translates
 * OpenROADM termination point types into the corresponding TAPI photonic
 * sublayers.
 *
 * <p>The mapping is based on the semantic role of the termination point
 * (e.g. SRG or DEGREE) and reflects how such termination points are represented
 * in the TAPI topology model.
 */
public class DefaultTapiPhotonicSublayerMapper implements TapiPhotonicSublayerMapper {

    private static final Map<OpenroadmTpType, Set<NepPhotonicSublayer>> DEFAULT_MAPPER;

    static {
        Set<NepPhotonicSublayer> srgMapper = Set.of(
                NepPhotonicSublayer.OTSI_MC,
                NepPhotonicSublayer.MC,
                NepPhotonicSublayer.PHTNC_MEDIA_OTS);

        Set<NepPhotonicSublayer> degreeMapper = Set.of(
                NepPhotonicSublayer.PHTNC_MEDIA_OTS,
                NepPhotonicSublayer.PHTNC_MEDIA_OMS);

        DEFAULT_MAPPER = Map.of(
                OpenroadmTpType.SRGTXRXPP,     srgMapper,
                OpenroadmTpType.SRGRXPP,       srgMapper,
                OpenroadmTpType.SRGTXPP,       srgMapper,
                OpenroadmTpType.DEGREETXRXTTP, degreeMapper,
                OpenroadmTpType.DEGREERXTTP,   degreeMapper,
                OpenroadmTpType.DEGREETXTTP,   degreeMapper
        );
    }

    private final Map<OpenroadmTpType, Set<NepPhotonicSublayer>> mapper;

    public DefaultTapiPhotonicSublayerMapper() {
        this(DEFAULT_MAPPER);
    }

    /**
     * Creates a mapper using the provided mapping table.
     *
     * <p>The provided map is defensively copied. Callers should ensure the mapping
     * is complete for the OpenROADM TP types they expect to support.
     *
     * @param mapper mapping from OpenROADM TP type to the corresponding set of TAPI photonic sublayers
     * @throws NullPointerException if {@code mapper} is null
     */
    public DefaultTapiPhotonicSublayerMapper(
            Map<OpenroadmTpType, Set<NepPhotonicSublayer>> mapper) {

        this.mapper = validateAndCopy(mapper);
    }

    @Override
    public Set<NepPhotonicSublayer> nepPhotonicSublayer(OpenroadmTpType tp) {
        if (mapper.containsKey(tp)) {
            return mapper.get(tp);
        }

        throw new IllegalArgumentException(String.format("Termination point type %s is not supported", tp));
    }

    @Override
    public Optional<Set<NepPhotonicSublayer>> optionalNepPhotonicSublayer(OpenroadmTpType tp) {

        if (mapper.containsKey(tp)) {
            return Optional.of(mapper.get(tp));
        }

        return Optional.empty();
    }

    @Override
    public Optional<Set<NepPhotonicSublayer>> optionalNepPhotonicSublayer(
            OpenroadmTpType tp,
            Set<NepPhotonicSublayer> filter) {

        Set<NepPhotonicSublayer> mapped = mapper.get(tp);
        if (mapped == null) {
            return Optional.empty();
        }

        Set<NepPhotonicSublayer> result = mapped.stream()
                .filter(filter::contains)
                .collect(Collectors.toUnmodifiableSet());

        return result.isEmpty() ? Optional.empty() : Optional.of(result);
    }


    private static Map<OpenroadmTpType, Set<NepPhotonicSublayer>> validateAndCopy(
            Map<OpenroadmTpType, Set<NepPhotonicSublayer>> mapper) {

        Objects.requireNonNull(mapper, "mapper");

        for (Map.Entry<OpenroadmTpType, Set<NepPhotonicSublayer>> entry : mapper.entrySet()) {
            OpenroadmTpType tpType = entry.getKey();
            Set<NepPhotonicSublayer> sublayers = entry.getValue();

            if (tpType == null) {
                throw new IllegalArgumentException("mapper contains null OpenroadmTpType key");
            }
            if (sublayers == null) {
                throw new IllegalArgumentException("mapper contains null sublayer set for tpType=" + tpType);
            }
            if (sublayers.isEmpty()) {
                throw new IllegalArgumentException("mapper contains empty sublayer set for tpType=" + tpType);
            }
            if (sublayers.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("mapper contains null sublayer value for tpType=" + tpType);
            }
        }

        // Defensive copy:
        // - Map.copyOf: unmodifiable
        // - Set.copyOf: unmodifiable and prevents accidental later mutation
        return mapper.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> Set.copyOf(e.getValue())
                ));
    }
}
