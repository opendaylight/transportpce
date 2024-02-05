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
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.Restorable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.resiliency.ServiceResiliency;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.resiliency.ServiceResiliencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceReroute;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceRerouteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
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
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).modificationType();
        verify(service, times(2)).dataBefore();
        verify(service, never()).dataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    void testOnDataTreeChangedWhenServiceBecomesOutOfService() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        Services serviceDown = buildService(State.OutOfService, AdminStates.InService);
        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service.dataAfter()).thenReturn(serviceDown);
        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).modificationType();
        verify(service, times(3)).dataBefore();
        verify(service, times(1)).dataAfter();
        try {
            verify(notificationPublishService, times(1))
                    .putNotification(buildNotificationAlarmService(serviceDown, "The service is now outOfService"));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    void testOnDataTreeChangedWhenShouldNeverHappen() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.SUBTREE_MODIFIED);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(2)).modificationType();
        verify(service, times(2)).dataBefore();
        verify(service, never()).dataAfter();
        try {
            verify(notificationPublishService, never()).putNotification(any(PublishNotificationAlarmService.class));
        } catch (InterruptedException e) {
            fail("Failed publishing notification");
        }
    }

    @Test
    void testOnDataTreeChangedWhenServiceDegradedShouldBeRerouted() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        ServiceResiliency serviceResiliency = new ServiceResiliencyBuilder().setResiliency(Restorable.VALUE).build();
        Services serviceAfter = new ServicesBuilder(buildService(State.OutOfService, AdminStates.InService))
                .setServiceResiliency(serviceResiliency)
                .build();
        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(service.dataBefore())
            .thenReturn(new ServicesBuilder(buildService(State.InService, AdminStates.InService))
                        .setServiceResiliency(serviceResiliency)
                        .build());
        when(service.dataAfter()).thenReturn(serviceAfter);
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
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).modificationType();
        verify(service, times(3)).dataBefore();
        verify(service, times(1)).dataAfter();
//        verify(servicehandler, times(1)).serviceDelete(any());

        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        listener.onDataTreeChanged(changes);
//        verify(servicehandler, times(1)).serviceCreate(any());
    }

    @Test
    void testOnDataTreeChangedWhenServiceDegradedShouldNotBeRerouted() {
        @SuppressWarnings("unchecked") final DataObjectModification<Services> service =
                mock(DataObjectModification.class);
        final List<DataTreeModification<Services>> changes = new ArrayList<>();
        @SuppressWarnings("unchecked") final DataTreeModification<Services> ch = mock(DataTreeModification.class);
        changes.add(ch);
        when(ch.getRootNode()).thenReturn(service);

        Services serviceAfter = buildService(State.OutOfService, AdminStates.InService);
        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.WRITE);
        when(service.dataBefore()).thenReturn(buildService(State.InService, AdminStates.InService));
        when(service.dataAfter()).thenReturn(serviceAfter);
        ServiceListener listener = new ServiceListener(rpcService, serviceDataStoreOperations,
                notificationPublishService);
        listener.onDataTreeChanged(changes);
        verify(ch, times(1)).getRootNode();
        verify(service, times(1)).modificationType();
        verify(service, times(3)).dataBefore();
        verify(service, times(1)).dataAfter();
//        verify(servicehandler, times(0)).serviceDelete(any());

        when(service.modificationType()).thenReturn(DataObjectModification.ModificationType.DELETE);
        listener.onDataTreeChanged(changes);
//        verify(servicehandler, times(0)).serviceCreate(any());
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
                        .setServiceRate(Uint32.valueOf(1))
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
                .setServiceRate(Uint32.valueOf(1))
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