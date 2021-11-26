/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.listeners;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ServiceListenerTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private NotificationPublishService notificationPublishService;

    @Test
    public void testOnDataTreeChangedWhenDeleteService() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final Collection<DataTreeModification<Services>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        when(service.getModificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(service.getDataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        ServiceListener listener = new ServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).getModificationType();
        verify(service, times(2)).getDataBefore();
        verify(service, never()).getDataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    public void testOnDataTreeChangedWhenAddService() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final Collection<DataTreeModification<Services>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        Services serviceDown = buildService(State.OutOfService, AdminStates.OutOfService);
        when(service.getModificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(service.getDataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service.getDataAfter()).thenReturn(serviceDown);
        ServiceListener listener = new ServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).getModificationType();
        verify(service, times(3)).getDataBefore();
        verify(service, times(2)).getDataAfter();
        PublishNotificationAlarmService publishNotificationAlarmService =
                buildNotificationAlarmService(serviceDown, "The service is now outOfService");
        try {
            verify(notificationPublishService, times(1))
                    .putNotification(publishNotificationAlarmService);
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    public void testOnDataTreeChangedWhenShouldNeverHappen() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final Collection<DataTreeModification<Services>> changes = new HashSet<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        when(service.getModificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(service.getDataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        ServiceListener listener = new ServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(2)).getModificationType();
        verify(service, times(2)).getDataBefore();
        verify(service, never()).getDataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    private Services buildService(State state, AdminStates adminStates) {
        ServiceAEnd serviceAEnd = getServiceAEndBuild().build();
        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
                    .setClli("clli")
                    .setServiceFormat(ServiceFormat.OC)
                    .setServiceRate(Uint32.valueOf(1))
                    .setNodeId(new NodeIdType("XPONDER-3-2"))
                    .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                    .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                    .build();
        ServicesBuilder builtInput = new ServicesBuilder()
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setCustomer("Customer")
                .setServiceName("service 1")
                .setServiceAEnd(serviceAEnd)
                .setServiceZEnd(serviceZEnd)
                .setOperationalState(state)
                .setAdministrativeState(adminStates);

        return builtInput.build();
    }

    private ServiceAEndBuilder getServiceAEndBuild() {
        return new ServiceAEndBuilder()
                .setClli("clli")
                .setServiceFormat(ServiceFormat.OC)
                .setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()),getTxDirection()))
                .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()));
    }

    private TxDirection getTxDirection() {
        return new TxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortDeviceName("device name")
                        .setPortName("port name")
                        .setPortRack("port rack")
                        .setPortShelf("port shelf")
                        .setPortSlot("port slot")
                        .setPortSubSlot("port subslot")
                        .setPortType("port type")
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name")
                        .setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf")
                        .build())
                .setIndex(Uint8.ZERO)
                .build();
    }

    private RxDirection getRxDirection() {
        return new RxDirectionBuilder()
                .setPort(new PortBuilder()
                        .setPortDeviceName("device name")
                        .setPortName("port name")
                        .setPortRack("port rack")
                        .setPortShelf("port shelf")
                        .setPortSlot("port slot")
                        .setPortSubSlot("port subslot")
                        .setPortType("port type")
                        .build())
                .setLgx(new LgxBuilder()
                        .setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name")
                        .setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf")
                        .build())
                .setIndex(Uint8.ZERO)
                .build();
    }

    private PublishNotificationAlarmService buildNotificationAlarmService(Services services, String message) {
        return new PublishNotificationAlarmServiceBuilder()
                .setServiceName("service 1")
                .setConnectionType(ConnectionType.Service)
                .setMessage(message)
                .setOperationalState(services.getOperationalState())
                .setPublisherName("ServiceListener")
                .build();
    }
}
