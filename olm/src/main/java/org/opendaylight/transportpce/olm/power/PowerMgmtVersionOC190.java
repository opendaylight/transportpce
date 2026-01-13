/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import java.util.Map;
import org.opendaylight.transportpce.common.catalog.CatalogUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PowerMgmtVersionOC190 {
    private static final Logger LOG = LoggerFactory.getLogger(PowerMgmtVersionOC190.class);

    private PowerMgmtVersionOC190() {
    }

    /**
     * This method gets the MinTx and MaxTx output power.
     *
     * @param operationalMode opmode from which the power should be considered
     * @param networkTransactionService networkTranscation
     * @return Map with MinTx and MaxTx.
     */
    public static Map<String, Double> getXponderPowerRange(String operationalMode,
            NetworkTransactionService networkTransactionService) {
        CatalogUtils catalogUtils = new CatalogUtils(networkTransactionService);
        return catalogUtils.getMinMaxOutputPower(operationalMode);
    }
}

