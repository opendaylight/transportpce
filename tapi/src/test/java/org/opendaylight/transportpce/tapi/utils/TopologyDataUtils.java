/*
 * Copyright © 2019 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import edu.umd.cs.findbugs.annotations.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.transportpce.common.converter.XMLDataObjectConverter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev181210.GetTopologyDetailsInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopologyDataUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TopologyDataUtils.class);
    private static final String TOPOLOGY_FILE =
        "src/test/resources/openroadm-topology2.xml";
    private static final String PORTMAPPING_FILE =
        "src/test/resources/portmapping-example.xml";

    public static GetTopologyDetailsInput buildGetTopologyDetailsInput() {
        GetTopologyDetailsInputBuilder builtInput = new GetTopologyDetailsInputBuilder();
        builtInput.setTopologyIdOrName("topo1");
        return builtInput.build();
    }

    public static void writeTopologyFromFileToDatastore(DataStoreContext dataStoreContextUtil) {
        Networks result = null;
        File topoFile = new File(TOPOLOGY_FILE);
        if (topoFile.exists()) {
            String fileName = topoFile.getName();
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(topoFile);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .getDataObject(transformIntoNormalizedNode.get(), Networks.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    result = (Networks) dataObject.get();
                }
            } catch (FileNotFoundException e) {
                LOG.error("File not found : {} at {}", e.getMessage(), e.getLocalizedMessage());
            }
        } else {
            LOG.error("xml file {} not found at {}", topoFile.getName(), topoFile.getAbsolutePath());
        }
        LOG.info("Storing openroadm-topology in datastore");
        InstanceIdentifier<Networks> ietfNetworksIID = InstanceIdentifier.builder(Networks.class).build();
        writeTransaction(dataStoreContextUtil.getDataBroker(), ietfNetworksIID, result);
        LOG.info("openroadm-topology stored with success in datastore");
    }

    private static boolean writeTransaction(DataBroker dataBroker, InstanceIdentifier instanceIdentifier,
        DataObject object) {
        @NonNull
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, instanceIdentifier, object);
        transaction.commit();
        return true;
    }

    public static void writePortmappingFromFileToDatastore(DataStoreContext dataStoreContextUtil) {
        org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev190702.Network result = null;
        File portmappingFile = new File(PORTMAPPING_FILE);
        if (portmappingFile.exists()) {
            String fileName = portmappingFile.getName();
            InputStream targetStream;
            try {
                targetStream = new FileInputStream(portmappingFile);
                Optional<NormalizedNode<?, ?>> transformIntoNormalizedNode = null;
                transformIntoNormalizedNode = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .transformIntoNormalizedNode(targetStream);
                if (!transformIntoNormalizedNode.isPresent()) {
                    throw new IllegalStateException(String.format(
                        "Could not transform the input %s into normalized nodes", fileName));
                }
                Optional<DataObject> dataObject = XMLDataObjectConverter.createWithDataStoreUtil(dataStoreContextUtil)
                    .getDataObject(transformIntoNormalizedNode.get(), org.opendaylight.yang.gen.v1.http.org.opendaylight
                    .transportpce.portmapping.rev190702.Network.QNAME);
                if (!dataObject.isPresent()) {
                    throw new IllegalStateException("Could not transform normalized nodes into data object");
                } else {
                    result = (org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev190702
                        .Network) dataObject.get();
                }
            } catch (FileNotFoundException e) {
                LOG.error("File not found : {} at {}", e.getMessage(), e.getLocalizedMessage());
            }
        } else {
            LOG.error("xml file {} not found at {}", portmappingFile.getName(), portmappingFile.getAbsolutePath());
        }
        InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev190702
            .Network> portmappingIID = InstanceIdentifier.builder(org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.portmapping.rev190702.Network.class).build();
        writeTransaction(dataStoreContextUtil.getDataBroker(), portmappingIID, result);
        LOG.info("portmapping-example stored with success in datastore");
    }

    private TopologyDataUtils() {

    }

}
