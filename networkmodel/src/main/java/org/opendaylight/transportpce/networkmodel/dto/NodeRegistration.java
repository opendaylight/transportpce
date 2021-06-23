/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener;
import org.opendaylight.transportpce.networkmodel.listeners.AlarmNotificationListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener;
import org.opendaylight.transportpce.networkmodel.listeners.DeOperationsListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener121;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener221;
import org.opendaylight.transportpce.networkmodel.listeners.DeviceListener710;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener;
import org.opendaylight.transportpce.networkmodel.listeners.TcaListener221;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev161014.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.de.operations.rev161014.OrgOpenroadmDeOperationsListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeRegistration {
    private static final Logger LOG = LoggerFactory.getLogger(NodeRegistration.class);
    private final String nodeId;
    private final String nodeVersion;
    private final NotificationService notificationService;
    private final DataBroker dataBroker;
    private final PortMapping portMapping;
    private final List<ListenerRegistration<?>> listeners;

    public NodeRegistration(String nodeId, String nodeVersion, NotificationService notificationService,
            DataBroker dataBroker, PortMapping portMapping) {
        this.nodeId = nodeId;
        this.nodeVersion = nodeVersion;
        this.notificationService = notificationService;
        this.dataBroker = dataBroker;
        this.portMapping = portMapping;
        listeners = new ArrayList<ListenerRegistration<?>>();
    }

    public void registerListeners() {
        switch (this.nodeVersion) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                registerListeners121();
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                registerListeners221();
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                registerListeners710();
                break;
            default:
                LOG.debug("Unable to register listeners - unknown device version for {}", this.nodeId);
                break;
        }
    }

    public void unregisterListeners() {
        LOG.info("Unregistering notification listeners for node: {}", this.nodeId);
        for (ListenerRegistration<?> listenerRegistration : listeners) {
            listenerRegistration.close();
        }
    }

    private void registerListeners121() {
        OrgOpenroadmAlarmListener alarmListener = new AlarmNotificationListener(this.dataBroker);
        LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(alarmListener));

        OrgOpenroadmDeOperationsListener deOperationsListener = new DeOperationsListener();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(deOperationsListener));

        OrgOpenroadmDeviceListener deviceListener = new DeviceListener121(nodeId, this.portMapping);
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(deviceListener));

        TcaListener tcaListener = new TcaListener();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(tcaListener));
    }

    private void registerListeners221() {
        AlarmNotificationListener221 alarmListener = new AlarmNotificationListener221(dataBroker);
        LOG.info("Registering notification listener on OrgOpenroadmAlarmListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(alarmListener));

        DeOperationsListener221 deOperationsListener = new DeOperationsListener221();
        LOG.info("Registering notification listener on OrgOpenroadmDeOperationsListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(deOperationsListener));

        DeviceListener221 deviceListener = new DeviceListener221(nodeId, this.portMapping);
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(deviceListener));

        TcaListener221 tcaListener = new TcaListener221();
        LOG.info("Registering notification listener on OrgOpenroadmTcaListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(tcaListener));
    }

    private void registerListeners710() {
        DeviceListener710 deviceListener = new DeviceListener710(nodeId, this.portMapping);
        LOG.info("Registering notification listener on OrgOpenroadmDeviceListener for node: {}", nodeId);
        listeners.add(notificationService.registerNotificationListener(deviceListener));
    }
}
