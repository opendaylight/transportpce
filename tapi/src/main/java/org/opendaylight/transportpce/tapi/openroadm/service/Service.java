/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.service;

public interface Service {

    /**
     * Copies an OpenROADM service to TAPI.
     *
     * @param openRoadmServiceName the name of the OpenROADM service to copy
     * @return true if the service was successfully copied, false otherwise
     */
    boolean copyServiceToTAPI(String openRoadmServiceName);

}
