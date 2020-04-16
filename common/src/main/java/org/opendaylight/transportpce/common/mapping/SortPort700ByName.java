/*
 * Copyright © 2020 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.io.Serializable;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200327.Port;


/**
 * Class to compare two String containing integer.
 *
 * @author Martial Coulibaly (martial.coulibaly@gfi.com) on behalf of Orange
 *
 */
public class SortPort700ByName implements Comparator<Port>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Port port1, Port port2) {
        return extractInt(port1) - extractInt(port2);
    }

    private int extractInt(Port port) {
        String num = port.getPortName().replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}

