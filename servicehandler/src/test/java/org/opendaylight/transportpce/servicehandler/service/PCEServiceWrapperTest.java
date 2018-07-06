/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.CancelResourceReserveInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInputBuilder;

public class PCEServiceWrapperTest extends AbstractTest {

    private PathComputationService pathComputationService;
    private PCEServiceWrapper pceServiceWrapper;
    private Method method;
    private static String METHOD_NAME = "mappingCancelResourceReserve";
    private Class[] parameterTypes;
    private Object[] parameters;



    public PCEServiceWrapperTest() {
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        this.pathComputationService = new PathComputationServiceImpl(getDataBroker(), notificationPublishService);
    }

    @Before
    public void init() throws NoSuchMethodException {
        this.pceServiceWrapper = new PCEServiceWrapper(this.pathComputationService);
        this.parameterTypes = new Class[2];
        this.parameterTypes[0] = java.lang.String.class;
        this.parameterTypes[1] = SdncRequestHeader.class;
        this.method = this.pceServiceWrapper.getClass().getDeclaredMethod(METHOD_NAME, this.parameterTypes);
        this.method.setAccessible(true);
        this.parameters = new Object[2];
    }

    @Test
    public void performPCENullSdncRequestHeader() {
        ServiceCreateInput input =  ServiceDataUtils.buildServiceCreateInput();
        input = new ServiceCreateInputBuilder(input).setSdncRequestHeader(null).build();
        PathComputationRequestOutput pceResponse = this.pceServiceWrapper.performPCE(input, true);
        Assert.assertEquals(ResponseCodes.FINAL_ACK_YES, pceResponse.getConfigurationResponseCommon()
                .getAckFinalIndicator());
    }

    @Test
    public void mappingCancelResourceReserveNullSdncRequestHeader()
        throws InvocationTargetException, IllegalAccessException {
        this.parameters[0] = "service 1";
        this.parameters[1] = null;
        CancelResourceReserveInput result = (CancelResourceReserveInput)this.method.invoke(this.pceServiceWrapper,
                this.parameters);
        Assert.assertEquals("service 1", result.getServiceName());
    }

    @Test
    public void mappingCancelResourceReserveValidSdncRequestHeader()
        throws InvocationTargetException, IllegalAccessException {
        this.parameters[0] = "service 1";
        this.parameters[1] = new SdncRequestHeaderBuilder().setRequestId("request 1")
            .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build();
        CancelResourceReserveInput result = (CancelResourceReserveInput)this.method.invoke(this.pceServiceWrapper,
                this.parameters);
        Assert.assertEquals("service 1", result.getServiceName());
        Assert.assertEquals("request 1", result.getServiceHandlerHeader().getRequestId());
    }
}
