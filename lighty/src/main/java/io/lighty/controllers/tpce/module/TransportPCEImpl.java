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
import java.util.Arrays;
import java.util.List;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl121;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl221;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl710;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion121;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion221;
import org.opendaylight.transportpce.common.mapping.PortMappingVersion710;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsProvider;
import org.opendaylight.transportpce.networkmodel.NetConfTopologyListener;
import org.opendaylight.transportpce.networkmodel.NetworkModelProvider;
import org.opendaylight.transportpce.networkmodel.NetworkUtilsImpl;
import org.opendaylight.transportpce.networkmodel.R2RLinkDiscovery;
import org.opendaylight.transportpce.networkmodel.listeners.PortMappingListener;
import org.opendaylight.transportpce.networkmodel.service.FrequenciesService;
import org.opendaylight.transportpce.networkmodel.service.FrequenciesServiceImpl;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelServiceImpl;
import org.opendaylight.transportpce.olm.OlmPowerServiceRpcImpl;
import org.opendaylight.transportpce.olm.OlmProvider;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerService;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumer;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.impl.PceProvider;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.renderer.RendererProvider;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface121;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterface710;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
// Adding OTN interface
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface221;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmOtnInterface710;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
// Add OTN
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperationsImpl;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerProvider;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.PceListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.RendererListenerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.ServiceListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.R2RTapiLinkDiscovery;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetconfTopologyListener;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelServiceImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkUtilsImpl;
import org.opendaylight.transportpce.tapi.topology.TapiOrLinkListener;
import org.opendaylight.transportpce.tapi.topology.TapiPortMappingListener;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiListener;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev220630.TransportpceNetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev210408.TransportpceTapinetworkutilsService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OrgOpenroadmServiceService;
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
    // service-handler beans
    private final ServicehandlerProvider servicehandlerProvider;
    // T-api
    private TapiProvider tapiProvider;
    // nbi-notifications beans
    private NbiNotificationsProvider nbiNotificationsProvider;
    /**
     * List of publisher topics.
     */
    private final List<String> publisherServiceList = Arrays.asList("PceListener", "ServiceHandlerOperations",
            "ServiceHandler", "RendererListener");
    private final List<String> publisherAlarmList = Arrays.asList("ServiceListener");

    public TransportPCEImpl(LightyServices lightyServices, boolean activateNbiNotification, boolean activateTapi,
                            String olmtimer1, String olmtimer2) {
        LOG.info("Initializing transaction providers ...");
        deviceTransactionManager = new DeviceTransactionManagerImpl(lightyServices.getBindingMountPointService(),
                MAX_DURATION_TO_SUBMIT_TRANSACTION);
        RequestProcessor requestProcessor = new RequestProcessor(lightyServices.getBindingDataBroker());
        networkTransaction = new NetworkTransactionImpl(requestProcessor);

        LOG.info("Creating network-model beans ...");
        R2RLinkDiscovery linkDiscoveryImpl = new R2RLinkDiscovery(lightyServices.getBindingDataBroker(),
                deviceTransactionManager, networkTransaction);
        TransportpceNetworkutilsService networkutilsServiceImpl = new NetworkUtilsImpl(
                lightyServices.getBindingDataBroker());
        PortMapping portMapping = initPortMapping(lightyServices);
        NetworkModelService networkModelService = new NetworkModelServiceImpl(networkTransaction, linkDiscoveryImpl,
                portMapping, lightyServices.getBindingNotificationPublishService());
        FrequenciesService networkModelWavelengthService =
                new FrequenciesServiceImpl(lightyServices.getBindingDataBroker());
        NetConfTopologyListener netConfTopologyListener = new NetConfTopologyListener(networkModelService,
                lightyServices.getBindingDataBroker(), deviceTransactionManager, portMapping);
        PortMappingListener portMappingListener = new PortMappingListener(networkModelService);
        networkModelProvider = new NetworkModelProvider(networkTransaction, lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderService(), networkutilsServiceImpl, netConfTopologyListener,
                lightyServices.getNotificationService(), networkModelWavelengthService, portMappingListener);

        LOG.info("Creating PCE beans ...");
        // TODO: pass those parameters through command line
        GnpyConsumer gnpyConsumer = new GnpyConsumerImpl("http://127.0.0.1:8008",
                "gnpy", "gnpy", lightyServices.getAdapterContext().currentSerializer());
        PathComputationService pathComputationService = new PathComputationServiceImpl(
                networkTransaction,
                lightyServices.getBindingNotificationPublishService(),
                gnpyConsumer,
                portMapping
                );
        pceProvider = new PceProvider(lightyServices.getRpcProviderService(), pathComputationService);

        LOG.info("Creating OLM beans ...");
        MappingUtils mappingUtils = new MappingUtilsImpl(lightyServices.getBindingDataBroker());
        CrossConnect crossConnect = initCrossConnect(mappingUtils);
        OpenRoadmInterfaces openRoadmInterfaces = initOpenRoadmInterfaces(mappingUtils, portMapping);
        PowerMgmt powerMgmt = new PowerMgmtImpl(lightyServices.getBindingDataBroker(), openRoadmInterfaces,
                crossConnect, deviceTransactionManager, portMapping, olmtimer1, olmtimer2);
        OlmPowerService olmPowerService = new OlmPowerServiceImpl(lightyServices.getBindingDataBroker(), powerMgmt,
                deviceTransactionManager, portMapping, mappingUtils, openRoadmInterfaces);
        TransportpceOlmService olmPowerServiceRpc = new OlmPowerServiceRpcImpl(olmPowerService);
        olmProvider = new OlmProvider(lightyServices.getRpcProviderService(), olmPowerServiceRpc);

        LOG.info("Creating renderer beans ...");
        OpenRoadmInterfaceFactory openRoadmInterfaceFactory = initOpenRoadmFactory(mappingUtils, openRoadmInterfaces,
                portMapping);
        DeviceRendererService deviceRendererService = new DeviceRendererServiceImpl(
                lightyServices.getBindingDataBroker(), deviceTransactionManager, openRoadmInterfaceFactory,
                openRoadmInterfaces, crossConnect, portMapping);
        OtnDeviceRendererService otnDeviceRendererService = new OtnDeviceRendererServiceImpl(openRoadmInterfaceFactory,
                crossConnect, openRoadmInterfaces, deviceTransactionManager, networkModelService);
        rendererProvider = initRenderer(lightyServices, olmPowerServiceRpc, deviceRendererService,
                otnDeviceRendererService, portMapping);

        LOG.info("Creating service-handler beans ...");
        RendererServiceOperations rendererServiceOperations = new RendererServiceOperationsImpl(deviceRendererService,
                otnDeviceRendererService, olmPowerServiceRpc, lightyServices.getBindingDataBroker(),
                lightyServices.getBindingNotificationPublishService(), portMapping);
        ServiceDataStoreOperations serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(
                lightyServices.getBindingDataBroker());
        RendererListenerImpl rendererListenerImpl = new RendererListenerImpl(pathComputationService,
            lightyServices.getBindingNotificationPublishService(), networkModelService);
        PceListenerImpl pceListenerImpl = new PceListenerImpl(rendererServiceOperations, pathComputationService,
            lightyServices.getBindingNotificationPublishService(), serviceDataStoreOperations);
        NetworkModelListenerImpl networkModelListenerImpl = new NetworkModelListenerImpl(
                lightyServices.getBindingNotificationPublishService(), serviceDataStoreOperations);
        ServicehandlerImpl servicehandler = new ServicehandlerImpl(lightyServices.getBindingDataBroker(),
            pathComputationService, rendererServiceOperations, lightyServices.getBindingNotificationPublishService(),
            pceListenerImpl, rendererListenerImpl, networkModelListenerImpl, serviceDataStoreOperations);
        ServiceListener serviceListener = new ServiceListener(servicehandler, serviceDataStoreOperations,
                lightyServices.getBindingNotificationPublishService());
        servicehandlerProvider = new ServicehandlerProvider(lightyServices.getBindingDataBroker(),
                lightyServices.getRpcProviderService(), lightyServices.getNotificationService(),
                serviceDataStoreOperations, pceListenerImpl, serviceListener, rendererListenerImpl,
                networkModelListenerImpl, servicehandler);
        if (activateTapi) {
            LOG.info("Creating tapi beans ...");
            TapiLink tapiLink = new TapiLink(networkTransaction);
            R2RTapiLinkDiscovery tapilinkDiscoveryImpl = new R2RTapiLinkDiscovery(networkTransaction,
                    deviceTransactionManager, tapiLink);
            TapiRendererListenerImpl tapiRendererListenerImpl = new TapiRendererListenerImpl(lightyServices
                    .getBindingDataBroker(), lightyServices.getBindingNotificationPublishService());
            TapiPceListenerImpl tapiPceListenerImpl = new TapiPceListenerImpl(lightyServices.getBindingDataBroker());
            TapiServiceHandlerListenerImpl tapiServiceHandlerListener = new TapiServiceHandlerListenerImpl(
                    lightyServices.getBindingDataBroker());
            TransportpceTapinetworkutilsService tapiNetworkutilsServiceImpl = new TapiNetworkUtilsImpl(
                    networkTransaction, tapiLink);
            TapiNetworkModelService tapiNetworkModelService = new TapiNetworkModelServiceImpl(
                    tapilinkDiscoveryImpl, networkTransaction, tapiLink,
                    lightyServices.getBindingNotificationPublishService());
            TapiNetconfTopologyListener tapiNetConfTopologyListener =
                    new TapiNetconfTopologyListener(tapiNetworkModelService);
            TapiOrLinkListener orLinkListener = new TapiOrLinkListener(tapiLink, networkTransaction);
            TapiPortMappingListener tapiPortMappingListener =
                    new TapiPortMappingListener(tapiNetworkModelService);
            TapiNetworkModelListenerImpl tapiNetworkModelListenerImpl =
                    new TapiNetworkModelListenerImpl(networkTransaction,
                            lightyServices.getBindingNotificationPublishService());

            tapiProvider = initTapi(lightyServices, servicehandler, networkTransaction, serviceDataStoreOperations,
                    tapiNetConfTopologyListener, tapiPortMappingListener, tapiNetworkutilsServiceImpl,
                    tapiPceListenerImpl, tapiRendererListenerImpl, tapiServiceHandlerListener,
                    lightyServices.getNotificationService(), orLinkListener, tapiNetworkModelListenerImpl);
        }
        if (activateNbiNotification) {
            LOG.info("Creating nbi-notifications beans ...");
            nbiNotificationsProvider = new NbiNotificationsProvider(
                    publisherServiceList, publisherAlarmList, null, null, lightyServices.getRpcProviderService(),
                    lightyServices.getNotificationService(), lightyServices.getAdapterContext().currentSerializer(),
                    networkTransaction);
        }
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
        if (tapiProvider != null) {
            LOG.info("Initializing tapi provider ...");
            tapiProvider.init();
        }
        if (nbiNotificationsProvider != null) {
            LOG.info("Initializing nbi-notifications provider ...");
            nbiNotificationsProvider.init();
        }
        LOG.info("Init done.");
        return true;
    }

    @Override
    protected boolean stopProcedure() {
        if (nbiNotificationsProvider != null) {
            nbiNotificationsProvider.close();
            LOG.info("Shutting down nbi-notifications provider ...");
        }
        if (tapiProvider != null) {
            tapiProvider.close();
            LOG.info("Shutting down service-handler provider ...");
        }
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

    private TapiProvider initTapi(LightyServices lightyServices, OrgOpenroadmServiceService servicehandler,
                                  NetworkTransactionService networkTransactionService,
                                  ServiceDataStoreOperations serviceDataStoreOperations,
                                  TapiNetconfTopologyListener tapiNetConfTopologyListener,
                                  TapiPortMappingListener tapiPortMappingListener,
                                  TransportpceTapinetworkutilsService tapiNetworkutilsServiceImpl,
                                  TapiPceListenerImpl pceListenerImpl, TapiRendererListenerImpl rendererListenerImpl,
                                  TapiServiceHandlerListenerImpl serviceHandlerListenerImpl,
                                  NotificationService notificationService, TapiOrLinkListener orLinkListener,
                                  TapiNetworkModelListenerImpl tapiNetworkModelListenerImpl) {
        return new TapiProvider(lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(),
            servicehandler, serviceDataStoreOperations, new TapiListener(), networkTransactionService,
            tapiNetConfTopologyListener, tapiPortMappingListener, tapiNetworkutilsServiceImpl, pceListenerImpl,
            rendererListenerImpl, serviceHandlerListenerImpl, notificationService, orLinkListener,
            tapiNetworkModelListenerImpl);
    }

    private RendererProvider initRenderer(LightyServices lightyServices, TransportpceOlmService olmPowerServiceRpc,
            DeviceRendererService deviceRendererService, OtnDeviceRendererService otnDeviceRendererService,
            PortMapping portMapping) {
        DeviceRendererRPCImpl deviceRendererRPC = new DeviceRendererRPCImpl(deviceRendererService,
                otnDeviceRendererService);
        RendererServiceOperationsImpl rendererServiceOperations = new RendererServiceOperationsImpl(
                deviceRendererService, otnDeviceRendererService, olmPowerServiceRpc,
                lightyServices.getBindingDataBroker(), lightyServices.getBindingNotificationPublishService(),
                portMapping);
        return new RendererProvider(lightyServices.getRpcProviderService(), deviceRendererRPC,
                rendererServiceOperations);
    }

    private OpenRoadmInterfaceFactory initOpenRoadmFactory(MappingUtils mappingUtils,
            OpenRoadmInterfaces openRoadmInterfaces, PortMapping portMapping) {
        OpenRoadmInterface121 openRoadmInterface121 = new OpenRoadmInterface121(portMapping, openRoadmInterfaces);
        OpenRoadmInterface221 openRoadmInterface221 = new OpenRoadmInterface221(portMapping, openRoadmInterfaces);
        OpenRoadmInterface710 openRoadmInterface710 = new OpenRoadmInterface710(portMapping, openRoadmInterfaces);
        OpenRoadmOtnInterface221 openRoadmOtnInterface221 = new OpenRoadmOtnInterface221(portMapping,
                openRoadmInterfaces);
        OpenRoadmOtnInterface710 openRoadmOtnInterface710 = new OpenRoadmOtnInterface710(portMapping,
            openRoadmInterfaces);
        return new OpenRoadmInterfaceFactory(mappingUtils, openRoadmInterface121, openRoadmInterface221,
            openRoadmInterface710, openRoadmOtnInterface221, openRoadmOtnInterface710);
    }

    private PortMapping initPortMapping(LightyServices lightyServices) {
        PortMappingVersion710 portMappingVersion710 = new PortMappingVersion710(lightyServices.getBindingDataBroker(),
                deviceTransactionManager);
        PortMappingVersion221 portMappingVersion221 = new PortMappingVersion221(lightyServices.getBindingDataBroker(),
                deviceTransactionManager);
        PortMappingVersion121 portMappingVersion121 = new PortMappingVersion121(lightyServices.getBindingDataBroker(),
                deviceTransactionManager);
        return new PortMappingImpl(lightyServices.getBindingDataBroker(), portMappingVersion710,
            portMappingVersion221, portMappingVersion121);
    }

    private OpenRoadmInterfaces initOpenRoadmInterfaces(MappingUtils mappingUtils, PortMapping portMapping) {
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121 = new OpenRoadmInterfacesImpl121(
                deviceTransactionManager);
        OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221 = new OpenRoadmInterfacesImpl221(
                deviceTransactionManager, portMapping, portMapping.getPortMappingVersion221());
        OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710 = new OpenRoadmInterfacesImpl710(
                deviceTransactionManager, portMapping, portMapping.getPortMappingVersion710());
        return new OpenRoadmInterfacesImpl(deviceTransactionManager, mappingUtils, openRoadmInterfacesImpl121,
                openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);
    }

    private CrossConnect initCrossConnect(MappingUtils mappingUtils) {
        CrossConnectImpl121 crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        CrossConnectImpl221 crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        CrossConnectImpl710 crossConnectImpl710 = new CrossConnectImpl710(deviceTransactionManager);
        return new CrossConnectImpl(deviceTransactionManager, mappingUtils, crossConnectImpl121, crossConnectImpl221,
            crossConnectImpl710);
    }
}
