/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcePathDescription {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    private List<PceLink> pathAtoZ = null;
    private PceResult rc;

    public PceResult getReturnStructure() {
        return this.rc;
    }

    private Map<LinkId, PceLink> allPceLinks = null;

    private List<PceLink> pathZtoA = null;

    public PcePathDescription(List<PceLink> pathAtoZ, Map<LinkId, PceLink> allPceLinks, PceResult rc) {
        super();
        this.allPceLinks = allPceLinks;
        this.pathAtoZ = pathAtoZ;
        this.rc = rc;
    }

    public PceResult buildDescriptions() {
        LOG.info("In buildDescriptions: AtoZ {}", this.pathAtoZ.toString());
        List<AToZ> atozList = new ArrayList<AToZ>();
        if (this.pathAtoZ == null) {
            this.rc.setRC(ResponseCodes.RESPONSE_FAILED);
            LOG.error("In buildDescriptions: there is empty AtoZ path");
            return this.rc;
        }

        buildAtoZ(atozList, this.pathAtoZ);

        this.rc.setAtoZDirection(new AToZDirectionBuilder().setRate(this.rc.getRate())
                .setAToZWavelengthNumber(this.rc.getResultWavelength()).setAToZ(atozList).build());

        this.pathZtoA = ImmutableList.copyOf(this.pathAtoZ).reverse();
        LOG.info("In buildDescriptions: ZtoA {}", this.pathZtoA.toString());

        List<ZToA> ztoaList = new ArrayList<ZToA>();
        if (this.pathZtoA == null) {
            this.rc.setRC(ResponseCodes.RESPONSE_FAILED);
            LOG.error("In buildDescriptions: there is empty ZtoA path");
            return this.rc;
        }
        buildZtoA(ztoaList, this.pathZtoA);

        this.rc.setZtoADirection(new ZToADirectionBuilder().setRate(this.rc.getRate())
                .setZToAWavelengthNumber(this.rc.getResultWavelength()).setZToA(ztoaList).build());

        return this.rc;
    }

    private void buildAtoZ(List<AToZ> etoeList, List<PceLink> path) {

        Integer index = 0;
        PceLink lastLink = null;
        AToZ lastResource = null;

        // build A side Client TP
        String tpName = path.get(0).getClient();
        String xname = path.get(0).getSourceId().getValue();
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpId(tpName).setTpNodeId(xname)
                .build();

        AToZKey clientKey = new AToZKey(index.toString());
        Resource clientResource = new ResourceBuilder().setResource(stp).build();
        AToZ firstResource = new AToZBuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        etoeList.add(firstResource);

        index++;

        for (PceLink pcelink : path) {

            String srcName = pcelink.getSourceId().getValue();

            // Nodes
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Node sourceNode = new NodeBuilder()
                .setNodeId(srcName)
                .build();

            // Source Resource
            AToZKey sourceKey = new AToZKey(index.toString());
            Resource nodeResource1 = new ResourceBuilder().setResource(sourceNode).build();
            AToZ srcResource = new AToZBuilder().setId(srcName).withKey(sourceKey).setResource(nodeResource1).build();
            index++;
            etoeList.add(srcResource);

            // source TP
            tpName = pcelink.getSourceTP().toString();
            stp = new TerminationPointBuilder()
                    .setTpNodeId(srcName).setTpId(tpName)
                    .build();

            // Resource
            AToZKey srcTPKey = new AToZKey(index.toString());// tpName);
            Resource tpResource1 = new ResourceBuilder().setResource(stp).build();
            AToZ stpResource = new AToZBuilder().setId(tpName).withKey(srcTPKey).setResource(tpResource1).build();
            index++;
            etoeList.add(stpResource);

            String linkName = pcelink.getLinkId().getValue();
            // Link
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Link atozLink = new LinkBuilder()
                .setLinkId(linkName)
                .build();

            // Link Resource
            AToZKey linkKey = new AToZKey(index.toString());
            Resource nodeResource2 = new ResourceBuilder().setResource(atozLink).build();
            AToZ linkResource = new AToZBuilder().setId(linkName).withKey(linkKey).setResource(nodeResource2).build();
            index++;
            etoeList.add(linkResource);

            String destName = pcelink.getDestId().getValue();
            // target TP
            tpName = pcelink.getDestTP().toString();
            TerminationPoint dtp = new TerminationPointBuilder()
                    .setTpNodeId(destName).setTpId(tpName)
                    .build();

            // Resource
            AToZKey destTPKey = new AToZKey(index.toString());
            Resource tpResource2 = new ResourceBuilder().setResource(dtp).build();
            AToZ ttpResource = new AToZBuilder().setId(tpName).withKey(destTPKey).setResource(tpResource2).build();
            index++;
            etoeList.add(ttpResource);

            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Node targetNode = new NodeBuilder()
                .setNodeId(destName)
                .build();

            // Target Resource
            AToZKey targetKey = new AToZKey(index.toString());
            Resource nodeResource3 = new ResourceBuilder().setResource(targetNode).build();
            lastResource = new AToZBuilder().setId(destName).withKey(targetKey).setResource(nodeResource3).build();

            lastLink = pcelink;
        }

        etoeList.add(lastResource);

        // build Z side Client TP
        tpName = lastLink.getClient();
        xname = lastLink.getDestId().getValue();
        stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName)
                .build();


        index++;
        clientKey = new AToZKey(index.toString());
        clientResource = new ResourceBuilder().setResource(stp).build();
        lastResource = new AToZBuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        etoeList.add(lastResource);

    }

    private void buildZtoA(List<ZToA> etoelist, List<PceLink> path) {
        Integer index = 0;
        PceLink lastLink = null;
        ZToA lastResource = null;

        // build Z size Client TP
        PceLink pcelink = this.allPceLinks.get(path.get(0).getOppositeLink());
        String tpName = pcelink.getClient();
        String xname = pcelink.getSourceId().getValue();
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName)
                .build();

        ZToAKey clientKey = new ZToAKey(index.toString());
        Resource clientResource = new ResourceBuilder().setResource(stp).build();
        ZToA firstResource = new ZToABuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        etoelist.add(firstResource);
        index++;

        for (PceLink pcelinkAtoZ : path) {

            pcelink = this.allPceLinks.get(pcelinkAtoZ.getOppositeLink());
            LOG.debug("link to oppsite: {} to {}", pcelinkAtoZ.toString(), pcelink.toString());

            String srcName = pcelink.getSourceId().getValue();


            // Nodes
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Node sourceNode = new NodeBuilder()
                .setNodeId(srcName).build();


            // Source Resource
            ZToAKey sourceKey = new ZToAKey(index.toString());
            Resource nodeResource1 = new ResourceBuilder().setResource(sourceNode).build();
            ZToA srcResource = new ZToABuilder().setId(srcName).withKey(sourceKey).setResource(nodeResource1).build();
            index++;
            etoelist.add(srcResource);

            // source TP
            tpName = pcelink.getSourceTP().toString();
            stp = new TerminationPointBuilder()
                    .setTpNodeId(srcName).setTpId(tpName)
                    .build();

            // Resource
            ZToAKey srcTPKey = new ZToAKey(index.toString());
            Resource tpResource1 = new ResourceBuilder().setResource(stp).build();
            ZToA stpResource = new ZToABuilder().setId(tpName).withKey(srcTPKey).setResource(tpResource1).build();
            index++;
            etoelist.add(stpResource);

            String linkName = pcelink.getLinkId().getValue();
            // Link
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Link ztoaLink = new LinkBuilder()
                .setLinkId(linkName).build();

            // Link Resource
            ZToAKey linkKey = new ZToAKey(index.toString());
            Resource nodeResource2 = new ResourceBuilder().setResource(ztoaLink).build();
            ZToA linkResource = new ZToABuilder().setId(linkName).withKey(linkKey).setResource(nodeResource2).build();
            index++;
            etoelist.add(linkResource);

            String destName = pcelink.getDestId().getValue();
            // target TP
            tpName = pcelink.getDestTP().toString();
            TerminationPoint ttp = new TerminationPointBuilder()
                    .setTpNodeId(destName).setTpId(tpName).build();

            // Resource
            ZToAKey destTPKey = new ZToAKey(index.toString());
            Resource tpResource2 = new ResourceBuilder().setResource(ttp).build();
            ZToA ttpResource = new ZToABuilder().setId(tpName).withKey(destTPKey).setResource(tpResource2).build();
            index++;
            etoelist.add(ttpResource);


            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce
                .resource.resource.resource.Node targetNode = new NodeBuilder()
                .setNodeId(destName).build();
            // Target Resource
            ZToAKey targetKey = new ZToAKey(index.toString());
            Resource nodeResource3 = new ResourceBuilder().setResource(targetNode).build();
            lastResource = new ZToABuilder().setId(destName).withKey(targetKey).setResource(nodeResource3).build();

            lastLink = pcelink;
        }
        etoelist.add(lastResource);

        // build Z side Client TP
        tpName = lastLink.getClient();
        xname = lastLink.getDestId().getValue();
        stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName).build();


        index++;
        clientKey = new ZToAKey(index.toString());
        clientResource = new ResourceBuilder().setResource(stp).build();
        lastResource = new ZToABuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        etoelist.add(lastResource);

    }

}
