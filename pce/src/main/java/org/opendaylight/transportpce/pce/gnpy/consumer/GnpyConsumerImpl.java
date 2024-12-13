/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "org.opendaylight.transportpce.pce")
public class GnpyConsumerImpl implements GnpyConsumer {
    @ObjectClassDefinition
    public @interface Configuration {
        @AttributeDefinition
        String url() default "http://127.0.0.1:8008";
        @AttributeDefinition
        String username() default "gnpy";
        @AttributeDefinition
        String password() default "gnpy";
    }

    private static final Logger LOG = LoggerFactory.getLogger(GnpyConsumerImpl.class);

    private final GnpyResource api;
    private Client client;

    @Activate
    public GnpyConsumerImpl(final Configuration configuration,
            @Reference BindingDOMCodecServices bindingDOMCodecServices) {
        this(configuration.url(), configuration.username(), configuration.password(), bindingDOMCodecServices);
    }

    public GnpyConsumerImpl(String baseUrl, String username, String password,
            BindingDOMCodecServices bindingDOMCodecServices) {
        JsonStringConverter<Request> gnpyRequestConverter = new JsonStringConverter<>(bindingDOMCodecServices);
        JsonStringConverter<Result> resultConverter = new JsonStringConverter<>(bindingDOMCodecServices);

        Client client = ClientBuilder.newClient();
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic(username, password);
        client.register(authFeature);
        client.register(new LoggingFeature(java.util.logging.Logger.getLogger(this.getClass().getName())))
            .register(JacksonFeature.class)
                .register(new ResultMessageBodyReader(resultConverter))
                .register(new RequestMessageBodyWriter(gnpyRequestConverter));
        api = WebResourceFactory.newResource(GnpyResource.class, client.target(baseUrl));
    }

    @Deactivate
    public void close() {
        if (this.client != null) {
            LOG.info("Closing client {}", this.client);
            this.client.close();
        }
        this.client = null;
    }

    @Override
    public boolean isAvailable() {
        try {
            api.getStatus();
            LOG.info("GNPy is available");
            return true;
        } catch (WebApplicationException | ProcessingException e) {
            LOG.info("GNPy is not available ", e);
            return false;
        }
    }

    @Override
    public Result computePaths(Request request) {
        try {
            return api.computePathRequest(request);
        } catch (WebApplicationException | ProcessingException e) {
            LOG.info("Something went wrong while requesting GNPy ", e);
            return null;
        }
    }
}
