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

import java.net.URL;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class HttpsConnectorProvider extends ProviderTrait<ServerConnector> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpsConnectorProvider.class);

    @Inject
    private RestconfConfiguration cfg;
    @Inject
    private Server server;

    @Override
    protected ServerConnector create() {
        if (!cfg.isRestconfHttpsEnabled()) {
            LOG.debug("RESTCONF HTTPS disabled, skipping initialization");
            return null;
        }
        LOG.info("Starting RESTCONF HTTPS");
        // SSL Context Factory
        // Based on:
        // https://github.com/eclipse/jetty.project/blob/jetty-9.3.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java
        // https://wiki.eclipse.org/Jetty/Howto/Configure_SSL#Loading_Keys_and_Certificates_via_PKCS12
        // Keystore created with:
        // openssl genrsa -des3 -out honeycomb.key
        // openssl req -new -x509 -key honeycomb.key -out honeycomb.crt
        // openssl pkcs12 -inkey honeycomb.key -in honeycomb.crt -export -out honeycomb.pkcs12
        // keytool -importkeystore -srckeystore honeycomb.pkcs12 -srcstoretype PKCS12 -destkeystore honeycomb-keystore
        SslContextFactory sslContextFactory = new SslContextFactory();
        URL keystoreURL = getClass().getResource(cfg.restconfKeystore.get());
        sslContextFactory.setKeyStorePath(keystoreURL.getPath());
        sslContextFactory.setKeyStorePassword(cfg.keystorePassword.get());
        sslContextFactory.setKeyManagerPassword((cfg.keystoreManagerPassword.get()));
        URL truststoreURL = getClass().getResource(cfg.restconfTruststore.get());
        sslContextFactory.setTrustStorePath(truststoreURL.getPath());
        sslContextFactory.setTrustStorePassword((cfg.truststorePassword.get()));
        // TODO HONEYCOMB-167 make this more configurable
        sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
                "SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
                "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

        // SSL Connector
        ServerConnector sslConnector =
                new ServerConnector(server, cfg.httpsAcceptorsSize.get(), cfg.httpsSelectorsSize.get(),
                        // The ssl connection factory delegates the real processing to http connection factory
                        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                        // That's why http connection factory is also required here
                        // Order is IMPORTANT here
                        new HttpConnectionFactory()
                );
        sslConnector.setHost(cfg.restconfHttpsBindingAddress.get());
        sslConnector.setPort(cfg.restconfHttpsPort.get());
        server.addConnector(sslConnector);
        return sslConnector;
    }
}
