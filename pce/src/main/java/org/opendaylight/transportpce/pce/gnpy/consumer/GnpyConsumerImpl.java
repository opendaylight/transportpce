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
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Request;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GnpyConsumerImpl implements GnpyConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyConsumerImpl.class);

    private GnpyResource api;
    JsonStringConverter<Request> gnpyRequestConverter;
    JsonStringConverter<Result> resultConverter;

    public GnpyConsumerImpl(String baseUrl, String username, String password,
            BindingDOMCodecServices bindingDOMCodecServices) {
        gnpyRequestConverter = new JsonStringConverter<>(bindingDOMCodecServices);
        resultConverter = new JsonStringConverter<>(bindingDOMCodecServices);

        JsonConfigurator jsonConfigurator = new JsonConfigurator(gnpyRequestConverter, resultConverter);
        Client client = ClientBuilder.newClient();
        HttpAuthenticationFeature authFeature = HttpAuthenticationFeature.basic(username, password);
        client.register(authFeature);
        client.register(new LoggingFeature(java.util.logging.Logger.getLogger(this.getClass().getName())))
            .register(JacksonFeature.class).register(jsonConfigurator);
        api = WebResourceFactory.newResource(GnpyResource.class, client.target(baseUrl));
    }

    @Override
    public boolean isAvailable() {
        try {
            api.checkStatus();
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
