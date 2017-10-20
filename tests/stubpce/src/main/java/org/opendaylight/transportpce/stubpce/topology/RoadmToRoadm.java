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
 * Class to create list of links
 * between Supernode.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
@JacksonXmlRootElement(localName = "roadm-to-roadm")
public class RoadmToRoadm {
    /** List of links. */
    @JacksonXmlElementWrapper(localName = "links")
    @JacksonXmlProperty(localName = "link")
    private List<String> links;

    /**
     * RoadmToRoadm structure.
     *
     * @param links list of links
     */
    public RoadmToRoadm(@JacksonXmlProperty(localName = "Rlinks") final List<String> links) {
        setLinks(links);
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    @Override
    public String toString() {
        int index;
        int size;
        java.lang.String name = "RoadmToRoadm [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        index = 0;
        size = links.size();
        builder.append("Links [");
        for (String tmp : links) {
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
