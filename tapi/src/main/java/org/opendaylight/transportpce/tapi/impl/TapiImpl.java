/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.tapi.utils.GenericServiceEndpoint;
import org.opendaylight.transportpce.tapi.utils.MappingUtils;
import org.opendaylight.transportpce.tapi.utils.TapiUtils;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
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
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiImpl implements TapiConnectivityService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiImpl.class);

    private ServiceHandlerOperations serviceHandler;

    public TapiImpl(ServiceHandlerOperations serviceHandler) {
        LOG.info("inside TapiImpl constructor");
        this.serviceHandler = serviceHandler;
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(
        CreateConnectivityServiceInput input) {
        LOG.info("RPC create-connectivity received");
        LOG.info(input.getEndPoint().toString());
        OperationResult validationResult = CreateConnectivityServiceValidation.validateCreateConnectivityServiceRequest(
            input);
        if (validationResult.isSuccess()) {
            LOG.info("input parameter of RPC create-connectivity are being handled");
            // check uuid of SIP in the map
            Map<Uuid, GenericServiceEndpoint> map = MappingUtils.getMap();

            if (map.containsKey(input.getEndPoint().get(0).getServiceInterfacePoint().getServiceInterfacePointUuid())
                && map.containsKey(input.getEndPoint().get(1).getServiceInterfacePoint()
                    .getServiceInterfacePointUuid())) {
                ServiceCreateInput sci = TapiUtils.buildServiceCreateInput(map.get(input.getEndPoint().get(0)
                    .getServiceInterfacePoint()
                    .getServiceInterfacePointUuid()), map.get(input.getEndPoint().get(1).getServiceInterfacePoint()
                        .getServiceInterfacePointUuid()));
                ServiceCreateOutput output = this.serviceHandler.serviceCreate(sci);
            } else {
                LOG.error("Unknown UUID");
            }

        }

        List<EndPoint> endPointList = new ArrayList<>();
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
        endPointList.add(endpoint1);
        endPointList.add(endpoint2);
        List<Connection> connectionList = new ArrayList<>();
        Connection connection1 = new ConnectionBuilder().setConnectionUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        connectionList.add(connection1);
        ConnectivityService service = new ConnectivityServiceBuilder().build();
        List<Name> serviceNameList = new ArrayList<>();
        Name serviceName = new NameBuilder().setValueName("Service Name").setValue("SENDATE Service 1").build();
        serviceNameList.add(serviceName);
        CreateConnectivityServiceOutput output = new CreateConnectivityServiceOutputBuilder()
            .setService(new ServiceBuilder(service)
                .setUuid(new Uuid(UUID.randomUUID().toString()))
                .setName(serviceNameList)
                .setServiceLayer(input.getEndPoint().get(0).getLayerProtocolName())
                .setEndPoint(endPointList)
                .setConnection(connectionList)
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
        LOG.info("RPC delete-connectivity received");
        ServiceDeleteInput inputSh = null;
        this.serviceHandler.serviceDelete(inputSh);
        //TODO to continue...
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
