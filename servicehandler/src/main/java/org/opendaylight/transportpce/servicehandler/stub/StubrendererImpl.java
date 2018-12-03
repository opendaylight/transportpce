/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Class to implement StubrendererService.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class StubrendererImpl {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubrendererImpl.class);
    /** check service sdnc-request-header compliancy. */
    private final NetworkModelWavelengthService networkModelWavelengthService;
    private final DataBroker dataBroker;

    public StubrendererImpl(NetworkModelWavelengthService networkModelWavelengthService, DataBroker dataBroker) {
        this.networkModelWavelengthService = networkModelWavelengthService;
        this.dataBroker = dataBroker;
    }

    public ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("ServiceDelete request ...");
        String serviceName = input.getServiceName();
        String message = "";
        String responseCode = null;
        try {
            LOG.info("Wait for 1s til beginning the Renderer serviceDelete request");
            Thread.sleep(1000); //sleep for 1s
        } catch (InterruptedException e) {
            message = "deleting service failed !";
            LOG.error("deleting service failed !", e);
            responseCode = ResponseCodes.RESPONSE_FAILED;
        }
        // Obtain path description
        Optional<PathDescription> pathDescriptionOpt = getPathDescriptionFromDatastore(serviceName);
        PathDescription pathDescription;
        if (pathDescriptionOpt.isPresent()) {
            pathDescription = pathDescriptionOpt.get();
            this.networkModelWavelengthService.freeWavelengths(pathDescription);
            responseCode = ResponseCodes.RESPONSE_OK;
            message = "service deleted !";
        } else {
            LOG.error("failed to get pathDescription for service : {}", serviceName);
            responseCode = ResponseCodes.RESPONSE_FAILED;
            message = "failed to get pathDescription for service : " + serviceName;
        }
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceDeleteOutput output =  new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    public ListenableFuture<RpcResult<ServiceImplementationRequestOutput>>
        serviceImplementation(ServiceImplementationRequestInput input) {
        LOG.info("serviceImplementation request ...");
        String message = "";
        String responseCode = null;
        try {
            LOG.info("Wait for 1s til beginning the Renderer serviceDelete request");
            Thread.sleep(1000); //sleep for 1s
        } catch (InterruptedException e) {
            message = "implementing service failed !";
            LOG.error(message);
            responseCode = ResponseCodes.RESPONSE_FAILED;
        }
        this.networkModelWavelengthService.useWavelengths(input.getPathDescription());
        message = "service implemented !";
        responseCode = ResponseCodes.RESPONSE_OK;
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator(ResponseCodes.FINAL_ACK_YES)
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceImplementationRequestOutput output =  new ServiceImplementationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    private Optional<PathDescription> getPathDescriptionFromDatastore(String serviceName) {
        InstanceIdentifier<PathDescription> pathDescriptionIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName)).child(PathDescription.class);
        ReadOnlyTransaction pathDescReadTx = this.dataBroker.newReadOnlyTransaction();
        try {
            LOG.debug("Getting path description for service {}", serviceName);
            return pathDescReadTx.read(LogicalDatastoreType.OPERATIONAL, pathDescriptionIID)
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Exception while getting path description from datastore {} for service {}!", pathDescriptionIID,
                    serviceName, e);
            return Optional.absent();
        }
    }
}
