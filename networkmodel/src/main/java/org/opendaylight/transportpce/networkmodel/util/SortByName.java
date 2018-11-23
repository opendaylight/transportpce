/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util;

import java.util.Comparator;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;

/**
 * Class to compare two String containing integer.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class SortByName implements Comparator<Ports> {

    @Override
    public int compare(Ports port1, Ports port2) {
        return extractInt(port1.getPortName()) - extractInt(port2.getPortName());
    }

    int extractInt(String str) {
        String num = str.replaceAll("\\D", "");
        // return 0 if no digits found
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}
