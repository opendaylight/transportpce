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
import org.opendaylight.yang.gen.v1.http.openconfig.net.yang.terminal.device.rev170708.terminal.device.top.TerminalDevice;
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
final class TerminalDeviceChangeListener implements DataTreeChangeListener<TerminalDevice> {

    private static final Logger LOG = LoggerFactory.getLogger(TerminalDeviceChangeListener.class);
    private final DataBroker dataBroker;

    public TerminalDeviceChangeListener(DataBroker deviceDataBroker) {
        this.dataBroker = deviceDataBroker;
        Preconditions.checkArgument(this.dataBroker != null, "terminal-device datastore is null");
    }


    /**
     * Delete change from terminal-device
     * oper datastore.
     *
     * @param id container identifier
     */
    private void deleteContainer(DataTreeIdentifier<TerminalDevice> rootPath,
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
     * @param id TerminalDevice identifier
     * @param dataAfter TerminalDevice to be merged
     */
    private void processChange(final InstanceIdentifier<TerminalDevice> id,
            DataObjectModification<? extends DataObject> modified, final TerminalDevice dataAfter) {
        LOG.info("processing change ...");
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        if (writeTx != null) {
            LOG.info("WriteTransactions are ok, merge terminal-device data to datastores");
            if(dataAfter != null) {
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, id, dataAfter);
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    future.get();
                    LOG.info("terminal-device merged to terminal-device oper datastore");
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to merge terminal-device to datastores");
                }
            } else {
                LOG.error("terminal-device is null");
            }
        } else {
            LOG.error("WriteTransaction object is null");
        }
    }

@Override
public void onDataTreeChanged(@NonNull Collection<DataTreeModification<TerminalDevice>> changes) {
  LOG.info("onDataTreeChanged for terminal-device");
        for (DataTreeModification<TerminalDevice> change : changes) {
            final DataObjectModification<TerminalDevice> rootNode = change.getRootNode();
            final DataTreeIdentifier<TerminalDevice> rootPath = change.getRootPath();
            if (rootNode != null ) {
                final TerminalDevice dataBefore = rootNode.getDataBefore();
                final TerminalDevice dataAfter = rootNode.getDataAfter();
                LOG.info("Received terminal-device change({}):\n before={} \n after={}", rootNode.getModificationType(), dataBefore,
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
                        LOG.info("terminal-device config datastore is deleted !");
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
