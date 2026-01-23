/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicLayer;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;

/**
 * Maps a NEP photonic sublayer to a higher-level photonic layer grouping.
 *
 * <p>The layer grouping is typically used for naming/UUID derivations where
 * multiple sublayers share a common identity namespace.
 */
public interface NepPhotonicLayerMapper {
    NepPhotonicLayer layerOf(NepPhotonicSublayer sublayer);
}
