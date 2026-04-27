/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.format;

public interface LinkIdFormatter {
    String linkId(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId);
}
