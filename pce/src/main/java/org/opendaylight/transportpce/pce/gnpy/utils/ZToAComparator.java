/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy.utils;

import java.io.Serializable;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev200629.path.description.ztoa.direction.ZToA;

public class ZToAComparator implements Comparator<ZToA>, Serializable {

    private static final long serialVersionUID = -2706122285035800936L;

    @Override
    public int compare(ZToA o1, ZToA o2) {
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) {
            return 1;
        } else if (o2 == null) {
            return -1;
        } else if (o1.getId() == null && o2.getId() == null) {
            return 0;
        } else if (o1.getId() == null) {
            return 1;
        } else if (o2.getId() == null) {
            return -1;
        } else {
            return Integer.valueOf(o1.getId()).compareTo(Integer.valueOf(o2.getId()));
        }
    }

}
