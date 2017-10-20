/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.ServiceEndpointSp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for checking missing info on Tx/Rx for A/Z end.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 */
public class StubpceTxRxCheck {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubpceTxRxCheck.class);
    /** ServiceEndpoint. */
    private ServiceEndpointSp serviceEnd;
    /** Response message from procedure. */
    private String message;
    /** type serviceEndpoint : serviceAEnd / serviceZEnd. */
    private String service = null;

    /**
     * ServicehandlerTxRxCheck class constructor.
     *
     * @param endPoint
     *            ServiceEndpoint
     * @param value
     *            Integer to define ServiceAEND/ZEND
     */
    public StubpceTxRxCheck(ServiceEndpointSp endPoint, int value) {
        this.serviceEnd = endPoint;
        this.setMessage("");
        if (value > 0) {
            service = MyEndpoint.forValue(value).name();
        }
    }

    /**
     * Check if a String is not null and not equal to ''.
     *
     * @param value
     *            String value
     * @return true if String ok false if not
     */
    public Boolean checkString(String value) {
        Boolean result = false;
        if (value != null && value.compareTo("") != 0) {
            result = true;
        }
        return result;

    }

    /**
     * check if Port info is compliant.
     *
     * @param port
     *            port info
     * @return true if String ok false if not
     */
    public Boolean checkPort(Port port) {
        Boolean result = false;
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
    public Boolean checkLgx(Lgx lgx) {
        Boolean result = false;
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
    public boolean checkTxOrRxInfo(TxDirection txDirection, RxDirection rxDirection) {
        Boolean result = true;
        if (txDirection != null) {
            if (!checkPort(txDirection.getPort())) {
                result = false;
                message = "Service TxDirection Port is not correctly set";
            } else if (rxDirection != null) {
                if (!checkPort(rxDirection.getPort())) {
                    result = false;
                    message = "Service RxDirection Port is not correctly set";
                }
            } else {
                result = false;
                message = "Service RxDirection is not correctly set";
            }
        } else {
            result = false;
            message = "Service TxDirection is not correctly set";
        }
        return result;
    }

    /**
     * Check Compliancy of Service TxRx info.
     *
     * @return true if String ok false if not
     */
    public Boolean check() {
        Boolean result = true;
        if (serviceEnd != null) {
            Long serviceRate = serviceEnd.getServiceRate();
            ServiceFormat serviceformat = serviceEnd.getServiceFormat();
            String clli = serviceEnd.getClli();
            if (serviceRate != null && serviceRate <= 0) {
                result = false;
                message = "Service " + service + " rate is not set";
                LOG.info(message);
            } else if (serviceformat == null) {
                result = false;
                message = "Service " + service + " format is not set";
                LOG.info(message);
            } else if (!checkString(clli)) {
                result = false;
                message = "Service" + service + " clli format is not set";
                LOG.info(message);
            } else {
                if (!checkTxOrRxInfo(serviceEnd.getTxDirection(), serviceEnd.getRxDirection())) {
                    result = false;
                }
            }
        } else {
            result = false;
            message = service + " is not set";
            LOG.info(message);
        }
        return result;

    }

    public static void main(String[] args) {

    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
