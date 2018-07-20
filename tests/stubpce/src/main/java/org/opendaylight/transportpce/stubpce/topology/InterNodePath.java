/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.transportpce.stubpce.topology;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.opendaylight.transportpce.stubpce.TpNodeTp;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.link.LinkIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * class to build all path between superNode elements
 * ( XPDR and ROADM).
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */

public class InterNodePath {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(InterNodePath.class);
    private SuperNode superNode;
    private List<NodePath> nodepaths;
    private List<AToZDirection> atoz;
    private List<ZToADirection> ztoa;

    /**
     * InterNodePath constructor.
     *
     * @param supernode Supernode
     */
    public InterNodePath(SuperNode supernode) {
        setSuperNode(supernode);
        setNodepaths(new ArrayList<NodePath>());
        setAtoz(new ArrayList<AToZDirection>());
        setZtoa(new ArrayList<ZToADirection>());
    }

    /**
     * build AToZdirection.
     *
     * @param paths <code>Path</code> List
     */
    private void getAToZDirection(List<Path> paths) {
        int order = 0;
        AToZDirectionBuilder atozDirection = new AToZDirectionBuilder();
        List<AToZ> atozList = new ArrayList<AToZ>();
        TpNodeTp tpNodeTp;
        for (Path path : paths) {
            tpNodeTp = path.getTpNodeTp();
            tpNodeTp.createAToZListHop(order);
            for (AToZ atoz : tpNodeTp.getAToZ()) {
                atozList.add(atoz);
                order++;
            }
            Link link = path.getLink();
            AToZKey atozKey = new AToZKey(Integer.toString(order));
            Resource resource = new ResourceBuilder().setResource(link).build();
            AToZ hop = new AToZBuilder().setId(atozKey.getId()).withKey(atozKey).setResource(resource).build();
            atozList.add(hop);
            order++;
        }
        atozDirection.setRate((long) 100).setAToZWavelengthNumber((long) 200).setAToZ(atozList);
        this.atoz.add(atozDirection.build());
    }

    /**
     * build ZToAdirection.
     *
     * @param paths <code>Path</code> List
     */
    private void getZToADirection(List<Path> paths) {
        int order = 0;
        ZToADirectionBuilder ztoaDirection = new ZToADirectionBuilder();
        List<ZToA> ztoaList = new ArrayList<ZToA>();
        TpNodeTp tpNodeTp;
        for (Path path : paths) {
            tpNodeTp = path.getTpNodeTp();
            tpNodeTp.createZToAListHop(order);
            for (ZToA ztoa : tpNodeTp.getZToA()) {
                ztoaList.add(ztoa);
                order++;
            }
            Link link = path.getLink();
            ZToAKey ztoaKey = new ZToAKey(Integer.toString(order));
            Resource resource = new ResourceBuilder().setResource(link).build();
            ZToA hop = new ZToABuilder().setId(ztoaKey.getId()).withKey(ztoaKey).setResource(resource).build();
            ztoaList.add(hop);
            order++;
        }
        ztoaDirection.setRate((long) 100).setZToAWavelengthNumber((long) 200).setZToA(ztoaList);
        this.ztoa.add(ztoaDirection.build());
    }

    /**
     * Build direction path
     * for all Supernode elements.
     *
     * @param skip Boolean to specify an ROADM without XPDR
     * @param nodeId Supernode element nodeId
     * @param zend Supernode path ZEnd nodeId
     */
    public void build(Boolean skip, String nodeId,String zend) {
        LOG.info("Building direction ...");
        if (skip) {
            buildDegToDeg(false);
            buildDegToDeg(true);
        } else {
            boolean reverse = false;
            if (nodeId.contains(zend)) {
                reverse = true;
            }
            NodePath xpdr = null;
            NodePath srg = null;
            NodePath deg1 = null;
            NodePath deg2 = null;
            for (NodePath node : this.nodepaths) {
                //LOG.info(node.toString());
                String id = node.getNodeId();
                if (id.contains("XPDR")) {
                    xpdr = node;
                }
                if (id.contains("SRG")) {
                    srg = node;
                }
                if (id.contains("DEG1")) {
                    deg1 = node;
                }
                if (id.contains("DEG2")) {
                    deg2 = node;
                }
            }
            buildXpdrToSrgToDeg(xpdr,srg,deg1,deg2,reverse);
            buildDegToSrgToXpdr(xpdr,srg,deg1,deg2,reverse);
        }
    }

