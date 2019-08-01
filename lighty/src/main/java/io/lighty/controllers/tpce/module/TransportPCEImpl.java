/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.controllers.tpce.module;

import io.lighty.core.controller.api.AbstractLightyModule;
import io.lighty.core.controller.api.LightyServices;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.networkmodel.NetConfTopologyListener;
import org.opendaylight.transportpce.networkmodel.NetworkModelProvider;
import org.opendaylight.transportpce.networkmodel.NetworkUtilsImpl;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelServiceImpl;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmFactory;
import org.opendaylight.transportpce.networkmodel.util.OpenRoadmTopology22;
import org.opendaylight.transportpce.olm.OlmPowerServiceRpcImpl;
import org.opendaylight.transportpce.olm.OlmProvider;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.pce.impl.PceProvider;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthServiceImpl;
import org.opendaylight.transportpce.renderer.RendererProvider;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperationsImpl;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportPCEImpl extends AbstractLightyModule implements TransportPCE {

    private static final Logger LOG = LoggerFactory.getLogger(TransportPCEImpl.class);
    private static final long MaxDurationToSubmitTransaction = 1500;

    // common beans
    private final DeviceTransactionManagerImpl deviceTransactionManager;
    private final MappingUtilsImpl mappingUtils;
    private final OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private final OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private final OpenRoadmInterfacesImpl openRoadmInterfaces;
    private final PortMappingVersion221 portMappingVersion221;
    private final RequestProcessor requestProcessor;
    private final NetworkTransactionImpl networkTransaction;
    private final PortMappingVersion121 portMappingVersion121;
    private final PortMappingImpl portMapping;
    private final CrossConnectImpl121 crossConnectImpl121;
    private final CrossConnectImpl221 crossConnectImpl221;
    private final CrossConnectImpl crossConnect;
    private final FixedFlexImpl fixedFlex;

    // pce beans
    private final PathComputationServiceImpl pathComputationService;
    private final PceProvider pceProvider;

    // network model beans
    private final OpenRoadmTopology22 openRoadmTopology22;
    private final OpenRoadmFactory openRoadmFactory;
    private final R2RLinkDiscovery linkDiscoveryImpl;
    private final NetworkUtilsImpl networkutilsServiceImpl;
    private final NetworkModelServiceImpl networkModelService;
    private final NetConfTopologyListener netConfTopologyListener;
    private final NetworkModelProvider networkModelProvider;

    // OLM beans
    private final PowerMgmt powerMgmt;
    private final OlmPowerServiceImpl olmPowerService;
    private final OlmProvider olmProvider;
    private final OlmPowerServiceRpcImpl olmPowerServiceRpc;

    // renderer beans
    private final OpenRoadmInterface121 openRoadmInterface121;
    private final OpenRoadmInterface221 openRoadmInterface22;
    private final OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private final DeviceRendererServiceImpl deviceRendererService;
    private final DeviceRendererRPCImpl deviceRendererRPC;
    private final NetworkModelWavelengthServiceImpl networkModelWavelengthService;
    private final RendererServiceOperationsImpl rendererServiceOperations;
    private final RendererProvider rendererProvider;

    // service-handler beans
    private final ServicehandlerProvider servicehandlerProvider;

    public TransportPCEImpl(LightyServices lightyServices) {
        LOG.info("Creating common beans ...");
        deviceTransactionManager = new DeviceTransactionManagerImpl(lightyServices.getControllerBindingMountPointService(), MaxDurationToSubmitTransaction);
        mappingUtils = new MappingUtilsImpl(lightyServices.getControllerBindingPingPongDataBroker());
        openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(deviceTransactionManager);
        openRoadmInterfaces = new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils, openRoadmInterfacesImpl121, openRoadmInterfacesImpl221);
        portMappingVersion221 = new PortMappingVersion221(lightyServices.getControllerBindingPingPongDataBroker(), deviceTransactionManager, openRoadmInterfaces);
        requestProcessor = new RequestProcessor(lightyServices.getControllerBindingPingPongDataBroker());
        networkTransaction = new NetworkTransactionImpl(requestProcessor);
        portMappingVersion121 = new PortMappingVersion121(lightyServices.getControllerBindingPingPongDataBroker(), deviceTransactionManager, openRoadmInterfaces);
        portMapping = new PortMappingImpl(lightyServices.getControllerBindingPingPongDataBroker(), portMappingVersion221, portMappingVersion121);
        crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        crossConnect = new CrossConnectImpl(deviceTransactionManager, mappingUtils, crossConnectImpl121, crossConnectImpl221);
        fixedFlex = new FixedFlexImpl();

        LOG.info("Creating PCE beans ...");
        pathComputationService = new PathComputationServiceImpl(networkTransaction, lightyServices.getControllerBindingNotificationPublishService());
        pceProvider = new PceProvider(lightyServices.getControllerRpcProviderRegistry(), pathComputationService);

        LOG.info("Creating network-model beans ...");
        openRoadmTopology22 = new OpenRoadmTopology22(networkTransaction, deviceTransactionManager);
        openRoadmFactory = new OpenRoadmFactory(mappingUtils, openRoadmTopology22);
        linkDiscoveryImpl = new R2RLinkDiscovery(lightyServices.getControllerBindingPingPongDataBroker(), deviceTransactionManager, openRoadmFactory, networkTransaction);
        networkutilsServiceImpl = new NetworkUtilsImpl(lightyServices.getControllerBindingPingPongDataBroker(), openRoadmFactory);
        networkModelService = new NetworkModelServiceImpl(networkTransaction, linkDiscoveryImpl, deviceTransactionManager, openRoadmFactory, portMapping);
        netConfTopologyListener = new NetConfTopologyListener(networkModelService, lightyServices.getControllerBindingPingPongDataBroker(), deviceTransactionManager);
        networkModelProvider = new NetworkModelProvider(networkTransaction, lightyServices.getControllerBindingPingPongDataBroker(), lightyServices.getControllerRpcProviderRegistry(), networkutilsServiceImpl, netConfTopologyListener, openRoadmFactory);

        LOG.info("Creating OLM beans ...");
        powerMgmt = new PowerMgmtImpl(lightyServices.getControllerBindingPingPongDataBroker(), openRoadmInterfaces, crossConnect, deviceTransactionManager);
        olmPowerService = new OlmPowerServiceImpl(lightyServices.getControllerBindingPingPongDataBroker(), powerMgmt, deviceTransactionManager, portMapping, mappingUtils, openRoadmInterfaces);
        olmProvider = new OlmProvider(lightyServices.getControllerRpcProviderRegistry(), olmPowerService);
        olmPowerServiceRpc = new OlmPowerServiceRpcImpl(olmPowerService);

        LOG.info("Creating renderer beans ...");
        openRoadmInterface121 = new OpenRoadmInterface121(portMapping, openRoadmInterfaces);
        openRoadmInterface22 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces, fixedFlex);
        openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(mappingUtils, openRoadmInterface121, openRoadmInterface22);
        deviceRendererService = new DeviceRendererServiceImpl(lightyServices.getControllerBindingPingPongDataBroker(), deviceTransactionManager, openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect, portMapping);
        deviceRendererRPC = new DeviceRendererRPCImpl(deviceRendererService);
        networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(lightyServices.getControllerBindingPingPongDataBroker());
        rendererServiceOperations = new RendererServiceOperationsImpl(deviceRendererService, olmPowerServiceRpc, lightyServices.getControllerBindingPingPongDataBroker(), networkModelWavelengthService, lightyServices.getControllerBindingNotificationPublishService());
        rendererProvider = new RendererProvider(lightyServices.getControllerRpcProviderRegistry(), deviceRendererRPC, rendererServiceOperations);

        LOG.info("Creating service-handler beans ...");
        servicehandlerProvider = new ServicehandlerProvider(lightyServices.getControllerBindingPingPongDataBroker(), lightyServices.getControllerRpcProviderRegistry(), lightyServices.getControllerBindingNotificationService(),
                pathComputationService, rendererServiceOperations, networkModelWavelengthService, lightyServices.getControllerBindingNotificationPublishService());

    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Initializing common beans ...");
        LOG.info("Initializing PCE beans ...");
        pathComputationService.init();
        pceProvider.init();
        LOG.info("Initializing network-model beans ...");
        networkModelProvider.init();
        LOG.info("Initializing OLM beans ...");
        olmPowerService.init();
        olmProvider.init();
        LOG.info("Initializing renderer beans ...");
        rendererProvider.init();
        LOG.info("Initializing service-handler beans ...");
        servicehandlerProvider.init();
        LOG.info("Init done.");
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        LOG.info("Shutting down service-handler beans ...");
        servicehandlerProvider.close();
        LOG.info("Shutting down renderer beans ...");
        rendererProvider.close();
        LOG.info("Shutting down OLM beans ...");
        olmProvider.close();
        olmPowerService.close();
        LOG.info("Shutting down network-model beans ...");
        networkModelProvider.close();
        LOG.info("Shutting down PCE beans ...");
        pathComputationService.close();
        pceProvider.close();
        LOG.info("Shutting down common beans ...");
        networkTransaction.close();
        deviceTransactionManager.preDestroy();
        LOG.info("Shutdown done.");
        return true;
    }

}
