/*
 * Copyright © 2020 AT&T and others.  All rights reserved.
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

public final class OtnDeviceRenderingResult extends OperationResult {
    private final List<NodeInterface> renderedNodeInterfaces;
    private final List<LinkTp> otnLinkTps;


    private OtnDeviceRenderingResult(boolean success, String message, List<NodeInterface> renderedNodeInterfaces,
            List<LinkTp> otnLinkTps) {
        super(success, message);
        this.renderedNodeInterfaces =
            renderedNodeInterfaces == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(renderedNodeInterfaces);
        this.otnLinkTps =
            otnLinkTps == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(otnLinkTps);
    }

    public List<NodeInterface> getRenderedNodeInterfaces() {
        return this.renderedNodeInterfaces;
    }

    public List<LinkTp> getOtnLinkTps() {
        return this.otnLinkTps;
    }

    public static OtnDeviceRenderingResult failed(String message) {
        return new OtnDeviceRenderingResult(false, message, null, null);
    }

    public static OtnDeviceRenderingResult ok(List<NodeInterface> renderedNodeInterfaces,
            List<LinkTp> otnLinkTps) {
        return new OtnDeviceRenderingResult(true, "", renderedNodeInterfaces, otnLinkTps);
    }

}
