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
        resources = new ArrayList<Resource>();
        atoz = new ArrayList<AToZ>();
        ztoa = new ArrayList<ZToA>();
        ids = new ArrayList<String>();

    }

    /**
     * create resource List.
     */
    public void createListResource() {
        ids.clear();
        resources.clear();
        atoz.clear();
        ztoa.clear();

        resources.add(new ResourceBuilder().setResource(tpIn).build());
        ids.add(tpIn.getTerminationPointIdentifier().getNodeId().concat("-")
                .concat(tpIn.getTerminationPointIdentifier().getTpId()));
        resources.add(new ResourceBuilder().setResource(node).build());
        ids.add(node.getNodeIdentifier().getNodeId());
        resources.add(new ResourceBuilder().setResource(tpOut).build());
        ids.add(tpOut.getTerminationPointIdentifier().getNodeId().concat("-")
                .concat(tpOut.getTerminationPointIdentifier().getTpId()));
    }

    /**
     * Create an hop in AtoZList.
     * @param odr hop number
     */
    public void createAToZListHop(int odr) {
        AToZ hop = null;
        AToZKey atozKey = null;
        createListResource();
        for (Resource resource : resources) {
            atozKey = new AToZKey(Integer.toString(odr));
            resource = new ResourceBuilder().setResource(resource.getResource()).build();
            hop = new AToZBuilder()
                    .setKey(atozKey)
                    .setResource(resource)
                    .build();
            atoz.add(hop);
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
        for (Resource resource : resources) {
            ztoaKey = new ZToAKey(Integer.toString(odr));
            resource = new ResourceBuilder().setResource(resource.getResource()).build();
            hop = new ZToABuilder()
                    .setKey(ztoaKey)
                    .setResource(resource)
                    .build();
            ztoa.add(hop);
            odr++;
        }
    }

    public TpNodeTp reverse() {
        return new TpNodeTp(tpOut, tpIn, node);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("[ ");
        result.append("tpIn : " + tpIn.getTerminationPointIdentifier().getTpId());
        result.append(" - Node : " + node.getNodeIdentifier().getNodeId());
        result.append(" - tpOut : " + tpOut.getTerminationPointIdentifier().getTpId());
        result.append(" ]");
        return result.toString();

    }

    public List<AToZ> getAToZ() {
        return atoz;
    }

    public List<ZToA> getZToA() {
        return ztoa;
    }

    public Node getNode() {
        return node;
    }

    public TerminationPoint getTpIn() {
        return tpIn;
    }

    public TerminationPoint getTpOut() {
        return tpOut;
    }
}
