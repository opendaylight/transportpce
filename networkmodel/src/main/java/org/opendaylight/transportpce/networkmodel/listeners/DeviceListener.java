/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
    private final DataBroker databroker;
    private final String deviceId;
    private final DeviceTransactionManager deviceTransactionManager;

    public DeviceListener(String deviceId, DeviceTransactionManager deviceTransactionManager, DataBroker dataBroker) {
        this.databroker = dataBroker;
        this.deviceId = deviceId;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.info("Notification {} on node {} received {}", ChangeNotification.QNAME, this.deviceId ,notification);
        if (this.deviceTransactionManager.isDeviceMounted(deviceId)) {
            InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
            LOG.info("Path arguments of infoIID: {}", infoIID.getPathArguments().toString());
            LOG.info("Target type of infoIID: {}", infoIID.getTargetType().toString());
            Optional<Info> infoOpt =
                    this.deviceTransactionManager.getDataFromDevice(this.deviceId,
                                                                    LogicalDatastoreType.OPERATIONAL,
                                                                    infoIID, Timeouts.DEVICE_READ_TIMEOUT,
                                                                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (infoOpt.isPresent()) {
                LOG.info("Info of device {} from datastore: {}", this.deviceId, infoOpt.get().toString());
            } else {
                LOG.warn("Could not get info from device");
            }
        } else {
            LOG.warn("Device {} not mounted", this.deviceId);
        }




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