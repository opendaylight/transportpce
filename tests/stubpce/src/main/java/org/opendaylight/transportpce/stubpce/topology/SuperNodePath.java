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
import java.util.SortedSet;
import java.util.TreeSet;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.stubpce.rev170426.path.description.list.PathDescriptionsBuilder;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.link.LinkIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Class to build Path between
 * two Supernode.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class SuperNodePath {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(SuperNodePath.class);
    /** List of NodeLinkNode. */
    private List<NodeLinkNode> paths;
    /** Supernode topology. */
    private Network network;

    /**
     * SuperNodePath constructor.
     *
     * @param network Supernode topology
     */
    public SuperNodePath(Network network) {
        setPaths(new ArrayList<NodeLinkNode>());
        this.network = network;
    }

    /**
     * test if Supernode is an
     * extremity of path to build.
     *
     * @param end extremity node Id
     * @param supernodes Supernodes list to build path
     * @return true if link extremity, false else
     */
    private Boolean endNode(String end, List<String> supernodes) {
        Boolean result = false;
        if ((end != null) && (end.compareTo(" ") != 0)) {
            for (String node : supernodes) {
                if (node.compareTo(end) == 0) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * get Supernode
     * with supernode Id.
     *
     * @param nodeId supernode id to get
     * @return SuperNode supernode gets
     */
    public SuperNode getSuperNode(String nodeId) {
        SuperNode result = null;
        if (this.network != null) {
            for (SuperNode tmp : this.network.getSuperNodes()) {
                if (tmp.getSuperNodeId().compareTo(nodeId) == 0) {
                    result = tmp;
                    break;
                }
            }
        }
        return result;
    }

    /**
     *find links between
     *two Supernodes.
     *
     * @param aend begin extremity
     * @param zend end extremity
     * @param links Roadm to Roadm links
     * @param direct determine if link is direct or not
     * @return String list of links name
     */
    private List<String> findLinks(String aend, String zend, List<String> links, boolean direct) {
        List<String> result = new ArrayList<String>();
        if (links.size() > 0) {
            aend = aend.replace("Node", "ROADM");
            zend = zend.replace("Node", "ROADM");
            String atozlink = null;
            String ztoalink = null;
            for (String tmp : links) {
                if (tmp.contains(aend)
                        && tmp.contains(zend)) {
                    LOG.info("direct path found for : {} / {}", aend, zend);
                    if (tmp.startsWith(aend)) {
                        atozlink = tmp;
                    }
                    if (tmp.startsWith(zend)) {
                        ztoalink = tmp;
                    }
                    if ((atozlink != null) && (ztoalink != null)) {
                        result.add(atozlink.concat("/").concat(ztoalink));
                        atozlink = null;
                        ztoalink = null;
                    }
                }
            }
        } else {
            LOG.info("no roadm-to-roadm links !");
        }
        return result;
    }

    /**
     *find next Supernode hop.
     *
     * @param link roadm to roadm link
     * @param aend begin supernode
     * @param node list of supernode id
     * @return String  supernodeId next hop
     */
    private String findHop(String link, String aend, List<String> node) {
        String result = null;
        aend = aend.replace("Node", "ROADM");
        for (String tmp : node) {
            tmp = tmp.replace("Node", "ROADM");
            if (tmp.compareTo(aend) != 0) {
                if (link.contains(aend) && link.contains(tmp)) {
                    LOG.info("hop : {}", tmp);
                    result = tmp;
                }
            }
        }
        return result;
    }

    /**
     *get all Supernode in
     *topology.
     *
     * @return String list of Supernode Id
     */
    private List<String> getSuperNodeId() {
        List<String> result = new ArrayList<String>();
        if (this.network.getSuperNodes().size() > 0) {
            for (SuperNode tmp : this.network.getSuperNodes()) {
                result.add(tmp.getSuperNodeId());
            }
        }
        return result;
    }

    /**
     * get all roadm to roadm
     * links in topology.
     *
     * @return String list of roadm to roadm links
     */
    private List<String> getRoadmLinks() {
        List<String> result = new ArrayList<String>();
        if (this.network.getRoadmToroadm().getLinks().size() > 0) {
            for (String tmp : this.network.getRoadmToroadm().getLinks()) {
                result.add(tmp);
            }
        }
        return result;
    }

    /**
     * create NodeLinkNode
     * structure.
     *
     * @param links String list of roadm to roadm links
     * @param aend beginning Supernode
     * @param zend ending Supernode
     * @param direct determine if link is direct or not
     */
    private void fill(List<String> links,String aend, String zend, boolean direct) {
        String term = "indirect";
        if (direct) {
            term = "direct";
        }
        if (!links.isEmpty()) {
            List<String> atoz = new ArrayList<String>();
            List<String> ztoa = new ArrayList<String>();
            for (String tmp : links) {
                String [] split = tmp.split("/");
                if (split.length == 2) {
                    atoz.add(split[0]);
                    ztoa.add(split[1]);
                }
            }
            if (!atoz.isEmpty() && (atoz.size() == ztoa.size())) {
                NodeLinkNode node = new NodeLinkNode(aend, zend, atoz,ztoa,direct);
                this.paths.add(node);
            }

        } else {
            LOG.info("{} links not found !", term);
        }
    }

    /**
     * launch SupernodePath process
     * to build NodeLinkNode.
     *
     * @param aend beginning extremity path
     * @param zend ending extremity path
     */
    public void run(String aend, String zend) {
        if (this.network != null) {
            List<String> supernodes = getSuperNodeId();
            List<String> roadmLinks = getRoadmLinks();
            if ((aend != null) && (zend != null)) {
                int size = supernodes.size();
                String hop = null;
                List<String> links = null;
                if (size > 0) {
                    if (endNode(aend, supernodes) && endNode(zend, supernodes)) {
                        LOG.info("Getting direct links ...");
                        links = new ArrayList<String>();
                        links = findLinks(aend,zend,roadmLinks,true);
                        fill(links, aend, zend, true);
                        LOG.info("Getting indirect links ..");
                        links = new ArrayList<String>();
                        for (String tmp : roadmLinks) {
                            hop = findHop(tmp, aend, supernodes);
                            if (hop != null) {
                                if (hop.compareTo(zend.replace("Node", "ROADM")) != 0) {
                                    LOG.info("next hop found : {}", hop);
                                    links.addAll(findLinks(aend,hop,roadmLinks,false));
                                    links.addAll(findLinks(hop,zend,roadmLinks,false));
                                } else {
                                    break;
                                }
                            }
                        }
                        fill(links, aend, zend, false);
                    } else {
                        LOG.info("aend or/and zend not exists !");
                    }
                }
            } else {
                LOG.info("aend or/and is null !");
            }
        } else {
            LOG.info("network is null !!");
        }
    }

    /**
     * modify all AToZ Id
     * in AToZ List containing
     * in AToZdirection.
     *
     * @param order beginning order
     * @param atozDirection AToZdirection List
     * @return AToZdirection List
     */
    public List<AToZDirection> modifyOrder(int order, List<AToZDirection> atozDirection) {
        List<AToZDirection> result = new ArrayList<AToZDirection>();
        for (AToZDirection tmp : atozDirection) {
            List<AToZ> atozList = tmp.getAToZ();
            int size = atozList.size();
            if (size > 0) {
                for (ListIterator<AToZ> it = atozList.listIterator(); it.hasNext();) {
                    AToZ atoz = it.next();
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription
                        .rev170426.pce.resource.resource.Resource res = atoz.getResource().getResource();
                    int tmpkey = order + Integer.parseInt(atoz.key().getId());
                    AToZKey atozKey = new AToZKey(Integer.toString(tmpkey));
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface
                        .pathdescription.rev170426.pce.resource.Resource resource = new ResourceBuilder()
                        .setResource(res).build();
                    AToZ hop = new AToZBuilder().setId(atozKey.getId()).withKey(atozKey).setResource(resource).build();
                    it.remove();
                    it.add(hop);
                    tmpkey++;
                }
                result.add(tmp);
            }
        }
        return result;
    }

    /**
     * merge two AToZdirection List.
     *
     * @param cleanInterA first AToZdirection List
     * @param cleanInterZ second AToZdirection List
     * @param first boolean to determine if it is the first time merge is done
     * @return AToZDirection List
     */
    public List<AToZDirection> merge(List<AToZDirection> cleanInterA, List<AToZDirection> cleanInterZ,
            boolean first) {
        List<AToZDirection> result = new ArrayList<AToZDirection>();
        if (!cleanInterA.isEmpty()) {
            int order  = cleanInterA.get(0).getAToZ().size();
            if (order > 0) {
                List<AToZDirection> modify = modifyOrder(order, cleanInterZ);
                if (!modify.isEmpty()) {
                    for (AToZDirection tmp : cleanInterA) {
                        List<AToZ> atozList = new ArrayList<AToZ>(tmp.getAToZ());
                        for (AToZDirection add : modify) {
                            ListIterator<AToZ> it = atozList.listIterator();
                            /** on va a la fin de la liste */
                            while (it.hasNext()) {
                                it.next();
                            }
                            List<AToZ> addList = add.getAToZ();
                            for (AToZ atoz : addList) {
                                it.add(atoz);
                            }
                            AToZDirectionBuilder newDirection = new AToZDirectionBuilder();
                            newDirection.setRate((long) 100).setAToZWavelengthNumber((long) 200).setAToZ(atozList);
                            result.add(newDirection.build());
                            atozList = new ArrayList<AToZ>(tmp.getAToZ());
                        }
                    }
                } else {
                    LOG.info("modify List is empty ! ");
                }
            } else {
                LOG.info("order is not superior to 0");
            }
        } else {
            if (first && !cleanInterZ.isEmpty()) {
                LOG.info("first merge so result is a copy of second AToZDirection List !");
                result = new ArrayList<AToZDirection>(cleanInterZ);
            } else {
                LOG.info("cleanInterA is empty !");
            }
        }
        return result;
    }

    /**
     * gets Degree number
     * for roadm links.
     *
     * @param atozLink atoz roadm link
     * @param ztoaLink ztoa roadm link
     * @return String list of degree
     */
    public List<String> getDeg(String atozLink, String ztoaLink) {
        List<String> result = new ArrayList<String>();
        if ((atozLink != null) && (ztoaLink != null)) {
            String [] split = atozLink.split("-", 4);
            if (split.length == 4) {
                result = Lists.newArrayList(split[1],split[3]);
            }
        } else {
            LOG.info("atozlink and/or ztoalink is null !");
        }
        return result;
    }

    /**
     * reverse link name
     * (ROADMA-DEG1-ROADMZ-DEG2
     * to
     * ROADMZ-DEG2-ROADMA-DEG1).
     *
     * @param linkId Link name
     * @return String link name reversed
     */
    public String reverseLinkId(String linkId) {
        StringBuilder builder = new StringBuilder();
        String [] split = linkId.split("-");
        int size = split.length;
        switch (size) {
            case 3:
                if (linkId.contains("XPDR")) {
                    if (linkId.startsWith("XPDR")) {
                        builder.append(split[1]).append("-")
                        .append(split[2]).append("-")
                        .append(split[0]);
                    } else {
                        builder.append(split[2]).append("-")
                        .append(split[0]).append("-")
                        .append(split[1]);
                    }
                }
                break;

            case 4:
                builder.append(split[2]).append("-")
                .append(split[3]).append("-")
                .append(split[0]).append("-")
                .append(split[1]);
                break;

            default:
                break;
        }
        return builder.toString();
    }

    /**
     * convert AToAdirection to
     * ZToAdirection.
     *
     * @param atozDirection AToZdirection to convert
     * @return ZToAdirection
     */
    public ZToADirection convertToZtoaDirection(AToZDirection atozDirection) {
        ZToADirectionBuilder ztoaDirection = new ZToADirectionBuilder();
        if (atozDirection != null) {
            List<AToZ> atozList = atozDirection.getAToZ();
            List<ZToA> ztoaList = new ArrayList<ZToA>();
            if (!atozList.isEmpty()) {
                List<AToZ> reverse = Lists.reverse(atozList);
                /** Building path. */
                ZToAKey ztoaKey = null;
                Resource resource = null;
                org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription
                    .rev170426.pce.resource.resource.Resource resLink = null;
                ZToA hop = null;
                int odr = 0;
                for (AToZ atoz : reverse) {
                    ztoaKey = new ZToAKey(Integer.toString(odr));
                    resLink = atoz.getResource().getResource();
                    if (resLink != null) {
                        if (resLink instanceof Link) {
                            Link link = (Link) resLink;
                            String newLinkId = reverseLinkId(link.getLinkIdentifier().getLinkId());
                            if (newLinkId != null) {
                                resource = new ResourceBuilder().setResource(new LinkBuilder()
                                        .setLinkIdentifier(new LinkIdentifierBuilder()
                                                .setLinkId(newLinkId)
                                                .build())
                                        .build())
                                    .build();
                            }

                        } else {
                            resource = new ResourceBuilder().setResource(resLink).build();
                        }
                    }
                    if (resource != null) {
                        hop = new ZToABuilder()
                                .withKey(ztoaKey)
                                .setResource(resource)
                                .build();
                        ztoaList.add(hop);
                        odr++;
                    } else {
                        LOG.info("resource is null ");
                    }
                }
                if (!ztoaList.isEmpty()) {
                    ztoaDirection.setRate((long) 100).setZToAWavelengthNumber((long) 200).setZToA(ztoaList);
                } else {
                    LOG.info("ztoaList is empty !");
                }
            }
        }
        return ztoaDirection.build();
    }

    /**
     *build Pathdescritions ordered List
     *to be loaded in Pathdescriptions
     *datastore.
     *
     * @param atozDirection AToZdirection List
     * @param term direct ou indirect
     * @return PathDescriptionsOrdered List
     */
    private SortedSet<PathDescriptionsOrdered> buildPathDescription(List<AToZDirection> atozDirection, String term) {
        SortedSet<PathDescriptionsOrdered> result = new TreeSet<PathDescriptionsOrdered>();
        PathDescriptionsBuilder pathDescr = new PathDescriptionsBuilder();
        int size = atozDirection.size();
        if (!atozDirection.isEmpty()) {
            LOG.info("result list AToZDirection size  : {}", atozDirection.size());
            List<ZToADirection> ztoadirList = new ArrayList<ZToADirection>();
            for (AToZDirection atozdir : atozDirection) {
                ZToADirection ztodir = convertToZtoaDirection(atozdir);
                if (ztodir != null) {
                    ztoadirList.add(ztodir);
                }
            }
            if (!ztoadirList.isEmpty() && (size == ztoadirList.size())) {
                LOG.info("building PathDescriptions ...");
                int index = 1;
                String pathName = null;
                for (int indexPath = 0 ; indexPath < size ; indexPath++) {
                    pathName = term.concat(Integer.toString(index));
                    LOG.info("pathName : {}", pathName);
                    pathDescr.setAToZDirection(atozDirection.get(indexPath))
                    .setZToADirection(ztoadirList.get(indexPath)).setPathName(pathName);
                    LOG.info("pathdesciption : {}", pathDescr.build().toString());
                    result.add(new PathDescriptionsOrdered(pathDescr.build(),index));
                    index++;
                }
            } else {
                LOG.info("Something wrong happen during atodir conversion...");
            }

        } else {
            LOG.info("atozDirection is empty");
        }
        return result;
    }

    /**
     * gets link extremity.
     *
     * @param link link
     * @return Supernode List of extremities
     */
    public List<SuperNode> getSuperNodeEndLink(String link) {
        List<SuperNode> result = new ArrayList<SuperNode>();
        if (link != null) {
            String [] split = link.split("-");
            if (split.length == 4) {
                String aend = split[0].replaceAll("ROADM", "Node");
                String zend = split[2].replaceAll("ROADM", "Node");
                if ((aend != null) && (zend != null)) {
                    LOG.info("getting super node for : {} and {}", aend, zend);
                    SuperNode aendSp = getSuperNode(aend);
                    SuperNode zendSp = getSuperNode(zend);
                    if ((aendSp != null) && (zendSp != null)) {
                        result.add(aendSp);
                        result.add(zendSp);
                    }
                }
            }
        }
        return result;
    }

    /**
     * build all direct paths.
     *
     * @param aend beginning extremity path
     * @param zend ending extremity path
     * @param nodeLinkNodes list paths
     * @return PathDescriptionsOrdered List of direct paths
     */
    public SortedSet<PathDescriptionsOrdered> getDirectPathDesc(String aend, String zend,
            List<NodeLinkNode> nodeLinkNodes) {
        List<AToZDirection> atozdirectionPaths = new ArrayList<AToZDirection>();
        SortedSet<PathDescriptionsOrdered> result = new TreeSet<PathDescriptionsOrdered>();
        SuperNode aendSp = getSuperNode(aend);
        SuperNode zendSp = getSuperNode(zend);
        if (!nodeLinkNodes.isEmpty()) {
            for (NodeLinkNode tmp : nodeLinkNodes) {
                if (tmp.getDirect()) {
                    LOG.info("Direct NodeLinkNode : {}", tmp.toString());
                    String atozLink = null;
                    String ztoaLink = null;
                    atozLink = tmp.getAtozLink().get(0);
                    ztoaLink = tmp.getZtoaLink().get(0);
                    if ((atozLink != null) && (ztoaLink != null)) {
                        LOG.info("atozlink : {}", atozLink);
                        LOG.info("ztoalink : {}", ztoaLink);
                        InterNodePath interAend = new InterNodePath(aendSp);
                        interAend.buildPath(zend);
                        InterNodePath interZend = new InterNodePath(zendSp);
                        interZend.buildPath(zend);
                        List<String> deg = getDeg(atozLink,ztoaLink);
                        LOG.info("deg : {}", deg.toString());
                        if (deg.size() == 2) {
                            List<AToZDirection> cleanInterA =
                                    interAend.replaceOrRemoveAToZDirectionEndLink(deg.get(0),"",atozLink,
                                            interAend.getAtoz(),false);
                            List<AToZDirection> cleanInterZ =
                                    interZend.replaceOrRemoveAToZDirectionEndLink("TAIL-LINKS",deg.get(1),"",
                                            interZend.getAtoz(),true);
                            if (!cleanInterA.isEmpty() && !cleanInterZ.isEmpty()) {
                                atozdirectionPaths.addAll(merge(cleanInterA,cleanInterZ,false));
                            } else {
                                LOG.info("cleanInterA ad/or cleanInterZ is empty !");
                            }
                        } else {
                            LOG.info("deg size is not correct, must be 2 ! ");
                        }
                    } else {
                        LOG.info("atozlink and / or ztoalink is null");
                    }
                }
            }

        } else {
            LOG.info("List of direct path is empty !");
        }
        if (!atozdirectionPaths.isEmpty()) {
            LOG.info("result size : {}", result.size());
            result = buildPathDescription(atozdirectionPaths,aend.concat("To").concat(zend).concat("_direct_"));
        } else {
            LOG.info("result is empty");
        }
        return result;
    }

    /**
     * build all indirect paths.
     *
     * @param aend beginning extremity path
     * @param zend ending extremity path
     * @param nodeLinkNodes list paths
     * @return PathDescriptionsOrdered List of indirect paths
     */
    public SortedSet<PathDescriptionsOrdered> getIndirectPathDesc(String aend, String zend,
            List<NodeLinkNode> nodeLinkNodes) {
        List<AToZDirection> atozdirectionPaths = new ArrayList<AToZDirection>();
        SortedSet<PathDescriptionsOrdered> result = new TreeSet<PathDescriptionsOrdered>();
        SuperNode aendSp = getSuperNode(aend);
        SuperNode zendSp = getSuperNode(zend);
        if (!nodeLinkNodes.isEmpty()) {
            for (NodeLinkNode tmp : nodeLinkNodes) {
                if (!tmp.getDirect()) {
                    LOG.info("Indirect NodeLinkNode : {}", tmp.toString());
                    int size = tmp.getAtozLink().size();
                    /** must be two for now just one hop. */
                    LOG.info("number of links  : {}", size);
                    boolean first = true;
                    if (size == 2) {
                        List<String> atozLinks = tmp.getAtozLink();
                        List<String> ztoaLinks = tmp.getZtoaLink();
                        if (!atozLinks.isEmpty() && !ztoaLinks.isEmpty()) {
                            LOG.info("atozlink : {}", atozLinks.toString());
                            LOG.info("ztoalink : {}", ztoaLinks.toString());
                            int loop = 0;
                            while (loop < 2) {
                                List<SuperNode> hop = getSuperNodeEndLink(atozLinks.get(loop));
                                if (!hop.isEmpty() && (hop.size() == 2)) {
                                    aendSp = hop.get(0);
                                    zendSp = hop.get(1);
                                    InterNodePath interAend = new InterNodePath(aendSp);
                                    interAend.buildPath(zend);
                                    LOG.info("interAend : {}", interAend.getAtoz().toString());
                                    InterNodePath interZend = new InterNodePath(zendSp);
                                    interZend.buildPath(zend);
                                    LOG.info("interZend : {}", interZend.getAtoz().toString());
                                    List<String> deg1 = getDeg(atozLinks.get(loop),ztoaLinks.get(loop));
                                    LOG.info("deg1 : {}", deg1.toString());
                                    if (!deg1.isEmpty() && (deg1.size() == 2)) {
                                        List<AToZDirection> cleanInterA = null;
                                        List<AToZDirection> cleanInterZ = null;
                                        if (zendSp.getSuperNodeId().compareTo(zend) == 0) {
                                            cleanInterA = interAend.replaceOrRemoveAToZDirectionEndLink(deg1.get(0),
                                                    "",atozLinks.get(loop),interAend.getAtoz(),false);
                                            LOG.info("next hop is zend");
                                            cleanInterZ = interZend.replaceOrRemoveAToZDirectionEndLink("TAIL-LINKS",
                                                    deg1.get(1),"",interZend.getAtoz(),true);
                                        } else if (loop < 1) {
                                            cleanInterA = interAend.replaceOrRemoveAToZDirectionEndLink(deg1.get(0),
                                                    "",atozLinks.get(loop),interAend.getAtoz(),false);
                                            cleanInterZ = interZend.getAToZDirectionEndBy(deg1.get(1),
                                                    interZend.getAtoz(), 1);
                                        }
                                        if (!cleanInterA.isEmpty() && !cleanInterZ.isEmpty()) {
                                            atozdirectionPaths = merge(atozdirectionPaths,cleanInterA,first);
                                            atozdirectionPaths = merge(atozdirectionPaths, cleanInterZ,false);
                                            first = false;
                                        } else {
                                            LOG.info("cleanInterA ad/or cleanInterZ is empty !");
                                            break;
                                        }
                                    }
                                } else {
                                    LOG.info("Hop list is empty");
                                }
                                loop++;
                            }
                        }
                    } else {
                        LOG.info("Link size is not supported , must be two !");
                    }
                }
            }
        } else {
            LOG.info("List of indirect path is empty !");
        }
        if (!atozdirectionPaths.isEmpty()) {
            LOG.info("result size : {}", result.size());
            result = buildPathDescription(atozdirectionPaths,aend.concat("To").concat(zend).concat("_indirect_"));
        } else {
            LOG.info("result is empty");
        }
        return result;
    }

    public static void main(String[] args) {
        Topology topo = new Topology();
        topo.start();
        SuperNodePath path = new SuperNodePath(topo.getNetwork());
        String aend = "NodeA";
        String zend = "NodeZ";
        path.run(aend, zend);
        path.getDirectPathDesc(aend, zend, path.getPaths());
        path.getIndirectPathDesc(aend, zend, path.getPaths());
    }

    public List<NodeLinkNode> getPaths() {
        return this.paths;
    }

    public void setPaths(List<NodeLinkNode> paths) {
        this.paths = paths;
    }
}
