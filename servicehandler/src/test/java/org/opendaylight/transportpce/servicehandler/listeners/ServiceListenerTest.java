/*
 * Copyright © 2021 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModified;
import org.opendaylight.mdsal.binding.api.DataObjectWritten;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.Restorable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.resiliency.ServiceResiliency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.service.resiliency.ServiceResiliencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceReroute;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

@ExtendWith(MockitoExtension.class)
public class ServiceListenerTest {

    @Mock
    private RpcService rpcService;
    @Mock
    private ServiceDataStoreOperations serviceDataStoreOperations;
    @Mock
    private NotificationPublishService notificationPublishService;
    @Mock
    private PathComputationService pathComputationService;
    @Mock
    private ServiceDelete serviceDelete;
    @Mock
    private ServiceReroute serviceReroute;
    @Mock
    private ServiceCreate serviceCreate;

    @Test
    void testOnDataTreeChangedWhenDeleteService() {
        final DataObjectDeleted<Services> service = mock();
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));

        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).dataBefore();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    void testOnDataTreeChangedWhenServiceBecomesOutOfService() {
        final DataObjectWritten<Services> service = mock();
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        Services serviceDown = buildService(State.OutOfService, AdminStates.InService);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service.dataAfter()).thenReturn(serviceDown);

        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);

        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).dataBefore();
        verify(service, times(1)).dataAfter();
        try {
            verify(notificationPublishService, times(1))
                    .putNotification(buildNotificationAlarmService(serviceDown, "The service is now outOfService"));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
        verify(rpcService, never()).getRpc(ServiceReroute.class);
    }

    @Test
    void testOnDataTreeChangedWhenShouldNeverHappen() {
        final DataObjectModified<Services> service = mock();
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        when(service.dataBefore()).thenReturn(buildService(State.OutOfService, AdminStates.OutOfService));
        when(service.dataAfter()).thenReturn(buildService(State.InService, AdminStates.OutOfService));
        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).dataBefore();
        verify(service, times(1)).dataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
        verify(rpcService, never()).getRpc(ServiceReroute.class);
    }

    @Test
    void testOnDataTreeChangedWhenServiceDegradedShouldBeRerouted() {
        final DataObjectWritten<Services> service1 = mock();
        final DataObjectDeleted<Services> service2 = mock();
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        final DataTreeModification<Services> ch1 = mock(DataTreeModification.class);
        final DataTreeModification<Services> ch2 = mock(DataTreeModification.class);
        changes.add(ch1);
        changes.add(ch2);
        when(ch1.getRootNode()).thenReturn(service1);
        when(ch2.getRootNode()).thenReturn(service2);

        ServiceResiliency serviceResiliency = new ServiceResiliencyBuilder()
                .setResiliency(Restorable.VALUE)
                .build();
        Services serviceAfter = new ServicesBuilder(buildService(State.OutOfService, AdminStates.InService))
                .setServiceResiliency(serviceResiliency)
                .build();
        when(service1.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service2.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service1.dataAfter()).thenReturn(serviceAfter);
        when(serviceDataStoreOperations.getService(anyString())).thenReturn(Optional.of(serviceAfter));
        when(rpcService.getRpc(ServiceDelete.class)).thenReturn(serviceDelete);
        when(serviceDelete.invoke(any()))
            .thenReturn(RpcResultBuilder.success(new ServiceDeleteOutputBuilder()
                                .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                                        .setResponseCode(ResponseCodes.RESPONSE_OK)
                                        .build())
                                .build())
                        .buildFuture());
        when(rpcService.getRpc(ServiceReroute.class)).thenReturn(serviceReroute);
        when(serviceReroute.invoke(any()))
            .thenReturn(RpcResultBuilder.success(new ServiceRerouteOutputBuilder()
                                .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                                        .setResponseCode(ResponseCodes.RESPONSE_OK)
                                        .build())
                                .build())
                        .buildFuture());
        when(rpcService.getRpc(ServiceCreate.class)).thenReturn(serviceCreate);
        when(serviceCreate.invoke(any()))
            .thenReturn(RpcResultBuilder.success(new ServiceCreateOutputBuilder()
                                .setConfigurationResponseCommon(new ConfigurationResponseCommonBuilder()
                                        .setResponseCode(ResponseCodes.RESPONSE_OK)
                                        .build())
                                .build())
                        .buildFuture());

        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);

        verify(ch1, times(1)).getRootNode();
        verify(service1, times(1)).dataBefore();
        verify(service1, times(1)).dataAfter();
        verify(rpcService, times(1)).getRpc(ServiceReroute.class);
        verify(rpcService, times(1)).getRpc(ServiceDelete.class);

        verify(ch2, times(1)).getRootNode();
        verify(rpcService, times(1)).getRpc(ServiceCreate.class);
    }

    private Services buildService(State state, AdminStates adminStates) {
        return new ServicesBuilder()
                .setSdncRequestHeader(new SdncRequestHeaderBuilder().build())
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setCustomer("Customer")
                .setServiceName("service 1")
                .setServiceAEnd(getServiceAEndBuild().build())
                .setServiceZEnd(new ServiceZEndBuilder()
                        .setClli("clli")
                        .setServiceFormat(ServiceFormat.Ethernet)
                        .setServiceRate(Uint32.ONE)
                        .setNodeId(new NodeIdType("XPONDER-3-2"))
                        .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()), getTxDirection()))
                        .setRxDirection(Map.of(new RxDirectionKey(getRxDirection().key()), getRxDirection()))
                        .build())
                .setOperationalState(state)
                .setAdministrativeState(adminStates)
                .build();
    }

    private ServiceAEndBuilder getServiceAEndBuild() {
        return new ServiceAEndBuilder()
                .setClli("clli")
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.ONE)
                .setNodeId(new NodeIdType("XPONDER-1-2"))
                .setTxDirection(Map.of(new TxDirectionKey(getTxDirection().key()), getTxDirection()))
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