/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import java.util.List;

/**
 * Class to create structure
 * NodeToLinkToNode.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class NodeLinkNode {
    /** aend first endpoint. */
    private String aend;
    /** zend second endpoint. */
    private String zend;
    /** atoz link. */
    private List<String> atozLink;
    /** ztoa link. */
    private List<String> ztoaLink;
    /** direct boolean to specify direct path. */
    private Boolean direct;

    /**
     * NodeLinkNode Constructor.
     *
     * @param aend first endpoint
     * @param zend second endpoint
     * @param link1 atoz link
     * @param link2 ztoa link
     * @param direct boolean to specify direct path
     */
    public NodeLinkNode(String aend, String zend, List<String> link1, List<String> link2,Boolean direct) {
        setAend(aend);
        setZend(zend);
        setAtozLink(link1);
        setZtoaLink(link2);
        setDirect(direct);
    }

    public Boolean getDirect() {
        return direct;
    }

    public void setDirect(Boolean direct) {
        this.direct = direct;
    }

    public String getAend() {
        return aend;
    }

    public void setAend(String aend) {
        this.aend = aend;
    }

    public String getZend() {
        return zend;
    }

    public void setZend(String zend) {
        this.zend = zend;
    }

    @Override
    public String toString() {
        java.lang.String name = "NodeLinkNode [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (aend != null) {
            builder.append("aend=");
            builder.append(aend);
            builder.append(", ");
        }
        if (atozLink != null) {
            builder.append("atozLink=");
            builder.append(atozLink);
            builder.append(", ");
        }
        if (ztoaLink != null) {
            builder.append("ztoaLink=");
            builder.append(ztoaLink);
            builder.append(", ");
        }
        if (zend != null) {
            builder.append("zend=");
            builder.append(zend);
            builder.append(", ");
        }
        builder.append(", direct=");
        builder.append(direct);
        return builder.append(']').toString();
    }

    public List<String> getAtozLink() {
        return atozLink;
    }

    public void setAtozLink(List<String> atozLink) {
        this.atozLink = atozLink;
    }

    public List<String> getZtoaLink() {
        return ztoaLink;
    }

    public void setZtoaLink(List<String> ztoaLink) {
        this.ztoaLink = ztoaLink;
    }
}
