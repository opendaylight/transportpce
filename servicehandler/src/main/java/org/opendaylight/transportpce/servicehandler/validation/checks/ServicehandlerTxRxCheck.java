/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for checking missing info on Tx/Rx for A/Z end.
 *
 */
public final class ServicehandlerTxRxCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerTxRxCheck.class);

    /**
     * Check if a String is not null and not equal to ''.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    /**
     * check if Port info is compliant.
     *
     * @param port
     *            port info
     * @return true if String ok false if not
     */
    public static boolean checkPort(Port port) {
        boolean result = false;
        if (port != null) {
            String portDeviceName = port.getPortDeviceName();
            String portType = port.getPortType();
            String portName = port.getPortName();
            String portRack = port.getPortRack();
            String portShelf = port.getPortShelf();

            if (checkString(portDeviceName) && checkString(portType) && checkString(portName) && checkString(portRack)
                    && checkString(portShelf)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Check if lgx info is compliant.
     *
     * @param lgx
     *            lgx info
     * @return true if String ok false if not
     */
    public static boolean checkLgx(Lgx lgx) {
        boolean result = false;
        if (lgx != null) {
            String lgxDeviceName = lgx.getLgxDeviceName();
            String lgxPortName = lgx.getLgxPortName();
            String lgxPortRack = lgx.getLgxPortRack();
            String lgxPortShelf = lgx.getLgxPortShelf();
            if (checkString(lgxDeviceName) && checkString(lgxPortName) && checkString(lgxPortRack)
                    && checkString(lgxPortShelf)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Check if Tx/Rx Direction complaincy info.
     *
     * @param txDirection
     *            TxDirection
     * @param rxDirection
     *            RxDirection
     *
     * @return <code>true</code> if check is ok <code>false</code> else
     */
    public static ComplianceCheckResult checkTxOrRxInfo(TxDirection txDirection, RxDirection rxDirection) {
        boolean result = true;
        String message = "";
        if (txDirection != null) {
            if (!checkPort(txDirection.getPort())) {
                result = false;
                message = "Service TxDirection Port is not correctly set";
            } else if (!checkLgx(txDirection.getLgx())) {
                result = false;
                message = "Service TxDirection Lgx is not correctly set";
            } else if (rxDirection != null) {
                if (!checkPort(rxDirection.getPort())) {
                    result = false;
                    message = "Service RxDirection Port is not correctly set";
                } else if (!checkLgx(rxDirection.getLgx())) {
                    result = false;
                    message = "Service RxDirection Lgx is not correctly set";
                }
            } else {
                result = false;
                message = "Service RxDirection is not correctly set";
            }
        } else {
            result = false;
            message = "Service TxDirection is not correctly set";
        }
        return new ComplianceCheckResult(result, message);
    }

    /**
     * Check Compliancy of Service TxRx info.
     * @param serviceEnd Service Endpoint
     * @param endpointType Endpoint type
     *
     * @return true if String ok false if not
     */
    public static ComplianceCheckResult check(ServiceEndpoint serviceEnd, ServiceEndpointType endpointType) {
        boolean result = true;
        String message = "";
        if (serviceEnd != null) {
            try {
                Long serviceRate = serviceEnd.getServiceRate();
                ServiceFormat serviceformat = serviceEnd.getServiceFormat();
                String clli = serviceEnd.getClli();
                if ((serviceRate == null) || (serviceRate <= 0)) {
                    result = false;
                    message = "Service " + endpointType + " rate is not set";
                    LOG.debug(message);
                } else if (serviceformat == null) {
                    result = false;
                    message = "Service " + endpointType + " format is not set";
                    LOG.debug(message);
                } else if (!checkString(clli)) {
                    result = false;
                    message = "Service" + endpointType + " clli format is not set";
                    LOG.debug(message);
                } else {
                    ComplianceCheckResult complianceCheckResult
                            = checkTxOrRxInfo(serviceEnd.getTxDirection(), serviceEnd.getRxDirection());
                    if (!complianceCheckResult.hasPassed()) {
                        result = false;
                        message = complianceCheckResult.getMessage();
                    }
                }
            } catch (NullPointerException e) {
                message = "Service " + endpointType + " rate, format or clli is not set";
                LOG.error(message, e);
                return new ComplianceCheckResult(false, message);
            }
        } else {
            result = false;
            message = endpointType + " is not set";
            LOG.debug(message);
        }
        return new ComplianceCheckResult(result, message);
    }

    private ServicehandlerTxRxCheck() {
    }

}
