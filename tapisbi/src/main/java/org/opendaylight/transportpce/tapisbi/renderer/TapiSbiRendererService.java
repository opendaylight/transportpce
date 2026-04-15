/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapisbi.renderer;

import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.transportpce.renderer.provisiondevice.transaction.history.History;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev251001.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev251001.ServicePathOutput;


public interface TapiSbiRendererService {

    /**
     * This method create an Sbi Service (Wavelength Tunnel) writing in the DataStore of the SouthBound NMS/Controller.
     * It does not rely on TAPI RPcs so that it can scale to release higher than 2.4.
     *
     * @param input
     *            Input parameter from the service-path yang model
     * @param direction
     *            Service Path direction
     *
     * @return Result Simplified path with impairment awareness parameters if request successful
     *         otherwise specific reason of failure.
     */
    ServicePathOutput createSbiService(ServicePathInput input, ServicePathDirection direction);



    /**
     * This method delete an Sbi Service (Wavelength Tunnel) writing in the DataStore of the SouthBound NMS/Controller.
     * It does not rely on TAPI RPcs so that it can scale to release higher than 2.4.
     * @param input
     *            Input parameter from the service-path yang model
     *
     * @return Result result of the request.
     */
    ServicePathOutput deleteSbiService(ServicePathInput input);

}
