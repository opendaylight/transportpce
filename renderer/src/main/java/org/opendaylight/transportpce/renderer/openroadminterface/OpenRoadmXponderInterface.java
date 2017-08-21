/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.OchAttributes.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.RateIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmXponderInterface extends OpenRoadmInterfaces {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmXponderInterface.class);
    private final  String serviceName;

    public OpenRoadmXponderInterface(DataBroker db, MountPointService mps, String nodeId, String logicalConnPoint,
        String serviceName) {
        super(db, mps, nodeId, logicalConnPoint,serviceName);
        this.serviceName = serviceName;

    }

    public boolean createLineInterfaces(Long waveNumber, Class<? extends RateIdentity> rate, ModulationFormat format) {

        String supportOchInterface = new OpenRoadmOchInterface(db, mps, nodeId, logicalConnPoint,serviceName)
                .createInterface(waveNumber, rate, format);
        String supportingOtuInterface;
        String supportingOduInterface;

        if (supportOchInterface != null) {
            supportingOtuInterface = new OpenRoadmOtu4Interface(db, mps, nodeId, logicalConnPoint,serviceName)
                    .createInterface(supportOchInterface);
        } else {
            LOG.error("Unable to create OCH interface on the transponder");
            return false;
        }
        if (supportingOtuInterface != null) {
            supportingOduInterface = new OpenRoadmOdu4Interface(db, mps, nodeId, logicalConnPoint,serviceName)
                    .createInterface(supportingOtuInterface);
        } else {
            LOG.error("Unable to create OTU interface on the transponder");
            return false;
        }
        if (supportingOduInterface != null) {
            return true;
        } else {
            LOG.error("Unable to create ODU interface on the transponder");
            return false;
        }
    }

    public boolean createClientInterfaces() {

        if (new OpenRoadmEthIterface(db, mps, nodeId, logicalConnPoint,serviceName).createInterface() != null) {
            return true;
        }
        return false;
    }
}
