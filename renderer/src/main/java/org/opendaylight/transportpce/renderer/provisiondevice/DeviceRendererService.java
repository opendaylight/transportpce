/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.provisiondevice.servicepath.ServicePathDirection;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutput;


public interface DeviceRendererService {

    /**
     * This method set's wavelength path based on following steps.
     *
     * <p>
     * For each node:
     * 1. Create Och interface on source termination point.
     * 2. Create Och interface on destination termination point.
     * 3. Create cross connect between source and destination tps created in step 1
     *    and 2.
     *
     * Naming convention used for OCH interfaces name : tp-wavenumber Naming
     * convention used for cross connect name : src-dest-wavenumber
     * </p>
     *
     * @param input
     *            Input parameter from the service-path yang model
     * @param direction
     *            Service Path direction
     *
     * @return Result list of all nodes if request successful otherwise specific
     *         reason of failure.
     */
    ServicePathOutput setupServicePath(ServicePathInput input, ServicePathDirection direction);

    /**
     * This method removes wavelength path based on following steps.
     *
     * <p>
     * For each node:
     * 1. Delete Cross connect between source and destination tps.
     * 2. Delete Och interface on source termination point.
     * 3. Delete Och interface on destination termination point.
     *
     * Naming convention used for OCH interfaces name : tp-wavenumber Naming
     * convention used for cross connect name : src-dest-wavenumber
     * </p>
     *
     * @param input
     *            Input parameter from the service-path yang model
     *
     * @return Result result of the request.
     */
    ServicePathOutput deleteServicePath(ServicePathInput input);

    /**
     * Rollback created interfaces and cross connects specified by input.
     *
     * @param input Lists of created interfaces and connections per node
     * @return Success flag and nodes which failed to rollback
     */
    RendererRollbackOutput rendererRollback(RendererRollbackInput input);

    /**
     * This method creates the basis of ots and oms interfaces on a specific ROADM degree.
     *
     * @param input
     *             Input parameter from the create-ots-oms yang model
     * @return Success flag and names of interfaces created
     * @throws OpenRoadmInterfaceException OpenRoadmInterfaceException
     */
    CreateOtsOmsOutput createOtsOms(CreateOtsOmsInput input) throws OpenRoadmInterfaceException;
}
