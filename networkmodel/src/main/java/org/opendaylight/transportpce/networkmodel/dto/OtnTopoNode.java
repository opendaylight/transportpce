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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;

/**
 * Node model for an OTN node in the org-openroadm-otn-network-topology layer.
 */
public class OtnTopoNode {
    private String nodeId;
    private String clli;
    private XpdrNodeTypes nodeType;
    private int xpdrNb;
    private Map<String, String> xpdrNetConnectionMap;
    private Map<String, String> xpdrCliConnectionMap;
    private List<Mapping> xpdrClMappings;
    private List<Mapping> xpdrNetMappings;

    /**
     * Instantiate the OtnTopoNode.
     * @param nodeid Node name
     * @param clli CLLI
     * @param xpdrNb XPDR number
     * @param xpdrNodeTypes Type of XPDR (TPDR, MxPDR, Switch...)
     * @param xpdrNetConnectionMap Connection map for network port
     * @param xpdrCliConnectionMap Connection map for client port
     * @param xpdrNetMaps List of mappings for network ports
     * @param xpdrClMaps List of mapping for client ports
     */
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

    /**
     * Get the Node Id.
     * @return node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Get the XPDR type.
     * @return XpdrNodeTypes
     */
    public XpdrNodeTypes getNodeType() {
        return nodeType;
    }

    /**
     * Get the number of Network TP of the XPDR device.
     * @return Number
     */
    public int getNbTpNetwork() {
        return xpdrNetConnectionMap.size();
    }

    /**
     * Get the number for client TP of the XPDR device.
     * @return Number
     */
    public int getNbTpClient() {
        return xpdrCliConnectionMap.size();
    }

    /**
     * Get the number of XPDR declared in the OpenROADM XPDR device.
     * @return Number
     */
    public int getXpdrNb() {
        return xpdrNb;
    }

    /**
     * Get the CLLI configured on the OpenROADM device.
     * @return String
     */
    public String getClli() {
        return clli;
    }

    /**
     * Get the connection map of the network ports of the device.
     * @return a connection map
     */
    public Map<String, String> getXpdrNetConnectionMap() {
        return xpdrNetConnectionMap;
    }

    /**
     * Get the connection map of the client ports of the device.
     * @return a connection map
     */
    public Map<String, String> getXpdrCliConnectionMap() {
        return xpdrCliConnectionMap;
    }

    /**
     * Get the list of mappings for the client port of the device.
     * @return list of mappings
     */
    public List<Mapping> getXpdrClMappings() {
        return xpdrClMappings;
    }

    /**
     * Get the list of mappings for the network port of the device.
     * @return list of mappings
     */
    public List<Mapping> getXpdrNetMappings() {
        return xpdrNetMappings;
    }
}
