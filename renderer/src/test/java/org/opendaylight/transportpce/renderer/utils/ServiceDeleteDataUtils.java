/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.endpoint.sp.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.endpoint.sp.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.PathDescriptionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev171016.service.path.ServiceZEndBuilder;

public final class ServiceDeleteDataUtils {

    private ServiceDeleteDataUtils() {

    }

    public static PathDescription createTransactionPathDescription(String tpId) {
        List<AToZ> atoZList = new ArrayList<AToZ>();
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        TerminationPoint terminationPoint = terminationPointBuilder.setTpNodeId("node2" + tpId)
                        .setTpId(tpId).build();
        TerminationPoint terminationPoint2 = terminationPointBuilder.setTpNodeId("node1" + tpId)
                        .setTpId(tpId).build();
        AToZ atoZ = new AToZBuilder().setId("1").withKey(new AToZKey("1")).setResource(new ResourceBuilder()
                .setResource(terminationPoint).build()).build();
        AToZ atoZ2 = new AToZBuilder().setId("2").withKey(new AToZKey("2")).setResource(new ResourceBuilder()
                .setResource(terminationPoint2).build()).build();
        atoZList.add(atoZ);
        atoZList.add(atoZ2);
        AToZDirection atozDirection = new AToZDirectionBuilder()
                .setRate(20L)
                .setAToZWavelengthNumber(20L)
                .setAToZ(atoZList)
                .setModulationFormat("OC")
                .build();
        List<ZToA> ztoAList = new ArrayList<ZToA>();
        ZToA ztoA = new ZToABuilder().setId("1").withKey(new ZToAKey("1")).setResource(new ResourceBuilder()
                .setResource(terminationPoint).build()).build();
        ZToA ztoA2 = new ZToABuilder().setId("2").withKey(new ZToAKey("2")).setResource(new ResourceBuilder()
                .setResource(terminationPoint).build()).build();
        ztoAList.add(ztoA);
        ztoAList.add(ztoA2);
        ZToADirection ztoaDirection = new ZToADirectionBuilder()
                .setRate(20L)
                .setZToAWavelengthNumber(20L)
                .setZToA(ztoAList)
                .setModulationFormat("OC")
                .build();
        PathDescriptionBuilder pathDescriptionBuilder = new PathDescriptionBuilder();
        pathDescriptionBuilder.setAToZDirection(atozDirection);
        pathDescriptionBuilder.setZToADirection(ztoaDirection);
        return pathDescriptionBuilder.build();
    }

    public static ServiceAEndBuilder getServiceAEndBuild() {
        return new ServiceAEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-1-2")
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build());
    }

    public static ServiceZEndBuilder getServiceZEndBuild() {
        return new ServiceZEndBuilder()
            .setClli("clli").setServiceFormat(ServiceFormat.OC).setServiceRate((long) 1).setNodeId("XPONDER-2-3")
            .setTxDirection(
                new TxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build())
            .setRxDirection(
                new RxDirectionBuilder()
                    .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                        .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                        .setPortSubSlot("port subslot").setPortType("port type").build())
                    .build());
    }



}
