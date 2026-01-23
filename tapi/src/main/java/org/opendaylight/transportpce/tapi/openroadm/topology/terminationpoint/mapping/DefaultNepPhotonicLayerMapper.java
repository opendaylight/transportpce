/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicLayer;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;

/**
 * Default mapping of {@link NepPhotonicSublayer} to {@link NepPhotonicLayer}.
 */
public class DefaultNepPhotonicLayerMapper implements NepPhotonicLayerMapper {

    private static final Map<NepPhotonicSublayer, NepPhotonicLayer> DEFAULT =
            Map.of(
                    NepPhotonicSublayer.PHTNC_MEDIA_OTS, NepPhotonicLayer.PHOTONIC_MEDIA,
                    NepPhotonicSublayer.PHTNC_MEDIA_OMS, NepPhotonicLayer.PHOTONIC_MEDIA,
                    NepPhotonicSublayer.MC, NepPhotonicLayer.PHOTONIC_MEDIA,
                    NepPhotonicSublayer.OTSI_MC, NepPhotonicLayer.PHOTONIC_MEDIA
            );

    private final Map<NepPhotonicSublayer, NepPhotonicLayer> mapping;

    public DefaultNepPhotonicLayerMapper() {
        this(DEFAULT);
    }

    public DefaultNepPhotonicLayerMapper(Map<NepPhotonicSublayer, NepPhotonicLayer> mapping) {
        Objects.requireNonNull(mapping, "mapping");
        // EnumMap gives fast lookups + null-safety expectations for enums.
        this.mapping = new EnumMap<>(mapping);
    }

    @Override
    public NepPhotonicLayer layerOf(NepPhotonicSublayer sublayer) {
        Objects.requireNonNull(sublayer, "sublayer");
        NepPhotonicLayer layer = mapping.get(sublayer);
        if (layer == null) {
            throw new IllegalArgumentException("No photonic layer mapping for sublayer=" + sublayer);
        }
        return layer;
    }
}
