/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.network;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class NetworkTransactionImpl implements NetworkTransactionService {

    RequestProcessor requestProcessor;

    public NetworkTransactionImpl(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;

    }

    public <T extends DataObject> ListenableFuture<java.util.Optional<T>>
        read(LogicalDatastoreType store, InstanceIdentifier<T> path) {
        return requestProcessor.read(store, path);
    }



    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        requestProcessor.delete(store, path);
    }



    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {
        requestProcessor.put(store, path, data);
    }

    public FluentFuture<? extends @NonNull CommitInfo> commit() {
        return requestProcessor.commit();
    }

    @Override
    public void close() {

        requestProcessor.close();
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {
        requestProcessor.merge(store, path, data);
    }


}
