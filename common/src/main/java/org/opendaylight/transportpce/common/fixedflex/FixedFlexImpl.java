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
        long mappedWL = wlIndex - 36;
        this.centerFrequency = 193.1 + (50.0 / 1000.0) * mappedWL;
        this.start = 193.1 + (50.0 * mappedWL - 25) / 1000.0;
        this.stop = 193.1 + (50.0 * mappedWL + 25) / 1000.0;
        this.wavelength = 1528.77 + ((wlIndex - 1) * 0.39);
    }

    @Override
    /**
     * @param index Wavelength number
     * @return Returns FixedFlexImp object with the calculated result.
     **/
    public FixedFlexImpl getFixedFlexWaveMapping(long wlIndex) {
        FixedFlexImpl fixedFlex = new FixedFlexImpl();
        long mappedWL = wlIndex - 36;
        fixedFlex.centerFrequency = 193.1 + (50.0 / 1000.0) * mappedWL;
        fixedFlex.start = 193.1 + (50.0 * mappedWL - 25) / 1000.0;
        fixedFlex.stop = 193.1 + (50.0 * mappedWL + 25) / 1000.0;
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
