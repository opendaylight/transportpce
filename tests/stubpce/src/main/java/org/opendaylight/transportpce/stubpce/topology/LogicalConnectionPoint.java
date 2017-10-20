/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;


/**
 * class to build Logical
 * Connection Point.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
@JacksonXmlRootElement(localName = "logical-connection-point")
public class LogicalConnectionPoint {
    @JacksonXmlProperty(localName = "tp-id")
    private String tpId;
    @JacksonXmlProperty(localName = "node-id")
    private String nodeId;

    public LogicalConnectionPoint(@JacksonXmlProperty(localName = "tp-id") final String tpId,
            @JacksonXmlProperty(localName = "node-id") final String nodeId) {
        this.setTpId(tpId);
        this.setNodeId(nodeId);
    }

    public String getTpId() {
        return tpId;
    }

    public void setTpId(String tpId) {
        this.tpId = tpId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public String toString() {
        java.lang.String name = "[";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (tpId != null) {
            builder.append("tpId=");
            builder.append(tpId);
            builder.append(", ");
        }
        if (nodeId != null) {
            builder.append("nodeId=");
            builder.append(nodeId);
        }
        return builder.append(']').toString();

    }

}
