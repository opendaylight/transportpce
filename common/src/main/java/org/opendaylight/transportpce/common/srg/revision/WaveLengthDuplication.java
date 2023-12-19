/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.srg.revision;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.shared.risk.group.SharedRiskGroupKey;

/**
 * This interface is primarily intended to define methods implemented
 * by a translator/converter between SRG information from a device and
 * port mapping used by TPCE.
 *
 */
public interface WaveLengthDuplication {

    /**
     * SRG in Rev250905.
     *
     * @return a map of SharedRiskGroup in Rev250905 format
     */
    Map<SharedRiskGroupKey, SharedRiskGroup> srg();

}
