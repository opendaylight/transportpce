/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

public final class FixedFlexImpl implements FixedFlexInterface {
    private long index;
    private double centerFrequency;
    private double start;
    private double stop;
    private double wavelength;

    public FixedFlexImpl(Long index, double centreFrequency, double start, double stop, double wavelength) {
        this.index = index;
        this.centerFrequency = centreFrequency;
        this.start = start;
        this.stop = stop;
        this.wavelength = wavelength;
    }

    public FixedFlexImpl() {
        this.index = 0L;
        this.centerFrequency = 0;
        this.start = 0;
        this.stop = 0;
        this.wavelength = 0;
    }

    public FixedFlexImpl(long wlIndex) {
        this.index = wlIndex;
        this.centerFrequency = 196.1 - (wlIndex - 1) * 0.05;
        this.start = this.centerFrequency - 0.025;
        this.stop = this.centerFrequency + 0.025;
        this.wavelength = 1528.77 + ((wlIndex - 1) * 0.39);
    }

    @Override
    /**
     * @param index Wavelength number
     * @return Returns FixedFlexImp object with the calculated result.
     **/
    public FixedFlexImpl getFixedFlexWaveMapping(long wlIndex) {
        FixedFlexImpl fixedFlex = new FixedFlexImpl();
        fixedFlex.centerFrequency = 196.1 - (wlIndex - 1) * 0.05;
        fixedFlex.wavelength = 1528.77 + ((wlIndex - 1) * 0.39);
        fixedFlex.start = fixedFlex.centerFrequency - 0.025;
        fixedFlex.stop = fixedFlex.centerFrequency + 0.025;
        return fixedFlex;
    }

    public double getCenterFrequency() {
        return centerFrequency;
    }

    public double getStart() {
        return start;
    }

    public long getIndex() {
        return index;
    }

    public double getStop() {
        return stop;
    }

    public double getWavelength() {
        return wavelength;
    }
}
