/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

/**
 * The Class OtsPmHolder.
 */
public class OtsPmHolder {

    /** The ots parameter name.*/
    private String otsParameterName;
    /** The ots parameter value.*/
    private Double otsParameterVal;

    /** The ots interface name.*/
    private String otsInterfaceName;

    public OtsPmHolder(String otsParameterName,Double otsParameterVal,String otsInterfaceName) {
        this.otsParameterName = otsParameterName;
        this.otsParameterVal = otsParameterVal;
        this.otsInterfaceName = otsInterfaceName;
    }

    public String getOtsParameterName() {
        return otsParameterName;
    }

    public void setOtsParameterName(String otsParameterName) {
        this.otsParameterName = otsParameterName;
    }

    public Double getOtsParameterVal() {
        return otsParameterVal;
    }

    public void setOtsParameterVal(Double otsParameterVal) {
        this.otsParameterVal = otsParameterVal;
    }

    public String getOtsInterfaceName() {
        return otsInterfaceName;
    }

    public void setOtsInterfaceName(String otsInterfaceName) {
        this.otsInterfaceName = otsInterfaceName;
    }

}
