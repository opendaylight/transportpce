/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.listener;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiRendererRpcResultSp;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiSbiRendererNotificationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiRendererNotificationHandler.class);
    private Uuid serviceUuid;
    private TapiSbiRendererRpcResultSp serviceRpcResultSp;

    public TapiSbiRendererNotificationHandler() {
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(TapiSbiRendererRpcResultSp.class, this::onTapiSbiRendererRpcResultSp)));
    }

    private void onTapiSbiRendererRpcResultSp(TapiSbiRendererRpcResultSp notification) {
        if (compareServiceRpcResultSp(notification)) {
            LOG.warn("ServiceRpcResultSp already wired !");
            return;
        }
        serviceRpcResultSp = notification;
        int notifType = serviceRpcResultSp.getNotificationType().getIntValue();
        LOG.info("Renderer '{}' Notification received : {}", serviceRpcResultSp.getNotificationType().getName(),
                notification);
        /* service-implementation-request. */
        if (notifType == 3) {
            onServiceImplementationResult(notification);
        }
    }

    /**
     * Process service implementation result for serviceName.
     * @param notification RendererRpcResultSp
     */
    private void onServiceImplementationResult(TapiSbiRendererRpcResultSp notification) {
        switch (serviceRpcResultSp.getStatus()) {
            case Successful:
                if (this.serviceUuid != null) {
                    onSuccededServiceImplementation();
                }
                break;
            case Failed:
                onFailedServiceImplementation(notification.getServiceName());
                break;
            case  Pending:
                LOG.warn("Tapi SBI Service Implementation still pending according to RpcStatusEx");
                break;
            default:
                LOG.warn("Tapi SBI Service Implementation has an unknown RpcStatusEx code");
                break;
        }
    }

    /**
     * Process succeeded service implementation for service.
     */
    private void onSuccededServiceImplementation() {
        // TODO: implement this method, notably sending all required notification to trigger the rest of the service
        // (broken down service) creation.
    }

    /**
     * Process failed service implementation for serviceName.
     * @param serviceName String
     */
    private void onFailedServiceImplementation(String serviceName) {
        // TODO: implement this method, notably sending all required notification to block the rest of the service
        // (broken down service) creation.
    }

    @SuppressFBWarnings(
            value = "ES_COMPARING_STRINGS_WITH_EQ",
            justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServiceRpcResultSp(TapiSbiRendererRpcResultSp notification) {
        if (serviceRpcResultSp == null) {
            return false;
        }
        if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
            return false;
        }
        return serviceRpcResultSp.getStatusMessage() == notification.getStatusMessage();
    }

    public void setServiceUuid(Uuid serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
}
