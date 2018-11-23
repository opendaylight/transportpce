/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;

/**
 * Data holder for topology fragment.
 */
public class TopologyShard {

    private final List<Node> nodes;
    private final List<Link> links;
    private final List<Mapping> mappings;

    public List<Mapping> getMappings() {
        return mappings;
    }

    public TopologyShard(List<Node> nodes, List<Link> links, List<Mapping> mappings) {
        this.nodes = nodes;
        this.links = links;
        this.mappings = mappings;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Link> getLinks() {
        return links;
    }

}
