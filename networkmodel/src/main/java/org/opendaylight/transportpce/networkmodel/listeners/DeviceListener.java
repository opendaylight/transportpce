/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
    private static final long GET_DATA_SUBMIT_TIMEOUT = 5000;
    private static final TimeUnit MAX_DURATION_TO_SUBMIT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private final DeviceTransactionManager deviceTransactionManager;
    private final String nodeId;
    private final NetworkModelService networkModelService;

    public DeviceListener(DeviceTransactionManager deviceTransactionManager, String nodeId,
                          final NetworkModelService networkModelService) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.nodeId = nodeId;
        this.networkModelService = networkModelService;
    }

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.info("Notification {} received {}", ChangeNotification.QNAME, notification);
        // NETCONF event notification handling
        String deviceComponentChanged = null;
        // Seems like there is only one edit in the NETCONF notification (from honeynode experience)
        Edit edit = Objects.requireNonNull(notification.getEdit()).get(0);
        deviceComponentChanged = Objects.requireNonNull(edit.getTarget()).getTargetType().getSimpleName();
        // Only circuitPack type handled
        switch (deviceComponentChanged) {
            case "Interface":
                // do changes
                LOG.info("Interface modified on device {}", this.nodeId);
                break;
            case "CircuitPacks":
                LOG.info("Circuit Pack modified on device {}", this.nodeId);
                // 1. Get the name of the component modified
                Iterable<InstanceIdentifier.PathArgument> pathArguments = edit.getTarget().getPathArguments();
                String cpackId = null;
                for (InstanceIdentifier.PathArgument pathArgument : pathArguments) {
                    if (pathArgument.toString().contains("CircuitPacks")) {
                        String cpackKey = StringUtils.substringBetween(pathArgument.toString(), "{", "}");
                        cpackId = StringUtils.substringAfter(cpackKey, "=");
                    }
                }
                // 2. Get new configuration of component from device
                if (cpackId != null) {
                    LOG.info("Circuit Pack {} modified on device {}", cpackId, this.nodeId);
                    if (this.deviceTransactionManager.isDeviceMounted(nodeId)) {
                        InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                                .child(CircuitPacks.class, new CircuitPacksKey(cpackId));
                        /* Creating runnable to perform configuration retrieval and topology update in a new thread
                        to avoid JIRA TRNSPRTCE-251 */
                        Runnable handlenetconfEvent = new Runnable() {
                            private CircuitPacks circuitPacks;

                            public CircuitPacks getCircuitPacks() {
                                return circuitPacks;
                            }

                            public void setCircuitPacks(CircuitPacks circuitPacks) {
                                this.circuitPacks = circuitPacks;
                            }

                            @Override
                            public void run() {
                                Optional<CircuitPacks> cpacksOptional = deviceTransactionManager
                                        .getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, cpIID,
                                                GET_DATA_SUBMIT_TIMEOUT, MAX_DURATION_TO_SUBMIT_TIMEUNIT);
                                if (cpacksOptional.isPresent()) {
                                    setCircuitPacks(cpacksOptional.get());
                                    LOG.info("Component {} configuration: {}", getCircuitPacks().getCircuitPackName(),
                                            getCircuitPacks());
                                    // 3. Update openroadm-topology
                                    // TODO
                                    // networkModelService.updateOpenRoadmNode(nodeId, getCircuitPacks());
                                } else {
                                    LOG.error("Couldnt read from device datastore");
                                }
                            }
                        };
                        Thread thread = new Thread(handlenetconfEvent);
                        thread.start();
                    } else {
                        LOG.error("Device {} not mounted yet", nodeId);
                    }
                }
                break;
            default:
                // TODO: handle more component types --> it implies development on honeynode simulator
                LOG.warn("Component {} change not supported", deviceComponentChanged);
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