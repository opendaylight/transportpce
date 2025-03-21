/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.rpc.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBase;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;



/**
 * This class calculates Spanloss for all Roadm to Roadm links,
 * part of active inventory in Network Model or for newly added links
 * based on input src-type.
 *
 * <p>Calculate-Spanloss-Base: This operation performs following steps:
 *    Step1: Read all Roadm-to-Roadm links from network model or get data for given linkID.
 *    Step2: Retrieve PMs for each end point for OTS interface
 *    Step3: Calculates Spanloss
 *    Step4: Posts calculated spanloss in Device and in network model
 *
 * <p>The signature for this method was generated by yang tools from the
 * renderer API model.
 */
public class CalculateSpanlossBaseImpl implements CalculateSpanlossBase {
    private final OlmPowerService olmPowerService;

    public CalculateSpanlossBaseImpl(final OlmPowerService olmPowerService) {
        this.olmPowerService = requireNonNull(olmPowerService);
    }

    @Override
    public ListenableFuture<RpcResult<CalculateSpanlossBaseOutput>> invoke(CalculateSpanlossBaseInput input) {
        return RpcResultBuilder.success(this.olmPowerService.calculateSpanlossBase(input)).buildFuture();
    }

}
