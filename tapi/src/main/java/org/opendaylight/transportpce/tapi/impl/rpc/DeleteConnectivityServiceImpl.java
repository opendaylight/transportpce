/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDelete;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteConnectivityServiceImpl implements DeleteConnectivityService {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteConnectivityServiceImpl.class);

    private final RpcService rpcService;
    private final TapiContext tapiContext;
    private final NetworkTransactionService networkTransactionService;

    public DeleteConnectivityServiceImpl(RpcService rpcService, TapiContext tapiContext,
            NetworkTransactionService networkTransactionService) {
        this.rpcService = rpcService;
        this.tapiContext = tapiContext;
        this.networkTransactionService = networkTransactionService;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> invoke(DeleteConnectivityServiceInput input) {
        List<String> serviceName = null;
        if (input.getUuid() != null) {
            try {
                serviceName = getNameFromUuid(input.getUuid(), "Service");
            } catch (ExecutionException e) {
                LOG.error("Service {} to be deleted not found in the DataStore", e.getMessage());
                return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
                        .withError(ErrorType.RPC, "Failed to delete Service").buildFuture();
            } catch (NoSuchElementException e) {
                LOG.error("Service {} to be deleted not found in the DataStore", e.getMessage());
                return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
                        .withError(ErrorType.RPC, "Failed to delete Service").buildFuture();
            }
            LOG.debug("The service {}, of name {} has been found in the DS", input.getUuid().toString(), serviceName);
            try {
                Uuid serviceUuid = input.getUuid();
                this.tapiContext.deleteConnectivityService(serviceUuid);
                ListenableFuture<RpcResult<ServiceDeleteOutput>> output = rpcService.getRpc(ServiceDelete.class)
                        .invoke(new ServiceDeleteInputBuilder()
                                .setServiceDeleteReqInfo(
                                        new ServiceDeleteReqInfoBuilder().setServiceName(input.getUuid().getValue())
                                                .setTailRetention(ServiceDeleteReqInfo.TailRetention.No).build())
                                .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request-1")
                                        .setNotificationUrl("notification url").setRequestSystemId("appname")
                                        .setRpcAction(RpcActions.ServiceDelete).build())
                                .build());
                RpcResult<ServiceDeleteOutput> rpcResult = output.get();
                if (!rpcResult.getResult().getConfigurationResponseCommon().getResponseCode()
                        .equals(ResponseCodes.RESPONSE_FAILED)) {
                    LOG.info("Service is being deleted and devices are being rolled back");
                    return RpcResultBuilder.success(new DeleteConnectivityServiceOutputBuilder().build()).buildFuture();
                }
                LOG.error("Failed to delete service. Deletion process failed");
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to delete service.", e);
                return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
                        .withError(ErrorType.RPC, "Failed to delete Service").buildFuture();
            }
        }
        return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "Failed to delete Service, service uuid in input is null").buildFuture();
    }

    public List<String> getNameFromUuid(Uuid uuid, String typeOfNode)
            throws ExecutionException, NoSuchElementException {
        Map<NameKey, Name> nameMap = new HashMap<>();
        if ("Service".equals(typeOfNode)) {
            ConnectivityService conServ = null;
            InstanceIdentifier<ConnectivityService> nodeIID = InstanceIdentifier.builder(Context.class)
                    .augmentation(
                            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1.class)
                    .child(ConnectivityContext.class).child(ConnectivityService.class, new ConnectivityServiceKey(uuid))
                    .build();
            ListenableFuture<Optional<ConnectivityService>> conServFuture = this.networkTransactionService
                    .read(LogicalDatastoreType.OPERATIONAL, nodeIID);
            try {
                conServ = conServFuture.get().orElseThrow();
            } catch (InterruptedException e) {
                LOG.error("GetNamefromUuid Interrupt exception: Service not in Datastore, Interruption of the process");
                Thread.currentThread().interrupt();
                // TODO: investigate on how to throw Interrupted exception (generate a check
                // violation error)
            } catch (ExecutionException e) {
                throw new ExecutionException("Unable to get from mdsal service: "
                        + nodeIID.firstKeyOf(ConnectivityService.class).getUuid().getValue(), e);
            } catch (NoSuchElementException e) {
                throw new NoSuchElementException("Unable to get from mdsal service: "
                        + nodeIID.firstKeyOf(ConnectivityService.class).getUuid().getValue(), e);
                // return null;
            }
            nameMap = conServ.getName();
        }

        List<String> nameList = new ArrayList<>();
        for (Map.Entry<NameKey, Name> entry : nameMap.entrySet()) {
            nameList.add(entry.getValue().getValue());
        }
        LOG.debug("The service name of service {}, is {}", uuid.toString(), nameList.toString());
        return nameList;
    }

}
