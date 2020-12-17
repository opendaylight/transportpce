/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.test.AbstractTest;

public class NbiNotificationsProviderTest  extends AbstractTest {

    @Mock
    RpcProviderService rpcProviderRegistry;

    @Mock
    private NotificationService notificationService;

    @Before
    public void init() {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    public void testInitRegisterNbiNotificationsToRpcRegistry() {
        NbiNotificationsProvider provider =  new NbiNotificationsProvider(getDataBroker(),
                Arrays.asList("topic1", "topic2"), rpcProviderRegistry, notificationService);
        provider.init();
        verify(rpcProviderRegistry, times(1))
                .registerRpcImplementation(any(), any(NbiNotificationsImpl.class));
    }
}
