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
 * Enumeration of TAPI photonic sublayers used when deriving
 * {@code OwnedNodeEdgePointName}s from OpenROADM termination points.
 *
 * <p>In TAPI, a single OpenROADM termination point may correspond to multiple
 * owned node edge points, each representing a specific photonic sublayer.
 * This enum provides a strongly-typed representation of those photonic
 * sublayers and their associated TAPI name values.
 *
 * <p>The enum values map directly to the string constants defined in
 * {@link TapiConstants}.
 */
public enum NepPhotonicSublayer {
    PHTNC_MEDIA_OTS(TapiConstants.PHTNC_MEDIA_OTS),
    PHTNC_MEDIA_OMS(TapiConstants.PHTNC_MEDIA_OMS),
    OTSI_MC(TapiConstants.OTSI_MC);

    /**
     * The TAPI name string representing this photonic sublayer.
     */
    public final String photonic;

    NepPhotonicSublayer(String photonic) {
        this.photonic = photonic;
    }

    /**
     * Returns the TAPI name associated with this photonic sublayer.
     *
     * <p>This value is typically used when constructing TAPI
     * {@code Name} instances, such as owned node edge point names.
     *
     * @return the TAPI photonic sublayer name
     */
    public String tapiName() {
        return photonic;
    }


    @Override
    public String toString() {
        return photonic;
    }
}
