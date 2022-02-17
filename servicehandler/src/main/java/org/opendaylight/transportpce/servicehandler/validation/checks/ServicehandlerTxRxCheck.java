/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.transportpce.servicehandler.ServiceEndpointType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;

/**
 * Class for checking missing info on Tx/Rx for A/Z end.
 *
 */
public final class ServicehandlerTxRxCheck {

    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        private static final String SERVICE = "Service ";
        public static final String TXDIR_NOT_SET;
        public static final String TXDIR_PORT_NOT_SET;
        public static final String TXDIR_LGX_NOT_SET;
        public static final String RXDIR_NOT_SET;
        public static final String RXDIR_PORT_NOT_SET;
        public static final String RXDIR_LGX_NOT_SET;

        // Static blocks are generated once and spare memory.
        static {
            TXDIR_NOT_SET = "Service TxDirection is not correctly set";
            RXDIR_NOT_SET = "Service RxDirection is not correctly set";
            TXDIR_PORT_NOT_SET = "Service TxDirection Port is not correctly set";
            TXDIR_LGX_NOT_SET = "Service TxDirection Lgx is not correctly set";
            RXDIR_PORT_NOT_SET = "Service RxDirection Port is not correctly set";
            RXDIR_LGX_NOT_SET = "Service RxDirection Lgx is not correctly set";
        }

        public static String endpointTypeNotSet(ServiceEndpointType endpointType) {
            return SERVICE + endpointType + " is not set";
        }

        public static String rateNotSet(ServiceEndpointType endpointType) {
            return SERVICE + endpointType + " rate is not set";
        }

        public static String formatNotSet(ServiceEndpointType endpointType) {
            return SERVICE + endpointType + " format is not set";
        }

        public static String clliNotSet(ServiceEndpointType endpointType) {
            return SERVICE + endpointType + " clli is not set";
        }

        private LogMessages() {
        }
    }


    /**
     * Check if a String is not null and not equal to ''.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public static boolean checkString(String value) {
        return (value != null && !value.isEmpty());
    }

    /**
     * check if Port info is compliant.
     *
     * @param port
     *            port info
     * @return true if String ok false if not
     */
    @SuppressWarnings("java:S1067")
    //sonar issue Reduce the number of conditional operators (4) used in the expression (maximum allowed 3)
    //won't be fixed because of functional checks needed
    public static boolean checkPort(Port port) {
        if (port == null) {
            return false;
        }
        String portDeviceName = port.getPortDeviceName();
        String portType = port.getPortType();
        String portName = port.getPortName();
        String portRack = port.getPortRack();
        String portShelf = port.getPortShelf();

        return checkString(portDeviceName)
                && checkString(portType)
                && checkString(portName)
                && checkString(portRack)
                && checkString(portShelf);
    }

    /**
     * Check if lgx info is compliant.
     *
     * @param lgx
     *            lgx info
     * @return true if String ok false if not
     */
    public static boolean checkLgx(Lgx lgx) {
        if (lgx == null) {
            return false;
        }
        String lgxDeviceName = lgx.getLgxDeviceName();
        String lgxPortName = lgx.getLgxPortName();
        String lgxPortRack = lgx.getLgxPortRack();
        String lgxPortShelf = lgx.getLgxPortShelf();
        return checkString(lgxDeviceName)
                && checkString(lgxPortName)
                && checkString(lgxPortRack)
                && checkString(lgxPortShelf);
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
        if (txDirection == null) {
            return new ComplianceCheckResult(false, LogMessages.TXDIR_NOT_SET);
        }
        if (rxDirection == null) {
            return new ComplianceCheckResult(false, LogMessages.RXDIR_NOT_SET);
        }
// TODO : as soon as all leaves under port and lgx are not mandatory, these checks seems to be not relevant at all
//        if (!checkPort(txDirection.getPort())) {
//            return new ComplianceCheckResult(false, LogMessages.TXDIR_PORT_NOT_SET);
//        }
//        if (!checkLgx(txDirection.getLgx())) {
//            return new ComplianceCheckResult(false, LogMessages.TXDIR_LGX_NOT_SET);
//        }
//        if (!checkPort(rxDirection.getPort())) {
//            return new ComplianceCheckResult(false, LogMessages.RXDIR_PORT_NOT_SET);
//        }
//        if (!checkLgx(rxDirection.getLgx())) {
//            return new ComplianceCheckResult(false, LogMessages.RXDIR_LGX_NOT_SET);
//        }
        return new ComplianceCheckResult(true, "");
    }

    /**
     * Check Compliance of Service TxRx info.
     * @param serviceEnd Service Endpoint
     * @param endpointType Endpoint type
     *
     * @return true if String ok false if not
     */
    public static ComplianceCheckResult check(ServiceEndpoint serviceEnd, ServiceEndpointType endpointType) {
        if (serviceEnd == null) {
            return new ComplianceCheckResult(false, LogMessages.endpointTypeNotSet(endpointType));
        }

        if (serviceEnd.getServiceRate() == null) {
            String message = "Something wrong when accessing Service " + endpointType + " rate, format or clli";
            return new ComplianceCheckResult(false, message);
        }
        Long serviceRate = serviceEnd.getServiceRate().toJava();
        ServiceFormat serviceformat = serviceEnd.getServiceFormat();
        String clli = serviceEnd.getClli();
        if (serviceRate <= 0) {
            return new ComplianceCheckResult(false, LogMessages.rateNotSet(endpointType));
        }
        if (serviceformat == null) {
            return new ComplianceCheckResult(false, LogMessages.formatNotSet(endpointType));
        }
        if (!checkString(clli)) {
            return new ComplianceCheckResult(false, LogMessages.clliNotSet(endpointType));
        }

        ComplianceCheckResult complianceCheckResult
                = checkTxOrRxInfo(serviceEnd.getTxDirection(), serviceEnd.getRxDirection());
        if (!complianceCheckResult.hasPassed()) {
            return new ComplianceCheckResult(false, complianceCheckResult.getMessage());
        }

        return new ComplianceCheckResult(true, "");
    }

    private ServicehandlerTxRxCheck() {
    }

}
