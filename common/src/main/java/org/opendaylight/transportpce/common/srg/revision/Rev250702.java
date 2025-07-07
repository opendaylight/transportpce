/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.revision;

import java.util.Map;

public interface Rev250702 {

    /**
     * SRG in Rev250702.
     *
     * @return a map of SharedRiskGroup in Rev250702 format
     */
    Map<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.srg
            .rev250702.network.network.nodes.SharedRiskGroupKey, org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.srg.rev250702.network.network.nodes.SharedRiskGroup> srg();

}
