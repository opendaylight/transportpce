/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

/**
 * Class to create topology
 * structure according to the idea of
 * SuperNode.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
@JacksonXmlRootElement(localName = "network")
public class Network {
    /** SuperNode List.*/
    @JacksonXmlElementWrapper(localName = "super-nodes")
    @JacksonXmlProperty(localName = "super-node")
    private List<SuperNode> superNodes;
    /** Links between Supernodes. */
    @JacksonXmlProperty(localName = "roadm-to-roadm")
    private RoadmToRoadm roadmToroadm;

    /**
     * Network constructor.
     *
     * @param nodes Supernode list
     * @param links roadmtoroadm links
     */
    public Network(
            @JacksonXmlProperty(localName = "Nsupernode") final List<SuperNode> nodes,
            @JacksonXmlProperty(localName = "roadm-to-roadm") final RoadmToRoadm links) {
        setSuperNodes(nodes);
        setRoadmToroadm(links);
    }

    public List<SuperNode> getSuperNodes() {
        return superNodes;
    }

    public void setSuperNodes(List<SuperNode> superNodes) {
        this.superNodes = superNodes;
    }


    public RoadmToRoadm getRoadmToroadm() {
        return roadmToroadm;
    }

    public void setRoadmToroadm(RoadmToRoadm roadmToroadm) {
        this.roadmToroadm = roadmToroadm;
    }

    @Override
    public String toString() {
        int index;
        int size;
        java.lang.String name = "Network [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        index = 0;
        size = superNodes.size();
        builder.append("SuperNodes [");
        if (size > 0) {
            for (SuperNode tmp : superNodes) {
                builder.append(tmp.toString());
                index++;
                if (index < size) {
                    builder.append(", ");
                }
            }
        }
        builder.append("]");
        if (roadmToroadm != null) {
            builder.append(", roadmToroadm=");
            builder.append(roadmToroadm.toString());
        }
        return builder.append(']').toString();

    }

}
