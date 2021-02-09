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
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200909.service.PathRequest;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/gnpy/api/v1.0/files")
public class GnpyStub {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyStub.class);

    @HEAD
    public Response testConnection() {
        return Response.ok().build();
    }

    @POST
    @Produces({ "application/json" })
    @Consumes({ "application/json" })
    public Response computePath(String request) {
        LOG.info("Received path request {}", request);
        URI location = URI.create("http://127.0.0.1:9998/gnpy/api/v1.0/files");
        // TODO: return different response based on body data
        QName pathQname = QName.create("gnpy:gnpy-api", "2019-01-03", "gnpy-api");
        YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
        JsonStringConverter<GnpyApi> converter = new JsonStringConverter<>(
                AbstractTest.getDataStoreContextUtil().getBindingDOMCodecServices());
        try {
            String response = null;
            request = request.replace("Transceiver", "gnpy-network-topology:Transceiver")
                    .replace("Roadm", "gnpy-network-topology:Roadm")
                    .replace("Fiber", "gnpy-network-topology:Fiber")
                    .replace("km", "gnpy-network-topology:km")
                    .replace("route-include-ero", "gnpy-path-computation-simplified:route-include-ero");
            GnpyApi data = converter.createDataObjectFromJsonString(yangId,
                    request, JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02);
            LOG.info("Converted request {}", data);
            List<PathRequest> pathRequest = new ArrayList<>(data.getServiceFile().nonnullPathRequest().values());
            // this condition is totally arbitrary and could be modified
            if (!pathRequest.isEmpty() && "127.0.0.31".contentEquals(pathRequest.get(0).getSource().stringValue())) {
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
