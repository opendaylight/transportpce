/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.listener;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnderlayNetworkChangeListener implements DataTreeChangeListener<Network> {

    private static final Logger LOG = LoggerFactory.getLogger(UnderlayNetworkChangeListener.class);

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Network>> changes) {
        LOG.info("Underlay network changed {}", changes);
    }

}
