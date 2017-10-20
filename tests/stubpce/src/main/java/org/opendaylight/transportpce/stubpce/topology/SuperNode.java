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

import java.util.ArrayList;
import java.util.List;


/**
 * class to create Supernode
 * structure.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
@JacksonXmlRootElement(localName = "super-node")
public class SuperNode {
    /** Supernode Id. */
    @JacksonXmlProperty(localName = "super-node-id")
    private String superNodeId;
    /** list of elements cotaining in Supernode. */
    @JacksonXmlElementWrapper(localName = "resources")
    @JacksonXmlProperty(localName = "resource")
    private List<Resource> resources;

    /**
     * SuperNode constructor.
     *
     * @param supernodeid superNode Id
     * @param resources List of Supernode elements
     */
    public SuperNode(@JacksonXmlProperty(localName = "super-node-id") final String supernodeid,
            @JacksonXmlProperty(localName = "Spresource") final List<Resource> resources) {
        setSuperNodeId(supernodeid);
        setResources(resources);
    }

    /**
     * SuperNode constructor.
     *
     * @param supernodeid supernode Id
     */
    public SuperNode(String supernodeid) {
        setSuperNodeId(supernodeid);
        setResources(new ArrayList<Resource>());
    }

    /**
     *Test if Supernode contains
     *an XPDR.
     * @return true if XPDR present, false else
     */
    public boolean isXpdrSrgAbsent() {
        boolean result = true;
        int present = 0;
        if (resources.size() > 0) {
            for (Resource resource : resources) {
                String nodeId = resource.getNodeId();
                if (nodeId != null) {
                    if (nodeId.contains("XPDR")) {
                        present++;
                    }
                    if (nodeId.contains("SRG")) {
                        present++;
                    }
                }
            }
        }
        if (present == 2) {
            result = false;
        }
        return result;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public String getSuperNodeId() {
        return superNodeId;
    }

    public void setSuperNodeId(String superNodeId) {
        this.superNodeId = superNodeId;
    }

    @Override
    public String toString() {
        int index;
        int size;
        java.lang.String name = "SuperNode [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (superNodeId != null) {
            builder.append("superNodeId=");
            builder.append(superNodeId);
        }
        index = 0;
        size = resources.size();
        builder.append(", Resources [");
        for (Resource tmp : resources) {
            builder.append(tmp.toString());
            index++;
            if (index < size) {
                builder.append(", ");
            }
        }
        builder.append("]");
        return builder.append(']').toString();
    }

}
