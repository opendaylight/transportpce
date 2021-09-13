/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Sort Ports by Name.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class SortPortsByName implements Comparator<String>, Serializable {

    @Override
    public int compare(String port1, String port2) {
        int num = extractInt(port1) - extractInt(port2);
        int letter = extractString(port1).compareToIgnoreCase(extractString(port2));
        int diff = port1.length() - port2.length();
        if ((diff == 0) || (Math.abs(diff) == 1)) {
            return num;
        } else {
            return letter;
        }
    }

    int extractInt(String name) {
        String num = name.replaceAll("\\D", "");
        // return 0 if no digits found
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    String extractString(String name) {
        String letter = name.replaceAll("\\d", "");
        return (letter != null) ? letter : "";
    }
}

