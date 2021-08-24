/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.utils;

import com.google.common.util.concurrent.FluentFuture;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyDataUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataUtils.class);

    @SuppressWarnings({"unchecked","rawtypes"})
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    public static <T> void writeTopologyFromFileToDatastore(DataStoreContext dataStoreContextUtil, String file,
        InstanceIdentifier ii) throws InterruptedException, ExecutionException {
        Networks networks = null;
        File topoFile = new File(file);
        if (topoFile.exists()) {
            String fileName = topoFile.getName();
            try (InputStream targetStream = new FileInputStream(topoFile)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = XMLDataObjectConverter
                        .createWithDataStoreUtil(dataStoreContextUtil).transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .getDataObject(transformIntoNormalizedNode.get(), Networks.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    networks = (Networks) dataObject.get();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", file, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", topoFile.getName(), topoFile.getAbsolutePath());
        }
        if (networks == null) {
            throw new IllegalStateException("Network is null cannot write it to datastore");
        }
        FluentFuture<? extends CommitInfo> commitFuture = writeTransaction(dataStoreContextUtil.getDataBroker(), ii,
                networks.nonnullNetwork().values().stream().findFirst().get());
        commitFuture.get();
        LOG.info("extraction from {} stored with success in datastore", topoFile.getName());
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    private static FluentFuture<? extends CommitInfo> writeTransaction(DataBroker dataBroker,
        InstanceIdentifier instanceIdentifier, DataObject object) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        return transaction.commit();
    }

    public static void writePortmappingFromFileToDatastore(DataStoreContext dataStoreContextUtil, String file)
        throws InterruptedException, ExecutionException {
        Network result = null;
        File portmappingFile = new File(file);
        if (portmappingFile.exists()) {
            String fileName = portmappingFile.getName();
            try (InputStream targetStream = new FileInputStream(portmappingFile)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .getDataObject(transformIntoNormalizedNode.get(), Network.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    result = (Network) dataObject.get();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", file, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", portmappingFile.getName(), portmappingFile.getAbsolutePath());
        }
        InstanceIdentifier<Network> portmappingIID = InstanceIdentifier.builder(Network.class).build();
        FluentFuture<? extends CommitInfo> writeTransaction = writeTransaction(dataStoreContextUtil.getDataBroker(),
            portmappingIID, result);
        writeTransaction.get();
        LOG.info("portmapping-example stored with success in datastore");
    }

    private TopologyDataUtils() {
    }

}
