/*
 * Copyright (c) 2018 Orange and/or its affiliates.
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
package io.fd.honeycomb.transportpce.device.read;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.config.util.capability.Capability;
import org.opendaylight.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.netconf.api.monitoring.NetconfMonitoringService;
import org.opendaylight.netconf.api.monitoring.SessionEvent;
import org.opendaylight.netconf.api.monitoring.SessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Yang;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Capabilities;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.CapabilitiesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Schemas;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SchemasBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.Sessions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.SessionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.Schema.Location.Enumeration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.schemas.SchemaKey;

public class DummyMonitoringService implements NetconfMonitoringService {

    private static final Sessions EMPTY_SESSIONS = new SessionsBuilder().setSession(Collections.emptyList()).build();
    private static final Function<Capability, Uri> CAPABILITY_URI_FUNCTION =
        capability -> new Uri(capability.getCapabilityUri());

    private static final Function<Capability, Schema> CAPABILITY_SCHEMA_FUNCTION = new Function<Capability, Schema>() {
        @Nullable
        @Override
        public Schema apply(@Nonnull final Capability capability) {
            return new SchemaBuilder()
                    .setIdentifier(capability.getModuleName().get())
                    .setNamespace(new Uri(capability.getModuleNamespace().get()))
                    .setFormat(Yang.class)
                    .setVersion(capability.getRevision().get())
                    .setLocation(Collections.singletonList(new Location(Enumeration.NETCONF)))
                    .setKey(new SchemaKey(Yang.class, capability.getModuleName().get(),
                        capability.getRevision().get()))
                    .build();
        }
    };

    private final Capabilities capabilities;
    private final ArrayListMultimap<String, Capability> capabilityMultiMap;
    private final Schemas schemas;

    public DummyMonitoringService(final Set<Capability> capabilities) {

        this.capabilities = new CapabilitiesBuilder().setCapability(
                Lists.newArrayList(Collections2.transform(capabilities, CAPABILITY_URI_FUNCTION))).build();

        Set<Capability> moduleCapabilities = Sets.newHashSet();
        this.capabilityMultiMap = ArrayListMultimap.create();
        for (Capability cap : capabilities) {
            if (cap.getModuleName().isPresent()) {
                capabilityMultiMap.put(cap.getModuleName().get(), cap);
                moduleCapabilities.add(cap);
            }
        }

        this.schemas = new SchemasBuilder().setSchema(
            Lists.newArrayList(Collections2.transform(moduleCapabilities, CAPABILITY_SCHEMA_FUNCTION))).build();
    }

    @Override
    public Sessions getSessions() {
        return EMPTY_SESSIONS;
    }

    @Override
    public SessionListener getSessionListener() {
        return new SessionListener() {
            @Override
            public void onSessionUp(final NetconfManagementSession session) {
                //no op
            }

            @Override
            public void onSessionDown(final NetconfManagementSession session) {
                //no op
            }

            @Override
            public void onSessionEvent(final SessionEvent event) {
                //no op
            }
        };
    }

    @Override
    public Schemas getSchemas() {
        return schemas;
    }

    @Override
    public String getSchemaForCapability(final String moduleName, final Optional<String> revision) {

        for (Capability capability : capabilityMultiMap.get(moduleName)) {
            if (capability.getRevision().get().equals(revision.get())) {
                return capability.getCapabilitySchema().get();
            }
        }
        throw new IllegalArgumentException(
            "Module with name: " + moduleName + " and revision: " + revision + " does not exist");
    }

    @Override
    public Capabilities getCapabilities() {
        return capabilities;
    }

    @Override
    public AutoCloseable registerCapabilitiesListener(final CapabilitiesListener listener) {
        return null;
    }

    @Override
    public AutoCloseable registerSessionsListener(final SessionsListener listener) {
        return null;
    }

}