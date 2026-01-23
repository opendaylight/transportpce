/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping;


import java.util.Set;

public record TerminationPointMapping(
        TerminationPointId terminationPointId,
        Set<OwnedNodeEdgePointName> nodeEdgePointNames) {
}
