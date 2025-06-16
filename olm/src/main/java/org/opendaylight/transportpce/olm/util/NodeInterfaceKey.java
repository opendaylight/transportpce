/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;
import org.opendaylight.yangtools.binding.lib.CodeHelpers;

/*
 * This class represents a key for a logical connection point on a node.
 * It should be viewed as a key composed of the id of a node, and the ID of the logical connection point.
 */
public record NodeInterfaceKey(String nodeId, String logicalConnectionPoint) {

    @Override
    public boolean equals(Object key) {
        if (this == key) {
            return true;
        }
        if (key instanceof NodeInterfaceKey(String id, String connectionPoint)) {
            return this.logicalConnectionPoint.equals(connectionPoint)
                    && this.nodeId.equals(id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return CodeHelpers.wrapperHashCode(nodeId + logicalConnectionPoint);
    }
}
