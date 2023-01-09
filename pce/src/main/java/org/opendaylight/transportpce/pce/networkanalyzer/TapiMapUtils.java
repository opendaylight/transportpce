/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Link;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TapiMapUtils {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(TapiMapUtils.class);

    private TapiMapUtils() {
    }

    public static Set<String> getSRLG(Link link) {
        // List<RiskCharacteristic> rc = new ArrayList<>();
        Set<String> riskIdList;
        riskIdList = link.getRiskCharacteristic().values().stream()
            .filter(rc -> rc.getRiskCharacteristicName().equalsIgnoreCase("SRLG")).findFirst().orElseThrow()
            .getRiskIdentifierList();
        if (riskIdList == null) {
            LOG.debug("TapiMapUtils : No SRLG available in the risk-characteristic of the link {} named {}",
                link.getUuid().toString(), link.getName().toString());
        }
        return riskIdList;
    }

    public static Double getAvailableBandwidth(Link link) {
        if (link.getAvailableCapacity().getTotalSize().getValue() == null) {
            LOG.warn("TapiMapUtils: no Available Bandwidth available for link {{} named {}", link.getUuid().toString(),
                link.getName().toString());
            return null;
        }
        return link.getAvailableCapacity().getTotalSize().getValue().doubleValue();
    }

    public static Double getUsedBandwidth(Link link) {
        if (link.getTotalPotentialCapacity().getTotalSize().getValue() == null
            || link.getAvailableCapacity().getTotalSize().getValue() == null) {
            LOG.warn("TapiMapUtils: incomplete Bandwidth information for link {{} named {}", link.getUuid().toString(),
                link.getName().toString());
            return null;
        }
        return ((link.getTotalPotentialCapacity().getTotalSize().getValue().doubleValue()
            - link.getAvailableCapacity().getTotalSize().getValue().doubleValue()));
    }

    public static Uuid extractOppositeLink(Link link) {
        Uuid linkOppositeUuid = null;
        LOG.error("TapiMapUtils: no way to extract opposite link outside the generic analyseNW code, return null");
        // TODO : try to build this method in the absence of this kind of
        // parameter in
        // T-API!!!!!
        return linkOppositeUuid;
    }

}
