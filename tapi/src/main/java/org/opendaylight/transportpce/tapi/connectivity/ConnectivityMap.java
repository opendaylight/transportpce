/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ConnectivityMap(
        List<@NonNull String> xpdrClientTplist,
        List<@NonNull String> xpdrNetworkTplist,
        List<@NonNull String> rdmAddDropTplist,
        List<@NonNull String> rdmDegTplist,
        List<@NonNull String> rdmNodelist,
        List<@NonNull String> xpdrNodelist) implements Connectivity {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMap.class);

    public ConnectivityMap(
            List<String> xpdrClientTplist,
            List<String> xpdrNetworkTplist,
            List<String> rdmAddDropTplist,
            List<String> rdmDegTplist,
            List<String> rdmNodelist,
            List<String> xpdrNodelist) {

        this.xpdrClientTplist = xpdrClientTplist;
        this.xpdrNetworkTplist = xpdrNetworkTplist;
        this.rdmAddDropTplist = rdmAddDropTplist;
        this.rdmDegTplist = rdmDegTplist;
        this.rdmNodelist = rdmNodelist;
        this.xpdrNodelist = xpdrNodelist;

        LOG.info("ROADM node list = {}", rdmNodelist);
        LOG.info("ROADM degree list = {}", rdmDegTplist);
        LOG.info("ROADM addrop list = {}", rdmAddDropTplist);
        LOG.info("XPDR node list = {}", xpdrNodelist);
        LOG.info("XPDR network list = {}", xpdrNetworkTplist);
        LOG.info("XPDR client list = {}", xpdrClientTplist);
    }
}
