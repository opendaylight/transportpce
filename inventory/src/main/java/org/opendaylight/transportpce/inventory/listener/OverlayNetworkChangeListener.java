/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.listener;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlayNetworkChangeListener implements DataTreeChangeListener<Network> {

    private static final Logger LOG = LoggerFactory.getLogger(OverlayNetworkChangeListener.class);

    @Override
    public void onDataTreeChanged(List<DataTreeModification<Network>> changes) {
        LOG.info("Overlay network changed {}", changes);
    }
}
