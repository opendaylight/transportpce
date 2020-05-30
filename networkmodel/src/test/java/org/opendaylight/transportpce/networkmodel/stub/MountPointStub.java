/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.stub;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.eclipse.jdt.annotation.NonNull;
import org.mockito.Mock;
import org.opendaylight.mdsal.binding.api.BindingService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.NotificationListener;


public class MountPointStub implements MountPoint {

    private DataBroker dataBroker;

    private RpcConsumerRegistry rpcConsumerRegistry;


    private NotificationService notificationService = new NotificationService() {
        @Override
        public @NonNull <T extends NotificationListener> ListenerRegistration<T> registerNotificationListener(@NonNull T listener) {
            return null;
        }
    };

    public MountPointStub(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setRpcConsumerRegistry(
            RpcConsumerRegistry rpcConsumerRegistry) {
        this.rpcConsumerRegistry = rpcConsumerRegistry;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BindingService> Optional<T> getService(Class<T> service) {
        if (service.isInstance(dataBroker)) {
            return Optional.ofNullable((T) dataBroker);
        }
        if (service.isInstance(rpcConsumerRegistry)) {
            return Optional.ofNullable((T) rpcConsumerRegistry);
        }
        if (service.isInstance(notificationService)) {
            return Optional.ofNullable((T) notificationService);
        }
        return Optional.empty();
    }

    @Nonnull
    @Override
    public InstanceIdentifier<?> getIdentifier() {
        throw new UnsupportedOperationException();
    }
}
