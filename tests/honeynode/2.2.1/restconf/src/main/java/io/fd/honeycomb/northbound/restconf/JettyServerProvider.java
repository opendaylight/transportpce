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

package io.fd.honeycomb.northbound.restconf;

import com.google.inject.Inject;
import io.fd.honeycomb.binding.init.ProviderTrait;
import io.fd.honeycomb.northbound.CredentialsConfiguration;
import java.net.URL;
import java.util.Collections;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;

final class JettyServerProvider extends ProviderTrait<Server> {

    private static final String REALM = "HCRealm";
    // Mime types to be compressed when requested
    private static final String[] GZIP_MIME_TYPES = {"application/xml",
        "xml",
        "application/yang.data+xml",
        "application/json",
        "application/yang.data+json"};

    @Inject
    private RestconfConfiguration cfg;

    @Inject
    private CredentialsConfiguration credentialsCfg;

    @Override
    protected Server create() {
        Server server = new Server(new QueuedThreadPool(cfg.restPoolMaxSize.get(), cfg.restPoolMinSize.get()));

        // Load Realm for basic auth
        HashLoginService service = new HashLoginService(REALM);
        // Reusing the name as role
        service.putUser(credentialsCfg.username, new Password(credentialsCfg.password),
                new String[]{credentialsCfg.username});
        server.addBean(service);

        final URL resource = getClass().getResource("/");
        WebAppContext webapp = new WebAppContext(resource.getPath(), cfg.restconfRootPath.get());

        server.setHandler(getGzip(service, webapp));
        return server;
    }

    private GzipHandler getGzip(final HashLoginService service, final WebAppContext webapp) {
        final GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedMimeTypes(GZIP_MIME_TYPES);
        gzipHandler.setHandler(getBaseAuth(service, webapp));
        return gzipHandler;
    }

    private ConstraintSecurityHandler getBaseAuth(HashLoginService service, WebAppContext webapp) {
        Constraint constraint = new Constraint();
        constraint.setName("auth");
        constraint.setAuthenticate(true);
        constraint.setRoles(new String[]{credentialsCfg.username});

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        mapping.setConstraint(constraint);

        ConstraintSecurityHandler security = new ConstraintSecurityHandler();
        security.setConstraintMappings(Collections.singletonList(mapping));
        security.setAuthenticator(new BasicAuthenticator());
        security.setLoginService(service);

        security.setHandler(webapp);
        return security;
    }
}
