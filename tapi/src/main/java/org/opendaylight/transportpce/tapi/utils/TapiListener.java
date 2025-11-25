/*
 * Copyright © 2018 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.List;
import java.util.Map;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectDeleted;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataObjectWritten;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.service._interface.points.ServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.service._interface.points.ServiceEndPointKey;
import org.opendaylight.yangtools.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiListener implements DataTreeChangeListener<ServiceInterfacePoints> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiListener.class);
    private static final String SE_JAVA_INTF =
        "interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928"
            + ".service._interface.points.ServiceEndPoint";

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<ServiceInterfacePoints>> changes) {
        LOG.info("onDataTreeChanged in TapiListener");
        for (DataTreeModification<ServiceInterfacePoints> change : changes) {
            DataObjectModification<ServiceInterfacePoints> rootSIP = change.getRootNode();
            switch (rootSIP) {
                case DataObjectWritten<ServiceInterfacePoints> written -> {
                    LOG.info("onDataTreeChanged in TapiListener : WRITE");
                    MappingUtils.deleteMap();
                    for (ServiceEndPoint sep : written.dataAfter().getServiceEndPoint().values()) {
                        MappingUtils.addMapSEP(sep);
                    }
                    MappingUtils.afficheMap();
                }
                case DataObjectModification.WithDataAfter<ServiceInterfacePoints> present -> {
                    LOG.info("onDataTreeChanged in TapiListener : SUBTREE_MODIFIED");

                    Map<ServiceEndPointKey, ServiceEndPoint> sepBefore = present.dataBefore().nonnullServiceEndPoint();
                    Map<ServiceEndPointKey, ServiceEndPoint> sepAfter = present.dataAfter().nonnullServiceEndPoint();
                    for (DataObjectModification<? extends DataObject> sep :
                            rootSIP.getModifiedChildren(ServiceEndPoint.class)) {
                        ServiceEndPointKey sepUuid = sep.coerceKeyStep(ServiceEndPoint.class).key();
                        // to delete existing child entry
                        if (sepBefore.containsKey(sepUuid) && !sepAfter.containsKey(sepUuid)) {
                            MappingUtils.deleteMapEntry(sepUuid.getUuid());
                            MappingUtils.afficheMap();
                            continue;
                        }
                        // to add new child entry
                        if (!sepBefore.containsKey(sepUuid) && sepAfter.containsKey(sepUuid)) {
                            MappingUtils.addMapSEP((ServiceEndPoint) sep);
                            MappingUtils.afficheMap();
                        }
                    }
                }
                case DataObjectDeleted<ServiceInterfacePoints> ignored -> {
                    LOG.info("onDataTreeChanged in TapiListener : DELETE");
                    MappingUtils.deleteMap();
                }
            }
        }
    }

}
