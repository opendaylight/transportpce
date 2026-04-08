/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException(String message) {
        super(message);
    }

    public LinkNotFoundException(String srcNode, String srcTp, String destNode, String destTp) {
        super(String.format(
                "Link not found from %s/%s to %s/%s",
                srcNode, srcTp, destNode, destTp
        ));
    }

    public LinkNotFoundException(
            String srcNodeId,
            String srcTpId,
            String destNodeId,
            String destTpId,
            String linkId) {
        super("Link not found in network topology: %s/%s -> %s/%s (linkId=%s)"
                .formatted(srcNodeId, srcTpId, destNodeId, destTpId, linkId));
    }
}
