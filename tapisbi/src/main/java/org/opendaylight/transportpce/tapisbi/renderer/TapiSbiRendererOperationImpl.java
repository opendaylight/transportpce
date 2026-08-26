/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.renderer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.RpcService;
import org.opendaylight.transportpce.renderer.provisiondevice.notification.Notification;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev250530.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class TapiSbiRendererOperationImpl implements TapiSbiRendererOperation {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiRendererOperationImpl.class);

    private static final int NUMBER_OF_THREADS = 4;
    private final Notification notification;
    private ListeningExecutorService executor;

    @Activate
    public TapiSbiRendererOperationImpl(
                @Reference DataBroker dataBroker,
                @Reference Notification notification,
                @Reference RpcService rpcService) {
        this.notification = notification;
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
        LOG.debug("TapiSbiRendererServiceOperationsImpl instantiated");
    }

    @Override
    public ListenableFuture<TapiSbiServiceImplementationRequestOutput> serviceImplementation(
            TapiSbiServiceImplementationRequestInput input, boolean isTempService) {
        // TODO:  Implement This method to create an Sbi sub-service in a SouthBound Controller using the TAPI RPC
        // create-connectivity-service. This is a first step to provide a solution for SBI interface based on TAPI
        // 2.4 or previous release which still provides RPC support.
        // This approach does not scale to TAPI 2.5 where RPC support has been cancelled.
        // It is the solution to be used when SBI controller is based on transportPCE, which does not support
        // service-creation directly writing in the DATA STORE.
        // Note : Provided only as guidance for service creation respecting the same spirit as the one used with
        // service-implementation-request in the OR Renderer.
        LOG.info("Calling Tapi SBI service impl request {}", input.getServiceName());
        LOG.debug("Check if it is temp-service {}", isTempService);
        return executor.submit(new Callable<TapiSbiServiceImplementationRequestOutput>() {

            @Override
            public TapiSbiServiceImplementationRequestOutput call() throws Exception {
                sendNotifications(
                    // TODO: Change notification type to TapiSbiServiceImplementationRequest
                    // after it has been added in the right place
                    ServicePathNotificationTypes.ServiceImplementationRequest,
                    input.getServiceName(),
                    RpcStatusEx.Pending,
                    "Service compliant, submitting service implementation Request ...");
                // The service creation process is following the same approach as the one used in the renderer
                // RendererServiceOperation Class which provides implementation of service-implementation-request
                // 2 Methods can be created to manage service creation depending on whether we are at the WDM or
                // the OTN layer. This is the reason why serviceType parameters is provided as an input
                // Nothing provided to handle service-path/otn-service-path RPC, but the management of these RPC
                // as an intermediate step might be useful, to guaranty service continuity (from a wavelength
                // perspective at the WDM layer, and trib-port/trib-slot perspective at the OTN layer.

                return null;
            }
        });
    }


    @Override
    public ListenableFuture<TapiSbiServiceDeleteOutput> serviceDelete(
            TapiSbiServiceDeleteInput input, Services service) {
        // TODO:  Implement This method to delete an Sbi sub-service in a SouthBound Controller using the TAPI RPC
        // delete-connectivity-service. This is a first step to provide a solution for SBI interface based on TAPI
        // 2.4 or previous release which still provides RPC support.
        // This approach does not scale to TAPI 2.5 where RPC support has been cancelled.
        // It is the solution to be used when SBI controller is based on transportPCE, which does not support
        // service-creation directly writing in the DATA STORE.
        // Note : Provided only as guidance for service creation respecting the same spirit as the one used with
        // service-implementation-request in the OR Renderer.
        String serviceName = input.getServiceName();
        LOG.info("Calling TAPI sbi service delete request {}", serviceName);
        return executor.submit(new Callable<TapiSbiServiceDeleteOutput>() {

            @Override
            public TapiSbiServiceDeleteOutput call() throws Exception {
                sendNotifications(
                        // TODO: Change notification type to TapiSbiServiceDelete
                        // after it has been added in the right place
                        ServicePathNotificationTypes.ServiceDelete,
                        serviceName,
                        RpcStatusEx.Pending,
                        "Service compliant, submitting tapi sbi service delete Request ...");
                // The service deletion process is following the same approach as the one used in the renderer
                // RendererServiceOperation Class which provides implementation of service-delete
                // 2 Methods can be created to manage service deletion depending on whether we are at the WDM or
                // the OTN layer. This is the reason why serviceType parameters is provided as an input
                return null;
            }
        });
    }


    @SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private Uint32 getServiceRate(ServiceImplementationRequestInput input) {
        if (input.getServiceAEnd() == null) {
            LOG.warn("Unable to get service-rate for service {}", input.getServiceName());
            return Uint32.ZERO;
        }
        if (input.getServiceAEnd().getServiceRate() != null) {
            return input.getServiceAEnd().getServiceRate();
        }
        LOG.warn("Input should have rate if you are using 200 or 300G");
        // TODO: missing 200, and 300G rates here, OTUCn cannot always be 400G
        Map<ServiceFormat, Map<String, Uint32>> formatRateMap  = Map.of(
                ServiceFormat.OTU, Map.of(
                    "OTUCn", Uint32.valueOf(400),
                    "OTU4", Uint32.valueOf(100),
                    "OTU2", Uint32.TEN,
                    "OTU2e", Uint32.TEN),
                ServiceFormat.ODU, Map.of(
                    "ODUCn",Uint32.valueOf(400),
                    "ODU4", Uint32.valueOf(100),
                    "ODU2", Uint32.TEN,
                    "ODU2e", Uint32.TEN,
                    "ODU0", Uint32.ONE));
        if (!formatRateMap.containsKey(input.getServiceAEnd().getServiceFormat())) {
            LOG.warn("Unable to get service-rate for service {} - unsupported service format {}",
                input.getServiceName(), input.getServiceAEnd().getServiceFormat());
            return Uint32.ZERO;
        }
        String serviceName =
            ServiceFormat.OTU.getName().equals(input.getServiceAEnd().getServiceFormat().getName())
                ? input.getServiceAEnd().getOtuServiceRate().toString().split("\\{")[0]
                : input.getServiceAEnd().getOduServiceRate().toString().split("\\{")[0];
        if (!formatRateMap.get(input.getServiceAEnd().getServiceFormat()).containsKey(serviceName)) {
            LOG.warn("Unable to get service-rate for service {} - unsupported service name {}",
                input.getServiceName(), serviceName);
            return Uint32.ZERO;
        }
        return formatRateMap
            .get(input.getServiceAEnd().getServiceFormat())
            .get(serviceName);
    }


    /**
     * Send Tapi sbi renderer notification.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     */
    private void sendNotifications(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName,
            RpcStatusEx rpcStatusEx,
            String message) {

        notification.send(
            servicePathNotificationTypes,
            serviceName,
            rpcStatusEx,
            message
        );
    }
}
