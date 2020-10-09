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

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class PceGraphEdge extends DefaultWeightedEdge {

    private PceLink link;

    public PceGraphEdge(PceLink link) {
        super();
        this.link = link;
    }

    public PceLink link() {
        return link;
    }

}
