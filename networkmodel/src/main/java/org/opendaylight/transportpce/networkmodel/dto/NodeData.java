/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;

public class NodeData {

    private final NodeBuilder nodeBuilder;
    private final List<Mapping> portMapList;
    private final List<CpToDegree> degreeCpList;

    public NodeData(NodeBuilder nodeBuilder, List<Mapping> portMapList, List<CpToDegree> degreeCpList) {
        this.nodeBuilder = nodeBuilder;
        this.portMapList = portMapList;
        this.degreeCpList = degreeCpList;
    }

    public NodeBuilder getNodeBuilder() {
        return nodeBuilder;
    }

    public List<Mapping> getPortMapList() {
        return portMapList;
    }

    public List<CpToDegree> getDegreeCpList() {
        return degreeCpList;
    }
}
