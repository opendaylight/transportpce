/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.dto;

import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;

public class OtnTopoNode {
    private String nodeId;
    private String clli;
    private XpdrNodeTypes nodeType;
    private int xpdrNb;
    private Map<String, String> xpdrNetConnectionMap;
    private Map<String, String> xpdrCliConnectionMap;
    private List<Mapping> xpdrClMappings;
    private List<Mapping> xpdrNetMappings;

    public OtnTopoNode(String nodeid, String clli, int xpdrNb, XpdrNodeTypes xpdrNodeTypes,
        Map<String, String> xpdrNetConnectionMap, Map<String, String> xpdrCliConnectionMap, List<Mapping> xpdrNetMaps,
        List<Mapping> xpdrClMaps) {
        super();
        this.nodeId = nodeid;
        this.clli = clli;
        this.nodeType = xpdrNodeTypes;
        this.xpdrNb = xpdrNb;
        this.xpdrNetConnectionMap = xpdrNetConnectionMap;
        this.xpdrCliConnectionMap = xpdrCliConnectionMap;
        this.xpdrNetMappings = xpdrNetMaps;
        this.xpdrClMappings = xpdrClMaps;
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

    public List<Mapping> getXpdrClMappings() {
        return xpdrClMappings;
    }

    public List<Mapping> getXpdrNetMappings() {
        return xpdrNetMappings;
    }
}
