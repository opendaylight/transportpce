/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.stub;

import com.google.common.base.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.BindingService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MountPointStub  implements MountPoint {

    private DataBroker dataBroker;

    private RpcConsumerRegistry rpcConsumerRegistry;

    public MountPointStub(DataBroker dataBroker) {

        this.dataBroker = dataBroker;
    }

    public void setRpcConsumerRegistry(RpcConsumerRegistry rpcConsumerRegistry) {
        this.rpcConsumerRegistry = rpcConsumerRegistry;
    }

    @Override
    public <T extends BindingService> Optional<T> getService(Class<T> service) {
        if (service.isInstance(dataBroker)) {
            return Optional.fromNullable((T)dataBroker);
        }
        if (service.isInstance(rpcConsumerRegistry)) {
            return Optional.fromNullable((T)rpcConsumerRegistry);
        }
        return Optional.absent();
    }

    @Nonnull
    @Override
    public InstanceIdentifier<?> getIdentifier() {
        return null;
    }
}
