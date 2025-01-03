/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.NotificationService.Listener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.LldpNbrInfoChange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev161014.LldpNbrInfoChange
 * notification.
 */
public class LldpListener implements Listener<LldpNbrInfoChange> {

    private static final Logger LOG = LoggerFactory.getLogger(LldpListener.class);
    private final NodeId nodeId;

    /**
     * Create instance of the device listener.
     *
     * @param nodeId Node name
     */
    public LldpListener(final String nodeId) {
        this.nodeId = new NodeId(nodeId);
    }

    /**
     * Callback for lldp-nbr-info-change.
     * @param notification LldpNbrInfoChange object
     */
    @Override
    public void onNotification(@NonNull LldpNbrInfoChange notification) {
        LOG.info("Notification {} received {} on node {}", LldpNbrInfoChange.QNAME, notification, nodeId);
    }
}
