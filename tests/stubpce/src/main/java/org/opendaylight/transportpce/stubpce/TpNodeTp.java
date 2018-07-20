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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPoint;

/**
 * Class to create structure
 * TerminationPoint
 * Node
 * TerminationPoint.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class TpNodeTp {
    private TerminationPoint tpOut;
    private TerminationPoint tpIn;
    private Node node;
    private List<Resource> resources;
    private List<AToZ> atoz;
    private List<ZToA> ztoa;
    private List<String> ids;

    /**
     * TpNodeTp Constructor.
     *
     * @param in TerminationPoint input
     * @param out TerminationPoint output
     * @param node Node Id
     */
    public TpNodeTp(TerminationPoint in, TerminationPoint out, Node node) {
        this.tpOut = out;
        this.tpIn = in;
        this.node = node;
        this.resources = new ArrayList<Resource>();
        this.atoz = new ArrayList<AToZ>();
        this.ztoa = new ArrayList<ZToA>();
        this.ids = new ArrayList<String>();

    }

    /**
     * create resource List.
     */
    public void createListResource() {
        this.ids.clear();
        this.resources.clear();
        this.atoz.clear();
        this.ztoa.clear();

        this.resources.add(new ResourceBuilder().setResource(this.tpIn).build());
        this.ids.add(this.tpIn.getTerminationPointIdentifier().getNodeId().concat("-")
                .concat(this.tpIn.getTerminationPointIdentifier().getTpId()));
        this.resources.add(new ResourceBuilder().setResource(this.node).build());
        this.ids.add(this.node.getNodeIdentifier().getNodeId());
        this.resources.add(new ResourceBuilder().setResource(this.tpOut).build());
        this.ids.add(this.tpOut.getTerminationPointIdentifier().getNodeId().concat("-")
                .concat(this.tpOut.getTerminationPointIdentifier().getTpId()));
    }

    /**
     * Create an hop in AtoZList.
     * @param odr hop number
     */
    public void createAToZListHop(int odr) {
        AToZ hop = null;
        AToZKey atozKey = null;
        createListResource();
        for (Resource resource : this.resources) {
            atozKey = new AToZKey(Integer.toString(odr));
            resource = new ResourceBuilder().setResource(resource.getResource()).build();
            hop = new AToZBuilder()
                    .withKey(atozKey)
                    .setResource(resource)
                    .build();
            this.atoz.add(hop);
            odr++;
        }
    }

    /**
     * Create an hop in ZtoAList.
     * @param odr hop number
     */
    public void createZToAListHop(int odr) {
        ZToA hop = null;
        ZToAKey ztoaKey = null;
        createListResource();
        for (Resource resource : this.resources) {
            ztoaKey = new ZToAKey(Integer.toString(odr));
            resource = new ResourceBuilder().setResource(resource.getResource()).build();
            hop = new ZToABuilder()
                    .withKey(ztoaKey)
                    .setResource(resource)
                    .build();
            this.ztoa.add(hop);
            odr++;
        }
    }

    public TpNodeTp reverse() {
        return new TpNodeTp(this.tpOut, this.tpIn, this.node);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[ ");
        result.append("tpIn : " + this.tpIn.getTerminationPointIdentifier().getTpId());
        result.append(" - Node : " + this.node.getNodeIdentifier().getNodeId());
        result.append(" - tpOut : " + this.tpOut.getTerminationPointIdentifier().getTpId());
        result.append(" ]");
        return result.toString();

    }

    public List<AToZ> getAToZ() {
        return this.atoz;
    }

    public List<ZToA> getZToA() {
        return this.ztoa;
    }

    public Node getNode() {
        return this.node;
    }

    public TerminationPoint getTpIn() {
        return this.tpIn;
    }

    public TerminationPoint getTpOut() {
        return this.tpOut;
    }
}
