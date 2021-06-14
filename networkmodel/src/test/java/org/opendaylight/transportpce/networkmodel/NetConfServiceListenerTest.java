/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.PublishNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.notification.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class NetConfServiceListenerTest {

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
        NetConfServiceListener listener = new NetConfServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).getModificationType();
        verify(service, times(2)).getDataBefore();
        verify(service, never()).getDataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationService.class));
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
        NetConfServiceListener listener = new NetConfServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).getModificationType();
        verify(service, times(3)).getDataBefore();
        verify(service, times(2)).getDataAfter();
        PublishNotificationService publishNotificationService =
                buildNotificationService(serviceDown, "The service is now Out of Service");
        try {
            verify(notificationPublishService, times(1)).putNotification(publishNotificationService);
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
        NetConfServiceListener listener = new NetConfServiceListener(dataBroker, notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(2)).getModificationType();
        verify(service, times(2)).getDataBefore();
        verify(service, never()).getDataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    private Services buildService(State state, AdminStates adminStates) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd
                serviceAEnd = getServiceAEndBuild()
                .build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .ServiceZEnd serviceZEnd = new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
                .types.rev190531.service.ServiceZEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-3-2"))
                .setTxDirection(getTxDirection())
                .setRxDirection(getRxDirection())
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

    private org.opendaylight.yang.gen.v1
            .http.org.openroadm.common.service.types.rev190531.service.ServiceAEndBuilder getServiceAEndBuild() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .ServiceAEndBuilder()
                .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate(Uint32.valueOf(1))
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(getTxDirection())
                .setRxDirection(getRxDirection());
    }

    private TxDirection getTxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .endpoint.TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("device name")
                .setPortName("port name").setPortRack("port rack").setPortShelf("port shelf")
                .setPortSlot("port slot").setPortSubSlot("port subslot").setPortType("port type").build())
                .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name").setLgxPortName("lgx port name")
                        .setLgxPortRack("lgx port rack").setLgxPortShelf("lgx port shelf").build())
                .build();
    }

    private RxDirection getRxDirection() {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service
                .endpoint.RxDirectionBuilder()
                .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                        .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                        .setLgxPortShelf("lgx port shelf").build())
                .build();
    }

    private PublishNotificationService buildNotificationService(Services services, String message) {
        return new PublishNotificationServiceBuilder().setServiceName("service 1")
                .setServiceAEnd(new ServiceAEndBuilder(services.getServiceAEnd()).build())
                .setServiceZEnd(new ServiceZEndBuilder(services.getServiceZEnd()).build())
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setResponseFailed("")
                .setMessage(message)
                .setOperationalState(services.getOperationalState())
                .setTopic("NetConfServiceListener")
                .build();
    }
}
