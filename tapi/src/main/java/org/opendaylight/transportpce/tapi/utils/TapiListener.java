/*
 * Copyright © 2018 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.ServiceInterfacePoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.service._interface.points.ServiceEndPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiListener implements DataTreeChangeListener<ServiceInterfacePoints> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiListener.class);

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<ServiceInterfacePoints>> changes) {
        LOG.info("onDataTreeChanged in TapiListener");
        for (DataTreeModification<ServiceInterfacePoints> change : changes) {
            DataObjectModification<ServiceInterfacePoints> rootSIP = change.getRootNode();
            switch (rootSIP.getModificationType()) {
                case WRITE:
                    LOG.info("onDataTreeChanged in TapiListener : WRITE");
                    ServiceInterfacePoints data = rootSIP.getDataAfter();
                    List<ServiceEndPoint> listSEP = new ArrayList<>(data.getServiceEndPoint().values());
                    MappingUtils.deleteMap();
                    for (ServiceEndPoint sep : listSEP) {
                        MappingUtils.addMapSEP(sep);
                    }
                    MappingUtils.afficheMap();
                    break;
                case SUBTREE_MODIFIED:
                    LOG.info("onDataTreeChanged in TapiListener : SUBTREE_MODIFIED");
                    Iterator<? extends DataObjectModification<? extends DataObject>> iterator = rootSIP
                        .getModifiedChildren().iterator();
                    while (iterator.hasNext()) {
                        DataObjectModification<? extends DataObject> dom = iterator.next();
                        // to delete existing child entry
                        if (dom.getDataAfter() == null) {
                            DataObject dataObject = dom.getDataBefore();
                            ServiceEndPoint sep = null;
                            sep = (ServiceEndPoint) dataObject;
                            Uuid uuid = sep.getUuid();
                            MappingUtils.deleteMapEntry(uuid);
                            MappingUtils.afficheMap();
                            break;
                        }

                        // to add new child entry
                        if (dom.getDataType().toString().compareTo("interface org.opendaylight.yang.gen.v1.urn.opendayl"
                            + "ight.params.xml.ns.yang.tapi.rev180928.service._interface.points.ServiceEndPoint") == 0
                            && dom.getDataBefore() == null) {
                            DataObject dataObject = dom.getDataAfter();
                            ServiceEndPoint sep = null;
                            sep = (ServiceEndPoint) dataObject;
                            MappingUtils.addMapSEP(sep);
                            MappingUtils.afficheMap();
                        } else {
                            LOG.error("data input type is not a valid 'service-end-point'");
                        }
                        break;
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
