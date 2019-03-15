/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.network;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;



public class NetworkTransactionImpl implements NetworkTransactionService {

    RequestProcessor requestProcessor;

    public NetworkTransactionImpl(RequestProcessor requestProcessor) {
        this.requestProcessor = requestProcessor;

    }

    public <T extends DataObject> CheckedFuture<com.google.common.base.Optional<T>,
        ReadFailedException> read(LogicalDatastoreType store, InstanceIdentifier<T> path) {
        return requestProcessor.read(store, path);
    }



    public void delete(LogicalDatastoreType store, InstanceIdentifier<?> path) {
        requestProcessor.delete(store, path);
    }


    public <T extends DataObject> void put(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data, boolean createMissingParents) {

        requestProcessor.put(store, path, data, createMissingParents);
    }

    @Override
    public <T extends DataObject> void put(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {
        requestProcessor.put(store, path, data);
    }

    public ListenableFuture<Void> submit() {
        return requestProcessor.submit();
    }

    @Override
    public void close() {

        requestProcessor.close();
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data) {
        requestProcessor.merge(store, path, data);
    }

    public <T extends DataObject> void merge(LogicalDatastoreType store,
        InstanceIdentifier<T> path, T data, boolean createMissingParents) {

        requestProcessor.merge(store, path, data, createMissingParents);
    }

}
