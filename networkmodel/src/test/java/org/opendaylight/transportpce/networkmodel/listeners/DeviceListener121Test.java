/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.EditBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditOperationType;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;

/**
 * DeviceListener121Test class.
 */
@ExtendWith(MockitoExtension.class)
public class DeviceListener121Test {
    @Mock
    private PortMapping portMapping;
    @Mock
    private Mapping oldMapping;
    @Mock
    private ChangeNotification notification;

    @Test
    void testOnChangeNotificationWhenPortUpdated() {
        ImmutableList<Edit> editList = createEditList();
        when(notification.getEdit()).thenReturn(editList);
        when(portMapping.getMapping("node1", "circuit-pack1", "port1")).thenReturn(oldMapping);

        DeviceListener121 listener = new DeviceListener121("node1", portMapping);
        listener.onChangeNotification(notification);
        verify(portMapping, times(1)).getMapping("node1", "circuit-pack1", "port1");
        verify(portMapping, timeout(1000).times(1)).updateMapping("node1", oldMapping);
    }

    @Test
    void testOnChangeNotificationWhenNoEditList() {
        when(notification.getEdit()).thenReturn(null);
        DeviceListener121 listener = new DeviceListener121("node1", portMapping);
        listener.onChangeNotification(notification);
        verify(portMapping, never()).getMapping(anyString(), anyString(), anyString());
        verify(portMapping, never()).updateMapping(anyString(), any());
    }

    @Test
    void testOnChangeNotificationWhenOtherthingUpdated() {
        ImmutableList<Edit> editList = createBadEditList();
        when(notification.getEdit()).thenReturn(editList);
        DeviceListener121 listener = new DeviceListener121("node1", portMapping);
        listener.onChangeNotification(notification);
        verify(portMapping, never()).getMapping(anyString(), anyString(), anyString());
        verify(portMapping, never()).updateMapping(anyString(), any());
    }

    private ImmutableList<Edit> createEditList() {
        DataObjectIdentifier<Ports> portId = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey("circuit-pack1"))
            .child(Ports.class, new PortsKey("port1"))
            .build();
        Edit edit = new EditBuilder()
            .setOperation(EditOperationType.Merge)
            .setTarget(portId)
            .build();
        ImmutableList<Edit> editList = ImmutableList.of(edit);
        return editList;
    }

    private ImmutableList<Edit> createBadEditList() {
        DataObjectIdentifier<CircuitPacks> cpId = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey("circuit-pack1"))
            .build();
        Edit edit = new EditBuilder()
            .setOperation(EditOperationType.Merge)
            .setTarget(cpId)
            .build();
        ImmutableList<Edit> editList = ImmutableList.of(edit);
        return editList;
    }
}
