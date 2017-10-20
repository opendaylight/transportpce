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
 * Class to create structure of
 * Supernode element.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
@JacksonXmlRootElement(localName = "resource")
public class Resource {
    /** element nodeId. */
    @JacksonXmlProperty(localName = "node-id")
    private String nodeId;
    /** list of element links. */
    @JacksonXmlElementWrapper(localName = "links")
    @JacksonXmlProperty(localName = "link")
    private List<String> links;
    /** list of element LogicalConnectionPoint. */
    @JacksonXmlElementWrapper(localName = "logical-connection-points")
    @JacksonXmlProperty(localName = "logical-connection-point")
    private List<LogicalConnectionPoint> lcps;


    /**
     * Resource constructor.
     *
     * @param nodeId element nodeId
     * @param links list of element links
     * @param lcps list of element LogicalConnectionPoint
     */
    public Resource(@JacksonXmlProperty(localName = "node-id") final String nodeId,
            @JacksonXmlProperty(localName = "Reslinks") final List<String> links,
            @JacksonXmlProperty(localName = "Reslcps") final List<LogicalConnectionPoint> lcps) {
        this.setNodeId(nodeId);
        this.setLinks(links);
        this.setLpcs(lcps);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public List<LogicalConnectionPoint> getLcps() {
        return lcps;
    }

    public void setLpcs(List<LogicalConnectionPoint> lcps) {
        this.lcps = lcps;
    }

    @Override
    public String toString() {
        int index;
        int size;
        java.lang.String name = "Resource [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (nodeId != null) {
            builder.append("nodeId=");
            builder.append(nodeId);
        }
        index = 0;
        size = lcps.size();
        builder.append(", LogicalConnectionPoints [");
        for (LogicalConnectionPoint tmp : lcps) {
            builder.append(tmp.toString());
            index++;
            if (index < size) {
                builder.append(", ");
            }
        }
        builder.append("]");
        index = 0;
        size = links.size();
        builder.append(", links [");
        for (String tmp : links) {
            builder.append(tmp);
            index++;
            if (index < size) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.append(']').toString();

    }

}
