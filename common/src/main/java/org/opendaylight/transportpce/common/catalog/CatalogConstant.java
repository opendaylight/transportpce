/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.catalog;



/**
 * Constant class common to fix constants used in catalog,
 * and impairment calculation.
 */


public final class CatalogConstant {

    public static final String ORW100GSC = "OR-W-100G-SC";
    public static final String ORW100GOFEC316GBD = "OR-W-100G-oFEC-31.6Gbd";
    public static final String ORW200GOFEC316GBD = "OR-W-200G-oFEC-31.6Gbd";
    public static final String ORW200GOFEC631GBD = "OR-W-200G-oFEC-63.1Gbd";
    public static final String ORW300GOFEC631GBD = "OR-W-300G-oFEC-63.1Gbd";
    public static final String ORW400GOFEC631GBD = "OR-W-400G-oFEC-63.1Gbd";
    public static final String MWWRCORE = "MW-WR-core";
    public static final String MWMWCORE = "MW-MW-core";
    public static final String MWISTANDARD = "MWi-standard";
    public static final String MWILOWNOISE = "MWi-low-noise";
    public static final double NLCONSTANTC1 = -2.0;
    public static final double NLCONSTANTC0UPTO875 = 43.4;
    public static final double NLCONSTANTC0UPTO1000 = 45.4;
    public static final double NLCONSTANTC0UPTO1125 = 46.1;
    public static final double NLCONSTANTC0UPTO1625 = 48.6;
    public static final double NLCONSTANTC0GT1625 = 60.0;
    public static final double NLCONSTANTCE = 11.33;
    public static final double NLCONSTANTEX = -0.09;

    public enum CatalogNodeType { ADD, DROP, EXPRESS, AMP, TSP }

    private CatalogConstant() {
    }
}
