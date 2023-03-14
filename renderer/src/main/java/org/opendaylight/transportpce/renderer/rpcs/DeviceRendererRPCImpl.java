/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.rpcs;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.service.ServiceTypes;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.CreateOtsOmsOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.RendererRollbackOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.ServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.TransportpceDeviceRendererService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRendererRPCImpl implements TransportpceDeviceRendererService {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRendererRPCImpl.class);
    private DeviceRendererService deviceRenderer;
    private OtnDeviceRendererService otnDeviceRendererService;

    public DeviceRendererRPCImpl(DeviceRendererService deviceRenderer,
                                 OtnDeviceRendererService otnDeviceRendererService) {
        this.deviceRenderer = deviceRenderer;
        this.otnDeviceRendererService = otnDeviceRendererService;
        LOG.debug("DeviceRendererRPCImpl instantiated");
    }

    /**
     * This method is the implementation of the 'service-path' RESTCONF service,
     * which is one of the external APIs into the renderer application. The
     * service provides two functions:
     *
     * <p>
     * 1. Create This operation results in provisioning the device for a given
     * wavelength and a list of nodes with each node listing its termination
     * points.
     *
     * <p>
     * 2. Delete This operation results in de-provisioning the device for a
     * given wavelength and a list of nodes with each node listing its
     * termination points.
     *
     * <p>
     * The signature for this method was generated by yang tools from the
     * renderer API model.
     *
     * @param input
     *            Input parameter from the service-path yang model
     *
     * @return Result of the request
     */
    @Override
    public ListenableFuture<RpcResult<ServicePathOutput>> servicePath(ServicePathInput input) {
        if (input.getOperation() != null) {
            if (input.getOperation().getIntValue() == 1) {
                LOG.info("Create operation request received");
                return RpcResultBuilder.success(
                        this.deviceRenderer.setupServicePath(input, null))
                        .buildFuture();
            } else if (input.getOperation().getIntValue() == 2) {
                LOG.info("Delete operation request received");
                return RpcResultBuilder
                        .success(this.deviceRenderer.deleteServicePath(input))
                        .buildFuture();
            }
        }
        return RpcResultBuilder
            .success(new ServicePathOutputBuilder().setResult("Invalid operation").build())
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<OtnServicePathOutput>> otnServicePath(OtnServicePathInput input) {
        if (input.getOperation() != null && input.getServiceFormat() != null && input.getServiceRate() != null) {
            String serviceType = ServiceTypes.getOtnServiceType(input.getServiceFormat(), input.getServiceRate());
            if (input.getOperation().getIntValue() == 1) {
                LOG.info("Create operation request received");
                return RpcResultBuilder.success(this.otnDeviceRendererService
                        .setupOtnServicePath(input, serviceType)).buildFuture();
            } else if (input.getOperation().getIntValue() == 2) {
                LOG.info("Delete operation request received");
                return RpcResultBuilder.success(this.otnDeviceRendererService
                        .deleteOtnServicePath(input, serviceType)).buildFuture();
            }
        }
        return RpcResultBuilder
            .success(new OtnServicePathOutputBuilder().setResult("Invalid operation").build())
            .buildFuture();
    }

    /**
     * Rollback created interfaces and cross connects specified by input.
     *
     * @param input
     *            Lists of created interfaces and connections per node
     * @return Success flag and nodes which failed to rollback
     */
    @Override
    public ListenableFuture<RpcResult<RendererRollbackOutput>> rendererRollback(RendererRollbackInput input) {
        return RpcResultBuilder.success(this.deviceRenderer.rendererRollback(input)).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<CreateOtsOmsOutput>> createOtsOms(CreateOtsOmsInput input) {
        LOG.info("Request received to create oms and ots interfaces on {}: {}", input.getNodeId(), input
            .getLogicalConnectionPoint());
        try {
            return RpcResultBuilder.success(deviceRenderer.createOtsOms(input)).buildFuture();
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("failed to send request to create oms and ots interfaces on {}: {}", input.getNodeId(),
                    input.getLogicalConnectionPoint(),e);
        }
        return null;
    }
}
