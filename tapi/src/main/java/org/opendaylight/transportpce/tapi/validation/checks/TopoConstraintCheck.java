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
        return ((value != null) && (!value.isEmpty()));
    }

    public static ComplianceCheckResult check(TopologyConstraint tc) {
        boolean result = true;
        String message = "";
        LOG.info("tc = {}", tc);
        if (checkNull(tc)) {
            LOG.info("tc is null");
            message = "Topology constraints are not managet yet";
        } else if (checkEmpty(tc)) {
            result = false;
            message = "Topology constraints are not managet yet";
        }

        return new ComplianceCheckResult(result, message);
    }

    private static boolean checkNull(TopologyConstraint tc) {
        if (tc == null) {
            return true;
        }
        if (tc.getAvoidTopology() == null) {
            return true;
        }
        if (tc.getExcludeLink() == null) {
            return true;
        }
        if (tc.getExcludeNode() == null) {
            return true;
        }
        if (tc.getExcludePath() == null) {
            return true;
        }
        if (tc.getIncludeLink() == null) {
            return true;
        }
        if (tc.getIncludeNode() == null) {
            return true;
        }
        if (tc.getIncludePath() == null) {
            return true;
        }
        if (tc.getIncludeTopology() == null) {
            return true;
        }
        return tc.getPreferredTransportLayer() == null;
    }

    //Due to number of check to do, cyclomatic complexity cannot be easily improved
    @java.lang.SuppressWarnings("squid:MethodCyclomaticComplexity")
    private static boolean checkEmpty(TopologyConstraint tc) {
        if (tc == null) {
            return true;
        }
        if (tc.getAvoidTopology() != null && tc.getAvoidTopology().isEmpty()) {
            return true;
        }
        if (tc.getExcludeLink() != null && tc.getExcludeLink().isEmpty()) {
            return true;
        }
        if (tc.getExcludeNode() != null && tc.getExcludeNode().isEmpty()) {
            return true;
        }
        if (tc.getExcludePath() != null && tc.getExcludePath().isEmpty()) {
            return true;
        }
        if (tc.getIncludeLink() != null && tc.getIncludeLink().isEmpty()) {
            return true;
        }
        if (tc.getIncludeNode() != null && tc.getIncludeNode().isEmpty()) {
            return true;
        }
        if (tc.getIncludePath() != null && tc.getIncludePath().isEmpty()) {
            return true;
        }
        if (tc.getIncludeTopology() != null && tc.getIncludeTopology().isEmpty()) {
            return true;
        }
        return tc.getPreferredTransportLayer() != null && tc.getPreferredTransportLayer().isEmpty();

    }
}
