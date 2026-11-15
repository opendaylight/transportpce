/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Service interface for transforming TAPI node and link JSON data to SBI topology.
 */
public interface TransformationService {

    /**
     * Transforms a node from TAPI JSON format to SBI topology normalized node.
     *
     * @param nodeJson the node JSON string
     * @param sbiTopologyUuid the SBI topology UUID
     * @return a ListenableFuture that completes when transformation is done
     */
    ListenableFuture<Void> updateNodeInSbiTopology(String nodeJson, String sbiTopologyUuid);

    /**
     * Transforms a link from TAPI JSON format to SBI topology normalized link.
     *
     * @param linkJson the link JSON string
     * @param sbiTopologyUuid the SBI topology UUID
     * @return a ListenableFuture that completes when transformation is done
     */
    ListenableFuture<Void> updateLinkInSbiTopology(String linkJson, String sbiTopologyUuid);

    /**
     * Transforms a profile from TAPI JSON format and stores it in the TAPI common context.
     * Adds controller-id and original-uuid name entries, and generates a new UUID.
     *
     * @param profileJson the profile JSON string
     * @return a ListenableFuture that completes when transformation is done
     */
    ListenableFuture<Void> updateProfileInContext(String profileJson);

    /**
     * Transforms a service interface point from TAPI JSON format and stores it in the TAPI common context.
     * Adds controller-id and original-uuid name entries, and generates a new UUID.
     *
     * @param sipJson the service interface point JSON string
     * @return a ListenableFuture that completes when transformation is done
     */
    ListenableFuture<Void> updateServiceInterfacePointInContext(String sipJson);

    /**
     * Removes the SBI topology (including all its nodes and links) associated
     * with the given controller-id from the MD-SAL datastore.
     *
     * @param controllerId the controller identifier whose topology should be removed
     * @return a ListenableFuture that completes when cleanup is done
     */
    ListenableFuture<Void> cleanupTopology(String controllerId);

    /**
     * Removes all profiles associated with the given controller-id from the
     * TAPI common context in the MD-SAL datastore. Profiles are matched by the {@code controller-id} name
     * entry added during transformation.
     *
     * @param controllerId the controller identifier whose profiles should be removed
     * @return a ListenableFuture that completes when cleanup is done
     */
    ListenableFuture<Void> cleanupProfiles(String controllerId);

    /**
     * Removes all service interface points associated with the given
     * controller-id from the TAPI common context in the MD-SAL datastore. SIPs are matched by the
     * {@code controller-id} name entry added during transformation.
     *
     * @param controllerId the controller identifier whose SIPs should be removed
     * @return a ListenableFuture that completes when cleanup is done
     */
    ListenableFuture<Void> cleanupServiceInterfacePoints(String controllerId);
}
