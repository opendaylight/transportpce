/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.executor;

public enum ThreadPoolName {
    OLM_GET_PM("olm-get-pm"),
    SERVICE_PATH("service-path"),
    OTN_SERVICE_PATH("otn-service-path");

    private final String name;

    ThreadPoolName(String name) {
        this.name = name;
    }

    public String poolName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
