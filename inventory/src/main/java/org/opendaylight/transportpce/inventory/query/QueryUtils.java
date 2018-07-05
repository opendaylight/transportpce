/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.query;

import static org.opendaylight.transportpce.inventory.utils.StringUtils.getCurrentTimestamp;

import com.google.common.base.Strings;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.Router;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;

/**
 * Query manipulation class.
 */
public final class QueryUtils {

    private QueryUtils() {
        // no instance, just static access
    }

    /**
     * This method modifies the prepared statement for {@link Queries}.
     *
     * @param stmt the prepared statement
     * @param servicePath the service path
     * @throws SQLException a SQL exception
     */
    public static void setCreateServiceParameters(PreparedStatement stmt, Services servicePath) throws SQLException {
        StatementBuilder builder = StatementBuilder.builder(stmt);
        builder.setParameter(servicePath.getServiceName());
        builder.setParameter(servicePath.getCommonId());

        String requestId = "";
        String rpcAction = "";
        String notificationUrl = "";
        String requestSystemId = "";

        if (servicePath.getSdncRequestHeader() != null) {
            requestId = getStringOf(servicePath.getSdncRequestHeader().getRequestId());
            rpcAction = servicePath.getSdncRequestHeader().getRpcAction().getName();
            notificationUrl = servicePath.getSdncRequestHeader().getNotificationUrl();
            requestSystemId = servicePath.getSdncRequestHeader().getRequestSystemId();
        }
        builder.setParameter(requestId).setParameter(rpcAction).setParameter(notificationUrl)
                .setParameter(requestSystemId);

        String connectionType = "";
        String lifecycleState = "";
        String administrativeState = "";
        String condition = "";
        if (servicePath.getConnectionType() != null) {
            connectionType = servicePath.getConnectionType().getName();
        }
        if (servicePath.getLifecycleState() != null) {
            lifecycleState = servicePath.getLifecycleState().getName();
        }
        if (servicePath.getAdministrativeState() != null) {
            administrativeState = servicePath.getAdministrativeState().getName();
        }
        if (servicePath.getCondition() != null) {
            condition = servicePath.getCondition().getName();
        }
        // status stuff
        builder.setParameter(connectionType);
        builder.setParameter(lifecycleState);
        builder.setParameter(administrativeState);
        builder.setParameter(condition);

        // status stuff
        ServiceAEnd serviceAEnd = servicePath.getServiceAEnd();
        builder.setParameter(serviceAEnd.getServiceFormat().getName()).setParameter(serviceAEnd.getServiceRate())
                .setParameter(serviceAEnd.getClli()).setParameter(serviceAEnd.getNodeId());

        // tx port stuff
        Port aendTxDirection = servicePath.getServiceAEnd().getTxDirection().getPort();
        builder.setParameter(aendTxDirection.getPortDeviceName()).setParameter(aendTxDirection.getPortName())
                .setParameter(aendTxDirection.getPortRack()).setParameter(aendTxDirection.getPortShelf())
                .setParameter(aendTxDirection.getPortSlot()).setParameter(aendTxDirection.getPortSubSlot());

        // tx lgx stuff
        TxDirection txDirectionA = servicePath.getServiceAEnd().getTxDirection();
        builder.setParameter(txDirectionA.getLgx().getLgxDeviceName())
                .setParameter(txDirectionA.getLgx().getLgxPortName())
                .setParameter(txDirectionA.getLgx().getLgxPortRack())
                .setParameter(txDirectionA.getLgx().getLgxPortShelf());
        builder.setParameter(txDirectionA.getTail().getTailRoadm().getNodeId());
        builder.setParameter(txDirectionA.getTail().getXponderPort().getCircuitPackName());
        builder.setParameter(txDirectionA.getTail().getXponderPort().getPortName());
        builder.setParameter(txDirectionA.getTail().getTailRoadmPortAid());
        builder.setParameter(txDirectionA.getTail().getTailRoadmPortRackLocation());

        // rx lgx stuff
        RxDirection rxDirectionA = servicePath.getServiceAEnd().getRxDirection();
        builder.setParameter(rxDirectionA.getLgx().getLgxDeviceName())
                .setParameter(rxDirectionA.getLgx().getLgxPortName())
                .setParameter(rxDirectionA.getLgx().getLgxPortRack())
                .setParameter(rxDirectionA.getLgx().getLgxPortShelf());
        builder.setParameter(rxDirectionA.getTail().getTailRoadm().getNodeId());
        builder.setParameter(rxDirectionA.getTail().getXponderPort().getCircuitPackName());
        builder.setParameter(rxDirectionA.getTail().getXponderPort().getPortName());
        builder.setParameter(rxDirectionA.getTail().getTailRoadmPortAid());
        builder.setParameter(rxDirectionA.getTail().getTailRoadmPortRackLocation());

        builder.setParameter(servicePath.getServiceAEnd().getOpticType().getName());

        Router routerA = servicePath.getServiceAEnd().getRouter();
        builder.setParameter(routerA.getNodeId()).setParameter(routerA.getIpAddress().toString())
                .setParameter(routerA.getUrl());
        builder.setParameter(servicePath.getServiceAEnd().getUserLabel());

        ServiceZEnd serviceZEnd = servicePath.getServiceZEnd();
        builder.setParameter(serviceZEnd.getServiceFormat().getName()).setParameter(serviceZEnd.getServiceRate())
                .setParameter(serviceZEnd.getClli()).setParameter(serviceZEnd.getNodeId());

        // tx port stuff
        Port zendTxDirection = serviceZEnd.getTxDirection().getPort();
        builder.setParameter(zendTxDirection.getPortDeviceName()).setParameter(zendTxDirection.getPortName())
                .setParameter(zendTxDirection.getPortRack()).setParameter(zendTxDirection.getPortShelf())
                .setParameter(zendTxDirection.getPortSlot()).setParameter(zendTxDirection.getPortSubSlot());

        // tx lgx stuff
        TxDirection txDirectionZ = serviceZEnd.getTxDirection();
        builder.setParameter(txDirectionZ.getLgx().getLgxDeviceName())
                .setParameter(txDirectionZ.getLgx().getLgxPortName())
                .setParameter(txDirectionZ.getLgx().getLgxPortRack())
                .setParameter(txDirectionZ.getLgx().getLgxPortShelf());
        builder.setParameter(txDirectionZ.getTail().getTailRoadm().getNodeId());
        builder.setParameter(txDirectionZ.getTail().getXponderPort().getCircuitPackName());
        builder.setParameter(txDirectionZ.getTail().getXponderPort().getPortName());
        builder.setParameter(txDirectionZ.getTail().getTailRoadmPortAid());
        builder.setParameter(txDirectionZ.getTail().getTailRoadmPortRackLocation());

        // rx lgx stuff
        RxDirection rxDirectionZ = servicePath.getServiceAEnd().getRxDirection();
        builder.setParameter(rxDirectionZ.getLgx().getLgxDeviceName())
                .setParameter(rxDirectionZ.getLgx().getLgxPortName())
                .setParameter(rxDirectionZ.getLgx().getLgxPortRack())
                .setParameter(rxDirectionZ.getLgx().getLgxPortShelf());
        builder.setParameter(rxDirectionZ.getTail().getTailRoadm().getNodeId());
        builder.setParameter(rxDirectionZ.getTail().getXponderPort().getCircuitPackName());
        builder.setParameter(rxDirectionZ.getTail().getXponderPort().getPortName());
        builder.setParameter(rxDirectionZ.getTail().getTailRoadmPortAid());
        builder.setParameter(rxDirectionZ.getTail().getTailRoadmPortRackLocation());

        builder.setParameter(servicePath.getServiceAEnd().getOpticType().getName());

        Router routerZ = servicePath.getServiceZEnd().getRouter();
        builder.setParameter(routerZ.getNodeId()).setParameter(routerZ.getIpAddress().toString())
                .setParameter(routerZ.getUrl());
        builder.setParameter(servicePath.getServiceZEnd().getUserLabel());

        String customerCode = "";
        if ((servicePath.getHardConstraints().getCustomerCode() == null)
                || servicePath.getHardConstraints().getCustomerCode().isEmpty()) {
            customerCode = servicePath.getHardConstraints().getCustomerCode().iterator().next();
        }
        builder.setParameter(customerCode);
        builder.setParameter(servicePath.getDueDate().getValue());
        builder.setParameter(servicePath.getEndDate().getValue());
        builder.setParameter(servicePath.getNcCode());
        builder.setParameter(servicePath.getNciCode());
        builder.setParameter(servicePath.getSecondaryNciCode());
        builder.setParameter(servicePath.getCustomer());
        builder.setParameter(servicePath.getCustomerContact());
        builder.setParameter(servicePath.getOperatorContact());
        builder.setParameter(servicePath.getLatency());
        String fiberSpanSrlgs = "";
        if ((servicePath.getFiberSpanSrlgs() == null) || servicePath.getFiberSpanSrlgs().isEmpty()) {
            fiberSpanSrlgs = servicePath.getFiberSpanSrlgs().iterator().next();
        }
        String supportingServiceName = "";
        if ((servicePath.getSupportingServiceName() == null) || servicePath.getSupportingServiceName().isEmpty()) {
            supportingServiceName = servicePath.getSupportingServiceName().iterator().next();
        }
        // TODO: hard constraints and soft constraints are missing
        builder.setParameter(fiberSpanSrlgs);
        builder.setParameter(supportingServiceName);
        builder.setParameter(getCurrentTimestamp());
        builder.setParameter(getCurrentTimestamp());
    }

    /**
     * If the input value is null or empty string returns an empty string otherwise
     * its value.
     *
     * @param value a value potentially NULL
     * @return String the string value potentially empty if value is NULL
     */
    private static String getStringOf(String value) {
        return Strings.isNullOrEmpty(value) ? "" : value;
    }
}
