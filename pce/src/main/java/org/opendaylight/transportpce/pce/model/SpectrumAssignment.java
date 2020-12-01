/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.model;

public class SpectrumAssignment {
    /**
     * Begin index for available spectrum assignment.
     */
    int beginIndex;

    /**
     * End index for available spectrum assignment.
     */
    int stopIndex;

    /**
     * True if spectrum assignement is for flexible grid, false otherwise (1.2.1 device).
     */
    boolean flexGrid;

    public SpectrumAssignment(int beginIndex, int stopIndex) {
        super();
        this.beginIndex = beginIndex;
        this.stopIndex = stopIndex;
    }

    /**
     * Return the begin index of spectrum assignment.
     * @return the beginIndex
     */
    public int getBeginIndex() {
        return beginIndex;
    }

    /**
     * Set the begin index of spectrum assignment.
     * @param beginIndex the beginIndex to set
     */
    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }

    /**
     * Return the end index of spectrum assignment.
     * @return the stopIndex
     */
    public int getStopIndex() {
        return stopIndex;
    }

    /**
     * Set the stop index of spectrum assignment.
     * @param stopIndex the stopIndex to set
     */
    public void setStopIndex(int stopIndex) {
        this.stopIndex = stopIndex;
    }

    /**
     * True if flexgrid false otherwise.
     * @return the flexGrid
     */
    public boolean isFlexGrid() {
        return flexGrid;
    }

    /**
     * Set it to true for flexgrid, false otherwise.
     * @param flexGrid the flexGrid to set
     */
    public void setFlexGrid(boolean flexGrid) {
        this.flexGrid = flexGrid;
    }

    /*
    * (non-Javadoc)
    *
    * @see java.lang.Object#toString()
    */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectrumAssignment [beginIndex=").append(beginIndex).append(", stopIndex=").append(stopIndex)
                .append(", flexGrid=").append(flexGrid).append("]");
        return builder.toString();
    }
}
