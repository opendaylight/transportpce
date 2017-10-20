/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.transportpce.stubpce.TpNodeTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPointBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * build all path with one resource
 * from Supernode.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class NodePath {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(NodePath.class);
    /** element of Supernode. */
    private org.opendaylight.transportpce.stubpce.topology.Resource resource;
    /** <code>TpNodeTp</code> list. */
    private List<TpNodeTp> tpNodeTps;
    /** <code>Link</code> list. */
    private List<Link> links;
    /** SuperNode element nodeId. */
    private String nodeId;
    /** Supernode Id. */
    private String superNodeId;
    /** boolean to determine if Supernode contains an XPDR. */
    private boolean isXpdrSrgAbsent;
    /** Path List. */
    private List<Path> path;

    /**
     *NodePath constructor.
     *
     * @param res element of Supernode
     * @param superNodeId supernode Id
     * @param isXpdrSrgAbsent boolean to determine if Supernode contains an XPDR
     */
    public NodePath(org.opendaylight.transportpce.stubpce.topology.Resource res,
            String superNodeId, boolean isXpdrSrgAbsent) {
        setResource(res);
        setNodeId(res.getNodeId());
        setSuperNodeId(superNodeId);
        setXpdrSrgAbsent(isXpdrSrgAbsent);
        setLinks(new ArrayList<Link>());
        setTpNodeTps(new ArrayList<TpNodeTp>());
        setPath(new ArrayList<Path>());
    }

    @Override
    public String toString() {
        java.lang.String name = "NodePath [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (nodeId != null) {
            builder.append("nodeId= ");
            builder.append(nodeId);
            builder.append(", ");
        }
        if (path.size() > 0) {
            builder.append("Paths [");
            builder.append(path.toString());
            builder.append(" ]");
        }
        return builder.append(']').toString();
    }

    /**
     *get all resources (links,
     *logicalConnectionpoints, nodeId)
     *from Supernode element.
     */
    public void fill() {
        Boolean xpdr = false;
        if (resource != null) {
            String nodeid = resource.getNodeId();
            List<String> resLinks = resource.getLinks();
            List<LogicalConnectionPoint> resLcps = resource.getLcps();
            for (String tmp : resLinks) {
                Link link = new LinkBuilder()
                        .setLinkId(tmp)
                        .build();
                links.add(link);
            }
            if (nodeid.contains("XPDR")) {
                LOG.info("building xpdr resource ...");
                xpdr = true;
                buildXpdrTopo(nodeid,resLinks,resLcps,xpdr);
                links.add(new LinkBuilder()
                        .setLinkId("TAIL-LINKS")
                        .build());
            }
            if (nodeid.contains("SRG")) {
                LOG.info("building SRG resource ...");
                buildSRGTopo(nodeid, resLinks, resLcps);
            }
            if (nodeid.contains("DEG")) {
                LOG.info("building DEG resource ...");
                xpdr = false;
                buildXpdrTopo(nodeid,resLinks,resLcps,xpdr);
                links.add(new LinkBuilder()
                        .setLinkId("EXTERNAL-LINKS")
                        .build());
            }
        }
        createDirection();
    }

    /**
     * reverse a TpNodeTp
     * List.
     *
     * @return TpNodeTp list
     */
    private List<TpNodeTp> reverseTpNodetpList() {
        List<TpNodeTp> result = new ArrayList<TpNodeTp>();
        for (TpNodeTp tpNodetp : tpNodeTps) {
            TpNodeTp tmp = tpNodetp.reverse();
            result.add(tmp);
        }
        return result;
    }

    /**
     *create list of Path
     *(TpNodeTp/Link).
     */
    private void createDirection() {
        /** create direction */
        LOG.info("creating direction ...");
        Path resourcePath = null;
        List<TpNodeTp> direction = new ArrayList<TpNodeTp>();
        for (Link link : links) {
            String linkId = link.getLinkId();
            LOG.info("LinkId : " + linkId);
            if (!isXpdrSrgAbsent) {
                if (StringUtils.countMatches(link.getLinkId(), "ROADM") < 2) {
                    if ((linkId.contains("XPDR") && linkId.startsWith("ROADM"))
                        || ((linkId.startsWith("DEG") && linkId.contains("SRG")))
                        || (nodeId.contains("XPDR") && linkId.contains("TAIL-LINKS"))) {
                        LOG.info("reversing TpNodetp list for link '" + linkId + "'");
                        direction = reverseTpNodetpList();
                    } else {
                        direction = tpNodeTps;
                    }
                } else {
                    LOG.info("link is deg to deg link !");
                }
            } else {
                if (StringUtils.countMatches(link.getLinkId(), "ROADM") == 2) {
                    direction = reverseTpNodetpList();
                } else {
                    direction = tpNodeTps;
                }
            }
            if (tpNodeTps.size() > 0) {
                for (TpNodeTp tpNodeTp : direction) {
                    resourcePath = new Path(tpNodeTp, link);
                    LOG.info(resourcePath.toString());
                    path.add(resourcePath);
                }
            } else {
                LOG.info("tpNodeTps is empty !");
            }
        }
    }

    /**
     * build a list of
     * Termination Point in
     * XPDR or DEG
     * ordered in AToZdirection
     * on a Supernode (XPDR to SRG)
     * (ROADMA-DEG to ROADMZ-DEG).
     *
     * @param xpdr determine if it is an XPDR or not
     * @param resLcps list of LogicalConnectionPoint
     * @return String list of TerminationPoints
     */
    private List<String> getOrderedTps(boolean xpdr, List<LogicalConnectionPoint> resLcps) {
        List<String> result = new ArrayList<String>();
        if (xpdr) {
            for (LogicalConnectionPoint lcp : resLcps) {
                String tmp = lcp.getTpId();
                if (tmp.contains("CLIENT")) {
                    result.add(0, tmp);
                } else if (tmp.contains("NETWORK")) {
                    result.add(1, tmp);
                }
            }
        } else {
            for (LogicalConnectionPoint lcp : resLcps) {
                String tmp = lcp.getTpId();
                if (tmp.contains("CTP")) {
                    result.add(0, tmp);
                } else if (tmp.contains("TTP")) {
                    result.add(1, tmp);
                }
            }
        }
        return result;
    }

    /**
     * build TpNodeTp
     * structure in an
     * XPDR.
     *
     * @param nodeid XPDR Id
     * @param resLinks list of links
     * @param resLcps list of LogicalConnectionPoints
     * @param xpdr determine if it is an XPDR or not
     */
    private void buildXpdrTopo(String nodeid, List<String> resLinks, List<LogicalConnectionPoint> resLcps,
            boolean xpdr) {
       /** build TpNodetp .*/
        TerminationPointBuilder in = null;
        TerminationPointBuilder out = null;
        List<String> lcps = getOrderedTps(xpdr,resLcps);
        if (lcps.size() == 2) {
            in = new TerminationPointBuilder()
                    .setTpNodeId(nodeid)
                    .setTpId(lcps.get(0));
            out = new TerminationPointBuilder()
                    .setTpNodeId(nodeid)
                    .setTpId(lcps.get(1));
        } else {
            LOG.info("lcps size not equal to 2");

        }
        Node node = new NodeBuilder()
                .setNodeId(nodeid)
                .build();
        TpNodeTp tmp = new TpNodeTp(in.build(), out.build(), node);
        tpNodeTps.add(tmp);
    }

    /**
     * build TpNodeTp
     * structure in an
     * SGR.
     *
     * @param nodeid SRG nodeId
     * @param resLinks list of links
     * @param resLcps list of LogicalConnectionPoints
     */
    private void buildSRGTopo(String nodeid, List<String> resLinks,List<LogicalConnectionPoint> resLcps) {
        /** build TpNodetp .*/
        Node node = new NodeBuilder()
                .setNodeId(nodeid)
                .build();
        TerminationPoint out = new TerminationPointBuilder()
                .setTpNodeId(nodeid)
                .setTpId("SRG1-CP-TXRX")
                .build();

        for (LogicalConnectionPoint lcp : resLcps) {
            if (lcp.getTpId().compareTo("SRG1-CP-TXRX") != 0) {
                TerminationPoint in = new TerminationPointBuilder()
                        .setTpNodeId(nodeid)
                        .setTpId(lcp.getTpId())
                        .build();
                tpNodeTps.add(new TpNodeTp(in, out, node));
            }
        }
    }

    public static void main(String[] args) {
        Topology topo = new Topology();
        topo.start();
        Network net = topo.getNetwork();
        if (net != null) {
            SuperNode superNode = net.getSuperNodes().get(0);
            LOG.info("SuperNode : " + superNode.getSuperNodeId());
            for (org.opendaylight.transportpce.stubpce.topology.Resource res :
                superNode.getResources()) {
                LOG.info(res.toString());
                NodePath path = new NodePath(res, superNode.getSuperNodeId(), superNode.isXpdrSrgAbsent());
                path.fill();
            }
        }
    }

    public org.opendaylight.transportpce.stubpce.topology.Resource getResource() {
        return resource;
    }

    public void setResource(org.opendaylight.transportpce.stubpce.topology.Resource resource) {
        this.resource = resource;
    }

    public List<TpNodeTp> getTpNodeTps() {
        return tpNodeTps;
    }

    public void setTpNodeTps(List<TpNodeTp> tpNodeTps) {
        this.tpNodeTps = tpNodeTps;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Path> getPath() {
        return path;
    }

    public void setPath(List<Path> path) {
        this.path = path;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getSuperNodeId() {
        return superNodeId;
    }

    public void setSuperNodeId(String superNodeId) {
        this.superNodeId = superNodeId;
    }

    public boolean isXpdrSrgAbsent() {
        return isXpdrSrgAbsent;
    }

    public void setXpdrSrgAbsent(boolean isXpdrSrgAbsent) {
        this.isXpdrSrgAbsent = isXpdrSrgAbsent;
    }
}
