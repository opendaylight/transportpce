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
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public final class NetworkTransactionImpl implements NetworkTransactionService {
    private final RequestProcessor requestProcessor;

    @Activate
    public NetworkTransactionImpl(@Reference DataBroker dataBroker) {
        this.requestProcessor = new RequestProcessor(dataBroker);
    }

    public <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
            DataObjectIdentifier<T> path) {
        return requestProcessor.read(store, path);
    }



    public void delete(LogicalDatastoreType store, DataObjectIdentifier<?> path) {
        requestProcessor.delete(store, path);
    }



    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        requestProcessor.put(store, path, data);
    }

    public FluentFuture<? extends @NonNull CommitInfo> commit() {
        return requestProcessor.commit();
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store, DataObjectIdentifier<T> path, T data) {
        requestProcessor.merge(store, path, data);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opendaylight.transportpce.common.network.NetworkTransactionService#getDataBroker()
     */
    @Override
    public DataBroker getDataBroker() {
        return requestProcessor.getDataBroker();
    }


}
