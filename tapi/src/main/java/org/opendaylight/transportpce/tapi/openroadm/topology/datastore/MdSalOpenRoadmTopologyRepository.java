/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.datastore;

import java.util.Objects;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;


/**
 * MD-SAL backed implementation of {@link OpenRoadmTopologyRepository}.
 *
 * <p>This repository delegates read operations to {@link NetworkTransactionService}.
 * The returned {@link Optional} reflects whether the requested {@link Network} exists in the datastore.
 *
 * <p>Checked exceptions from the underlying read future are wrapped into {@link IllegalStateException}.
 * If the calling thread is interrupted while waiting for the read to complete, the interrupt flag is restored.
 */
public final class MdSalOpenRoadmTopologyRepository implements OpenRoadmTopologyRepository {

    private final NetworkTransactionService nts;

    /**
     * Creates a repository backed by the provided {@link NetworkTransactionService}.
     *
     * @param nts transaction service used to perform MD-SAL datastore reads
     * @throws NullPointerException if {@code nts} is {@code null}
     */
    public MdSalOpenRoadmTopologyRepository(NetworkTransactionService nts) {
        this.nts = nts;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the read fails or the calling thread is interrupted
     */
    @Override
    public Optional<Network> read(LogicalDatastoreType store, DataObjectIdentifier<Network> iid) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(iid, "iid");

        try {
            return nts.read(store, iid).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading network: " + iid, e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("Failed to read network: " + iid, e);
        }
    }
}
