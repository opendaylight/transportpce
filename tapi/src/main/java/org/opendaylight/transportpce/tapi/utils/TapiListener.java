/*
 * Copyright © 2018 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.service._interface.points.ServiceEndPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiListener implements DataTreeChangeListener<ServiceInterfacePoints> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiListener.class);

    @Override
    public void onDataTreeChanged(@NonNull List<DataTreeModification<ServiceInterfacePoints>> changes) {
        LOG.info("onDataTreeChanged in TapiListener");
        for (DataTreeModification<ServiceInterfacePoints> change : changes) {
            DataObjectModification<ServiceInterfacePoints> rootSIP = change.getRootNode();
            switch (rootSIP.modificationType()) {
                case WRITE:
                    LOG.info("onDataTreeChanged in TapiListener : WRITE");
                    MappingUtils.deleteMap();
                    for (ServiceEndPoint sep : rootSIP.dataAfter().getServiceEndPoint().values()) {
                        MappingUtils.addMapSEP(sep);
                    }
                    MappingUtils.afficheMap();
                    break;
                case SUBTREE_MODIFIED:
                    LOG.info("onDataTreeChanged in TapiListener : SUBTREE_MODIFIED");
                    Iterator<? extends DataObjectModification<? extends DataObject>> iterator =
                        rootSIP.getModifiedChildren(ServiceEndPoint.class).iterator();
                    if (iterator.hasNext()) {
                        DataObjectModification<? extends DataObject> dom = iterator.next();
                        // to delete existing child entry
                        DataObject dataAfter = dom.dataAfter();
                        if (dataAfter == null) {
                            MappingUtils.deleteMapEntry(((ServiceEndPoint) dom.dataBefore()).getUuid());
                            MappingUtils.afficheMap();
                            break;
                        }

                        // to add new child entry
                        if (dom.dataBefore() != null || dom.dataType().toString().compareTo(
                            "interface org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928"
                                    + ".service._interface.points.ServiceEndPoint") != 0) {
                            LOG.error("data input type is not a valid 'service-end-point'");
                            break;
                        }
                        MappingUtils.addMapSEP((ServiceEndPoint) dataAfter);
                        MappingUtils.afficheMap();
                    }
                    break;
                case DELETE:
                    LOG.info("onDataTreeChanged in TapiListener : DELETE");
                    MappingUtils.deleteMap();
                    break;
                default:
                    LOG.error("Error of API REST modification type");
                    break;
            }

        }
    }

}