    /**
     * build direction path for
     * Supernode element DEG1, DEG2.
     *
     * @param twotoOne boolean to determine direction
     */
    private void buildDegToDeg(boolean twotoOne) {
        LOG.info("buildDegToDeg ...");
        NodePath deg1 = null;
        NodePath deg2 = null;
        for (NodePath node : this.nodepaths) {
            //LOG.info(node.toString());
            String nodeId = node.getNodeId();
            if (nodeId.contains("DEG1")) {
                deg1 = node;
            }
            if (nodeId.contains("DEG2")) {
                deg2 = node;
            }
        }
        if ((deg1 != null) && (deg2 != null)) {
            List<Path> result =  new ArrayList<Path>();
            NodePath deb = deg1;
            NodePath end = deg2;
            if (twotoOne) {
                deb = deg2;
                end = deg1;
            }
            for (Path deg1Path : deb.getPath()) {
                TpNodeTp tmpdeg = deg1Path.getTpNodeTp();
                if (tmpdeg.getTpIn().getTerminationPointIdentifier().getTpId().contains("TTP")) {
                    result.clear();
                    result.addAll(Lists.newArrayList(deg1Path));
                    if (!twotoOne) {
                        getAToZDirection(result);
                    } else {
                        getZToADirection(result);
                    }
                }
            }
            for (Path deg2Path : end.getPath()) {
                TpNodeTp tmpdeg = deg2Path.getTpNodeTp();
                if (tmpdeg.getTpIn().getTerminationPointIdentifier().getTpId().contains("CTP")) {
                    result.clear();
                    result.addAll(Lists.newArrayList(deg2Path));
                    if (!twotoOne) {
                        getAToZDirection(result);
                    } else {
                        getZToADirection(result);
                    }
                }
            }
        } else {
            LOG.info("deg1 or/and deg2 is null !");
        }
    }

