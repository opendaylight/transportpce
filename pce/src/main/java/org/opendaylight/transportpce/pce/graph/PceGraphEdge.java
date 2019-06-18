/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.graph;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;

public class PceGraphEdge extends DefaultWeightedEdge {

    private static final long serialVersionUID = 1L;
    private PceLink link;

    public PceGraphEdge(PceLink link) {
        super();
        this.link = link;
    }

    public PceLink link() {
        return link;
    }

}
