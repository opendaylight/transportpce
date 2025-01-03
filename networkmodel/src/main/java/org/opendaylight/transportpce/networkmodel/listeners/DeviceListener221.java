/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.CreateTechInfoNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.ExactDataObjectStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotification
 * notification.
 * This implementation is dedicated to yang model 2.2.1 revision. 
 */
public class DeviceListener221 {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener221.class);
    private final String nodeId;
    private final PortMapping portMapping;

    /**
     * Create instance of the device listener.
     *
     * @param nodeId Node name
     * @param portMapping Node abstractions stored
     */
    public DeviceListener221(String nodeId, PortMapping portMapping) {
        super();
        this.nodeId = nodeId;
        this.portMapping = portMapping;
    }

    /**
     * Get instances of a CompositeListener that could be used to unregister listeners.
     * @return a Composite listener containing listener implementations that will receive notifications
     */
    public CompositeListener getCompositeListener() {
        return new CompositeListener(Set.of(
            new CompositeListener.Component<>(ChangeNotification.class, this::onChangeNotification),
            new CompositeListener.Component<>(CreateTechInfoNotification.class, this::onCreateTechInfoNotification),
            new CompositeListener.Component<>(OtdrScanResult.class, this::onOtdrScanResult)
        ));
    }

    /**
     * Callback for change-notification.
     *
     * @param notification
     *            ChangeNotification object
     */
    void onChangeNotification(ChangeNotification notification) {
        LOG.info("notification received from device {}: {}", this.nodeId, notification.toString());
        if (notification.getEdit() == null) {
            LOG.warn("unable to handle {} notificatin received - list of edit is null", ChangeNotification.QNAME);
            return;
        }
        for (Edit edit : notification.getEdit()) {
            if (edit.getTarget() == null) {
                continue;
            }
            // 1. Detect the org-openroadm-device object modified
            DataObjectIdentifier<?> path = DataObjectIdentifier.ofUnsafeSteps(
                     (Iterable<? extends ExactDataObjectStep<?>>) edit.getTarget().steps());
            LOG.debug("Instance Identifier received = {} from node {}", path.toString(), nodeId);
            switch (path.lastStep().type().getSimpleName()) {
                case "Ports":
                    String portName = path.toLegacy().firstKeyOf(Ports.class).getPortName();
                    String cpName = path.toLegacy().firstKeyOf(CircuitPacks.class).getCircuitPackName();
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
                case "Interface":
                    String interfaceName = path.toLegacy().firstKeyOf(Interface.class).getName();
                    LOG.info("interface {} modified on device {}", interfaceName, this.nodeId);
                    Mapping oldMapping2 = portMapping.getMappingFromOtsInterface(nodeId, interfaceName);
                    if (oldMapping2 == null) {
                        return;
                    }
                    Runnable handleNetconfEvent2 = new Runnable() {
                        @Override
                        public void run() {
                            portMapping.updateMapping(nodeId, oldMapping2);
                            LOG.info("{} : mapping data for {} updated", nodeId,
                                oldMapping2.getLogicalConnectionPoint());
                        }
                    };
                    Thread thread2 = new Thread(handleNetconfEvent2);
                    thread2.start();
                    break;
                default:
                    LOG.debug("modification of type {} not managed yet", edit.getTarget().getClass());
                    break;
            }
        }
    }

    private void onCreateTechInfoNotification(CreateTechInfoNotification notification) {
    }

    /**
     * Callback for otdr-scan-result.
     *
     * @param notification
     *            OtdrScanResult object
     */
    private void onOtdrScanResult(OtdrScanResult notification) {
        LOG.info("Notification {} received {}", OtdrScanResult.QNAME, notification);
    }

}
