/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ClassToInstanceMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcProviderService;


@ExtendWith(MockitoExtension.class)
class NetworkUtilsImplTest {
    @Mock
    DataBroker dataBroker;
    @Mock
    RpcProviderService rpcProvider;

    @Test
    void networkUtilsInitTest() {
        new NetworkUtilsImpl(dataBroker, rpcProvider);

        verify(rpcProvider, times(1)).registerRpcImplementations(any(ClassToInstanceMap.class));
    }
}
