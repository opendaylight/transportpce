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
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.node.OwnedNodeEdgePointKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MD-SAL-backed implementation of {@link OwnedNodeEdgePointRepository}.
 *
 * <p>This repository reads and updates TAPI {@link OwnedNodeEdgePoint} instances in the
 * {@link LogicalDatastoreType#OPERATIONAL} datastore.
 *
 * <p>Reads are performed via {@link NetworkTransactionService#read}, and updates are performed
 * using {@link NetworkTransactionService#put} followed by {@link NetworkTransactionService#commit()}.
 *
 * <p>Failures during read/update operations are wrapped in {@link OwnedNodeEdgePointRepositoryException}.
 */
public class MdSalOwnedNodeEdgePointRepository implements OwnedNodeEdgePointRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MdSalOwnedNodeEdgePointRepository.class);

    private static final LogicalDatastoreType STORE = LogicalDatastoreType.OPERATIONAL;

    private final NetworkTransactionService networkTransactionService;

    /**
     * Creates a repository backed by the provided {@link NetworkTransactionService}.
     *
     * @param networkTransactionService transaction service used to read/write to MD-SAL
     * @throws NullPointerException if {@code networkTransactionService} is {@code null}
     */
    public MdSalOwnedNodeEdgePointRepository(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = Objects.requireNonNull(networkTransactionService, "networkTransactionService");
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnedNodeEdgePointRepositoryException on datastore read failures
     */
    @Override
    public Optional<OwnedNodeEdgePoint> read(
            DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> id) {

        Objects.requireNonNull(id, "id");

        LOG.debug("Reading TAPI ONEP {} from {}", id, STORE);

        try {
            return networkTransactionService.read(STORE, id).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OwnedNodeEdgePointRepositoryException(
                    "Interrupted while reading NEP from " + STORE + ": " + id, e);
        } catch (ExecutionException e) {
            throw new OwnedNodeEdgePointRepositoryException("Failed to read NEP from " + STORE + ": " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnedNodeEdgePointRepositoryException on datastore read failures
     */
    @Override
    public Optional<OwnedNodeEdgePoint> read(NepIdentifier nepIdentifier) {
        return read(nepIdentifier.iid());
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnedNodeEdgePointRepositoryException on datastore read failures
     */
    @Override
    public void update(
            DataObjectIdentifier.WithKey<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepId,
            OwnedNodeEdgePoint ownedNodeEdgePoint) {

        Objects.requireNonNull(nepId, "nepId");
        Objects.requireNonNull(ownedNodeEdgePoint, "ownedNodeEdgePoint");

        LOG.debug("Updating TAPI ONEP {} in {}", nepId, STORE);

        networkTransactionService.put(STORE, nepId, ownedNodeEdgePoint);

        try {
            networkTransactionService.commit().get();
            LOG.debug("TAPI ONEP updated");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OwnedNodeEdgePointRepositoryException(
                    "Interrupted while updating NEP in " + STORE + ": " + nepId, e);
        } catch (ExecutionException e) {
            throw new OwnedNodeEdgePointRepositoryException("Failed to update NEP in " + STORE + ": " + nepId, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnedNodeEdgePointRepositoryException on datastore read failures
     */
    @Override
    public void update(NepIdentifier nepIdentifier, OwnedNodeEdgePoint ownedNodeEdgePoint) {
        update(nepIdentifier.iid(), ownedNodeEdgePoint);
    }
}
