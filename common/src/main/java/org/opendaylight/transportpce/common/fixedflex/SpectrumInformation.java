/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.fixedflex;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes.ModulationFormat;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Spectrum information for cross connect and openroadm interfaces management.
 */
public class SpectrumInformation {
    private Uint32 waveLength;
    private int lowerSpectralSlotNumber;
    private int higherSpectralSlotNumber;
    private Decimal64 width = GridConstant.WIDTH_40;
    private BigDecimal centerFrequency;
    private BigDecimal minFrequency;
    private BigDecimal maxFrequency;
    private String modulationFormat = ModulationFormat.DpQpsk.name();
    private int scale = GridConstant.FIXED_GRID_FREQUENCY_PRECISION;

    public String getSpectralSlotFormat() {
        return String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber),
                String.valueOf(higherSpectralSlotNumber));
    }

    public String getIdentifierFromParams(String...params) {
        String joinedParams = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, params);
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, joinedParams, getSpectralSlotFormat());
    }

    /**
     * Get the wavelength.
     * @return the waveLength
     */
    public Uint32 getWaveLength() {
        return waveLength;
    }

    /**
     * Set the wavelength.
     * @param waveLength the waveLength to set
     */
    public void setWaveLength(Uint32 waveLength) {
        this.waveLength = waveLength;
        if (waveLength == null || waveLength.longValue() == GridConstant.IRRELEVANT_WAVELENGTH_NUMBER) {
            scale = GridConstant.FLEX_GRID_FREQUENCY_PRECISION;
        }
    }

    /**
     * Get the lower spectral slot number.
     * @return the lowerSpectralSlotNumber
     */
    public int getLowerSpectralSlotNumber() {
        return lowerSpectralSlotNumber;
    }

    /**
     * Set the lower spectral slot number.
     * @param lowerSpectralSlotNumber the lowerSpectralSlotNumber to set
     */
    public void setLowerSpectralSlotNumber(int lowerSpectralSlotNumber) {
        this.lowerSpectralSlotNumber = lowerSpectralSlotNumber;
    }

    /**
     * Get the higher spectral slot number.
     * @return the higherSpectralSlotNumber
     */
    public int getHigherSpectralSlotNumber() {
        return higherSpectralSlotNumber;
    }

    /**
     * Set the higher spectral slot number.
     * @param higherSpectralSlotNumber the higherSpectralSlotNumber to set
     */
    public void setHigherSpectralSlotNumber(int higherSpectralSlotNumber) {
        this.higherSpectralSlotNumber = higherSpectralSlotNumber;
    }

    /**
     * Get the frequency width.
     * @return the width
     */
    public Decimal64 getWidth() {
        return width;
    }

    /**
     * Set the frequency width.
     * @param width the width to set
     */
    public void setWidth(Decimal64 width) {
        this.width = width;
    }

    /**
     * Get center frequency.
     * @return the centerFrequency
     */
    public BigDecimal getCenterFrequency() {
        return centerFrequency;
    }

    /**
     * Set center frequency.
     * @param centerFrequency the centerFrequency to set
     */
    public void setCenterFrequency(BigDecimal centerFrequency) {
        this.centerFrequency = centerFrequency.setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Get min frequency.
     * @return the minFrequency
     */
    public BigDecimal getMinFrequency() {
        return minFrequency;
    }

    /**
     * Set min frequency.
     * @param minFrequency the minFrequency to set
     */
    public void setMinFrequency(BigDecimal minFrequency) {
        this.minFrequency = minFrequency.setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Get max frequency.
     * @return the maxFrequency
     */
    public BigDecimal getMaxFrequency() {
        return maxFrequency;
    }

    /**
     * Set max frequency.
     * @param maxFrequency the maxFrequency to set
     */
    public void setMaxFrequency(BigDecimal maxFrequency) {
        this.maxFrequency = maxFrequency.setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * Get the modulation format.
     * @return the modulationFormat
     */
    public String getModulationFormat() {
        return modulationFormat;
    }

    /**
     * Set the modulation format.
     * @param modulationFormat the modulationFormat to set
     */
    public void setModulationFormat(String modulationFormat) {
        this.modulationFormat = modulationFormat;
    }

    /*
    * (non-Javadoc)
    *
    * @see java.lang.Object#toString()
    */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectrumInformation [waveLength=").append(waveLength).append(", lowerSpectralSlotNumber=")
                .append(lowerSpectralSlotNumber).append(", higherSpectralSlotNumber=").append(higherSpectralSlotNumber)
                .append(", width=").append(width).append(", centerFrequency=").append(centerFrequency)
                .append(", minFrequency=").append(minFrequency).append(", maxFrequency=").append(maxFrequency)
                .append(", modulationFormat=").append(modulationFormat).append(", scale=").append(scale).append("]");
        return builder.toString();
    }
}
