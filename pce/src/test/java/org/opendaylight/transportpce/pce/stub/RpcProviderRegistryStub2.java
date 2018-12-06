/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.stub;

import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.rpc.RpcContextIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class RpcProviderRegistryStub2 implements RpcProviderRegistry {
    @Override public <T extends RpcService> BindingAwareBroker.RpcRegistration<T> addRpcImplementation(
        Class<T> serviceInterface, T implementation) throws IllegalStateException {
        return null;
    }

    @Override public <T extends RpcService> BindingAwareBroker.RoutedRpcRegistration<T> addRoutedRpcImplementation(
        Class<T> serviceInterface, T implementation) throws IllegalStateException {
        return null;
    }

    @Override public <L extends RouteChangeListener<RpcContextIdentifier,
        InstanceIdentifier<?>>> ListenerRegistration<L> registerRouteChangeListener(L listener) {
        return null;
    }

    @Override public <T extends RpcService> T getRpcService(Class<T> serviceInterface) {
        return null;
    }
}
