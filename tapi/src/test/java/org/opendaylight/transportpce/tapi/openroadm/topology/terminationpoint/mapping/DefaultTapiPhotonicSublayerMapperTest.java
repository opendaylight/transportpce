/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;

@DisplayName("DefaultTapiPhotonicSublayerMapper")
class DefaultTapiPhotonicSublayerMapperTest {

    @Test
    @DisplayName("Throws IllegalArgumentException for unsupported OpenROADM termination point type")
    void throwsForUnsupportedTerminationPointType() {
        TapiPhotonicSublayerMapper mapper = new DefaultTapiPhotonicSublayerMapper();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> mapper.nepPhotonicSublayer(OpenroadmTpType.SRGTXRXCP)
        );

        assertTrue(ex.getMessage().contains("is not supported"));
    }

    @Test
    @DisplayName("Returns expected photonic sublayers for SRG termination points (Optional API)")
    void returnsExpectedOptionalPhotonicSublayersForSrgTpType() {
        TapiPhotonicSublayerMapper mapper = new DefaultTapiPhotonicSublayerMapper();
        Set<NepPhotonicSublayer> expected = Set.of(
                NepPhotonicSublayer.OTSI_MC,
                NepPhotonicSublayer.MC,
                NepPhotonicSublayer.PHTNC_MEDIA_OTS
        );
        Optional<Set<NepPhotonicSublayer>> nepPhotonicSublayers = mapper.optionalNepPhotonicSublayer(
                OpenroadmTpType.SRGTXRXPP);

        assertEquals(expected, nepPhotonicSublayers.orElseThrow());
    }

    @Test
    @DisplayName("Returns empty Optional for unsupported OpenROADM termination point type (Optional API)")
    void returnsEmptyOptionalForUnsupportedTerminationPointType() {
        TapiPhotonicSublayerMapper mapper = new DefaultTapiPhotonicSublayerMapper();
        Optional<Set<NepPhotonicSublayer>> nepPhotonicSublayers =
                mapper.optionalNepPhotonicSublayer(OpenroadmTpType.SRGTXRXCP);
        assertTrue(nepPhotonicSublayers.isEmpty());
    }

    @Test
    @DisplayName("Returns only photonic sublayers that match the provided filter set")
    void returnsOnlySublayersMatchingProvidedFilter() {
        TapiPhotonicSublayerMapper mapper = new DefaultTapiPhotonicSublayerMapper();
        Set<NepPhotonicSublayer> expected = Set.of(NepPhotonicSublayer.MC);
        Optional<Set<NepPhotonicSublayer>> nepPhotonicSublayers = mapper.optionalNepPhotonicSublayer(
                OpenroadmTpType.SRGTXRXPP, expected);

        assertEquals(expected, nepPhotonicSublayers.orElseThrow());
    }
}
