/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.graph;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.opendaylight.transportpce.pce.networkanalyzer.PceORLink;

@SuppressWarnings("serial")
@SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class PceGraphEdge extends DefaultWeightedEdge {

    private PceORLink link;

    public PceGraphEdge(PceORLink link) {
        super();
        this.link = link;
    }

    public PceORLink link() {
        return link;
    }

}
