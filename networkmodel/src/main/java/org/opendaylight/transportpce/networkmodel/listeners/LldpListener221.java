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
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.LldpNbrInfoChange;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceNotificationType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the org.opendaylight.yang.gen.v1.http.org.openroadm.lldp.rev181019.LldpNbrInfoChange
 * notification.
 * This implementation is dedicated to yang model 2.2.1 revision.
 */
public class LldpListener221 implements Listener<LldpNbrInfoChange> {

    private static final Logger LOG = LoggerFactory.getLogger(LldpListener221.class);
    private final R2RLinkDiscovery linkDiscovery;
    private final NodeId nodeId;

    /**
     * Create instance of the device listener.
     *
     * @param linkDiscovery Object representing the ROADM-to-ROADM WDM link
     * @param nodeId Node name
     */
    public LldpListener221(final R2RLinkDiscovery linkDiscovery, final String nodeId) {
        this.linkDiscovery = linkDiscovery;
        this.nodeId = new NodeId(nodeId);
    }

    /**
     * Callback for lldp-nbr-info-change.
     * @param notification LldpNbrInfoChange object
     */
    @Override
    public void onNotification(@NonNull LldpNbrInfoChange notification) {
        LOG.info("Notification {} received {}", LldpNbrInfoChange.QNAME, notification);
        if (notification.getNotificationType().equals(ResourceNotificationType.ResourceCreation)) {
            linkDiscovery.createR2RLink(nodeId,notification.getResource(),
                                                notification.getNbrInfo().getRemoteSysName(),
                                                notification.getNbrInfo().getRemotePortId());
        } else if (notification.getNotificationType().equals(ResourceNotificationType.ResourceDeletion)) {
            linkDiscovery.deleteR2RLink(nodeId,notification.getResource(),
                                                notification.getNbrInfo().getRemoteSysName(),
                                                notification.getNbrInfo().getRemotePortId());
        }
    }
}
