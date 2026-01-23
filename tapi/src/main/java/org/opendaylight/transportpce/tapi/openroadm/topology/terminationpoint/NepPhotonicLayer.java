/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint;

import org.opendaylight.transportpce.tapi.TapiConstants;

/**
 * High-level TAPI photonic layer grouping used for NEP/node identity.
 *
 * <p>This is intentionally coarser than {@link NepPhotonicSublayer}. Multiple
 * sublayers may share the same layer group.
 */
public enum NepPhotonicLayer {
    PHOTONIC_MEDIA(TapiConstants.PHTNC_MEDIA);

    private final String tapiName;

    NepPhotonicLayer(String tapiName) {
        this.tapiName = tapiName;
    }

    public String tapiName() {
        return tapiName;
    }

    @Override
    public String toString() {
        return tapiName;
    }
}
