/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.renderer;

import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev251001.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev251001.ServicePathOutput;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

@Component
public class TapiSbiRendererServiceImpl implements TapiSbiRendererService {


    @Activate
    public TapiSbiRendererServiceImpl() {

    }

    @Override
    public ServicePathOutput createSbiService(ServicePathInput input, ServicePathDirection direction) {
        // TODO:  Implement This method to create an Sbi Service (Wavelength Tunnel) writing in the DataStore of
        // the SouthBound NMS/Controller without relying on TAPI RPcs so that it can scale to release higher than 2.4.
        return null;
    }


    @Override
    public ServicePathOutput deleteSbiService(ServicePathInput input) {
        // TODO:  Implement This method to delete an Sbi Service (Wavelength Tunnel) writing in the DataStore of
        // the SouthBound NMS/Controller without relying on TAPI RPcs so that it can scale to release higher than 2.4.
        return null;
    }



}
