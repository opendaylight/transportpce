/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationService;

@ExtendWith(MockitoExtension.class)
public class DmaapClientProviderTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void testInitRegisterNbiNotificationsToRpcRegistry() {
        new DmaapClientProvider(notificationService, "http://localhost", "username", "password");
        verify(notificationService, times(1))
            .registerCompositeListener(any(NotificationService.CompositeListener.class));
    }
}
