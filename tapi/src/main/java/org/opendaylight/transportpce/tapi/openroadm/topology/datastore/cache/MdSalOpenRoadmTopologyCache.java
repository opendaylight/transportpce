/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore.cache;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.tapi.openroadm.topology.datastore.OpenRoadmTopologyRepository;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * Caching implementation of {@link OpenRoadmTopologyRepository}.
 *
 * <p>This repository decorates another {@link OpenRoadmTopologyRepository} and caches the results
 * of {@link #read(LogicalDatastoreType, DataObjectIdentifier)} calls. Cached entries are keyed by
 * the combination of datastore type and instance identifier.
 *
 * <p>Subsequent reads with the same parameters are served from the cache, avoiding repeated
 * datastore access.
 *
 * <p>This implementation does not provide cache invalidation and is intended for use in contexts
 * where the underlying data is not expected to change during the lifetime of the cache.
 *
 * <p>Note: This implementation is not thread-safe.
 */
public final class MdSalOpenRoadmTopologyCache implements OpenRoadmTopologyRepository {

    private final OpenRoadmTopologyRepository delegate;

    private final Map<CacheKey, Optional<Network>> cache = new HashMap<>();

    public MdSalOpenRoadmTopologyCache(OpenRoadmTopologyRepository delegate) {
        this.delegate = requireNonNull(delegate, "delegate");
    }

    @Override
    public Optional<Network> read(LogicalDatastoreType store, DataObjectIdentifier<Network> iid) {
        requireNonNull(store, "store");
        requireNonNull(iid, "iid");

        return cache.computeIfAbsent(new CacheKey(store, iid), key -> delegate.read(key.store(), key.iid()));
    }

    private record CacheKey(
            LogicalDatastoreType store,
            DataObjectIdentifier<Network> iid
    ) {}
}
