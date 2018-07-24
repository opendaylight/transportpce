/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Executors;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.MountPoint;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthService;
import org.opendaylight.transportpce.renderer.NetworkModelWavelengthServiceImpl;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.transportpce.renderer.stub.OlmServiceStub;
import org.opendaylight.transportpce.renderer.utils.MountPointUtils;
import org.opendaylight.transportpce.renderer.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.OlmService;



public class RendererServiceOperationsImplTest extends AbstractTest {

    private static final int NUMBER_OF_THREADS = 4;
    private MountPointService mountPointService;
    private DeviceTransactionManager deviceTransactionManager;
    private RendererServiceOperationsImpl rendererServiceOperations;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private DeviceRendererService deviceRenderer;
    private PortMapping portMapping;
    private OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private CrossConnect crossConnect;
    private OlmService olmService;
    private ListeningExecutorService executor;
    private NetworkModelWavelengthService networkModelWavelengthService;

    private void setMountPoint(MountPoint mountPoint) {
        this.mountPointService = new MountPointServiceStub(mountPoint);
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(this.mountPointService, 3000);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager);
        this.portMapping = new PortMappingImpl(this.getDataBroker(), this.deviceTransactionManager,
            openRoadmInterfaces);
        this.openRoadmInterfaceFactory = new OpenRoadmInterfaceFactory(portMapping,
            openRoadmInterfaces);
        this.crossConnect = new CrossConnectImpl(this.deviceTransactionManager);
        this.deviceRenderer = new DeviceRendererServiceImpl(this.getDataBroker(),
            this.deviceTransactionManager, openRoadmInterfaceFactory, openRoadmInterfaces, crossConnect);
    }

    @Before
    public void setUp() {
        setMountPoint(new MountPointStub(getDataBroker()));
        this.olmService = new OlmServiceStub();
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));
        this.networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(getDataBroker());
        this.rendererServiceOperations =  new RendererServiceOperationsImpl(this.deviceRenderer, this.olmService,
            getDataBroker(), this.networkModelWavelengthService);

    }


    @Test
    public void serviceImplementationTerminationPointAsResourceTtp() {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(OpenRoadmInterfacesImpl.TTP_TOKEN);
        writePortMapping(input, OpenRoadmInterfacesImpl.TTP_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourcePp() {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(OpenRoadmInterfacesImpl.PP_TOKEN);
        writePortMapping(input, OpenRoadmInterfacesImpl.PP_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourceNetwork() {

        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(OpenRoadmInterfacesImpl.NETWORK_TOKEN);
        writePortMapping(input, OpenRoadmInterfacesImpl.NETWORK_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());

    }

    @Test
    public void serviceImplementationTerminationPointAsResourceClient() {
        ServiceImplementationRequestInput input = ServiceDataUtils
            .buildServiceImplementationRequestInputTerminationPointResource(OpenRoadmInterfacesImpl.CLIENT_TOKEN);
        writePortMapping(input, OpenRoadmInterfacesImpl.CLIENT_TOKEN);
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }

    @Test
    public void serviceImplementationTerminationPointAsResourceNoMapping() {
        String[] interfaceTokens = {
            OpenRoadmInterfacesImpl.NETWORK_TOKEN,
            OpenRoadmInterfacesImpl.CLIENT_TOKEN,
            OpenRoadmInterfacesImpl.TTP_TOKEN,
            OpenRoadmInterfacesImpl.PP_TOKEN
        };
        for (String tpToken : interfaceTokens) {
            ServiceImplementationRequestInput input = ServiceDataUtils
                .buildServiceImplementationRequestInputTerminationPointResource(tpToken);
            ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
            Assert.assertEquals(ResponseCodes.RESPONSE_FAILED,
                result.getConfigurationResponseCommon().getResponseCode());
        }
    }

    private void writePortMapping(ServiceImplementationRequestInput input, String tpToken) {
        MountPointUtils.writeMapping(
            input.getServiceAEnd().getNodeId(),
            input.getServiceAEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
        MountPointUtils.writeMapping(
            input.getServiceZEnd().getNodeId(),
            input.getServiceZEnd().getNodeId() + "-" + tpToken,
            this.deviceTransactionManager
        );
    }

    /*@Test
    public void serviceImplementationLinkAsResource() {
        ServiceImplementationRequestInput input = ServiceDataUtils.buildServiceImplementationRequestInputLinkResource();
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_OK, result.getConfigurationResponseCommon().getResponseCode());
    }*/

    /*@Test
    public void serviceImplementtionInvalidResource() {

        ServiceImplementationRequestInput input = ServiceDataUtils
        .buildServiceImplementationRequestInputInvalidResource();
        ServiceImplementationRequestOutput result = this.rendererServiceOperations.serviceImplementation(input);
        Assert.assertEquals(ResponseCodes.RESPONSE_FAILED, result.getConfigurationResponseCommon().getResponseCode());

    }*/

}
