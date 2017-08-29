/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce;

import java.util.ArrayList;
import java.util.List;

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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev170426.response.parameters.sp.response.parameters.PathDescriptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for Sending
 * PCE requests :
 * - path-computation-request
 * - cancel-resource-reserve.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class SendingPceRPCs {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(SendingPceRPCs.class);
    /* define procedure success (or not ). */
    private Boolean success;
    /* define type of request<br>
     * <code>true</code> pathcomputation <br>
     * <code>false</code> cancelresourcereserve .*/
    private AToZDirection atozdirection;
    private ZToADirection ztoadirection;
    private PathDescriptionBuilder pathDescription;


    public SendingPceRPCs() {
        success = true;
        atozdirection = null;
        ztoadirection = null;
        setPathDescription(null);
    }

    public void cancelResourceReserve() {
        LOG.info("Wait for 10s til beginning the PCE cancelResourceReserve request");
        try {
            Thread.sleep(10000); //sleep for 10s
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("cancelResourceReserve ...");
    }

    public void pathComputation() {
        LOG.info("Wait for 10s til beginning the PCE pathComputation request");
        try {
            Thread.sleep(10000); //sleep for 10s
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("PathComputation ...");
        buildAToZ();
        buildZToA();

        setPathDescription(new PathDescriptionBuilder()
            .setAToZDirection(atozdirection)
            .setZToADirection(ztoadirection));
    }

    public void buildAToZ() {
        Link atoB = new LinkBuilder()
            .setLinkId("AtoB")
            .build();
        Link btoZ = new LinkBuilder()
            .setLinkId("BtoZ")
            .build();

        Node roadmA = new NodeBuilder()
            .setNodeId("RoadmA")
            .build();
        Node roadmB = new NodeBuilder()
            .setNodeId("RoadmB")
            .build();
        Node roadmZ = new NodeBuilder()
            .setNodeId("RoadmZ")
            .build();

        /*A -> Z*/
        /*RoadmA*/
        AToZKey roadmAKey = new AToZKey("RoadmA");
        Resource roadmAResource = new ResourceBuilder()
            .setResource(roadmA)
            .build();
        AToZ atoz = new AToZBuilder()
            .setId("RoadmA")
            .setKey(roadmAKey)
            .setResource(roadmAResource)
            .build();
        /*Link AtoB*/
        AToZKey atoBKey = new AToZKey("AtoB");
        Resource atozResource = new ResourceBuilder()
            .setResource(atoB)
            .build();
        AToZ atob = new AToZBuilder()
            .setId("AtoB")
            .setKey(atoBKey)
            .setResource(atozResource)
            .build();
        /*RoadmB*/
        AToZKey roadmBKey = new AToZKey("RoadmB");
        Resource roadmBResource = new ResourceBuilder()
            .setResource(roadmB)
            .build();
        AToZ roadmb = new AToZBuilder()
            .setId("RoadmB")
            .setKey(roadmBKey)
            .setResource(roadmBResource)
            .build();
        /*Link BtoZ*/
        AToZKey btoZKey = new AToZKey("BtoZ");
        Resource botzResource = new ResourceBuilder()
            .setResource(btoZ)
            .build();
        AToZ btoz = new AToZBuilder()
            .setId("BtoZ")
            .setKey(btoZKey)
            .setResource(botzResource)
            .build();
        /*RoadmZ*/
        AToZKey roadmZKey = new AToZKey("RoadmZ");
        Resource roadmZResource = new ResourceBuilder()
            .setResource(roadmZ)
            .build();
        AToZ roadmz = new AToZBuilder()
            .setId("RoadmZ")
            .setKey(roadmZKey)
            .setResource(roadmZResource)
            .build();

        List<AToZ> atozList = new ArrayList<AToZ>();
        atozList.add(atoz);
        atozList.add(atob);
        atozList.add(roadmb);
        atozList.add(btoz);
        atozList.add(roadmz);

        atozdirection = new AToZDirectionBuilder()
            .setRate((long)100)
            .setAToZWavelengthNumber((long)200)
            .setAToZ(atozList)
            .build();
    }

    public void buildZToA() {

        Link btoA = new LinkBuilder()
            .setLinkId("BtoA")
            .build();
        Link ztoB = new LinkBuilder()
            .setLinkId("ZtoB")
            .build();

        Node roadmA = new NodeBuilder()
            .setNodeId("RoadmA")
            .build();
        Node roadmB = new NodeBuilder()
            .setNodeId("RoadmB")
            .build();
        Node roadmZ = new NodeBuilder()
            .setNodeId("RoadmZ")
            .build();

        /*Z -> A*/
        /*RoadmZ*/
        ZToAKey roadmZKey = new ZToAKey("RoadmZ");
        Resource roadmZResource = new ResourceBuilder()
            .setResource(roadmZ)
            .build();
        ZToA ztoa = new ZToABuilder()
            .setId("RoadmZ")
            .setKey(roadmZKey)
            .setResource(roadmZResource)
            .build();
        /*Link ZtoB*/
        ZToAKey ztoBKey = new ZToAKey("ZtoB");
        Resource ztoBResource = new ResourceBuilder()
            .setResource(ztoB)
            .build();
        ZToA ztob = new ZToABuilder()
            .setId("ZtoB")
            .setKey(ztoBKey)
            .setResource(ztoBResource)
            .build();
        /*RoadmB*/
        ZToAKey roadmBKey = new ZToAKey("RoadmB");
        Resource roadmBResource = new ResourceBuilder()
            .setResource(roadmB)
            .build();
        ZToA roadmb = new ZToABuilder()
            .setId("RoadmB")
            .setKey(roadmBKey)
            .setResource(roadmBResource)
            .build();
        /*Link BtoA*/
        ZToAKey btoAKey = new ZToAKey("BtoA");
        Resource btoAResource = new ResourceBuilder()
            .setResource(btoA)
            .build();
        ZToA btoa = new ZToABuilder()
            .setId("BtoA")
            .setKey(btoAKey)
            .setResource(btoAResource)
            .build();
        /* RoadmA*/
        ZToAKey roadmAKey = new ZToAKey("RoadmA");
        Resource roadmAResource = new ResourceBuilder()
            .setResource(roadmA)
            .build();
        ZToA roadma = new ZToABuilder()
            .setId("RoadmA")
            .setKey(roadmAKey)
            .setResource(roadmAResource)
            .build();

        List<ZToA> ztoaList = new ArrayList<ZToA>();
        ztoaList.add(ztoa);
        ztoaList.add(ztob);
        ztoaList.add(roadmb);
        ztoaList.add(btoa);
        ztoaList.add(roadma);

        ztoadirection = new ZToADirectionBuilder()
            .setRate((long)100)
            .setZToAWavelengthNumber((long)100)
            .setZToA(ztoaList)
            .build();
    }

    public PathDescriptionBuilder getPathDescription() {
        return pathDescription;
    }

    public void setPathDescription(PathDescriptionBuilder pathDescription) {
        this.pathDescription = pathDescription;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

}
