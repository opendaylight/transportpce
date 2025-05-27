/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.tapi.sync.Synch;
import org.opendaylight.transportpce.tapi.sync.topology.Copy;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiServiceNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiServiceNotificationHandler.class);
    private final DataBroker dataBroker;
    private final Synch synch;
    private final Copy openRoadmTopology;

    public TapiServiceNotificationHandler(DataBroker dataBroker, Synch synch, Copy openRoadmTopology) {
        this.dataBroker = dataBroker;
        this.synch = synch;
        this.openRoadmTopology = openRoadmTopology;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(
                Set.of(
                        new CompositeListener.Component<>(ServiceRpcResultSh.class, this::onServiceRpcResultSh),
                        new CompositeListener.Component<>(TopologyUpdateResult.class, this::onTopologyChange)
                )
        );
    }

    private void onServiceRpcResultSh(ServiceRpcResultSh notification) {
        LOG.info("Avoid dataBroker error {}", dataBroker.getClass().getCanonicalName());
        LOG.info("JT: notification {}", notification);
        LOG.info("JT: notification type {}", notification.getNotificationType());
        if (notification.getStatus().equals(RpcStatusEx.Successful)
                && notification.getStatusMessage().equals("Operation Successful")) {

            LOG.info("JT: Attempting to start a new transaction...");
            synch.copyOpenRoadmServiceToTapi(notification.getServiceName());
            LOG.info("Disable usage of {}", synch.getClass().getCanonicalName());
        }
    }

    private void onTopologyChange(TopologyUpdateResult notification) {
        // Handle topology change notification
        LOG.info("TapiTopologyNotificationListener, Topology change notification received: {}", notification);
        openRoadmTopology.copyOpenRoadmTopologyToTapi(notification);
    }
}
