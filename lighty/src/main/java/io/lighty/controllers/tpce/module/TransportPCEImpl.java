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
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.networkmodel.NetConfTopologyListener;
import org.opendaylight.transportpce.networkmodel.NetworkModelProvider;
import org.opendaylight.transportpce.networkmodel.NetworkUtilsImpl;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelServiceImpl;
import org.opendaylight.transportpce.olm.OlmPowerServiceRpcImpl;
import org.opendaylight.transportpce.olm.OlmProvider;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.pce.impl.PceProvider;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthServiceImpl;
import org.opendaylight.transportpce.renderer.RendererProvider;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
// Adding OTN interface
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface221;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
// Add OTN
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperationsImpl;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerProvider;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperationsImpl;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev170818.TransportpceNetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.TransportpceOlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransportPCEImpl extends AbstractLightyModule implements TransportPCE {
    private static final Logger LOG = LoggerFactory.getLogger(TransportPCEImpl.class);
    private static final long MAX_DURATION_TO_SUBMIT_TRANSACTION = 1500;
    // transaction beans
    // cannot use interface for DeviceTransactionManagerImpl
    // because implementation has additional public methods ...
    private final DeviceTransactionManagerImpl deviceTransactionManager;
    private final NetworkTransactionService networkTransaction;
    // pce beans
    private final PceProvider pceProvider;
    // network model beans
    private final NetworkModelProvider networkModelProvider;
    // OLM beans
    private final OlmProvider olmProvider;
    // renderer beans
    private final RendererProvider rendererProvider;
    // T-api
    private final TapiProvider tapiProvider;
    // service-handler beans
    private final ServicehandlerProvider servicehandlerProvider;

    public TransportPCEImpl(LightyServices lightyServices) {
        LOG.info("Initializing transaction providers ...");
        deviceTransactionManager = new DeviceTransactionManagerImpl(lightyServices.getBindingMountPointService(),
                MAX_DURATION_TO_SUBMIT_TRANSACTION);
        RequestProcessor requestProcessor = new RequestProcessor(lightyServices.getBindingDataBroker());
        networkTransaction = new NetworkTransactionImpl(requestProcessor);

        LOG.info("Creating PCE beans ...");
        PathComputationService pathComputationService = new PathComputationServiceImpl(networkTransaction,
                lightyServices.getBindingNotificationPublishService());
        pceProvider = new PceProvider(lightyServices.getRpcProviderService(), pathComputationService);

        LOG.info("Creating network-model beans ...");
        R2RLinkDiscovery linkDiscoveryImpl = new R2RLinkDiscovery(lightyServices.getBindingDataBroker(),
                deviceTransactionManager, networkTransaction);
        TransportpceNetworkutilsService networkutilsServiceImpl = new NetworkUtilsImpl(
                lightyServices.getBindingDataBroker());
        MappingUtils mappingUtils = new MappingUtilsImpl(lightyServices.getBindingDataBroker());
        OpenRoadmInterfaces openRoadmInterfaces = initOpenRoadmInterfaces(mappingUtils);
        PortMapping portMapping = initPortMapping(lightyServices, openRoadmInterfaces);
        NetworkModelService networkModelService = new NetworkModelServiceImpl(networkTransaction, linkDiscoveryImpl,
                portMapping);
        NetConfTopologyListener netConfTopologyListener = new NetConfTopologyListener(networkModelService,
                lightyServices.getBindingDataBroker(), deviceTransactionManager);
        networkModelProvider = new NetworkModelProvider(networkTransaction, lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderService(), networkutilsServiceImpl, netConfTopologyListener);

        LOG.info("Creating OLM beans ...");
        CrossConnect crossConnect = initCrossConnect(mappingUtils);
        PowerMgmt powerMgmt = new PowerMgmtImpl(lightyServices.getBindingDataBroker(), openRoadmInterfaces,
                crossConnect, deviceTransactionManager);
        OlmPowerService olmPowerService = new OlmPowerServiceImpl(lightyServices.getBindingDataBroker(), powerMgmt,
                deviceTransactionManager, portMapping, mappingUtils, openRoadmInterfaces);
        olmProvider = new OlmProvider(lightyServices.getRpcProviderService(), olmPowerService);
        TransportpceOlmService olmPowerServiceRpc = new OlmPowerServiceRpcImpl(olmPowerService);

        LOG.info("Creating renderer beans ...");
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory = initOpenRoadmFactory(mappingUtils, openRoadmInterfaces,
                portMapping);
        DeviceRendererService deviceRendererService = new DeviceRendererServiceImpl(
                lightyServices.getBindingDataBroker(), deviceTransactionManager, openRoadmInterfaceFactory,
                openRoadmInterfaces, crossConnect, portMapping, networkModelService);
        OtnDeviceRendererService otnDeviceRendererService = new OtnDeviceRendererServiceImpl(openRoadmInterfaceFactory,
                crossConnect, openRoadmInterfaces, deviceTransactionManager, networkModelService);
        NetworkModelWavelengthService networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(
                lightyServices.getBindingDataBroker());
        rendererProvider = initRenderer(lightyServices, olmPowerServiceRpc, networkModelWavelengthService,
                deviceRendererService, otnDeviceRendererService);

        LOG.info("Creating service-handler beans ...");
        RendererServiceOperations rendererServiceOperations = new RendererServiceOperationsImpl(deviceRendererService,
                otnDeviceRendererService, olmPowerServiceRpc, lightyServices.getBindingDataBroker(),
                networkModelWavelengthService, lightyServices.getBindingNotificationPublishService());
        servicehandlerProvider = new ServicehandlerProvider(lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderService(), lightyServices.getNotificationService(), pathComputationService,
                rendererServiceOperations, networkModelWavelengthService,
                lightyServices.getBindingNotificationPublishService());
        tapiProvider = initTapi(lightyServices, rendererServiceOperations, networkModelWavelengthService,
                pathComputationService);
    }

    @Override
    protected boolean initProcedure() {
        LOG.info("Initializing PCE provider ...");
        pceProvider.init();
        LOG.info("Initializing network-model provider ...");
        networkModelProvider.init();
        LOG.info("Initializing OLM provider ...");
        olmProvider.init();
        LOG.info("Initializing renderer provider ...");
        rendererProvider.init();
        LOG.info("Initializing service-handler provider ...");
        servicehandlerProvider.init();
        LOG.info("Initializing tapi provider ...");
        tapiProvider.init();
        LOG.info("Init done.");
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        tapiProvider.close();
        LOG.info("Shutting down service-handler provider ...");
        servicehandlerProvider.close();
        LOG.info("Shutting down renderer provider ...");
        rendererProvider.close();
        LOG.info("Shutting down OLM provider ...");
        olmProvider.close();
        LOG.info("Shutting down network-model provider ...");
        networkModelProvider.close();
        LOG.info("Shutting down PCE provider ...");
        pceProvider.close();
        LOG.info("Shutting down transaction providers ...");
        networkTransaction.close();
        deviceTransactionManager.preDestroy();
        LOG.info("Shutdown done.");
        return true;
    }

    /**
     * Init tapi provider beans.
     *
     * @param lightyServices LightyServices
     * @param rendererServiceOperations RendererServiceOperations
     * @param networkModelWavelengthService NetworkModelWavelengthService
     * @return TapiProvider instance
     */
    private TapiProvider initTapi(LightyServices lightyServices, RendererServiceOperations rendererServiceOperations,
            NetworkModelWavelengthService networkModelWavelengthService,
            PathComputationService pathComputationService) {
        RendererListenerImpl rendererListenerImpl = new RendererListenerImpl(pathComputationService,
                lightyServices.getBindingNotificationPublishService());
        ServiceDataStoreOperations serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(
                lightyServices.getBindingDataBroker());
        PceListenerImpl pceListenerImpl = new PceListenerImpl(rendererServiceOperations, pathComputationService,
                lightyServices.getBindingNotificationPublishService(), serviceDataStoreOperations);
        ServiceHandlerOperations serviceHandlerOperations = new ServiceHandlerOperationsImpl(
                lightyServices.getBindingDataBroker(), pathComputationService, rendererServiceOperations,
                lightyServices.getBindingNotificationPublishService(), pceListenerImpl, rendererListenerImpl,
                networkModelWavelengthService);
        return new TapiProvider(lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(),
                serviceHandlerOperations, new TapiListener());
    }

    /**
     * Init renderer provider beans.
     *
     * @param lightyServices LightyServices
     * @param olmPowerServiceRpc TransportpceOlmService
     * @param networkModelWavelengthService NetworkModelWavelengthService
     * @param deviceRendererService DeviceRendererService
     * @param otnDeviceRendererService OtnDeviceRendererService
     * @return RendererProvider instance
     */
    private RendererProvider initRenderer(LightyServices lightyServices, TransportpceOlmService olmPowerServiceRpc,
            NetworkModelWavelengthService networkModelWavelengthService, DeviceRendererService deviceRendererService,
            OtnDeviceRendererService otnDeviceRendererService) {
        DeviceRendererRPCImpl deviceRendererRPC = new DeviceRendererRPCImpl(deviceRendererService,
                otnDeviceRendererService);
        RendererServiceOperationsImpl rendererServiceOperations = new RendererServiceOperationsImpl(
                deviceRendererService, otnDeviceRendererService, olmPowerServiceRpc,
                lightyServices.getBindingDataBroker(), networkModelWavelengthService,
                lightyServices.getBindingNotificationPublishService());
        return new RendererProvider(lightyServices.getRpcProviderService(), deviceRendererRPC,
                rendererServiceOperations);
    }

    /**
     * Init OpenRoadmInterfaceFactory.
     *
     * @param mappingUtils MappingUtils
     * @param openRoadmInterfaces OpenRoadmInterfaces
     * @param portMapping PortMapping
     * @return OpenRoadmInterfaceFactory instance
     */
    private OpenRoadmInterfaceFactory initOpenRoadmFactory(MappingUtils mappingUtils,
            OpenRoadmInterfaces openRoadmInterfaces, PortMapping portMapping) {
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping, openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces,
                new FixedFlexImpl());
        OpenRoadmOtnInterface221 openRoadmOtnInterface221 = new OpenRoadmOtnInterface221(portMapping,
                openRoadmInterfaces);
        return new OpenRoadmInterfaceFactory(mappingUtils, openRoadmInterface121, openRoadmInterface221,
                openRoadmOtnInterface221);
    }

    /**
     * Init PortMapping.
     *
     * @param lightyServices LightyServices
     * @param openRoadmInterfaces OpenRoadmInterfaces
     * @return PortMapping instance
     */
    private PortMapping initPortMapping(LightyServices lightyServices, OpenRoadmInterfaces openRoadmInterfaces) {
        PortMappingVersion221 portMappingVersion221 = new PortMappingVersion221(lightyServices.getBindingDataBroker(),
                deviceTransactionManager, openRoadmInterfaces);
        PortMappingVersion121 portMappingVersion121 = new PortMappingVersion121(lightyServices.getBindingDataBroker(),
                deviceTransactionManager, openRoadmInterfaces);
        return new PortMappingImpl(lightyServices.getBindingDataBroker(), portMappingVersion221, portMappingVersion121);
    }

    /**
     * Init OpenRoadmInterfaces.
     *
     * @param mappingUtils MappingUtils
     * @return OpenRoadmInterfaces instance
     */
    private OpenRoadmInterfaces initOpenRoadmInterfaces(MappingUtils mappingUtils) {
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(
                deviceTransactionManager);
        OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(
                deviceTransactionManager);
        return new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils, openRoadmInterfacesImpl121,
                openRoadmInterfacesImpl221);
    }

    /**
     * Init CrossConnect.
     *
     * @param mappingUtils MappingUtils
     * @return CrossConnect instance
     */
    private CrossConnect initCrossConnect(MappingUtils mappingUtils) {
        CrossConnectImpl121 crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        CrossConnectImpl221 crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        return new CrossConnectImpl(deviceTransactionManager, mappingUtils, crossConnectImpl121, crossConnectImpl221);
    }
}
