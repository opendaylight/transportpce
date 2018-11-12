/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.servicehandler.stub;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Class to implement StubrendererService.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public final class StubrendererImpl {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(StubrendererImpl.class);
    /** check service sdnc-request-header compliancy. */

    private StubrendererImpl() {

    }


    public static ListenableFuture<RpcResult<ServiceDeleteOutput>> serviceDelete(ServiceDeleteInput input) {
        LOG.info("ServiceDelete request ...");
        String message = "";
        String responseCode = null;
        try {
            LOG.info("Wait for 1s til beginning the Renderer serviceDelete request");
            Thread.sleep(1000); //sleep for 1s
        } catch (InterruptedException e) {
            message = "deleting service failed !";
            LOG.error("deleting service failed !", e);
            responseCode = "500";
        }
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceDeleteOutput output =  new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceImplementationRequestOutput>>
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
            responseCode = "500";
        }
        ConfigurationResponseCommonBuilder configurationResponseCommon = new ConfigurationResponseCommonBuilder()
                .setAckFinalIndicator("yes")
                .setRequestId(input.getServiceHandlerHeader().getRequestId())
                .setResponseCode(responseCode)
                .setResponseMessage(message);
        ServiceImplementationRequestOutput output =  new ServiceImplementationRequestOutputBuilder()
                .setConfigurationResponseCommon(configurationResponseCommon.build())
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }
}
