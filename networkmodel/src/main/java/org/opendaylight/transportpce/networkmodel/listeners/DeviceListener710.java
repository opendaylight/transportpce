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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.CreateTechInfoNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.OduSwitchingPools;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.NonBlockingList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.odu.switching.pools.non.blocking.list.PortList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener710 implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener710.class);
    private final String nodeId;
    private final PortMapping portMapping;

    public DeviceListener710(String nodeId, PortMapping portMapping) {
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
    public void onChangeNotification(ChangeNotification notification) {
        LOG.debug("device71 notification received = {}", notification);
        if (notification.getEdit() == null) {
            LOG.warn("unable to handle {} notificatin received - list of edit is null", ChangeNotification.QNAME);
            return;
        }
        Map<Uint16, List<InstanceIdentifier<PortList>>> nbliidMap = new HashMap<>();
        InstanceIdentifier<OduSwitchingPools> ospIID = null;
        for (Edit edit : notification.getEdit()) {
            // 1. Detect the org-openroadm-device object modified
            LinkedList<PathArgument> path = new LinkedList<>();
            switch (edit.getTarget().getTargetType().getSimpleName()) {
                case "Ports":
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
                case "OduSwitchingPools":
                    LOG.info("odu-switching-pools modified on device {}", nodeId);
                    edit.getTarget().getPathArguments().forEach(p -> path.add(p));
                    ospIID = (InstanceIdentifier<OduSwitchingPools>) InstanceIdentifier.create(path);
                    break;
                case "PortList":
                    edit.getTarget().getPathArguments().forEach(p -> path.add(p));
                    InstanceIdentifier<PortList> plIID = (InstanceIdentifier<PortList>) InstanceIdentifier.create(path);
                    path.removeLast();
                    InstanceIdentifier<NonBlockingList> nblIID =
                        (InstanceIdentifier<NonBlockingList>) InstanceIdentifier.create(path);
                    Uint16 nblNb = InstanceIdentifier.keyOf(nblIID).getNblNumber();
                    List<InstanceIdentifier<PortList>> iidList = nbliidMap.containsKey(nblNb)
                        ? nbliidMap.get(nblNb) : new ArrayList<>();
                    iidList.add(plIID);
                    nbliidMap.put(nblNb, iidList);
                    break;
                default:
                    LOG.debug("modification of type {} not managed yet", edit.getTarget().getTargetType());
                    break;
            }
        }
        if (!nbliidMap.isEmpty() && ospIID != null) {
            InstanceIdentifier<OduSwitchingPools> id = ospIID;
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
