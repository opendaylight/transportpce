/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.utils;

import com.google.common.util.concurrent.FluentFuture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.test.DataStoreContext;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250905.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Context1;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyDataUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataUtils.class);

    @SuppressWarnings("rawtypes")
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    public static <T> void writeTopologyFromFileToDatastore(DataStoreContext dataStoreContextUtil, String file,
            DataObjectIdentifier ii) throws InterruptedException, ExecutionException {
        Networks networks = null;
        Path path = Path.of(file);
        if (Files.exists(path)) {
            String fileName = path.getFileName().toString();
            try (InputStream inputStream = Files.newInputStream(path)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = XMLDataObjectConverter
                        .createWithDataStoreUtil(dataStoreContextUtil).transformIntoNormalizedNode(inputStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.orElseThrow(), Networks.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    networks = (Networks) dataObject.orElseThrow();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", file, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", path.getFileName(), path.toAbsolutePath());
        }
        if (networks == null) {
            throw new IllegalStateException("Network is null cannot write it to datastore");
        }
        FluentFuture<? extends CommitInfo> commitFuture = writeTransaction(dataStoreContextUtil.getDataBroker(), ii,
                networks.nonnullNetwork().values().stream().findFirst().orElseThrow());
        commitFuture.get();
        LOG.info("extraction from {} stored with success in datastore", path.getFileName());
    }

    public static <T> void writeTapiTopologyFromFileToDatastore(DataStoreContext dataStoreContextUtil, String file,
            DataObjectIdentifier ii) throws InterruptedException, ExecutionException {
        Context context = null;
        Path path = Path.of(file);
        if (Files.exists(path)) {
            String fileName = path.getFileName().toString();
            LOG.info("filename = {}", fileName);
            try (InputStream inputStream = Files.newInputStream(path)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = XMLDataObjectConverter
                        .createWithDataStoreUtil(dataStoreContextUtil).transformIntoNormalizedNode(inputStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.orElseThrow(), Context.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    context = (Context) dataObject.orElseThrow();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", file, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", path.getFileName(), path.toAbsolutePath());
        }
        if (context == null) {
            throw new IllegalStateException("Network is null cannot write it to datastore");
        }
        FluentFuture<? extends CommitInfo> commitFuture = writeOperTransaction(dataStoreContextUtil.getDataBroker(), ii,
                context.augmentation(Context1.class).nonnullTopologyContext().getTopology().values().stream()
                .filter(tp -> tp.getUuid().getValue().equals("393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7"))
                .findFirst().orElseThrow());
        commitFuture.get();
        LOG.info("extraction from {} stored with success in datastore", path.getFileName());
    }

    public static <T> DataObject readTapiTopologyFromDatastore(DataStoreContext dataStoreContextUtil,
            DataObjectIdentifier ii) throws InterruptedException, ExecutionException {
        return readTransaction(dataStoreContextUtil.getDataBroker(), ii);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    private static FluentFuture<? extends CommitInfo> writeTransaction(DataBroker dataBroker,
            DataObjectIdentifier instanceIdentifier, DataObject object) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        return transaction.commit();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    private static FluentFuture<? extends CommitInfo> writeOperTransaction(DataBroker dataBroker,
            DataObjectIdentifier instanceIdentifier, DataObject object) {
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.OPERATIONAL, instanceIdentifier, object);
        return transaction.commit();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    private static <T extends DataObject> T readTransaction(DataBroker dataBroker,
            DataObjectIdentifier instanceIdentifier) {
        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        FluentFuture<Optional<T>> read = readTransaction.read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
        if (read.isDone()) {
            try {
                Optional<T> tpOpt;
                tpOpt = read.get();
                if (tpOpt.isPresent()) {
                    return tpOpt.orElseThrow();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Impossible read operational datastore", e);
            }
        }
        return null;
    }

    public static void writePortmappingFromFileToDatastore(DataStoreContext dataStoreContextUtil, String file)
            throws InterruptedException, ExecutionException {
        Network result = null;
        Path path = Path.of(file);
        if (Files.exists(path)) {
            String fileName = path.getFileName().toString();
            try (InputStream inputStream = Files.newInputStream(path)) {
                Optional<NormalizedNode> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .transformIntoNormalizedNode(inputStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                        .getDataObject(transformIntoNormalizedNode.orElseThrow(), Network.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    result = (Network) dataObject.orElseThrow();
                }
            } catch (IOException e) {
                LOG.error("An error occured while reading file {}", file, e);
            }
        } else {
            LOG.error("xml file {} not found at {}", path.getFileName(), path.toAbsolutePath());
        }
        DataObjectIdentifier<Network> portmappingIID = DataObjectIdentifier.builder(Network.class).build();
        FluentFuture<? extends CommitInfo> writeTransaction = writeTransaction(dataStoreContextUtil.getDataBroker(),
                portmappingIID, result);
        writeTransaction.get();
        LOG.info("portmapping-example stored with success in datastore");
    }

    private TopologyDataUtils() {
    }

}
