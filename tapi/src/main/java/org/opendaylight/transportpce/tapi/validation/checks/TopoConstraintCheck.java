/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation.checks;

import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.TopologyConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopoConstraintCheck {

    private static final Logger LOG = LoggerFactory.getLogger(TopoConstraintCheck.class);

    private TopoConstraintCheck() {
    }

    public static boolean checkString(String value) {
        return ((value != null) && (value.compareTo("") != 0));
    }

    public static ComplianceCheckResult check(TopologyConstraint tc) {
        boolean result = true;
        String message = "";
        LOG.info("tc = {}", tc.toString());
        if (tc.getAvoidTopology() == null || tc.getExcludeLink() == null || tc.getExcludeNode() == null || tc
            .getExcludePath() == null || tc.getIncludeLink() == null || tc.getIncludeNode() == null || tc
                .getIncludePath() == null || tc.getIncludeTopology() == null || tc
                    .getPreferredTransportLayer() == null) {
            LOG.info("tc is null");
            result = true;
            message = "Topology constraints are not managet yet";
        } else if (tc.getAvoidTopology().isEmpty() || tc.getExcludeLink().isEmpty() || tc.getExcludeNode().isEmpty()
            || tc
                .getExcludePath().isEmpty() || tc.getIncludeLink().isEmpty() || tc.getIncludeNode().isEmpty() || tc
                    .getIncludePath().isEmpty() || tc.getIncludeTopology().isEmpty() || tc.getPreferredTransportLayer()
                        .isEmpty()) {
            result = false;
            message = "Topology constraints are not managet yet";
        }

        return new ComplianceCheckResult(result, message);
    }
}
