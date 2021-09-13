/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.io.Serializable;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.Port;


/**
 * Class to compare two String containing integer.
 *
 * @author Martial Coulibaly (martial.coulibaly@gfi.com) on behalf of Orange
 *
 */
@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class SortPort221ByName implements Comparator<Port>, Serializable {

    @Override
    public int compare(Port port1, Port port2) {
        return extractInt(port1) - extractInt(port2);
    }

    private int extractInt(Port port) {
        String num = port.getPortName().replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}
