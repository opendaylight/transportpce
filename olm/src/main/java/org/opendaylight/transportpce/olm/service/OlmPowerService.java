/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;

public interface OlmPowerService {

    /**
     * This method is the implementation of the 'get-pm' service.
     *
     * <p>
     * 1. get-pm This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model
     *
     * @return Result of the request
     */
    GetPmOutput getPm(GetPmInput input);

    /**
     * This method is the implementation of the 'service-power-setup'.
     *
     * <p>
     * 1. service-power-setup: This operation performs following steps:
     *    Step1: Calculate Spanloss on all links which are part of service.
     *    TODO Step2: Calculate power levels for each Tp-Id
     *    TODO Step3: Post power values on roadm connections
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain nodeId and termination point
     *
     * @return Result of the request
     */
    ServicePowerSetupOutput servicePowerSetup(ServicePowerSetupInput input);

    /**
     * This method is the implementation of the 'service-power-trundown'.
     *
     * <p>
     * 1. service-power-turndown: This operation performs following steps:
     *    Step1: For each TP within Node sets interface outofservice .
     *    Step2: For each roam-connection sets power to -60dbm
     *    Step3: Turns power mode off
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain nodeId and termination point
     *
     * @return Result of the request
     */
    ServicePowerTurndownOutput servicePowerTurndown(ServicePowerTurndownInput input);

    /**
     * This method calculates Spanloss for all Roadm to Roadm links,
     * part of active inventory in Network Model or for newly added links
     * based on input src-type.
     *
     * <p>
     * 1. Calculate-Spanloss-Base: This operation performs following steps:
     *    Step1: Read all Roadm-to-Roadm links from network model or get data for given linkID.
     *    Step2: Retrieve PMs for each end point for OTS interface
     *    Step3: Calculates Spanloss
     *    Step4: Posts calculated spanloss in Device and in network model
     *
     * @param input
     *            Input parameter from the olm yang model
     *            Input will contain SourceType and linkId if srcType is Link
     *
     * @return Result of the request
     */
    CalculateSpanlossBaseOutput calculateSpanlossBase(CalculateSpanlossBaseInput input);

    CalculateSpanlossCurrentOutput calculateSpanlossCurrent(
            CalculateSpanlossCurrentInput input);

    ServicePowerResetOutput servicePowerReset(ServicePowerResetInput input);
}
