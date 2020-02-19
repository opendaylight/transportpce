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
import io.fd.honeycomb.binding.init.ProviderTrait;

import java.util.concurrent.ExecutorService;

import org.opendaylight.mdsal.common.api.LogicalDatastoreType;

import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;



public final class DataStoreProvider extends ProviderTrait<InMemoryDOMDataStore> {

    @Inject
    private DOMSchemaService schemaService;
    private String name;
    private LogicalDatastoreType type;

    public DataStoreProvider(final String name, final  LogicalDatastoreType type) {
        this.name = name;
        this.type=type;
    }

    @Override
    protected InMemoryDOMDataStore create() {
         final ExecutorService dataChangeListenerExecutor = createExecutorService(name, InMemoryDOMDataStoreConfigProperties.getDefault());
         final InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name, this.type, dataChangeListenerExecutor,
                 InMemoryDOMDataStoreConfigProperties.getDefault().getMaxDataChangeListenerQueueSize(), InMemoryDOMDataStoreConfigProperties.getDefault().getDebugTransactions());

         if (schemaService != null) {
             schemaService.registerSchemaContextListener(dataStore);
         }

         return dataStore;
    }

    private static ExecutorService createExecutorService(final String name,
            final InMemoryDOMDataStoreConfigProperties props) {
        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.
        return SpecialExecutors.newBlockingBoundedFastThreadPool(
            props.getMaxDataChangeExecutorPoolSize(), props.getMaxDataChangeExecutorQueueSize(),
            name + "-DCL", InMemoryDOMDataStore.class);
    }

}
