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

package io.fd.honeycomb.infra.distro.data.context;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import io.fd.honeycomb.data.ModifiableDataManager;
import io.fd.honeycomb.data.init.DataTreeInitializer;
import io.fd.honeycomb.infra.distro.data.BindingDataBrokerProvider;
import io.fd.honeycomb.infra.distro.data.DataTreeProvider;
import io.fd.honeycomb.infra.distro.data.PersistingDataTreeProvider;
import io.fd.honeycomb.translate.MappingContext;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;

public class ContextPipelineModule extends PrivateModule {

    public static final String HONEYCOMB_CONTEXT_NOPERSIST = "honeycomb-context-nopersist";
    public static final String HONEYCOMB_CONTEXT = "honeycomb-context";

    @Override
    protected void configure() {
        // Non persisting data tree for context
        DataTreeProvider.ContextDataTreeProvider noPersistDataTreeProvider =
                new DataTreeProvider.ContextDataTreeProvider();
        bind(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT_NOPERSIST))
                .toProvider(noPersistDataTreeProvider).in(Singleton.class);
        expose(DataTree.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT_NOPERSIST));
        // Persisting data tree wrapper for context
        PersistingDataTreeProvider.ContextPersistingDataTreeProvider dataTreeProvider =
                new PersistingDataTreeProvider.ContextPersistingDataTreeProvider();
        bind(DataTree.class).toProvider(dataTreeProvider).in(Singleton.class);

        // Data Tree manager (without any delegation) on top of context data tree
        bind(ModifiableDataManager.class).toProvider(ModifiableDTMgrProvider.class).in(Singleton.class);

        // DOMDataBroker interface on top of data tree manager
        HoneycombContextDOMDataBrokerProvider domBrokerProvider = new HoneycombContextDOMDataBrokerProvider();
        bind(DOMDataBroker.class).toProvider(domBrokerProvider).in(Singleton.class);

        // BA version of data broker for context
        bind(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT)).toProvider(BindingDataBrokerProvider.class)
                .in(Singleton.class);
        expose(DataBroker.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT));

        // Create initializer to init persisted config data
        bind(DataTreeInitializer.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT))
                .toProvider(PersistedContextInitializerProvider.class)
                .in(Singleton.class);
        expose(DataTreeInitializer.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT));

        // Mapping context is just a small adapter on top of BA data broker to simplify CRUD of context data
        bind(MappingContext.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT))
                .toProvider(RealtimeMappingContextProvider.class).in(Singleton.class);
        expose(MappingContext.class).annotatedWith(Names.named(HONEYCOMB_CONTEXT));
    }
}
