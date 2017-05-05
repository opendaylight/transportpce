/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Class to check coherency between hard & sof constraints.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class CheckCoherencyHardSoft {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(CheckCoherencyHardSoft.class);
    /* Hard Constraints. */
    private HardConstraints hard;
    /* Soft Constraints. */
    private SoftConstraints soft;

    public CheckCoherencyHardSoft(HardConstraints hard, SoftConstraints soft) {
        this.hard = hard;
        this.soft = soft;
    }

    /*function to check coherency between hard & soft constraints.
     * @return  <code> true </code>  if coherent
     *          <code> false </code> else
     */
    public boolean check() {
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

}
