/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.stub;

import java.util.Optional;
import org.opendaylight.mdsal.binding.api.BindingService;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MountPointStub implements MountPoint {

    private DataBroker dataBroker;

    private RpcService rpcService;

    public MountPointStub(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    public void setRpcService(RpcService rpcService) {
        this.rpcService = rpcService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BindingService> Optional<T> getService(Class<T> service) {
        if (service.isInstance(dataBroker)) {
            return Optional.ofNullable((T) dataBroker);
        }
        if (service.isInstance(rpcService)) {
            return Optional.ofNullable((T) rpcService);
        }
        return Optional.empty();
    }

    @Override
    public InstanceIdentifier<?> getIdentifier() {
        throw new UnsupportedOperationException();
    }
}
