/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.validation;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.servicehandler.validation.checks.ComplianceCheckResult;
import org.opendaylight.transportpce.tapi.validation.checks.ConnConstraintCheck;
import org.opendaylight.transportpce.tapi.validation.checks.EndPointCheck;
import org.opendaylight.transportpce.tapi.validation.checks.ResilienceConstraintCheck;
import org.opendaylight.transportpce.tapi.validation.checks.TopoConstraintCheck;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.ConnectivityConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.ResilienceConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.TopologyConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CreateConnectivityServiceValidation {

    private static final Logger LOG = LoggerFactory.getLogger(CreateConnectivityServiceValidation.class);

    private CreateConnectivityServiceValidation() {
    }

    public static OperationResult validateCreateConnectivityServiceRequest(CreateConnectivityServiceInput input) {

        LOG.info("checking rpc create-connectivity-service input parameters...");
        LOG.info("checking EndPoints...");
        if (input.getEndPoint() == null) {
            return OperationResult.failed("Service End-Point must not be null");
        }
        List<EndPoint> endPointList = new ArrayList<>(input.getEndPoint().values());
        ComplianceCheckResult endPointCheckResult = EndPointCheck.check(endPointList);
        if (endPointCheckResult.hasPassed()) {
            LOG.info("create-connectivity-service end-points compliant !");
        } else {
            return OperationResult.failed(endPointCheckResult.getMessage());
        }

        LOG.info("checking ConnConstraint...");
        ConnectivityConstraint connectivityConstraint = input.getConnectivityConstraint();
        ComplianceCheckResult conConstraintCheckResult = ConnConstraintCheck.check(connectivityConstraint);
        if (conConstraintCheckResult.hasPassed()) {
            LOG.info("create-connectivity-service connectivity constraints compliant !");
        } else {
            return OperationResult.failed(conConstraintCheckResult.getMessage());
        }

        LOG.info("checking ResilienceConstraint...");
        ResilienceConstraint resilienceConstraintList = input.getResilienceConstraint();
        ComplianceCheckResult resilienceConstraintCheckResult = ResilienceConstraintCheck.check(
            resilienceConstraintList);
        if (resilienceConstraintCheckResult.hasPassed()) {
            LOG.info("create-connectivity-service resilience constraints compliant !");
        } else {
            return OperationResult.failed(resilienceConstraintCheckResult.getMessage());
        }

        LOG.info("checking TopoConstraint...");
        TopologyConstraint topoConstraint = input.getTopologyConstraint();
        ComplianceCheckResult topoConstraintCheckResult = TopoConstraintCheck.check(topoConstraint);
        if (topoConstraintCheckResult.hasPassed()) {
            LOG.info("create-connectivity-service topo constraints compliant !");
        } else {
            return OperationResult.failed(topoConstraintCheckResult.getMessage());
        }
        return OperationResult.ok("Validation successful.");
    }

}
