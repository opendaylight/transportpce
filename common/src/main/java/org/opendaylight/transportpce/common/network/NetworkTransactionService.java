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
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;



public interface NetworkTransactionService {

    <T extends DataObject> ListenableFuture<Optional<T>> read(LogicalDatastoreType store,
                                                                                InstanceIdentifier<T> path);

    void delete(LogicalDatastoreType store, InstanceIdentifier<?> path);

    <T extends DataObject> void put(LogicalDatastoreType store, InstanceIdentifier<T> path,T data);

    <T extends DataObject> void merge(LogicalDatastoreType store, InstanceIdentifier<T> path, T data);

    FluentFuture<? extends @NonNull CommitInfo> commit();

    void close();
}
