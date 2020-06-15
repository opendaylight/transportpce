/*
 * Copyright © 2020 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

public class FlexGridImpl implements FlexGridInterface {
    private double start;
    private double stop;
    static final double PRECISION = 100000d;

    public FlexGridImpl(double start, double stop) {
        this.start = start;
        this.stop = stop;
    }

    public FlexGridImpl() {
        this.start = 0;
        this.stop = 0;
    }

    @Override
    public FlexGridImpl getFlexWaveMapping(float centerFrequency, float slotwidth) {
        FlexGridImpl flexGridImpl = new FlexGridImpl();
        flexGridImpl.start = centerFrequency - (slotwidth / 2) / 1000.0;
        flexGridImpl.start = Math.round(flexGridImpl.start * PRECISION) / PRECISION;
        flexGridImpl.stop = centerFrequency + (slotwidth / 2) / 1000.0;
        flexGridImpl.stop = Math.round(flexGridImpl.stop * PRECISION) / PRECISION;
        return flexGridImpl;
    }

    public double getStart() {
        return start;
    }

    public double getStop() {
        return stop;
    }
}

