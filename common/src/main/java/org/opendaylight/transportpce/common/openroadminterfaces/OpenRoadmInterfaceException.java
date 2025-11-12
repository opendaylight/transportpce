/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressWarnings("serial")
@SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class OpenRoadmInterfaceException extends Exception {

    public OpenRoadmInterfaceException(String message) {
        super(message);
    }

    public OpenRoadmInterfaceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final String mapping_msg_err(String node, String port) {
        return String.format(
            "Unable to get mapping from PortMapping for node %s and logical connection port %s", node, port);
    }

    public static final String mapping_xpdrtype_err(String node, String port) {
        return String.format(
                "Unable to get XpdrType from PortMapping for node %s and logical connection port %s", node, port);
    }
}
