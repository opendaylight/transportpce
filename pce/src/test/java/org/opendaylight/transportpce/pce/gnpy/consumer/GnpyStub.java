/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev201022.service.PathRequest;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/api/v1")
public class GnpyStub {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyStub.class);

    @HEAD
    public Response testConnection() {
        return Response.ok().build();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatus() {
        try {
            String response = Files.readString(Paths.get("src", "test", "resources", "gnpy", "gnpy_status.json"));
            return Response.ok(response).build();
        } catch (IOException e) {
            LOG.error("Cannot manage request", e);
            return Response.serverError().build();
        }
    }

    @POST
    @Produces({ "application/json" })
    @Consumes({ "application/json" })
    @Path("/path-computation")
    public Response computePath(String request) {
        LOG.info("Received path request {}", request);
        URI location = URI.create("http://127.0.0.1:9998/api/v1/path-computation");
        // TODO: return different response based on body data
        QName pathQname = Request.QNAME;
        YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
        JsonStringConverter<Request> converter = new JsonStringConverter<>(
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        try {
            String response = null;
            request = request.replace("Transceiver", "gnpy-network-topology:Transceiver")
                    .replace("Roadm", "gnpy-network-topology:Roadm")
                    .replace("Fiber", "gnpy-network-topology:Fiber")
                    .replace("km", "gnpy-network-topology:km");
//                    .replace("route-include-ero", "gnpy-path-computation-simplified:route-include-ero");
            Request data = converter.createDataObjectFromJsonString(yangId,
                    request, JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
            LOG.info("Converted request {}", data);
            List<PathRequest> pathRequest = new ArrayList<>(data.getService().nonnullPathRequest().values());
            // this condition is totally arbitrary and could be modified
            if (!pathRequest.isEmpty() && "127.0.0.31".contentEquals(pathRequest.get(0).getSource())) {
                response = Files
                        .readString(Paths.get("src", "test", "resources", "gnpy", "gnpy_result_with_path.json"));
            } else {
                response = Files.readString(Paths.get("src", "test", "resources", "gnpy", "gnpy_result_no_path.json"));
            }

            return Response.created(location).entity(response).build();
        } catch (IOException e) {
            LOG.error("Cannot manage request", e);
            return Response.serverError().build();
        }
    }

}
