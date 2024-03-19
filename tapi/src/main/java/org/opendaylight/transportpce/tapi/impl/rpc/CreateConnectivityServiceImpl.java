/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreate;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CreateConnectivityServiceImpl implements CreateConnectivityService {
    private static final Logger LOG = LoggerFactory.getLogger(CreateConnectivityServiceImpl.class);

    private final RpcService rpcService;
    private final TapiContext tapiContext;
    private final ConnectivityUtils connectivityUtils;
    private TapiPceNotificationHandler pceListenerImpl;
    private TapiRendererNotificationHandler rendererListenerImpl;

    public CreateConnectivityServiceImpl(RpcService rpcService, TapiContext tapiContext,
            ConnectivityUtils connectivityUtils, TapiPceNotificationHandler pceListenerImpl,
            TapiRendererNotificationHandler rendererListenerImpl) {
        this.rpcService = rpcService;
        this.tapiContext = tapiContext;
        this.connectivityUtils = connectivityUtils;
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> invoke(CreateConnectivityServiceInput input) {
        // TODO: later version of TAPI models include Name as an input parameter in connectivity.yang
        LOG.info("RPC create-connectivity received: {}", input.getEndPoint());
        Uuid serviceUuid = new Uuid(UUID.randomUUID().toString());
        this.pceListenerImpl.setInput(input);
        this.pceListenerImpl.setServiceUuid(serviceUuid);
        this.rendererListenerImpl.setServiceUuid(serviceUuid);
        OperationResult validationResult =
            CreateConnectivityServiceValidation.validateCreateConnectivityServiceRequest(input);
        if (!validationResult.isSuccess()) {
            return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "Failed to create service")
                .buildFuture();
        }
        LOG.info("input parameter of RPC create-connectivity are being handled");
        // check uuid of SIP in tapi context
        Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap =
                this.tapiContext.getTapiContext().getServiceInterfacePoint();
        if (sipMap == null) {
            return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "SIP list is empty")
                .buildFuture();
        }
        if (!sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().findFirst()
                    .orElseThrow().getServiceInterfacePoint().getServiceInterfacePointUuid()))
                || !sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().skip(1)
                    .findFirst().orElseThrow().getServiceInterfacePoint().getServiceInterfacePointUuid()))) {
            LOG.error("Unknown UUID");
            return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "SIPs do not exist in tapi context")
                .buildFuture();
        }
        LOG.info("SIPs found in sipMap");
        // TODO: differentiate between OTN service and GbE service in TAPI
        ServiceCreateInput sci = this.connectivityUtils.createORServiceInput(input, serviceUuid);
        if (sci == null) {
            return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "Couldnt map Service create input")
                .buildFuture();
        }
        LOG.info("Service Create input = {}", sci);
        ListenableFuture<RpcResult<ServiceCreateOutput>> output = rpcService.getRpc(ServiceCreate.class).invoke(sci);
        if (!output.isDone()) {
            return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "Service create RPC failed")
                .buildFuture();
        }
        LOG.info("Service create request was successful");
        try {
            if (output.get().getResult().getConfigurationResponseCommon().getResponseCode()
                    .equals(ResponseCodes.RESPONSE_FAILED)) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.RPC, "Failed to create service")
                    .buildFuture();
            }
            LOG.info("Output of service request = {}", output.get().getResult());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error checking response code of service create", e);
        }
        // Connections and states should be created/updated when the pce and renderer are done :)
        Name name = new NameBuilder()
            .setValueName("Connectivity Service Name")
            .setValue(serviceUuid.getValue())
            .build();
        ConnectivityService service = new ConnectivityServiceBuilder()
            .setUuid(serviceUuid)
            .setAdministrativeState(AdministrativeState.LOCKED)
            .setOperationalState(OperationalState.DISABLED)
            .setLifecycleState(LifecycleState.PLANNED)
            .setLayerProtocolName(input.getLayerProtocolName())
            .setConnectivityConstraint(
                new ConnectivityConstraintBuilder()
                    .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
                    .setServiceLevel(input.getConnectivityConstraint().getServiceLevel())
                    .build())
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(new HashMap<>())
            .setEndPoint(createEndPoints(input.getEndPoint()))
            .build();
        // add to tapi context
        this.tapiContext.updateConnectivityContext(Map.of(service.key(), service), new HashMap<>());
        LOG.info("Created locked service in Datastore. Waiting for PCE and Renderer to complete tasks...");
        // return ConnectivityServiceCreateOutput
        return RpcResultBuilder.success(
                new CreateConnectivityServiceOutputBuilder()
                    .setService(new ServiceBuilder(service).build())
                    .build())
            .buildFuture();
    }

    private Map<EndPointKey, EndPoint> createEndPoints(
            Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .create.connectivity.service.input.EndPointKey,
                org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                    .create.connectivity.service.input.EndPoint> endPoints) {
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint ep: endPoints.values()) {
            EndPoint endpoint = new EndPointBuilder()
                .setServiceInterfacePoint(
                    new ServiceInterfacePointBuilder()
                        .setServiceInterfacePointUuid(ep.getServiceInterfacePoint().getServiceInterfacePointUuid())
                        .build())
                .setName(ep.getName())
                .setAdministrativeState(ep.getAdministrativeState())
                .setDirection(ep.getDirection())
                .setLifecycleState(ep.getLifecycleState())
                .setOperationalState(ep.getOperationalState())
                .setLayerProtocolName(ep.getLayerProtocolName())
                .setCapacity(new CapacityBuilder()
                    .setTotalSize(new TotalSizeBuilder().build())
                // TODO: implement bandwidth profile //.setBandwidthProfile(new BandwidthProfileBuilder().build())
                    .build())
                .setProtectionRole(ep.getProtectionRole())
                .setRole(ep.getRole())
                .setLocalId(ep.getLocalId())
                .build();
            endPointMap.put(endpoint.key(), endpoint);
        }
        return endPointMap;
    }

}
