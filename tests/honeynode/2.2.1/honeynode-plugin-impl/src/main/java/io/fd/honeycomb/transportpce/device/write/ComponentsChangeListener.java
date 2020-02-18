/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.write;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.platform.rev180130.platform.component.top.Components;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
final class ComponentsChangeListener implements DataTreeChangeListener<Components> {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentsChangeListener.class);
    private final DataBroker dataBroker;

    public ComponentsChangeListener(DataBroker deviceDataBroker) {
        this.dataBroker = deviceDataBroker;
        Preconditions.checkArgument(this.dataBroker != null, "Device datastore is null");
    }


    /**
     * Delete change from device
     * oper datastore.
     *
     * @param id container identifier
     */
    private void deleteContainer(DataTreeIdentifier<Components> rootPath,
            DataObjectModification<? extends DataObject> modified) {
        Class<? extends DataObject> type = modified.getDataType();
        PathArgument path = modified.getIdentifier();
        LOG.info("deleting container type '{}' with id '{}' ...", type.toString(), path);
    }


    /**
     * Merge change to Honeycomb
     * config datastore and device
     * config datastore.
     *
     * @param id OrgOpenroadmDevice identifier
     * @param dataAfter OrgOpenroadmDevice to be merged
     */
    private void processChange(final InstanceIdentifier<Components> id,
            DataObjectModification<? extends DataObject> modified, final Components dataAfter) {
        LOG.info("processing change ...");
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        if (writeTx != null) {
            LOG.info("WriteTransactions are ok, merge device info to datastores");
            if(dataAfter != null) {
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, id, dataAfter);
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    future.get();
                    LOG.info("components merged to components oper datastore");
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to merge components to datastores");
                }
            } else {
                LOG.error("components is null");
            }
        } else {
            LOG.error("WriteTransaction object is null");
        }
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Components>> changes) {
         LOG.info("onDataTreeChanged for Components");
            for (DataTreeModification<Components> change : changes) {
                final DataObjectModification<Components> rootNode = change.getRootNode();
                final DataTreeIdentifier<Components> rootPath = change.getRootPath();
                if (rootNode != null ) {
                    final Components dataBefore = rootNode.getDataBefore();
                    final Components dataAfter = rootNode.getDataAfter();
                    LOG.info("Received Components change({}):\n before={} \n after={}", rootNode.getModificationType(), dataBefore,
                            dataAfter);
                    Collection<? extends DataObjectModification<? extends DataObject>> modifiedChildren = rootNode.getModifiedChildren();
                    switch (rootNode.getModificationType()) {
                        case SUBTREE_MODIFIED:
                            if (!modifiedChildren.isEmpty()) {
                                Iterator<? extends DataObjectModification<? extends DataObject>> iterator = modifiedChildren.iterator();
                                while (iterator.hasNext()) {
                                    DataObjectModification<? extends DataObject> modified = iterator.next();
                                    LOG.info("modified = \ndataType : {}\nid : {}\nmodifiedType : {}\noldData : {}\nnewData : {} \n",
                                            modified.getDataType(), modified.getIdentifier(),modified.getModificationType(),
                                            modified.getDataBefore(), modified.getDataAfter());
                                    switch (modified.getModificationType()) {
                                      case SUBTREE_MODIFIED:
                                      case WRITE :
                                          processChange(rootPath.getRootIdentifier(), modified, dataAfter);
                                          break;
                                      case DELETE:
                                          deleteContainer(rootPath, modified);
                                          break;
                                      default:
                                          break;
                                   }
                                }
                            }
                            //processChange(rootPath.getRootIdentifier(), dataAfter);
                            break;
                        case WRITE :
                            processChange(rootPath.getRootIdentifier(), null, dataAfter);
                            break;
                        case DELETE:
                            LOG.info("device config datastore is deleted !");
                            break;
                        default:
                            break;
                    }
                } else {
                    LOG.error("rootNode is null !");
                }
            }

    }


}
