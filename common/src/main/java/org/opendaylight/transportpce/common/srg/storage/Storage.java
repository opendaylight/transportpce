/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.Map;
import org.opendaylight.transportpce.common.srg.revision.Rev250702;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.NetworkNodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg.rev250702.network.network.nodes.SharedRiskGroupKey;

public interface Storage {

    /**
     * Save the Shared Risk Groups for a given node.
     *
     * @param nodeId the ID of the node
     * @param sharedRiskGroupMap a map of SharedRiskGroupKey to SharedRiskGroup
     * @return true if the save operation was successful, false otherwise
     */
    boolean save(String nodeId, Map<SharedRiskGroupKey, SharedRiskGroup> sharedRiskGroupMap);

    boolean save(String nodeId, Rev250702 rev250702);

    /**
     * Read the Shared Risk Groups for a given node.
     *
     * @param nodeId the ID of the node
     * @return a map of SharedRiskGroupKey to SharedRiskGroup
     */
    Map<SharedRiskGroupKey, SharedRiskGroup> read(String nodeId);

    /**
     * Read the Shared Risk Groups for a given node.
     *
     * @param nodeId the key of the node
     * @return a map of SharedRiskGroupKey to SharedRiskGroup
     */
    Map<SharedRiskGroupKey, SharedRiskGroup> read(NetworkNodesKey nodeId);
}
