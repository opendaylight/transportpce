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
package io.fd.honeycomb.transportpce.device.rpcs;

import io.fd.honeycomb.rpc.RpcService;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RpcStatus;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrailInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrailOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.GetConnectionPortTrailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.get.connection.port.trail.output.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.get.connection.port.trail.output.PortsBuilder;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to simulate get-connection-port-trail rpc.
 *
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class ConnectionPortTrailService implements RpcService<GetConnectionPortTrailInput, GetConnectionPortTrailOutput> {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPortTrailService.class);
    private static final String localName = "get-connection-port-trail";
    private static final QName name = QName.create(GetConnectionPortTrailInput.QNAME, localName);
    private static final SchemaPath schemaPath = SchemaPath.ROOT.createChild(name);

    @Override
    public SchemaPath getManagedNode() {
        return schemaPath;
    }

    /* (non-Javadoc)
     * @see io.fd.honeycomb.rpc.RpcService#invoke(org.opendaylight.yangtools.yang.binding.DataObject)
     */
    @Override
    public CompletionStage<GetConnectionPortTrailOutput> invoke(GetConnectionPortTrailInput arg0) {
        LOG.info("RPC GetConnectionPortTrail request received !");
        Ports port = new PortsBuilder()
                .setCircuitPackName("2/0")
                .setPortName("L1")
                .build();
        GetConnectionPortTrailOutput output = new GetConnectionPortTrailOutputBuilder()
                .setStatusMessage("OK")
                .setStatus(RpcStatus.Successful)
                .setPorts(Arrays.asList(port))
                .build();
        CompletableFuture<GetConnectionPortTrailOutput> result = new CompletableFuture<>();
        result.complete(output);
        return result;
    }

}
