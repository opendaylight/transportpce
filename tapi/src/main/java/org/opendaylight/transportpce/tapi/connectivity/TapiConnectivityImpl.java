/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.tapi.utils.GenericServiceEndpoint;
import org.opendaylight.transportpce.tapi.utils.MappingUtils;
import org.opendaylight.transportpce.tapi.utils.TapiUtils;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiConnectivityImpl implements TapiConnectivityService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImpl.class);

    private OrgOpenroadmServiceService serviceHandler;

    public TapiConnectivityImpl(OrgOpenroadmServiceService serviceHandler) {
        LOG.info("inside TapiImpl constructor");
        this.serviceHandler = serviceHandler;
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(
        CreateConnectivityServiceInput input) {
        LOG.info("RPC create-connectivity received: {}", input.getEndPoint());
        OperationResult validationResult = CreateConnectivityServiceValidation.validateCreateConnectivityServiceRequest(
            input);
        if (validationResult.isSuccess()) {
            LOG.info("input parameter of RPC create-connectivity are being handled");
            // check uuid of SIP in the map
            Map<Uuid, GenericServiceEndpoint> map = MappingUtils.getMap();

            if (map.containsKey(input.getEndPoint().values().stream().findFirst().get()
                    .getServiceInterfacePoint().getServiceInterfacePointUuid())
                && map.containsKey(input.getEndPoint().values().stream().skip(1).findFirst().get()
                    .getServiceInterfacePoint()
                    .getServiceInterfacePointUuid())) {
                ServiceCreateInput sci = TapiUtils.buildServiceCreateInput(
                    map.get(input.getEndPoint().values().stream().findFirst().get()
                        .getServiceInterfacePoint()
                        .getServiceInterfacePointUuid()),
                    map.get(input.getEndPoint().values().stream().skip(1).findFirst().get()
                        .getServiceInterfacePoint()
                        .getServiceInterfacePointUuid()));
                ListenableFuture<RpcResult<ServiceCreateOutput>> output = this.serviceHandler.serviceCreate(sci);
                if (!output.isDone()) {
                    return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().buildFuture();
                }
            } else {
                LOG.error("Unknown UUID");
            }

        }

        Map<EndPointKey, EndPoint> endPointList = new HashMap<>();
        EndPoint endpoint1 = new EndPointBuilder()
            .setLocalId(UUID.randomUUID().toString())
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(new Uuid(UUID
                .randomUUID().toString())).build())
            .build();
        EndPoint endpoint2 = new EndPointBuilder()
            .setLocalId(UUID.randomUUID().toString())
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(new Uuid(UUID
                .randomUUID().toString())).build())
            .build();
        endPointList.put(endpoint1.key(), endpoint1);
        endPointList.put(endpoint2.key(), endpoint2);
        Connection connection = new ConnectionBuilder().setConnectionUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        ConnectivityService service = new ConnectivityServiceBuilder()
            .setUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        Name serviceName = new NameBuilder().setValueName("Service Name").setValue("SENDATE Service 1").build();
        CreateConnectivityServiceOutput output = new CreateConnectivityServiceOutputBuilder()
            .setService(new ServiceBuilder(service)
                .setUuid(new Uuid(UUID.randomUUID().toString()))
                .setName(Map.of(serviceName.key(), serviceName))
                .setServiceLayer(input.getEndPoint().values().stream().findFirst().get().getLayerProtocolName())
                .setEndPoint(endPointList)
                .setConnection(Map.of(connection.key(), connection))
                .build())
            .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(
        GetConnectivityServiceDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(
        UpdateConnectivityServiceInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(
        GetConnectionDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(
        DeleteConnectivityServiceInput input) {
        //TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList(
        GetConnectivityServiceListInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionEndPointDetailsOutput>> getConnectionEndPointDetails(
        GetConnectionEndPointDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }
}
