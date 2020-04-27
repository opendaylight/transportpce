/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.node.TerminationPoint;

/**
 * Data holder for topology fragment.
 */
public class TopologyShard {

    private final List<Node> nodes;
    private final List<Link> links;
    private final List<TerminationPoint> tps;

    public TopologyShard(List<Node> nodes, List<Link> links) {
        this.nodes = nodes;
        this.links = links;
        this.tps = null;
    }

    public TopologyShard(List<Node> nodes, List<Link> links, List<TerminationPoint> tps) {
        this.nodes = nodes;
        this.links = links;
        this.tps = tps;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Link> getLinks() {
        return links;
    }

    public List<TerminationPoint> getTps() {
        return tps;
    }

}
