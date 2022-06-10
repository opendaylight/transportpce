/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.resiliency.ServiceResiliency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to check compliance of service-resiliency input.
 */
public final class ServicehandlerServiceResiliencyCheck {

    private static final Logger LOG = LoggerFactory.getLogger(ServicehandlerServiceResiliencyCheck.class);
    public static final String LOG_ATTRIBUTE_NOT_DEDICATED = "An attribute not dedicated for this type of "
            + "resiliency was filled";
    public static final String LOG_RESILIENCY_TYPE_UNMANAGED = "Resiliency unmanaged for compliance check !";
    public static final String LOG_RESILIENCY_NULL = "Resiliency is null !";

    /**
     * Function to check coherency of service-resiliency inputs.
     *
     * @param serviceResiliency ServiceResiliency to check
     * @return <code> true </code>  if coherent
     *         <code> false </code> otherwise
     */
    public static ComplianceCheckResult check(ServiceResiliency serviceResiliency) {
        if (serviceResiliency.getResiliency() != null) {
            switch (serviceResiliency.getResiliency().getSimpleName()) {
                case "Unprotected":
                    return checkUnprotectedResiliency(serviceResiliency);

                case "UnprotectedDiverselyRouted":
                    return checkUnprotectedDiverselyRoutedResiliency(serviceResiliency);

                case "Protected":
                    return checkProtectedResiliency(serviceResiliency);

                case "Restorable":
                case "ExternalTriggerRestorable":
                    return checkRestorableAndExternalTriggerRestorableResiliency(serviceResiliency);

                default:
                    LOG.warn(LOG_RESILIENCY_TYPE_UNMANAGED);
                    return new ComplianceCheckResult(false, LOG_RESILIENCY_TYPE_UNMANAGED);
            }
        } else {
            LOG.warn(LOG_RESILIENCY_NULL);
            return new ComplianceCheckResult(false, LOG_RESILIENCY_NULL);
        }
    }

    private static ComplianceCheckResult checkUnprotectedResiliency(ServiceResiliency serviceResiliency) {
        return serviceResiliency.getRevertive() == null
                && serviceResiliency.getWaitToRestore() == null
                && serviceResiliency.getHoldoffTime() == null
                && serviceResiliency.getPreCalculatedBackupPathNumber() == null
                && serviceResiliency.getCoupledService() == null
                ? new ComplianceCheckResult(true, "")
                : new ComplianceCheckResult(false, LOG_ATTRIBUTE_NOT_DEDICATED);
    }

    private static ComplianceCheckResult checkUnprotectedDiverselyRoutedResiliency(
            ServiceResiliency serviceResiliency) {
        return serviceResiliency.getRevertive() == null
                && serviceResiliency.getWaitToRestore() == null
                && serviceResiliency.getHoldoffTime() == null
                && serviceResiliency.getPreCalculatedBackupPathNumber() == null
                ? new ComplianceCheckResult(true, "")
                : new ComplianceCheckResult(false, LOG_ATTRIBUTE_NOT_DEDICATED);
    }

    private static ComplianceCheckResult checkProtectedResiliency(ServiceResiliency serviceResiliency) {
        return (serviceResiliency.getWaitToRestore() == null
                || (serviceResiliency.getRevertive() != null && serviceResiliency.getRevertive()))
                && serviceResiliency.getPreCalculatedBackupPathNumber() == null
                && serviceResiliency.getCoupledService() == null
                ? new ComplianceCheckResult(true, "")
                : new ComplianceCheckResult(false, LOG_ATTRIBUTE_NOT_DEDICATED);
    }

    private static ComplianceCheckResult checkRestorableAndExternalTriggerRestorableResiliency(
            ServiceResiliency serviceResiliency) {
        return (serviceResiliency.getWaitToRestore() == null
                || (serviceResiliency.getRevertive() != null && serviceResiliency.getRevertive()))
                && serviceResiliency.getCoupledService() == null
                ? new ComplianceCheckResult(true, "")
                : new ComplianceCheckResult(false, LOG_ATTRIBUTE_NOT_DEDICATED);
    }

    private ServicehandlerServiceResiliencyCheck() {
    }
}
