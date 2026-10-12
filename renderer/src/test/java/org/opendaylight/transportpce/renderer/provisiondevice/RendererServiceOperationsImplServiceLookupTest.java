/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.Notification;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescription;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;

/**
 * Tests how service-delete looks up the service when the renderer RPC gives only its name.
 */
@ExtendWith(MockitoExtension.class)
class RendererServiceOperationsImplServiceLookupTest {

    private static final String SERVICE = "service 1";

    @Mock
    private DeviceRendererService deviceRenderer;
    @Mock
    private OtnDeviceRendererService otnDeviceRenderer;
    @Mock
    private DataBroker dataBroker;
    @Mock
    private ReadTransaction readTx;
    @Mock
    private Notification notification;
    @Mock
    private PortMapping portMapping;
    @Mock
    private RpcService rpcService;

    private RendererServiceOperationsImpl rendererServiceOperations;

    @BeforeEach
    void setup() {
        rendererServiceOperations = new RendererServiceOperationsImpl(deviceRenderer, otnDeviceRenderer, dataBroker,
                notification, portMapping, rpcService);
    }

    @Test
    void serviceDeleteStopsWhenInterruptedWhileLookingUpTheService() throws Exception {
        CountDownLatch lookingUpService = new CountDownLatch(1);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTx);
        doAnswer(invocation -> {
            DataObjectIdentifier<?> path = invocation.getArgument(1);
            if (Services.class.equals(path.lastStep().type())) {
                // A read that does not finish before the request is interrupted
                lookingUpService.countDown();
                return FluentFuture.from(SettableFuture.create());
            }
            return FluentFutures.immediateFluentFuture(Optional.of(mock(PathDescription.class)));
        }).when(readTx).read(eq(LogicalDatastoreType.OPERATIONAL), any());

        ListenableFuture<ServiceDeleteOutput> delete = rendererServiceOperations.serviceDelete(
                new ServiceDeleteInputBuilder().setServiceName(SERVICE).build(), null);
        assertTrue(lookingUpService.await(10, TimeUnit.SECONDS));
        // Interrupts the running request, as at shutdown
        rendererServiceOperations.close();

        ExecutionException thrown = assertThrows(ExecutionException.class, () -> delete.get(10, TimeUnit.SECONDS));
        assertInstanceOf(InterruptedException.class, thrown.getCause());
        // Before, the interrupt was reported as a service missing from the service list
        verify(notification, never()).send(any(), anyString(), eq(RpcStatusEx.Failed), anyString());
    }
}
