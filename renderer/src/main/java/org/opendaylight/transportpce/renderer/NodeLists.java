/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.Nodes;

public class NodeLists {

    private List<Nodes> olmList;
    private List<Nodes> list;

    public NodeLists(List<Nodes> olmList, List<Nodes> list) {
        this.olmList = olmList;
        this.list = list;
    }

    public List<Nodes> getOlmList() {
        return olmList;
    }

    public List<Nodes> getList() {
        return list;
    }

}
