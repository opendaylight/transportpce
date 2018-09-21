/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listensers;

import org.junit.Test;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.ServicePathRpcResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.ServicePathRpcResultBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.ServicePathNotificationTypes;


public class PceListenserImplTest {

    private PceListenerImpl pceListener;
    private ServicePathRpcResult servicePathRpcResult = null;

    public PceListenserImplTest() {
        this.pceListener = new PceListenerImpl();
        this.servicePathRpcResult = ServiceDataUtils.buildServicePathRpcResult();
    }

    @Test
    public void onServicePathRpcResultInitial() {
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeat() {
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeatFailed() {
        this.servicePathRpcResult = ServiceDataUtils.buildFailedServicePathRpcResult();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase1() {
        this.servicePathRpcResult = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
        this.servicePathRpcResult = new ServicePathRpcResultBuilder(this.servicePathRpcResult)
            .setNotificationType(ServicePathNotificationTypes.CancelResourceReserve).build();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase2() {
        this.servicePathRpcResult = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
        this.servicePathRpcResult = new ServicePathRpcResultBuilder(this.servicePathRpcResult)
            .setServiceName("service 2").build();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase3() {
        this.servicePathRpcResult = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
        this.servicePathRpcResult = new ServicePathRpcResultBuilder(this.servicePathRpcResult)
            .setStatus(RpcStatusEx.Failed).build();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }

    @Test
    public void onServicePathRpcResultRepeatFailedCompareCase4() {
        this.servicePathRpcResult = ServiceDataUtils.buildServicePathRpcResult();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
        this.servicePathRpcResult = new ServicePathRpcResultBuilder(this.servicePathRpcResult)
            .setStatusMessage("failed").build();
        this.pceListener.onServicePathRpcResult(this.servicePathRpcResult);
    }
}
