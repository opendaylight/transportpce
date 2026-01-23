/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;

import java.util.Optional;
import java.util.Set;
import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.NepPhotonicSublayer;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev250110.OpenroadmTpType;

/**
 * Maps an OpenROADM termination point type to the corresponding set of
 * TAPI photonic sublayers.
 *
 * <p>In TAPI, a single OpenROADM termination point may be represented by multiple
 * owned node edge points (NEPs), typically one per photonic sublayer. Implementations
 * define how an {@link OpenroadmTpType} is translated into the applicable set of
 * {@link NepPhotonicSublayer}s.
 *
 * <p>This interface provides both a strict API ({@link #nepPhotonicSublayer(OpenroadmTpType)})
 * that fails fast for unsupported termination point types, and lenient APIs that return
 * {@link Optional#empty()} instead of throwing.
 */
public interface TapiPhotonicSublayerMapper {

    /**
     * Returns the set of TAPI photonic sublayers applicable to the given OpenROADM
     * termination point type.
     *
     * @param tp
     *     the OpenROADM termination point type
     * @return
     *     the set of corresponding TAPI photonic sublayers
     * @throws IllegalArgumentException
     *     if the given termination point type is not supported
     */
    Set<NepPhotonicSublayer> nepPhotonicSublayer(OpenroadmTpType tp);

    /**
     * Lenient variant of {@link #nepPhotonicSublayer(OpenroadmTpType)}.
     *
     * <p>Returns {@link Optional#empty()} when the termination point type is not supported,
     * rather than throwing an exception.
     *
     * @param tp
     *     the OpenROADM termination point type
     * @return
     *     an {@link Optional} containing the corresponding TAPI photonic sublayers if the
     *     termination point type is supported; otherwise {@link Optional#empty()}
     */
    Optional<Set<NepPhotonicSublayer>> optionalNepPhotonicSublayer(OpenroadmTpType tp);


    /**
     * Lenient variant of {@link #nepPhotonicSublayer(OpenroadmTpType)} that additionally
     * filters the returned sublayers.
     *
     * <p>If the termination point type is unsupported, this method returns {@link Optional#empty()}.
     * If supported but none of the mapped sublayers are contained in {@code filter}, this method
     * also returns {@link Optional#empty()}.
     *
     * @param tp
     *     the OpenROADM termination point type
     * @param filter
     *     the set of allowed photonic sublayers; only sublayers present in this set will be returned
     * @return
     *     an {@link Optional} containing the intersection of the mapped sublayers and {@code filter},
     *     or {@link Optional#empty()} if unsupported or if the intersection is empty
     */
    Optional<Set<NepPhotonicSublayer>> optionalNepPhotonicSublayer(OpenroadmTpType tp, Set<NepPhotonicSublayer> filter);
}
