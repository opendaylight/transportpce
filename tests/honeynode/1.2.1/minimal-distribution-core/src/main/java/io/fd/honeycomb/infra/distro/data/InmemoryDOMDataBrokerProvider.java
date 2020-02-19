/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.infra.distro.data;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.binding.init.ProviderTrait;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitDeadlockException;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

public final class InmemoryDOMDataBrokerProvider extends ProviderTrait<DOMDataBroker> {

    public static final String CONFIG = "config";
    public static final String OPERATIONAL = "operational";

    @Inject
    @Named(InmemoryDOMDataBrokerProvider.CONFIG)
    private InMemoryDOMDataStore cfgDataStore;
    @Inject
    @Named(InmemoryDOMDataBrokerProvider.OPERATIONAL)
    private InMemoryDOMDataStore operDataStore;

    @Override
    protected SerializedDOMDataBroker create() {
        // This Databroker is dedicated for netconf metadata, not expected to be under heavy load
        ExecutorService listenableFutureExecutor =
            SpecialExecutors.newBlockingBoundedCachedThreadPool(1, 100, "commits", getClass());
        ExecutorService commitExecutor =
            SpecialExecutors.newBoundedSingleThreadExecutor(100, "WriteTxCommit", getClass());
        // TODO HONEYCOMB-164 try to provide more lightweight implementation of DataBroker

        Map<LogicalDatastoreType, DOMStore> map = new LinkedHashMap<>();
        map.put(LogicalDatastoreType.CONFIGURATION, cfgDataStore);
        map.put(LogicalDatastoreType.OPERATIONAL, operDataStore);

        return new SerializedDOMDataBroker(map, new DeadlockDetectingListeningExecutorService(commitExecutor,
            TransactionCommitDeadlockException.DEADLOCK_EXCEPTION_SUPPLIER, listenableFutureExecutor));
    }
}
