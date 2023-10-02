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
import java.util.ArrayList;
import java.util.List;
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
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsProvider;
import org.opendaylight.transportpce.networkmodel.NetConfTopologyListener;
import org.opendaylight.transportpce.networkmodel.NetworkModelProvider;
import org.opendaylight.transportpce.networkmodel.NetworkUtilsImpl;
import org.opendaylight.transportpce.networkmodel.listeners.PortMappingListener;
import org.opendaylight.transportpce.networkmodel.service.FrequenciesServiceImpl;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelServiceImpl;
import org.opendaylight.transportpce.olm.OlmPowerServiceRpcImpl;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.service.OlmPowerServiceImpl;
import org.opendaylight.transportpce.pce.gnpy.consumer.GnpyConsumerImpl;
import org.opendaylight.transportpce.pce.impl.PceServiceRPCImpl;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
// Adding OTN interface
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererService;
import org.opendaylight.transportpce.renderer.provisiondevice.DeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererService;
// Add OTN
import org.opendaylight.transportpce.renderer.provisiondevice.OtnDeviceRendererServiceImpl;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperations;
import org.opendaylight.transportpce.renderer.provisiondevice.RendererServiceOperationsImpl;
import org.opendaylight.transportpce.renderer.rpcs.DeviceRendererRPCImpl;
import org.opendaylight.transportpce.renderer.rpcs.TransportPCEServicePathRPCImpl;
import org.opendaylight.transportpce.servicehandler.catalog.CatalogDataStoreOperationsImpl;
import org.opendaylight.transportpce.servicehandler.impl.ServiceHandlerProvider;
import org.opendaylight.transportpce.servicehandler.impl.ServicehandlerImpl;
import org.opendaylight.transportpce.servicehandler.listeners.NetworkModelNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.PceNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.RendererNotificationHandler;
import org.opendaylight.transportpce.servicehandler.listeners.ServiceListener;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperationsImpl;
import org.opendaylight.transportpce.tapi.impl.TapiProvider;
import org.opendaylight.transportpce.tapi.listeners.TapiNetworkModelNotificationHandler;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelService;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkModelServiceImpl;
import org.opendaylight.transportpce.tapi.topology.TapiNetworkUtilsImpl;
import org.opendaylight.transportpce.tapi.utils.TapiLink;
import org.opendaylight.transportpce.tapi.utils.TapiLinkImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.TransportpceOlmService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransportPCEImpl extends AbstractLightyModule implements TransportPCE {
    private static final Logger LOG = LoggerFactory.getLogger(TransportPCEImpl.class);
    private static final long MAX_TIME_FOR_TRANSACTION = 1500;
    // transaction beans
    // cannot use interface for DeviceTransactionManagerImpl
    // because implementation has additional public methods ...
    private final DeviceTransactionManagerImpl deviceTransactionManager;
    private final NetworkTransactionService networkTransaction;
    // network model beans
    private final NetworkModelProvider networkModelProvider;
    // service-handler beans
    private final ServiceHandlerProvider servicehandlerProvider;
    // T-api
    private TapiProvider tapiProvider;
    // nbi-notifications beans
    private NbiNotificationsProvider nbiNotificationsProvider;
    private List<Registration> rpcRegistrations = new ArrayList<>();

    public TransportPCEImpl(
            LightyServices lightyServices, boolean activateNbiNotification, boolean activateTapi,
            String olmtimer1, String olmtimer2) {
        LOG.info("Initializing transaction providers ...");
        deviceTransactionManager =
            new DeviceTransactionManagerImpl(lightyServices.getBindingMountPointService(), MAX_TIME_FOR_TRANSACTION);
        var lgServBDB = lightyServices.getBindingDataBroker();
        networkTransaction = new NetworkTransactionImpl(lgServBDB);

        LOG.info("Creating network-model beans ...");
        PortMapping portMapping = initPortMapping(lightyServices);
        var lgServBNPS = lightyServices.getBindingNotificationPublishService();
        NetworkModelService networkModelService = new NetworkModelServiceImpl(
                lgServBDB,
                deviceTransactionManager, networkTransaction, portMapping,
                lgServBNPS);
        new NetConfTopologyListener(
                networkModelService, lgServBDB, deviceTransactionManager, portMapping);
        new PortMappingListener(networkModelService);
        var lgServRPS = lightyServices.getRpcProviderService();
        var lgServNS = lightyServices.getNotificationService();
        new NetworkUtilsImpl(lgServBDB, lgServRPS);
        networkModelProvider = new NetworkModelProvider(
                networkTransaction,
                lgServBDB,
                networkModelService, deviceTransactionManager, portMapping,
                lgServNS,
                new FrequenciesServiceImpl(lgServBDB));

        LOG.info("Creating PCE beans ...");
        // TODO: pass those parameters through command line
        PathComputationService pathComputationService = new PathComputationServiceImpl(
                networkTransaction,
                lgServBNPS,
                new GnpyConsumerImpl(
                    "http://127.0.0.1:8008", "gnpy", "gnpy", lightyServices.getAdapterContext().currentSerializer()),
                portMapping);
        rpcRegistrations.add(
            new PceServiceRPCImpl(lgServRPS, pathComputationService)
                .getRegisteredRpc());
        LOG.info("Creating OLM beans ...");
        MappingUtils mappingUtils = new MappingUtilsImpl(lgServBDB);
        CrossConnect crossConnect = initCrossConnect(mappingUtils);
        OpenRoadmInterfaces openRoadmInterfaces = initOpenRoadmInterfaces(mappingUtils, portMapping);
        OlmPowerServiceRpcImpl olmPowerServiceRpc = new OlmPowerServiceRpcImpl(
            new OlmPowerServiceImpl(
                lgServBDB,
                new PowerMgmtImpl(
                    openRoadmInterfaces, crossConnect, deviceTransactionManager,
                    portMapping, Long.valueOf(olmtimer1).longValue(), Long.valueOf(olmtimer2).longValue()),
                deviceTransactionManager, portMapping, mappingUtils, openRoadmInterfaces),
            lgServRPS);
        rpcRegistrations.add(olmPowerServiceRpc.getRegisteredRpc());
        LOG.info("Creating renderer beans ...");
        initOpenRoadmFactory(mappingUtils, openRoadmInterfaces, portMapping);
        DeviceRendererService deviceRendererService = new DeviceRendererServiceImpl(
                lgServBDB,
                deviceTransactionManager, openRoadmInterfaces, crossConnect,
                mappingUtils, portMapping);
        OtnDeviceRendererService otnDeviceRendererService = new OtnDeviceRendererServiceImpl(
                crossConnect, openRoadmInterfaces, deviceTransactionManager, mappingUtils, portMapping);
        initRenderer(lightyServices, olmPowerServiceRpc, deviceRendererService, otnDeviceRendererService, portMapping);

        LOG.info("Creating service-handler beans ...");
        RendererServiceOperations rendererServiceOperations = new RendererServiceOperationsImpl(
                deviceRendererService, otnDeviceRendererService, olmPowerServiceRpc,
                lgServBDB,
                lgServBNPS,
                portMapping);
        ServiceDataStoreOperations serviceDataStoreOperations =
            new ServiceDataStoreOperationsImpl(lgServBDB);
        RendererNotificationHandler rendererNotificationHandler =
            new RendererNotificationHandler(pathComputationService, lgServBNPS, networkModelService);
        PceNotificationHandler pceListenerImpl = new PceNotificationHandler(
                rendererServiceOperations, pathComputationService,
                lgServBNPS, serviceDataStoreOperations);
        NetworkModelNotificationHandler networkModelNotificationHandler = new NetworkModelNotificationHandler(
                lgServBNPS, serviceDataStoreOperations);
        ServicehandlerImpl servicehandler = new ServicehandlerImpl(lgServRPS,
                pathComputationService, rendererServiceOperations,
                lgServBNPS, pceListenerImpl,
                rendererNotificationHandler, networkModelNotificationHandler, serviceDataStoreOperations,
                new CatalogDataStoreOperationsImpl(networkTransaction));
        rpcRegistrations.add(servicehandler.getRegisteredRpc());
        servicehandlerProvider = new ServiceHandlerProvider(
                lgServBDB,
                lgServNS, serviceDataStoreOperations, pceListenerImpl,
                rendererNotificationHandler, networkModelNotificationHandler,
                new ServiceListener(
                    servicehandler, serviceDataStoreOperations, lgServBNPS));
        if (activateTapi) {
            LOG.info("Creating tapi beans ...");
            TapiLink tapiLink = new TapiLinkImpl(networkTransaction);
            new TapiNetworkUtilsImpl(lgServRPS, networkTransaction, tapiLink);
            tapiProvider = initTapi(
                    lightyServices, servicehandler, networkTransaction, serviceDataStoreOperations,
                    new TapiNetworkModelNotificationHandler(
                        networkTransaction, lgServBNPS),
                    tapiLink,
                    new TapiNetworkModelServiceImpl(
                        networkTransaction, deviceTransactionManager, tapiLink,
                        lgServBNPS));
            rpcRegistrations.addAll(tapiProvider.getRegisteredRpcs());
        }
        if (activateNbiNotification) {
            LOG.info("Creating nbi-notifications beans ...");
            nbiNotificationsProvider = new NbiNotificationsProvider(
                    lgServRPS, lgServNS,
                    lightyServices.getAdapterContext().currentSerializer(),
                    networkTransaction, null);
        }
    }

    @Override
    protected boolean initProcedure() {
        if (tapiProvider != null) {
            LOG.info("Initializing tapi provider ...");
        }
        if (nbiNotificationsProvider != null) {
            LOG.info("Initializing nbi-notifications provider ...");
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
        LOG.info("Shutting down network-model provider ...");
        networkModelProvider.close();
        LOG.info("Shutting down transaction providers ...");
        deviceTransactionManager.preDestroy();
        LOG.info("Closing registered RPCs...");
        for (Registration reg : rpcRegistrations) {
            reg.close();
        }
        LOG.info("Shutdown done.");
        return true;
    }

    private TapiProvider initTapi(
            LightyServices lightyServices, OrgOpenroadmServiceService servicehandler,
            NetworkTransactionService networkTransactionService, ServiceDataStoreOperations serviceDataStoreOperations,
            TapiNetworkModelNotificationHandler tapiNetworkModelNotificationHandler, TapiLink tapiLink,
            TapiNetworkModelService tapiNetworkModelService) {
        return new TapiProvider(
            lightyServices.getBindingDataBroker(), lightyServices.getRpcProviderService(),
            lightyServices.getNotificationService(), lightyServices.getBindingNotificationPublishService(),
            networkTransactionService, servicehandler, serviceDataStoreOperations,
            tapiNetworkModelNotificationHandler, tapiNetworkModelService);
    }

    private void initRenderer(
            LightyServices lightyServices, TransportpceOlmService olmPowerServiceRpc,
            DeviceRendererService deviceRendererService, OtnDeviceRendererService otnDeviceRendererService,
            PortMapping portMapping) {
        rpcRegistrations.add(
            new DeviceRendererRPCImpl(
                    lightyServices.getRpcProviderService(),
                    deviceRendererService,
                    otnDeviceRendererService)
                .getRegisteredRpc());
        rpcRegistrations.add(
            new TransportPCEServicePathRPCImpl(
                    new RendererServiceOperationsImpl(
                            deviceRendererService,
                            otnDeviceRendererService,
                            olmPowerServiceRpc,
                            lightyServices.getBindingDataBroker(),
                            lightyServices.getBindingNotificationPublishService(),
                            portMapping),
                    lightyServices.getRpcProviderService())
                .getRegisteredRpc());
    }

    private OpenRoadmInterfaceFactory initOpenRoadmFactory(
            MappingUtils mappingUtils, OpenRoadmInterfaces openRoadmInterfaces, PortMapping portMapping) {
        return new OpenRoadmInterfaceFactory(mappingUtils, portMapping, openRoadmInterfaces);
    }

    private PortMapping initPortMapping(LightyServices lightyServices) {
        PortMappingVersion710 portMappingVersion710 =
            new PortMappingVersion710(lightyServices.getBindingDataBroker(), deviceTransactionManager);
        PortMappingVersion221 portMappingVersion221 =
            new PortMappingVersion221(lightyServices.getBindingDataBroker(), deviceTransactionManager);
        PortMappingVersion121 portMappingVersion121 =
            new PortMappingVersion121(lightyServices.getBindingDataBroker(), deviceTransactionManager);
        return new PortMappingImpl(
            lightyServices.getBindingDataBroker(), portMappingVersion710, portMappingVersion221, portMappingVersion121);
    }

    private OpenRoadmInterfaces initOpenRoadmInterfaces(MappingUtils mappingUtils, PortMapping portMapping) {
        OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121 =
            new OpenRoadmInterfacesImpl121(deviceTransactionManager);
        OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221 =
            new OpenRoadmInterfacesImpl221(deviceTransactionManager, portMapping);
        OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710 =
            new OpenRoadmInterfacesImpl710(deviceTransactionManager, portMapping);
        return new OpenRoadmInterfacesImpl(
            deviceTransactionManager, mappingUtils,
            openRoadmInterfacesImpl121, openRoadmInterfacesImpl221, openRoadmInterfacesImpl710);

    }

    private CrossConnect initCrossConnect(MappingUtils mappingUtils) {
        CrossConnectImpl121 crossConnectImpl121 = new CrossConnectImpl121(deviceTransactionManager);
        CrossConnectImpl221 crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        CrossConnectImpl710 crossConnectImpl710 = new CrossConnectImpl710(deviceTransactionManager);
        return new CrossConnectImpl(
            deviceTransactionManager, mappingUtils,
            crossConnectImpl121, crossConnectImpl221, crossConnectImpl710);
    }
}
