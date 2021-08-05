/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.Collections;
import java.util.List;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.link.tp.LinkTp;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.Nodes;

public final class DeviceRenderingResult extends OperationResult {

    private final List<Nodes> olmList;
    private final List<NodeInterface> renderedNodeInterfaces;
    private final List<LinkTp> otnLinkTps;

    private DeviceRenderingResult(boolean success, String message, List<Nodes> olmList,
            List<NodeInterface> renderedNodeInterfaces, List<LinkTp> otnLinkTps) {
        super(success, message);
        this.olmList =
            olmList == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(olmList);
        this.renderedNodeInterfaces =
            renderedNodeInterfaces == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(renderedNodeInterfaces);
        this.otnLinkTps =
            otnLinkTps == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(otnLinkTps);
    }

    public List<Nodes> getOlmList() {
        return this.olmList;
    }

    public List<NodeInterface> getRenderedNodeInterfaces() {
        return this.renderedNodeInterfaces;
    }

    public List<LinkTp> getOtnLinkTps() {
        return this.otnLinkTps;
    }

    public static DeviceRenderingResult failed(String message) {
        return new DeviceRenderingResult(false, message, null, null, null);
    }

    public static DeviceRenderingResult ok(List<Nodes> olmNodeList, List<NodeInterface> renderedNodeInterfaces,
            List<LinkTp> otnLinkTps) {
        return new DeviceRenderingResult(true, "", olmNodeList, renderedNodeInterfaces, otnLinkTps);
    }

}
