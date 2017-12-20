/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceNode {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PceCalculation.class);

    ////////////////////////// NODES ///////////////////////////
    /*
     */

    private boolean valid = true;

    private final Node node;
    private final NodeId nodeId;
    private final OpenroadmNodeType nodeType;

    // wavelength calculation per node type
    private List<Long> availableWLindex = new ArrayList<Long>();
    private List<String> usedXpndrNWTps = new ArrayList<String>();

    private List<PceLink> outgoingLinks = new ArrayList<PceLink>();

    private Map<String, String> clientPerNwTp = new HashMap<String, String>();

    public PceNode(Node node, OpenroadmNodeType nodeType, NodeId nodeId) {
        this.node = node;
        this.nodeId = nodeId;
        this.nodeType = nodeType;

        if ((node == null) || (nodeId == null) || (nodeType == null)) {
            LOG.error("PceNode: one of parameters is not populated : nodeId, node type");
            valid = false;
        }
    }

    public void initWLlist() {

        availableWLindex.clear();

        if (!isValid()) {
            return;
        }

        Node1 node1 = node.getAugmentation(Node1.class);

        switch (nodeType) {
            case SRG:
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                    .AvailableWavelengths> srgAvailableWL = node1.getSrgAttributes().getAvailableWavelengths();

                if (srgAvailableWL == null) {
                    valid = false;
                    LOG.error("initWLlist: SRG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }

                for (org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev170929.srg.node.attributes
                    .AvailableWavelengths awl : srgAvailableWL) {
                    availableWLindex.add(awl.getIndex());
                    LOG.debug("initWLlist: SRG next = {} in {}" , awl.getIndex() , this.toString());
                }

                break;

            case DEGREE:
                List<org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes
                    .AvailableWavelengths> degAvailableWL = node1.getDegreeAttributes().getAvailableWavelengths();

                if (degAvailableWL == null) {
                    valid = false;
                    LOG.error("initWLlist: DEG AvailableWavelengths is empty for node  {}", this.toString());
                    return;
                }

                for (org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev170929.degree.node.attributes
                    .AvailableWavelengths awl : degAvailableWL) {
                    availableWLindex.add(awl.getIndex());
                    LOG.debug("initWLlist: DEGREE next = {} in {}", awl.getIndex(), this.toString());
                }

                break;

            case XPONDER:

                // HARD CODED 96
                for (long i = 1; i <= 96; i++) {
                    availableWLindex.add(i);
                }

                break;

            default:
                LOG.error("initWLlist: unsupported node type {} in node {}" , nodeType, this.toString());
                break;
        }

        if (availableWLindex.size() == 0) {
            LOG.debug("initWLlist: There are no available wavelengths in node {}", this.toString());
            valid = false;
        }
        LOG.debug("initWLlist: availableWLindex size = {} in {}" , availableWLindex.size(), this.toString());

        return;
    }

    public void initXndrTps() {

        if (!isValid()) {
            return;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1 nodeTp =
            node.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Node1.class);

        List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint> allTps = nodeTp.getTerminationPoint();

        if (allTps == null) {
            valid = false;
            LOG.error("initXndrTps: XPONDER TerminationPoint list is empty for node {}", this.toString());
            return;
        }

        valid = false;

        for (org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.node
            .TerminationPoint tp : allTps) {

            TerminationPoint1 tp1 = tp.getAugmentation(TerminationPoint1.class);

            if (tp1.getTpType() == OpenroadmTpType.XPONDERNETWORK) {

                if (tp1.getXpdrNetworkAttributes().getWavelength() != null) {
                    usedXpndrNWTps.add(tp.getTpId().getValue());
                    LOG.debug("initWLlist: XPONDER tp = {} is used", tp.getTpId().getValue());
                } else {
                    valid = true;
                }

                // find Client of this network TP
                String client = tp1.getXpdrNetworkAttributes().getTailEquipmentId();
                if ((client.equals("")) || (client == null)) {
                    LOG.error("initXndrTps: XPONDER {} NW TP doesn't have defined Client {}",
                        this.toString(),tp.getTpId().getValue());
                    valid = false;
                }
                clientPerNwTp.put(tp.getTpId().getValue(), client);

            }
        }
        if (!isValid()) {
            LOG.error("initXndrTps: XPONDER doesn't have available wavelengths for node  {}", this.toString());
            return;
        }


    }

    public boolean checkTP(String tp) {
        if (usedXpndrNWTps.contains(tp)) {
            return false;
        }
        return true;
    }

    public boolean checkWL(long index) {
        if (availableWLindex.contains(index)) {
            return true;
        }
        return false;
    }

    public boolean isValid() {
        return valid;
    }

    public List<Long> getAvailableWLs() {
        return availableWLindex;
    }

    public void addOutgoingLink(PceLink outLink) {
        this.outgoingLinks.add(outLink);
    }

    public List<PceLink> getOutgoingLinks() {
        return outgoingLinks;
    }

    public String getClient(String tp) {
        return clientPerNwTp.get(tp);
    }

    @Override
    public String toString() {
        return "PceNode type=" + nodeType + " ID=" + nodeId.getValue();
    }

}
