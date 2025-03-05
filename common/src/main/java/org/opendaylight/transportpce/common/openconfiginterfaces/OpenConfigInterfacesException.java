/*
 * Copyright © 2024 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openconfiginterfaces;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("serial")
@SuppressFBWarnings(value = "SE_NO_SERIALVERSIONID", justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class OpenConfigInterfacesException extends Exception {

    private final Set<String> successfulEntities;

    public OpenConfigInterfacesException(String message) {
        super(message);
        this.successfulEntities = new HashSet<String>();
    }

    /**
     * Used during rollback operation.
     * Entities that were successfully configured in NE before hitting exception will be rolled back.
     */

    public OpenConfigInterfacesException(Set<String> successfulEntities, Throwable cause) {
        super(cause);
        this.successfulEntities = successfulEntities;
    }

    public Set<String> getSuccessfulEntities() {
        return successfulEntities;
    }

    public OpenConfigInterfacesException(String message, Throwable cause) {
        super(message, cause);
        this.successfulEntities = new HashSet<String>();

    }
}
