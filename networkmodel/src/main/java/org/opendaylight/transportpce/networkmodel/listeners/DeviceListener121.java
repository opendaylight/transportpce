/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.LinkedList;
import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev231221.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener121 {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener121.class);
    private final String nodeId;
    private final PortMapping portMapping;

    public DeviceListener121(String nodeId, PortMapping portMapping) {
        super();
        this.nodeId = nodeId;
        this.portMapping = portMapping;
    }

    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(ChangeNotification.class, this::onChangeNotification),
            new CompositeListener.Component<>(OtdrScanResult.class, this::onOtdrScanResult)
        ));
    }

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */

    void onChangeNotification(ChangeNotification notification) {
        if (notification.getEdit() == null) {
            LOG.warn("unable to handle {} notificatin received - list of edit is null", ChangeNotification.QNAME);
            return;
        }
        for (Edit edit : notification.getEdit()) {
            if (edit.getTarget() == null) {
                continue;
            }
            // 1. Detect the org-openroadm-device object modified
            switch (edit.getTarget().getTargetType().getSimpleName()) {
                case "Ports":
                    LinkedList<PathArgument> path = new LinkedList<>();
                    edit.getTarget().getPathArguments().forEach(p -> path.add(p));
                    InstanceIdentifier<Ports> portIID = InstanceIdentifier.unsafeOf(path);
                    String portName = InstanceIdentifier.keyOf(portIID).getPortName();
                    path.removeLast();
                    InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.unsafeOf(path);
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

    /**
     * Callback for otdr-scan-result.
     *
     * @param notification OtdrScanResult object
     */
    private void onOtdrScanResult(OtdrScanResult notification) {
        LOG.info("Notification {} received {}", OtdrScanResult.QNAME, notification);
    }

}