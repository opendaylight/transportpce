/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.stub;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class OlmServiceStub implements TransportpceOlmService {

    @Override public ListenableFuture<RpcResult<CalculateSpanlossCurrentOutput>> calculateSpanlossCurrent(
        CalculateSpanlossCurrentInput input) {
        return null;
    }

    @Override public ListenableFuture<RpcResult<GetPmOutput>> getPm(GetPmInput input) {
        GetPmOutput output = new GetPmOutputBuilder()
            .setNodeId("node1").setMeasurements(new ArrayList<>()).build();
        return RpcResultBuilder.success(
            output).buildFuture();
    }

    @Override public ListenableFuture<RpcResult<ServicePowerTurndownOutput>> servicePowerTurndown(
        ServicePowerTurndownInput input) {
        return RpcResultBuilder.success((new ServicePowerTurndownOutputBuilder())
                .setResult("Success").build()).buildFuture();
    }

    @Override public ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> calculateSpanlossBase(
        CalculateSpanlossBaseInput input) {
        return null;
    }

    @Override public ListenableFuture<RpcResult<ServicePowerResetOutput>> servicePowerReset(
            ServicePowerResetInput input) {
        return null;
    }

    @Override public ListenableFuture<RpcResult<ServicePowerSetupOutput>> servicePowerSetup(
            ServicePowerSetupInput input) {
        return RpcResultBuilder.success(new ServicePowerSetupOutputBuilder().build()).buildFuture();
    }
}
