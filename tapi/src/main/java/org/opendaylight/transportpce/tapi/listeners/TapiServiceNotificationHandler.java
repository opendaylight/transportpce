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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiServiceNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiServiceNotificationHandler.class);
    private final DataBroker dataBroker;
    private final Synch synch;

    public TapiServiceNotificationHandler(DataBroker dataBroker, Synch synch) {
        this.dataBroker = dataBroker;
        this.synch = synch;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(ServiceRpcResultSh.class, this::onServiceRpcResultSh)));
    }

    private void onServiceRpcResultSh(ServiceRpcResultSh notification) {
        LOG.info("Avoid dataBroker error {}", dataBroker.getClass().getCanonicalName());
        if (notification.getStatus().equals(RpcStatusEx.Successful)) {
            synch.copyOpenRoadmServiceToTapi(notification.getServiceName());
        }
    }
}
