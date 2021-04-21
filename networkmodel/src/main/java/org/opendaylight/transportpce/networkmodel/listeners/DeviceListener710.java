/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.CreateTechInfoNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OtdrScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener710 implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener710.class);

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.info("Notification {} received {}", ChangeNotification.QNAME, notification);
    }

    @Override
    public void onCreateTechInfoNotification(CreateTechInfoNotification notification) {

    }

    /**
     * Callback for otdr-scan-result.
     *
     * @param notification OtdrScanResult object
     */
    @Override
    public void onOtdrScanResult(OtdrScanResult notification) {

        LOG.info("Notification {} received {}", OtdrScanResult.QNAME, notification);
    }

}
