/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.storage;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;

public interface Storage {

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
    Map<SharedRiskGroupKey, SharedRiskGroup> read(NodesKey nodeId);

}
