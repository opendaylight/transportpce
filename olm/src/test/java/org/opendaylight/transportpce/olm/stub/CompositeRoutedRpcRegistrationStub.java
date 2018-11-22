/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.olm.stub;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMRpcProviderServiceAdapter;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.yangtools.concepts.ObjectRegistration;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.RpcService;

public class CompositeRoutedRpcRegistrationStub<T extends RpcService> implements
    BindingAwareBroker.RoutedRpcRegistration<T> {
    private final Class<T> type;
    private final T instance;
    private final BindingDOMRpcProviderServiceAdapter adapter;
    private final Map<InstanceIdentifier<?>, ObjectRegistration<T>> registrations = new HashMap(2);

    CompositeRoutedRpcRegistrationStub(final Class<T> type, final T impl,
        final BindingDOMRpcProviderServiceAdapter providerAdapter) {
        this.type = type;
        this.instance = impl;
        this.adapter = providerAdapter;
    }

    @Override public void registerInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> myinstance) {

    }

    @Override public void unregisterInstance(Class<? extends BaseIdentity> context, InstanceIdentifier<?> myinstance) {

    }

    @Override public void registerPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {

    }

    @Override public void unregisterPath(Class<? extends BaseIdentity> context, InstanceIdentifier<?> path) {

    }

    @Override public Class<T> getServiceType() {
        return null;
    }

    @Override public void close() {

    }

    @Nonnull
    @Override public T getInstance() {
        return null;
    }
}