    /**
     build direction from
     *Degree  to SRG to XPDR
     *in Supernode elements.
     *
     *@param xpdr NodePath XPDR
     *@param srg NodePath SRG
     *@param deg NodePath Degree
     *@param reverse boolean to determine the direction
     */
    private void buildDeg(NodePath xpdr,NodePath srg,NodePath deg, Boolean reverse) {
        List<Path> result =  new ArrayList<Path>();
        for (Path degPath : deg.getPath()) {
            TpNodeTp tmpdeg = degPath.getTpNodeTp();
            if (tmpdeg.getTpIn().getTerminationPointIdentifier().getTpId().contains("TTP")) {
                for (Path srgPath : srg.getPath()) {
                    TpNodeTp tmpsrg = srgPath.getTpNodeTp();
                    if (tmpsrg.getTpIn().getTerminationPointIdentifier().getTpId().contains("CP")) {
                        for (Path xpdrPath : xpdr.getPath()) {
                            TpNodeTp tmpxpdr = xpdrPath.getTpNodeTp();
                            if (tmpxpdr.getTpIn().getTerminationPointIdentifier().getTpId().contains("NETWORK")) {
                                result.clear();
                                result.addAll(Lists.newArrayList(degPath,srgPath,xpdrPath));
                                if (reverse) {
                                    getAToZDirection(result);
                                } else {
                                    getZToADirection(result);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     *build direction from
     *DEG1 / DEG2  to SRG to XPDR
     *in Supernode elements.
     *
     *@param xpdr NodePath XPDR
     *@param srg NodePath SRG
     *@param deg1 NodePath Degree 1
     *@param deg2 NodePath Degree 2
     *@param reverse boolean to determine the direction
     */
    private void buildDegToSrgToXpdr(NodePath xpdr,NodePath srg,NodePath deg1,NodePath deg2,boolean reverse) {
        LOG.info("buildDegToSrgToXpr ...");
        if ((xpdr != null) && (srg != null) && (deg1 != null) && (deg2 != null)) {
            buildDeg(xpdr, srg, deg1, reverse);
            buildDeg(xpdr, srg, deg2, reverse);
        } else {
            LOG.info("xpdr, deg or/and srg is null !");
        }
    }

    /**
     *build direction from
     *XPDR to SRG to DEG
     *in Supernode elements.
     *
     *@param xpdr NodePath XPDR
     *@param srg NodePath SRG
     *@param deg1 NodePath Degree 1
     *@param deg2 NodePath Degree 2
     *@param reverse boolean to determine the direction
     */
    private void buildXpdrToSrgToDeg(NodePath xpdr,NodePath srg,NodePath deg1,NodePath deg2,boolean reverse) {
        LOG.info("buildXpdrToSrgToDeg ...");
        if ((xpdr != null) && (srg != null) && (deg1 != null) && (deg2 != null)) {
            List<Path> result =  new ArrayList<Path>();
            for (Path xpdrPath : xpdr.getPath()) {
                TpNodeTp tmpxpdr = xpdrPath.getTpNodeTp();
                if (tmpxpdr.getTpIn().getTerminationPointIdentifier().getTpId().contains("CLIENT")) {
                    for (Path srgPath : srg.getPath()) {
                        TpNodeTp tmp = srgPath.getTpNodeTp();
                        Link srglink = srgPath.getLink();
                        if (tmp.getTpIn().getTerminationPointIdentifier().getTpId().contains("PP")) {
                            for (Path deg1Path : deg1.getPath()) {
                                TpNodeTp tmpdeg = deg1Path.getTpNodeTp();
                                if (tmpdeg.getTpIn().getTerminationPointIdentifier().getTpId().contains("CTP")
                                        && srglink.getLinkIdentifier().getLinkId().contains("DEG1")) {
                                    result.clear();
                                    result.addAll(Lists.newArrayList(xpdrPath,srgPath,deg1Path));
                                    if (reverse) {
                                        getZToADirection(result);
                                    } else {
                                        getAToZDirection(result);
                                    }
                                }
                            }
                            for (Path deg2Path : deg2.getPath()) {
                                TpNodeTp tmpdeg = deg2Path.getTpNodeTp();
                                if (tmpdeg.getTpIn().getTerminationPointIdentifier().getTpId().contains("CTP")
                                        && srglink.getLinkIdentifier().getLinkId().contains("DEG2")) {
                                    result.clear();
                                    result.addAll(Lists.newArrayList(xpdrPath,srgPath,deg2Path));
                                    if (reverse) {
                                        getZToADirection(result);
                                    } else {
                                        getAToZDirection(result);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LOG.info("xpdr, deg or/and srg is null !");
        }
    }

    /**
     * get all AToZdirection containing
     * a list of <code>AToZ</code> end by
     * a String.
     *
     * @param endBy String
     * @param atozDirection AToZdirection List
     * @param index Integer to determine if it for last Link(1) or last TerminationPoint(2)
     * @return <code>AToZDirection</code> List
     */
    public List<AToZDirection> getAToZDirectionEndBy(String endBy, List<AToZDirection> atozDirection, int index) {
        List<AToZDirection> result = new ArrayList<AToZDirection>();
        for (AToZDirection tmp : atozDirection) {
            List<AToZ> atozList = tmp.getAToZ();
            int size = atozList.size();
            if (size > 2) {
                String id = Integer.toString(size - index);
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription
                    .rev170426.pce.resource.resource.Resource res = null;
                for (AToZ atoz : atozList) {
                    if (atoz.getId().compareTo(id) == 0) {
                        res = atoz.getResource().getResource();
                        if (res != null) {

                            switch (index) {
                                case 1: //last link
                                    if (res instanceof Link) {
                                        Link tp = (Link) res;
                                        if ((tp != null) && tp.getLinkIdentifier().getLinkId().contains(endBy)) {
                                            result.add(tmp);
                                        }
                                    }
                                    break;

                                case 2: //last tp
                                    if (res instanceof TerminationPoint) {
                                        TerminationPoint tp = (TerminationPoint) res;
                                        if ((tp != null) && tp.getTerminationPointIdentifier().getTpId()
                                                .contains(endBy)) {
                                            result.add(tmp);
                                        }
                                    }
                                    break;

                                default :
                                    break;
                            }
                        }
                    }
                }
            } else {
                LOG.info("firstnodeTpId is null !");
            }
        }
        LOG.info("getAToZDirectionEndBy result size : {}\n{}", result.size(),result.toString());
        return result;
    }

    /**
     * replace or remove in
     * Link in <code>AToZ</code> list
     * containing in AToZdirection.
     *
     * @param endBy String
     * @param beginBy String
     * @param atozLink name of ROAMDTOROAMD link
     * @param atozDirection AToZdirection List
     * @param remove boolean to determine if removing or not
     * @return <code>AToZDirection</code> List
     */
    public List<AToZDirection> replaceOrRemoveAToZDirectionEndLink(String endBy,String beginBy,String atozLink,
            List<AToZDirection> atozDirection, boolean remove) {
        List<AToZDirection> result = new ArrayList<AToZDirection>();
        List<AToZDirection> tmp = new ArrayList<AToZDirection>();
        if (remove) {
            tmp = getAToZDirectionEndBy(endBy, atozDirection, 1);
            if (!tmp.isEmpty()) {
                tmp = getAToZDirectionBeginBy(beginBy, tmp);
            }
        } else {
            tmp = getAToZDirectionEndBy(endBy, atozDirection,2);
        }
        if (!tmp.isEmpty()) {
            for (AToZDirection atozdir : tmp) {
                List<AToZ> atozList = atozdir.getAToZ();
                int size = atozList.size();
                if (size > 0) {
                    String id = Integer.toString(size - 1);
                    for (ListIterator<AToZ> it = atozList.listIterator(); it.hasNext();) {
                        AToZ atoz = it.next();
                        if (atoz.getId().compareTo(id) == 0) {
                            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription
                                .rev170426.pce.resource.resource.Resource res = atoz.getResource().getResource();
                            if ((res != null)  && (res instanceof Link)) {
                                Link link = new LinkBuilder()
                                        .setLinkIdentifier(new LinkIdentifierBuilder()
                                                .setLinkId(atozLink)
                                                .build())
                                        .build();
                                AToZKey atozKey = new AToZKey(atoz.key());
                                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                                    .pathdescription.rev170426.pce.resource.Resource resource = new ResourceBuilder()
                                    .setResource(link).build();
                                AToZ hop = new AToZBuilder().setId(atozKey.getId()).withKey(atozKey)
                                        .setResource(resource).build();
                                it.remove();
                                if (!remove) {
                                    it.add(hop);
                                }
                                result.add(atozdir);
                            }
                        }
                    }
                }
            }
        } else {
            LOG.info("getAToZDirectionEndBy size : {}", tmp.size());
        }
        return result;
    }

    /**
     *get all AToZdirection containing
     * a list of <code>AToZ</code> begin by
     * a String.
     *
     * @param beginBy String
     * @param atozDirection AToZdirection List
     * @return <code>AToZDirection</code> List
     */
    public List<AToZDirection> getAToZDirectionBeginBy(String beginBy, List<AToZDirection> atozDirection) {
        List<AToZDirection> result = new ArrayList<AToZDirection>();
        for (AToZDirection tmp : atozDirection) {
            List<AToZ> atozList = tmp.getAToZ();
            int size = atozList.size();
            if (size > 0) {
                String id = Integer.toString(0);
                for (AToZ atoz : atozList) {
                    if (atoz.getId().compareTo(id) == 0) {
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription
                            .rev170426.pce.resource.resource.Resource res = atoz.getResource().getResource();
                        if ((res != null)  && (res instanceof TerminationPoint)) {
                            TerminationPoint tp = (TerminationPoint) res;
                            if ((tp != null) && tp.getTerminationPointIdentifier().getTpId().contains(beginBy)) {
                                LOG.info("tmp : {}", tmp.toString());
                                result.add(tmp);
                            }
                        }
                    }
                }
            }
        }
        LOG.info("result size : {}", result.size());
        return result;
    }

    /**
     * first launch process to
     * build/fill <code>NodePath</code>
     * And after that, start building
     * direction between Supernode
     * elements.
     *
     * @param zend path zend Supernode nodeId
     */
    public void buildPath(String zend) {
        if (this.superNode != null) {
            for (org.opendaylight.transportpce.stubpce.topology.Resource res : this.superNode.getResources()) {
                NodePath path = new NodePath(res, this.superNode.getSuperNodeId(), this.superNode.isXpdrSrgAbsent());
                path.fill();
                this.nodepaths.add(path);
            }
            LOG.info("nodepaths size : {}", this.nodepaths.size());
            build(this.superNode.isXpdrSrgAbsent(),this.superNode.getSuperNodeId(), zend);
        }
    }

    public static void main(String[] args) {
        Topology topo = new Topology();
        topo.start();
        Network net = topo.getNetwork();
        if (net != null) {
            SuperNode res = net.getSuperNodes().get(0);
            String zend = "NodeZ";
            LOG.info(res.getSuperNodeId().toString());
            InterNodePath path = new InterNodePath(res);
            path.buildPath(zend);
        }
    }

    public SuperNode getSuperNode() {
        return this.superNode;
    }

    public void setSuperNode(SuperNode superNode) {
        this.superNode = superNode;
    }

    public List<AToZDirection> getAtoz() {
        return this.atoz;
    }

    public void setAtoz(List<AToZDirection> atoz) {
        this.atoz = atoz;
    }

    public List<ZToADirection> getZtoa() {
        return this.ztoa;
    }

    public void setZtoa(List<ZToADirection> ztoa) {
        this.ztoa = ztoa;
    }

    public List<NodePath> getNodepaths() {
        return this.nodepaths;
    }

    public void setNodepaths(List<NodePath> nodepaths) {
        this.nodepaths = nodepaths;
    }

}
