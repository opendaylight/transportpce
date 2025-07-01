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
import org.opendaylight.transportpce.tapi.openroadm.service.Service;
import org.opendaylight.transportpce.tapi.openroadm.topology.Topology;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkmodel.rev201116.TopologyUpdateResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ServiceNotificationTypes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiServiceNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiServiceNotificationHandler.class);
    private final DataBroker dataBroker;
    private final Service openRoadmService;
    private final Topology openRoadmTopology;

    public TapiServiceNotificationHandler(DataBroker dataBroker, Service openRoadmService, Topology openRoadmTopology) {
        this.dataBroker = dataBroker;
        this.openRoadmService = openRoadmService;
        this.openRoadmTopology = openRoadmTopology;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
                new CompositeListener.Component<>(ServiceRpcResultSh.class, this::onServiceRpcResultSh),
                new CompositeListener.Component<>(TopologyUpdateResult.class, this::onTopologyChange)));
    }

    private void onServiceRpcResultSh(ServiceRpcResultSh notification) {
        LOG.info("Avoid dataBroker error {}", dataBroker.getClass().getCanonicalName());
        LOG.debug("Received notification: {}", notification);
        if (notification.getStatus().equals(RpcStatusEx.Successful)
                && notification.getNotificationType().equals(ServiceNotificationTypes.ServiceCreateResult)) {
            openRoadmService.copyServiceToTAPI(notification.getServiceName());
        }
    }

    private void onTopologyChange(TopologyUpdateResult notification) {
        // Handle topology change notification
        LOG.info("TapiTopologyNotificationListener, Topology change notification received: {}", notification);
        openRoadmTopology.copyTopologyToTAPI(notification);
    }
}
