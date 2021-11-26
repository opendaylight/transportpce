/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constraints.rev211210.routing.constraints.SoftConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to check coherency between hard and soft constraints.
 *
 */
public final class CheckCoherencyHardSoft {

    private static final Logger LOG = LoggerFactory.getLogger(CheckCoherencyHardSoft.class);

    /**
     * function to check coherency between hard and soft constraints.
     * @param hard Hard Constraints
     * @param soft Soft Constraints
     *
     * @return  <code> true </code>  if coherent
     *          <code> false </code> else
     */
    public static boolean check(HardConstraints hard, SoftConstraints soft) {
        boolean result = false;
        if (hard != null && soft != null) {
            /*
             * Check coherency with hard/soft constraints
             * hard/soft include/exclude coherency
             *
             */
        } else {
            LOG.info("HardConstraints or/and  SoftConstraints is null !");
            result = true;
        }
        return result;
    }

    private CheckCoherencyHardSoft() {
    }

}
