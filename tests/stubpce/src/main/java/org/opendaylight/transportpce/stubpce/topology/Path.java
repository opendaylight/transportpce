/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import org.opendaylight.transportpce.stubpce.TpNodeTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.Link;

/**
 * Class to create structure
 * TpNodeTp and link.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class Path {
    /** TpNodeTp. */
    private TpNodeTp tpNodeTp;
    /** Link at TpNodeTp. */
    private Link link;

    /**
     *Path constructor.
     *
     * @param tpnodetp TpNodeTp
     * @param link link at the end of TpNodeTp
     */
    public Path(TpNodeTp tpnodetp, Link link) {
        setTpNodeTp(tpnodetp);
        setLink(link);
    }

    public Path(TpNodeTp tpNodeTp) {
        setTpNodeTp(tpNodeTp);
    }

    public TpNodeTp getTpNodeTp() {
        return tpNodeTp;
    }

    public void setTpNodeTp(TpNodeTp tpNodeTp) {
        this.tpNodeTp = tpNodeTp;
    }

    public Link getLink() {
        return link;
    }

    public void setLink(Link link) {
        this.link = link;
    }

    @Override
    public String toString() {
        java.lang.String name = "Path [";
        java.lang.StringBuilder builder = new java.lang.StringBuilder(name);
        if (tpNodeTp != null) {
            builder.append("tpNodeTp= ");
            builder.append(tpNodeTp);
            builder.append(", ");
        }
        if (link != null) {
            builder.append(link);
        }
        return builder.append(']').toString();
    }
}
