/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener implements OrgOpenroadmDeviceListener {

    private static final long GET_DATA_SUBMIT_TIMEOUT = 5000;
    private static final TimeUnit MAX_DURATION_TO_SUBMIT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
    private final DeviceTransactionManager deviceTransactionManager;
    private final String deviceId;
    private final NetworkModelService networkModelService;

    public DeviceListener(DeviceTransactionManager deviceTransactionManager,
                          String deviceId, final NetworkModelService networkModelService) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.deviceId = deviceId;
        this.networkModelService = networkModelService;
        // this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
    }

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.info("Notification {} received {}", ChangeNotification.QNAME, notification);
        // Create method to obtain the target of the notification
        String targetType = null;
        try {
            targetType = notification.getEdit().get(0).getTarget().getTargetType().getSimpleName();
        } catch (NullPointerException e) {
            LOG.warn("Target type is null. {}", e.getMessage());
        }
        if (targetType != null) {
            switch (targetType) {
                case "Interface":
                    // do changes
                    LOG.info("Inferface change on device");
                    break;
                case "CircuitPacks":
                    // do changes

                    Iterable<InstanceIdentifier.PathArgument> pathArguments = null;
                    try {
                        pathArguments = notification.getEdit().get(0).getTarget().getPathArguments();
                    } catch (NullPointerException e) {
                        LOG.warn("Path arguments are null. {}", e.getMessage());
                    }
                    String circuitPackName = null;
                    if (pathArguments != null) {
                        for (InstanceIdentifier.PathArgument pathArgument : pathArguments) {
                            if (pathArgument.toString().contains("CircuitPacks")) {
                                String circuitPacksKey = StringUtils.substringBetween(pathArgument.toString(),
                                        "{", "}");
                                circuitPackName = StringUtils.substringAfter(circuitPacksKey, "=");
                            }
                        }
                    }

                    if (this.deviceTransactionManager.isDeviceMounted(deviceId)) {
                        InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                                .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName));

                        // runnable cannot return a result, whereas Callable can
                        Runnable runnable = new Runnable() {
                            private CircuitPacks cpaks;

                            public CircuitPacks getCpacks() {
                                return cpaks;
                            }

                            public void setCpacks(CircuitPacks cpaks) {
                                this.cpaks = cpaks;
                            }

                            @Override
                            public void run() {
                                // Create method to obtain the cpack changed
                                Optional<CircuitPacks> cpacksOpt =
                                        deviceTransactionManager.getDataFromDevice(deviceId,
                                                LogicalDatastoreType.OPERATIONAL,
                                                cpIID,
                                                GET_DATA_SUBMIT_TIMEOUT,
                                                MAX_DURATION_TO_SUBMIT_TIMEUNIT);
                                if (cpacksOpt.isPresent()) {
                                    setCpacks(cpacksOpt.get());
                                    // Managing only change in Ports state of circuit pack
                                    networkModelService.updateOpenRoadmNode(deviceId, getCpacks());
                                } else {
                                    LOG.warn("Could not get info from device");
                                }
                            }
                        };
                        Thread readDataThread = new Thread(runnable);
                        readDataThread.start();
                    } else {
                        LOG.warn("Device {} not mounted", deviceId);
                    }
                    break;
                default:
                    // to do
                    LOG.info("Type {} change not recognized", targetType);
            }
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