/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.TransportpcePceListener;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class PceListenerImpl implements TransportpcePceListener {

    private static final Logger LOG = LoggerFactory.getLogger(PceListenerImpl.class);

    private ServicePathRpcResult servicePathRpcResult = null;

    @Override
    public void onServicePathRpcResult(ServicePathRpcResult notification) {
        if (!compareServicePathRpcResult(notification)) {
            servicePathRpcResult = notification;
            StringBuilder build = new StringBuilder();
            build.append(
                    "Received '" + notification.getNotificationType() + "' StubPce notification " + "from service '"
                            + notification.getServiceName() + "' " + "with status '" + notification.getStatus() + "'");
            build.append(" with StatusMessage '" + notification.getStatusMessage() + "'");
            if ((notification.getStatus() == RpcStatusEx.Successful) && (notification.getNotificationType()
                    .getIntValue() == ServicePathNotificationTypes.PathComputationRequest.getIntValue())) {
                build.append(" PathDescription : " + notification.getPathDescription().toString());
                /*
                 * switch (action.getIntValue()) { case 1: //service-create case
                 * 3: //service-delete case 8: //service-reconfigure case 9:
                 * //service-restoration case 10://service-reversion case
                 * 11://service-reroute break;
                 *
                 * default: break; }
                 */
            }

            LOG.info(build.toString());
        } else {
            LOG.info("ServicePathRpcResult already wired !");
        }
    }

    @Deprecated
    private Boolean compareServicePathRpcResult(ServicePathRpcResult notification) {
        Boolean result = true;
        if (servicePathRpcResult == null) {
            result = false;
        } else {
            if (servicePathRpcResult.getNotificationType() != notification.getNotificationType()) {
                result = false;
            }
            if (servicePathRpcResult.getServiceName() != notification.getServiceName()) {
                result = false;
            }
            if (servicePathRpcResult.getStatus() != notification.getStatus()) {
                result = false;
            }
            if (servicePathRpcResult.getStatusMessage() != notification.getStatusMessage()) {
                result = false;
            }
        }
        return result;
    }

}
