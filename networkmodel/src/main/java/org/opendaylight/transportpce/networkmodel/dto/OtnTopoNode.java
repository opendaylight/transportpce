/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;

public class OtnTopoNode {
    private String nodeId;
    private String clli;
    private XpdrNodeTypes nodeType;
    private int xpdrNb;
    private Map<String, String> xpdrNetConnectionMap;
    private Map<String, String> xpdrCliConnectionMap;

    public OtnTopoNode(String nodeid, String clli, int xpdrNb, XpdrNodeTypes xpdrNodeTypes,
        Map<String, String> xpdrNetConnectionMap, Map<String, String> xpdrCliConnectionMap) {
        super();
        this.nodeId = nodeid;
        this.clli = clli;
        this.nodeType = xpdrNodeTypes;
        this.xpdrNb = xpdrNb;
        this.xpdrNetConnectionMap = xpdrNetConnectionMap;
        this.xpdrCliConnectionMap = xpdrCliConnectionMap;
    }

    public String getNodeId() {
        return nodeId;
    }

    public XpdrNodeTypes getNodeType() {
        return nodeType;
    }

    public int getNbTpNetwork() {
        return xpdrNetConnectionMap.size();
    }

    public int getNbTpClient() {
        return xpdrCliConnectionMap.size();
    }

    public int getXpdrNb() {
        return xpdrNb;
    }

    public String getClli() {
        return clli;
    }

    public Map<String, String> getXpdrNetConnectionMap() {
        return xpdrNetConnectionMap;
    }

    public Map<String, String> getXpdrCliConnectionMap() {
        return xpdrCliConnectionMap;
    }
}
