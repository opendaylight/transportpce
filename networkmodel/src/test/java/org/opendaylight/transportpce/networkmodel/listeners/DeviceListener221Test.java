/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.change.notification.EditBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditOperationType;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Ignore
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DeviceListener221Test {
    @Mock
    private PortMapping portMapping;

    @Test
    public void testOnChangeNotificationWhenPortUpdated() {
        ChangeNotification notification = mock(ChangeNotification.class);
        Mapping oldMapping = mock(Mapping.class);
        ImmutableList<Edit> editList = createEditList();
        when(notification.getEdit()).thenReturn(editList);
        when(portMapping.getMapping("node1", "circuit-pack1", "port 1")).thenReturn(oldMapping);

        DeviceListener221 listener = new DeviceListener221("node1", portMapping);
        listener.onChangeNotification(notification);
        verify(portMapping, times(1)).getMapping("node1", "circuit-pack1", "port 1");
        verify(portMapping, times(1)).updateMapping("node1", oldMapping);
    }

    private ImmutableList<Edit> createEditList() {
        InstanceIdentifier<Ports> portId = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(CircuitPacks.class, new CircuitPacksKey("circuit-pack1"))
            .child(Ports.class, new PortsKey("port1"));
        Edit edit = new EditBuilder()
            .setOperation(EditOperationType.Merge)
            .setTarget(portId)
            .build();
        ImmutableList<Edit> editList = ImmutableList.of(edit);
        return editList;
    }
}
