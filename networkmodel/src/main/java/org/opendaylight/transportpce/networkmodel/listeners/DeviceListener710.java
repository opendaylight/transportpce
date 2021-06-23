/*
 * Copyright © 2021 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.LinkedList;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.CreateTechInfoNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener710 implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener710.class);
    private final String nodeId;
    private final PortMapping portMapping;

    public DeviceListener710(String nodeId, PortMapping portMapping) {
        super();
        this.nodeId = nodeId;
        this.portMapping = portMapping;
    }

    /**
     * Callback for change-notification.
     *
     * @param notification
     *            ChangeNotification object
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onChangeNotification(ChangeNotification notification) {
        if (notification.getEdit() == null) {
            LOG.warn("unable to handle {} notificatin received - list of edit is null", ChangeNotification.QNAME);
            return;
        }
        for (Edit edit : notification.getEdit()) {
            // 1. Detect the org-openroadm-device object modified
            switch (edit.getTarget().getTargetType().getSimpleName()) {
                case "Ports":
                    LinkedList<PathArgument> path = new LinkedList<>();
                    edit.getTarget().getPathArguments().forEach(p -> path.add(p));
                    InstanceIdentifier<Ports> portIID = (InstanceIdentifier<Ports>) InstanceIdentifier
                        .create(path);
                    String portName = InstanceIdentifier.keyOf(portIID).getPortName();
                    path.removeLast();
                    InstanceIdentifier<CircuitPacks> cpIID = (InstanceIdentifier<CircuitPacks>) InstanceIdentifier
                        .create(path);
                    String cpName = InstanceIdentifier.keyOf(cpIID).getCircuitPackName();
                    LOG.info("port {} of circruit-pack {} modified on device {}", portName, cpName, this.nodeId);
                    Mapping oldMapping = portMapping.getMapping(nodeId, cpName, portName);
                    if (oldMapping == null) {
                        return;
                    }
                    Runnable handleNetconfEvent = new Runnable() {
                        @Override
                        public void run() {
                            portMapping.updateMapping(nodeId, oldMapping);
                            LOG.info("{} : mapping data for {} updated", nodeId,
                                oldMapping.getLogicalConnectionPoint());
                        }
                    };
                    Thread thread = new Thread(handleNetconfEvent);
                    thread.start();
                    break;
                default:
                    LOG.debug("modification of type {} not managed yet", edit.getTarget().getTargetType());
                    break;
            }
        }
    }

    @Override
    public void onCreateTechInfoNotification(CreateTechInfoNotification notification) {
    }

    /**
     * Callback for otdr-scan-result.
     *
     * @param notification
     *            OtdrScanResult object
     */
    @Override
    public void onOtdrScanResult(OtdrScanResult notification) {
        LOG.info("Notification {} received {}", OtdrScanResult.QNAME, notification);
    }

}
