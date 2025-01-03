/*
 * Copyright © 2021 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.NotificationService.CompositeListener;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.CreateTechInfoNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.ExactDataObjectStep;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.ChangeNotification
 * notification.
 * This implementation is dedicated to yang model 7.1 revision. 
 */
public class DeviceListener710 {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener710.class);
    private final String nodeId;
    private final PortMapping portMapping;

    /**
     * Create instance of the device listener.
     *
     * @param nodeId Node name
     * @param portMapping Node abstractions stored
     */
    public DeviceListener710(String nodeId, PortMapping portMapping) {
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
        LOG.debug("device71 notification received = {}", notification);
        if (notification.getEdit() == null) {
            LOG.warn("unable to handle {} notificatin received - list of edit is null", ChangeNotification.QNAME);
            return;
        }
        Map<Uint16, List<DataObjectIdentifier<PortList>>> nbliidMap = new HashMap<>();
        DataObjectIdentifier<OduSwitchingPools> ospIID = null;
        for (Edit edit : notification.getEdit()) {
            if (edit.getTarget() == null) {
                continue;
            }
            // 1. Detect the org-openroadm-device object modified
            DataObjectIdentifier<?> path = DataObjectIdentifier.ofUnsafeSteps(
                    (Iterable<? extends @NonNull ExactDataObjectStep<?>>) edit.getTarget().steps());
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
                case "OduSwitchingPools":
                    LOG.info("odu-switching-pools modified on device {}", nodeId);
                    ospIID = path.toLegacy().firstIdentifierOf(OduSwitchingPools.class).toIdentifier();
                    break;
                case "PortList":
                    Uint16 nblNb = path.toLegacy().firstKeyOf(NonBlockingList.class).getNblNumber();
                    List<DataObjectIdentifier<PortList>> iidList = nbliidMap.containsKey(nblNb)
                        ? nbliidMap.get(nblNb) : new ArrayList<>();
                    iidList.add(path.toLegacy().firstIdentifierOf(PortList.class).toIdentifier());
                    nbliidMap.put(nblNb, iidList);
                    break;
                default:
                    LOG.debug("modification of type {} not managed yet", edit.getTarget().getClass());
                    break;
            }
        }
        if (!nbliidMap.isEmpty() && ospIID != null) {
            DataObjectIdentifier<OduSwitchingPools> id = ospIID.toIdentifier();
            Runnable handleNetconfEvent = new Runnable() {
                @Override
                public void run() {
                    portMapping.updatePortMappingWithOduSwitchingPools(nodeId, id, nbliidMap);
                    LOG.info("{} : swiching-pool data updated", nodeId);
                }
            };
            Thread thread = new Thread(handleNetconfEvent);
            thread.start();
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
