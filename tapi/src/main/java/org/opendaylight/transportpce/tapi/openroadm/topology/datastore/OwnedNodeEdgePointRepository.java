/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Optional;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Repository for accessing and updating TAPI {@link OwnedNodeEdgePoint} instances.
 *
 * <p>This abstraction provides a small API for retrieving and persisting NEPs in the
 * TAPI topology model. Implementations typically operate against a backing datastore
 * (e.g. MD-SAL).
 *
 * <p>The repository API is intentionally narrow and focuses on the minimum operations
 * needed by the topology population/update logic.
 */
public interface OwnedNodeEdgePointRepository {

    /**
     * Reads an {@link OwnedNodeEdgePoint} from the underlying datastore.
     *
     * @param id identifier (with key) of the NEP to read
     * @return the NEP if present, otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code id} is {@code null}
     */
    Optional<OwnedNodeEdgePoint> read(DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> id);

    /**
     * Reads an {@link OwnedNodeEdgePoint} from the underlying datastore using a {@link NepIdentifier}.
     *
     * <p>This is a convenience overload that delegates to {@link #read(DataObjectIdentifier.WithKey)}.
     * The {@link NepIdentifier} additionally carries semantic seed strings, which can be useful for
     * logging or troubleshooting at call sites.
     *
     * @param nepIdentifier resolved NEP identifier (IID + derivation context)
     * @return the NEP if present, otherwise {@link Optional#empty()}
     * @throws NullPointerException if {@code nepIdentifier} is {@code null}
     */
    Optional<OwnedNodeEdgePoint> read(NepIdentifier nepIdentifier);

    /**
     * Updates an {@link OwnedNodeEdgePoint} in the underlying datastore.
     *
     * <p>Implementations may overwrite or merge the supplied NEP depending on how the datastore
     * write operation is performed.
     *
     * @param nepId identifier (with key) of the NEP to update
     * @param ownedNodeEdgePoint new NEP contents to store
     * @throws NullPointerException if {@code nepId} or {@code ownedNodeEdgePoint} is {@code null}
     */
    void update(
            DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepId,
            OwnedNodeEdgePoint ownedNodeEdgePoint);

    /**
     * Updates an {@link OwnedNodeEdgePoint} in the underlying datastore using a {@link NepIdentifier}.
     *
     * <p>This is a convenience overload that delegates to
     * {@link #update(DataObjectIdentifier.WithKey, OwnedNodeEdgePoint)} by using
     * {@link NepIdentifier#iid()}.
     *
     * <p>The {@link NepIdentifier} additionally carries semantic seed strings used for deterministic
     * UUID derivation, which can be useful for log output and troubleshooting at call sites.
     *
     * @param nepIdentifier resolved NEP identifier (IID + derivation context)
     * @param ownedNodeEdgePoint new NEP contents to store
     * @throws NullPointerException if {@code nepIdentifier} or {@code ownedNodeEdgePoint} is {@code null}
     */
    void update(NepIdentifier nepIdentifier, OwnedNodeEdgePoint ownedNodeEdgePoint);
}
