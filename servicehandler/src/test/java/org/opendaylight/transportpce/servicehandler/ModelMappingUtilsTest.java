/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.transportpce.pce.service.PathComputationService;
import org.opendaylight.transportpce.pce.service.PathComputationServiceImpl;
import org.opendaylight.transportpce.pce.utils.NotificationPublishServiceMock;
import org.opendaylight.transportpce.servicehandler.service.PCEServiceWrapper;
import org.opendaylight.transportpce.servicehandler.utils.ServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.constraints.co.routing.or.general.CoRoutingBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraintsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceReconfigureInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;


public class ModelMappingUtilsTest extends AbstractTest {

    private PathComputationRequestOutput pathComputationRequestOutput;
    private ServiceReconfigureInput serviceReconfigureInput;
    private PCEServiceWrapper pceServiceWrapper;

    public ModelMappingUtilsTest() {
        NotificationPublishService notificationPublishService = new NotificationPublishServiceMock();
        PathComputationService pathComputationService = new PathComputationServiceImpl(getDataBroker(),
            notificationPublishService);
        pceServiceWrapper = new PCEServiceWrapper(pathComputationService, notificationPublishService);
        this.pathComputationRequestOutput = pceServiceWrapper.performPCE(ServiceDataUtils.buildServiceCreateInput(),
            true);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
        OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime offsetDateTime2 = offsetDateTime.plusDays(10);
        this.serviceReconfigureInput = new ServiceReconfigureInputBuilder().setNewServiceName("service 1")
        .setServiceName("service 1").setCommonId("common id").setConnectionType(ConnectionType.Service)
        .setCustomer("customer").setCustomerContact("customer contact").setDueDate(new DateAndTime(
            dtf.format(offsetDateTime)))
        .setEndDate(new DateAndTime(dtf.format(offsetDateTime2)))
        .setNcCode("nc node").setNciCode("nci node").setSecondaryNciCode("secondry").setOperatorContact("operator")
        .setServiceAEnd(ServiceDataUtils.getServiceAEndBuildReconfigure().build())
        .setServiceZEnd(ServiceDataUtils.getServiceZEndBuildReconfigure().build())
        .setHardConstraints(new HardConstraintsBuilder()
            .setCoRoutingOrGeneral(new CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                    .constrains.rev161014.constraints.co.routing.or.general.co.routing
                    .CoRoutingBuilder().setExistingService(
                    Arrays.asList("Some existing-service")).build())
                .build())
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .build())
        .setSoftConstraints(new SoftConstraintsBuilder()
            .setCoRoutingOrGeneral(new CoRoutingBuilder()
                .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                    .constrains.rev161014.constraints.co.routing.or.general.co.routing
                    .CoRoutingBuilder().setExistingService(
                    Arrays.asList("Some existing-service")).build())
                .build())
            .setCustomerCode(Arrays.asList("Some customer-code"))
            .build())
        .build();

    }

    @Test
    public void mappingServicesNullServiceCreateInput() {
        Services services = ModelMappingUtils.mappingServices(null, null);
        Assert.assertEquals(new ServicesBuilder().build(), services);
    }

    @Test
    public void mappingServiceNotNullServiceReconfigureInput() {
        Services services = ModelMappingUtils.mappingServices(null, serviceReconfigureInput);
        Assert.assertEquals("service 1", services.getServiceName());
    }

    @Test
    public void mappingServiceValid() {
        Services services = ModelMappingUtils.mappingServices(ServiceDataUtils.buildServiceCreateInput(),
            serviceReconfigureInput);
        Assert.assertEquals("service 1", services.getServiceName());
    }

    @Test
    public void mappingServicesPathNullServiceCreateInput() {
        ServicePaths services = ModelMappingUtils.mappingServicePaths(null, this.pathComputationRequestOutput);
        Assert.assertEquals(new ServicePathsBuilder().build(), services);
    }

    /*@Test
    public void mappingServicePathsValid() {
        ServiceCreateInput input = new ServiceCreateInputBuilder(ServiceDataUtils.buildServiceCreateInput())
            .setHardConstraints(new HardConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                        .constrains.rev161014.constraints.co.routing.or.general.co.routing
                        .CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).setSoftConstraints(new SoftConstraintsBuilder()
                .setCoRoutingOrGeneral(new CoRoutingBuilder()
                    .setCoRouting(new org.opendaylight.yang.gen.v1.http.org.openroadm.routing
                        .constrains.rev161014.constraints.co.routing.or.general.co.routing
                        .CoRoutingBuilder().setExistingService(
                        Arrays.asList("Some existing-service")).build())
                    .build())
                .setCustomerCode(Arrays.asList("Some customer-code"))
                .build()).build();
        ServicePaths servicePaths = ModelMappingUtils.mappingServicePaths(input, serviceReconfigureInput,
            this.pathComputationRequestOutput);
        Assert.assertEquals("service 1", servicePaths.getServicePathName());
    }*/

}
