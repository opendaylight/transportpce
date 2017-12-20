/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeBuilder;

public class NodeData {

    private final NodeBuilder nodeBuilder;
    private final int portDirectionEnum;

    public NodeData(NodeBuilder nodeBuilder, int portDirectionEnum) {
        this.nodeBuilder = nodeBuilder;
        this.portDirectionEnum = portDirectionEnum;
    }

    public NodeBuilder getNodeBuilder() {
        return nodeBuilder;
    }

    public int getPortDirectionEnum() {
        return portDirectionEnum;
    }
}
