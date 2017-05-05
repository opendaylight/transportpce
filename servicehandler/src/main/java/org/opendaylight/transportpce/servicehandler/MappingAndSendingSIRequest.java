/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.StubrendererService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubrenderer.rev170426.service.implementation.request.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.service.handler.header.ServiceHandlerHeaderBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/*
 * Class for Mapping and Sending
 * Service Implemention requests :
 * - service implementation
 * - service delete.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class MappingAndSendingSIRequest {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(MappingAndSendingSIRequest.class);
    /* Permit to call Renderer RPCs. */
    private StubrendererService service;
    /* define procedure success (or not ). */
    private Boolean success = false;
    /* permit to call bundle service (PCE, Renderer, Servicehandler. */
    private RpcProviderRegistry rpcRegistry;
    /* store all error messages. */
    private String error;
    ServiceImplementationRequestInput serviceImplementationRequestInput = null;
    ServiceDeleteInput serviceDeleteInput = null;

    private final ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));


    /*
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceCreate.
     *
     * @param rpcRegistry rpcRegistry
     * @param serviceCreateInput serviceCreateInput
     * @param pathComputationOutput pathComputationOutput
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,
            ServiceCreateInput serviceCreateInput,PathComputationRequestOutput pathComputationOutput) {
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /* Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput. */
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (serviceCreateInput.getSdncRequestHeader() != null) {
            serviceHandlerHeader.setRequestId(serviceCreateInput.getSdncRequestHeader().getRequestId());
        }
        //.build();

        /*ServiceAEnd Build */
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.create.input.ServiceAEnd tempA = serviceCreateInput.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx =
            serviceCreateInput.getServiceAEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx =
            serviceCreateInput.getServiceAEnd().getTxDirection().getPort();

        /*Port portARx = new PortBuilder()
        .setPortDeviceName(tempAPortRx.getPortDeviceName())
        .setPortName(tempAPortRx.getPortName())
        .setPortRack(tempAPortRx.getPortRack())
        .setPortShelf(tempAPortRx.getPortShelf())
        .setPortSlot(tempAPortRx.getPortSlot())
        .setPortSubSlot(tempAPortRx.getPortSubSlot())
        .setPortType(tempAPortRx.getPortType())
        .build();

        Port portATx = new PortBuilder()
        .setPortDeviceName(tempAPortTx.getPortDeviceName())
        .setPortName(tempAPortTx.getPortName())
        .setPortRack(tempAPortTx.getPortRack())
        .setPortShelf(tempAPortTx.getPortShelf())
        .setPortSlot(tempAPortTx.getPortSlot())
        .setPortSubSlot(tempAPortTx.getPortSubSlot())
        .setPortType(tempAPortTx.getPortType())
        .build();

        RxDirection rxDirectionAEnd = new RxDirectionBuilder()
        .setPort(portARx )
        .build();

        TxDirection txDirectionAEnd = new TxDirectionBuilder()
        .setPort(portATx )
        .build();*/

        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();

        /* ServiceZEnd Build */
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.create.input.ServiceZEnd tempZ = serviceCreateInput.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx =
            serviceCreateInput.getServiceZEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx =
            serviceCreateInput.getServiceZEnd().getTxDirection().getPort();

        /*Port portZRx = new PortBuilder()
        .setPortDeviceName(tempZPortRx.getPortDeviceName())
        .setPortName(tempZPortRx.getPortName())
        .setPortRack(tempZPortRx.getPortRack())
        .setPortShelf(tempZPortRx.getPortShelf())
        .setPortSlot(tempZPortRx.getPortSlot())
        .setPortSubSlot(tempZPortRx.getPortSubSlot())
        .setPortType(tempZPortRx.getPortType())
        .build();

        Port portZTx = new PortBuilder()
        .setPortDeviceName(tempZPortTx.getPortDeviceName())
        .setPortName(tempZPortTx.getPortName())
        .setPortRack(tempZPortTx.getPortRack())
        .setPortShelf(tempZPortTx.getPortShelf())
        .setPortSlot(tempZPortTx.getPortSlot())
        .setPortSubSlot(tempZPortTx.getPortSubSlot())
        .setPortType(tempZPortTx.getPortType())
        .build();

        RxDirection rxDirectionZEnd = new RxDirectionBuilder()
        .setPort(portZRx )
        .build();

        TxDirection txDirectionZEnd = new TxDirectionBuilder()
        .setPort(portZTx )
        .build();*/

        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);

        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();


        /* ServiceImplementationRequestInput  Build*/
        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
            .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : " + e.toString());
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }
        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription.build())
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(serviceCreateInput.getServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();
    }

    /*
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceReconfigure.
     *
     * @param rpcRegistry rpcRegistry
     * @param serviceReconfigureInput serviceReconfigureInput
     * @param pathComputationOutput pathComputationOutput
     */

    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,
            ServiceReconfigureInput serviceReconfigureInput,PathComputationRequestOutput pathComputationOutput) {
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /* Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();

        /*ServiceAEnd Build .*/
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.reconfigure.input.ServiceAEnd tempA = serviceReconfigureInput.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx =
            serviceReconfigureInput.getServiceAEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx =
            serviceReconfigureInput.getServiceAEnd().getTxDirection().getPort();

        /*Port portARx = new PortBuilder()
        .setPortDeviceName(tempAPortRx.getPortDeviceName())
        .setPortName(tempAPortRx.getPortName())
        .setPortRack(tempAPortRx.getPortRack())
        .setPortShelf(tempAPortRx.getPortShelf())
        .setPortSlot(tempAPortRx.getPortSlot())
        .setPortSubSlot(tempAPortRx.getPortSubSlot())
        .setPortType(tempAPortRx.getPortType())
        .build();

        Port portATx = new PortBuilder()
        .setPortDeviceName(tempAPortTx.getPortDeviceName())
        .setPortName(tempAPortTx.getPortName())
        .setPortRack(tempAPortTx.getPortRack())
        .setPortShelf(tempAPortTx.getPortShelf())
        .setPortSlot(tempAPortTx.getPortSlot())
        .setPortSubSlot(tempAPortTx.getPortSubSlot())
        .setPortType(tempAPortTx.getPortType())
        .build();

        RxDirection rxDirectionAEnd = new RxDirectionBuilder()
        .setPort(portARx )
        .build();

        TxDirection txDirectionAEnd = new TxDirectionBuilder()
        .setPort(portATx )
        .build();*/

        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();

        /* ServiceZEnd Build .*/
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev161014
            .service.reconfigure.input.ServiceZEnd tempZ = serviceReconfigureInput.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx =
            serviceReconfigureInput.getServiceZEnd().getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx =
            serviceReconfigureInput.getServiceZEnd().getTxDirection().getPort();

        /*Port portZRx = new PortBuilder()
        .setPortDeviceName(tempZPortRx.getPortDeviceName())
        .setPortName(tempZPortRx.getPortName())
        .setPortRack(tempZPortRx.getPortRack())
        .setPortShelf(tempZPortRx.getPortShelf())
        .setPortSlot(tempZPortRx.getPortSlot())
        .setPortSubSlot(tempZPortRx.getPortSubSlot())
        .setPortType(tempZPortRx.getPortType())
        .build();

        Port portZTx = new PortBuilder()
        .setPortDeviceName(tempZPortTx.getPortDeviceName())
        .setPortName(tempZPortTx.getPortName())
        .setPortRack(tempZPortTx.getPortRack())
        .setPortShelf(tempZPortTx.getPortShelf())
        .setPortSlot(tempZPortTx.getPortSlot())
        .setPortSubSlot(tempZPortTx.getPortSubSlot())
        .setPortType(tempZPortTx.getPortType())
        .build();

        RxDirection rxDirectionZEnd = new RxDirectionBuilder()
        .setPort(portZRx )
        .build();

        TxDirection txDirectionZEnd = new TxDirectionBuilder()
        .setPort(portZTx )
        .build();*/

        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);
        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();


        /* ServiceImplementationRequestInput  Build.*/

        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types
            .rev170426.response.parameters.sp.response.parameters.PathDescription tmp = null;
        try {
            tmp = pathComputationOutput.getResponseParameters().getPathDescription();
        } catch (NullPointerException e) {
            LOG.error("PathDescription is null : " + e.toString());
        }
        PathDescriptionBuilder pathDescription = new PathDescriptionBuilder();
        if (tmp != null) {
            pathDescription = new PathDescriptionBuilder(tmp);
        }
        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription.build())
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(serviceReconfigureInput.getNewServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();
        LOG.info("ServiceImplementationRequestInput : " + serviceImplementationRequestInput.toString());
    }


    /*
     * MappingAndSendingSIRequest class constructor
     * for RPC serviceDelete.
     *
     * @param rpcRegistry RpcProviderRegistry
     * @param requestId Request ID
     * @param serviceName Service name
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,String requestId, String serviceName) {
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /* ServiceDeleteInput Build .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();
        if (requestId != null) {
            serviceHandlerHeader.setRequestId(requestId);
        }

        serviceDeleteInput = new ServiceDeleteInputBuilder()
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(serviceName)
        .build();
    }

    /*
     * MappingAndSendingSIRequest Class constructor
     * for modify Service in ODL Datastore.
     *
     * @param rpcRegistry RpcProviderRegistry
     * @param services Services
     */
    public MappingAndSendingSIRequest(RpcProviderRegistry rpcRegistry,Services services) {
        this.rpcRegistry = rpcRegistry;
        if (rpcRegistry != null) {
            service = rpcRegistry.getRpcService(StubrendererService.class);
        }
        setSuccess(false);
        setError("");

        /* Building ServiceImplementationRequestInput  / ServiceDeleteInput serviceDeleteInput .*/
        ServiceHandlerHeaderBuilder serviceHandlerHeader = new ServiceHandlerHeaderBuilder();

        /*ServiceAEnd Build .*/
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
            .types.rev161014.service.ServiceAEnd tempA = services.getServiceAEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortRx = tempA.getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempAPortTx = tempA.getTxDirection().getPort();


        RxDirection rxDirectionAEnd = getRxDirection(tempAPortRx);

        TxDirection txDirectionAEnd = getTxDirection(tempAPortTx);

        ServiceAEnd serviceAEnd = new ServiceAEndBuilder()
            .setClli(tempA.getClli())
            .setNodeId(tempA.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempA.getServiceFormat().getName()))
            .setServiceRate(tempA.getServiceRate())
            .setRxDirection(rxDirectionAEnd)
            .setTxDirection(txDirectionAEnd)
            .build();

        /* ServiceZEnd Build .*/
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service
            .types.rev161014.service.ServiceZEnd tempZ = services.getServiceZEnd();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortRx = tempZ.getRxDirection().getPort();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempZPortTx = tempZ.getTxDirection().getPort();

        RxDirection rxDirectionZEnd = getRxDirection(tempZPortRx);
        TxDirection txDirectionZEnd = getTxDirection(tempZPortTx);

        ServiceZEnd serviceZEnd = new ServiceZEndBuilder()
            .setClli(tempZ.getClli())
            .setNodeId(tempZ.getNodeId())
            .setServiceFormat(ServiceFormat.valueOf(tempZ.getServiceFormat().getName()))
            .setServiceRate(tempZ.getServiceRate())
            .setRxDirection(rxDirectionZEnd)
            .setTxDirection(txDirectionZEnd)
            .build();


        /* ServiceImplementationRequestInput  Build.*/


        List<AToZ> atozList = new ArrayList<AToZ>();
        List<ZToA> ztoaList = new ArrayList<ZToA>();

        for (org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZ
                    tmp : services.getTopology().getAToZ()) {

            AToZKey key = new AToZKey(tmp.getKey().getId());
            Resource atozresource = new ResourceBuilder()
                    .build();
            AToZ atoz = new AToZBuilder()
                .setId(tmp.getId())
                .setKey(key)
                .setResource(atozresource)
                .build();
            atozList.add(atoz);
        }

        for (org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToA
                    tmp : services.getTopology().getZToA()) {
            ZToAKey key = new ZToAKey(tmp.getKey().getId());
            Resource ztoaresource = new ResourceBuilder()
                    .build();
            ZToA ztoa = new ZToABuilder()
                .setId(tmp.getId())
                .setKey(key)
                .setResource(ztoaresource)
                .build();
            ztoaList.add(ztoa);
        }

        AToZDirection atozdirection = new AToZDirectionBuilder()
            .setAToZ(atozList)
            .build();

        ZToADirection ztoadirection = new ZToADirectionBuilder()
            .setZToA(ztoaList)
            .build();


        PathDescription pathDescription = new PathDescriptionBuilder()
            .setAToZDirection(atozdirection)
            .setZToADirection(ztoadirection)
            .build();

        serviceImplementationRequestInput  = new ServiceImplementationRequestInputBuilder()
        .setPathDescription(pathDescription)
        .setServiceHandlerHeader(serviceHandlerHeader.build())
        .setServiceName(services.getServiceName())
        .setServiceAEnd(serviceAEnd)
        .setServiceZEnd(serviceZEnd)
        .build();
        LOG.info("ServiceImplementationRequestInput : " + serviceImplementationRequestInput.toString());
    }

    /*
     * Create RxDirection with Port
     * information.
     *
     * @param tempPort Port
     * @return RxDirection
     */
    public RxDirection getRxDirection(org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempPort) {
        Port port = new PortBuilder()
            .setPortDeviceName(tempPort.getPortDeviceName())
            .setPortName(tempPort.getPortName())
            .setPortRack(tempPort.getPortRack())
            .setPortShelf(tempPort.getPortShelf())
            .setPortSlot(tempPort.getPortSlot())
            .setPortSubSlot(tempPort.getPortSubSlot())
            .setPortType(tempPort.getPortType())
            .build();

        RxDirection result = new RxDirectionBuilder()
            .setPort(port)
            .build();

        return result;
    }

    /*
     * Create TxDirection with Port
     * information.
     *
     * @param tempPort Port
     * @return TxDirection TxDirection
     */
    public TxDirection getTxDirection(org.opendaylight.yang.gen.v1.http.org.openroadm.common
            .service.types.rev161014.service.port.Port tempPort) {
        Port port = new PortBuilder()
            .setPortDeviceName(tempPort.getPortDeviceName())
            .setPortName(tempPort.getPortName())
            .setPortRack(tempPort.getPortRack())
            .setPortShelf(tempPort.getPortShelf())
            .setPortSlot(tempPort.getPortSlot())
            .setPortSubSlot(tempPort.getPortSubSlot())
            .setPortType(tempPort.getPortType())
            .build();

        TxDirection result = new TxDirectionBuilder()
            .setPort(port)
            .build();

        return result;
    }

    /*
     * Send serviceImplementation request to Render.
     *
     * @return ServiceImplementationRequestOutput data response from Renderer
     */
    public ListenableFuture<ServiceImplementationRequestOutput> serviceImplementation() {
        setSuccess(false);
        return executor.submit(new Callable<ServiceImplementationRequestOutput>() {

            @Override
            public ServiceImplementationRequestOutput call() throws Exception {
                ServiceImplementationRequestOutput output = null;
                if (serviceImplementationRequestInput != null) {
                    RpcResult<ServiceImplementationRequestOutput> rendererOutputResult = null;
                    Future<RpcResult<ServiceImplementationRequestOutput>> rendererOutputFuture =
                            service.serviceImplementationRequest(serviceImplementationRequestInput);
                    try {
                        rendererOutputResult = rendererOutputFuture.get();//wait to get  the result
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from renderer to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        rendererOutputFuture.cancel(true);
                    }

                    if (rendererOutputResult != null && rendererOutputResult.isSuccessful()) {
                        LOG.info("Renderer replied to serviceImplementation Request !");
                        setSuccess(true);
                        ServiceImplementationRequestOutput rendererOutput = rendererOutputResult.getResult();
                        output = new ServiceImplementationRequestOutputBuilder()
                        .setConfigurationResponseCommon(rendererOutput.getConfigurationResponseCommon())
                        .build();
                        setSuccess(true);
                    }
                } else {
                    LOG.info("serviceImplementationRequestInput is not valid");
                }

                return output;
            }
        });

    }

    /*
     * Send serviceDelete request to Render.
     *
     * @return ServiceDeleteOutput data response from Renderer
     */
    public ListenableFuture<ServiceDeleteOutput> serviceDelete() {
        setSuccess(false);
        return executor.submit(new Callable<ServiceDeleteOutput>() {

            @Override
            public ServiceDeleteOutput call() throws Exception {
                ServiceDeleteOutput output = null;
                if (serviceDeleteInput != null) {
                    RpcResult<ServiceDeleteOutput> rendererOutputResult = null;
                    Future<RpcResult<ServiceDeleteOutput>> rendererOutputFuture =
                            service.serviceDelete(serviceDeleteInput);

                    try {
                        rendererOutputResult = rendererOutputFuture.get();//wait to get  the result
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        setError("Did not receive the expected response from renderer to pathComputationRequest RPC "
                                + e.toString());
                        LOG.error(error);
                        rendererOutputFuture.cancel(true);
                    }

                    if (rendererOutputResult != null && rendererOutputResult.isSuccessful()) {
                        LOG.info("Renderer replied to serviceDelete Request!");
                        setSuccess(true);
                        ServiceDeleteOutput rendererOutput = rendererOutputResult.getResult();
                        output = new ServiceDeleteOutputBuilder()
                        .setConfigurationResponseCommon(rendererOutput.getConfigurationResponseCommon())
                        .build();
                        setSuccess(true);
                    }
                }
                return output;
            }
        });

    }


    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

}
