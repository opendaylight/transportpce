/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test;

import static java.util.Objects.requireNonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.transportpce.test.converter.XMLDataObjectConverter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class around {@link DataBroker} and {@link DOMDataBroker}.
 *
 */
public final class DeviceWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceWrapper.class);

    private final String identifier;
    private final DataBroker dataBroker;
    private final DOMDataBroker domDataBroker;

    /**
     * May be created only inside of {@link AbstractDeviceTest}.
     *
     * @param identifier id of this simulator
     * @param dataBroker data broker used in this simulator
     * @param domDataBroker dom data broker used in this simulator
     */
    private DeviceWrapper(String identifier, DataBroker dataBroker, DOMDataBroker domDataBroker) {
        this.identifier = identifier;
        this.dataBroker = dataBroker;
        this.domDataBroker = domDataBroker;
    }

    /**
     * Gets the data broker.
     *
     * @return the dataBroker
     */
    public DataBroker getDataBroker() {
        return dataBroker;
    }

    /**
     * Gets the DOM data broker.
     *
     * @return the domDataBroker
     */
    public DOMDataBroker getDeviceDomDataBroker() {
        return domDataBroker;
    }

    /**
     * Gets the identifier.
     *
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Creates a device wrapper.
     *
     * @see #createDeviceWrapper(String, List) with a single list element
     * @param key identifier of creating simulator
     * @param initialDataXmlInputStream {@link InputStream} of xml with initial data for simulator
     * @param intialDataQName {@link QName} of initial data
     * @return device simulator
     */
    public static DeviceWrapper createDeviceWrapper(@Nonnull String key, @Nonnull InputStream initialDataXmlInputStream,
            @Nonnull QName intialDataQName) {
        requireNonNull(initialDataXmlInputStream, "Input stream cannot be null");
        requireNonNull(intialDataQName, "QName cannot be null");
        return createDeviceWrapper(key, Lists.newArrayList(
                new AbstractMap.SimpleEntry<QName, InputStream>(intialDataQName, initialDataXmlInputStream)));
    }

    /**
     * Creates an instance of {@link DeviceWrapper} with initial data provided via xml
     * input streams and theirs {@link QName}s the xml data <b>must</b> be wrapped
     * inside a <b>data</b> tag provided by
     * <b>urn:ietf:params:xml:ns:netconf:base:1.0</b>.
     *
     * @param key string identifier for the device
     * @param initialData {@link List} of {@link Entry} values
     * @return created {@link DeviceWrapper} with all initial data provided by initial data
     */
    public static DeviceWrapper createDeviceWrapper(@Nonnull String key,
            @Nonnull List<Entry<QName, InputStream>> initialData) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(key), "The provided key cannot be null or empty");
        Preconditions.checkArgument(initialData != null && !initialData.isEmpty(),
                "Initial data cannot be null or empty");
        DataStoreContext dsContext = new DataStoreContextImpl();
        XMLDataObjectConverter xmlConverter = XMLDataObjectConverter.createWithDataStoreUtil(dsContext);
        for (Entry<QName, InputStream> entryData : initialData) {
            insertDataIntoDS(xmlConverter, entryData.getValue(), entryData.getKey(), dsContext.getDOMDataBroker());
        }
        return new DeviceWrapper(key, dsContext.getDataBroker(), dsContext.getDOMDataBroker());
    }

    private static void insertDataIntoDS(XMLDataObjectConverter xmlConverter, InputStream xmlDataInputStream,
            QName dataQName, DOMDataBroker domDataBroker) {
        Optional<NormalizedNode> initialDataNormalizedNodes =
                xmlConverter.transformIntoNormalizedNode(xmlDataInputStream);
        Preconditions.checkArgument(initialDataNormalizedNodes.isPresent(),
                "Initial data could not be converted to normalized nodes");
        LOG.debug("Input data converted into normalizedNodes");

        YangInstanceIdentifier initialDataIi = YangInstanceIdentifier.of(dataQName);
        LOG.debug("Searching for {} inside {}", initialDataIi, initialDataNormalizedNodes.get());
        Optional<NormalizedNode> dataNormalizedNodes =
                NormalizedNodes.findNode(initialDataNormalizedNodes.get(), initialDataIi);
        Preconditions.checkArgument(dataNormalizedNodes.isPresent());
        LOG.info("Initial data was successfully stored into ds");
        DOMDataTreeWriteTransaction writeOnlyTransaction = domDataBroker.newWriteOnlyTransaction();
        writeOnlyTransaction.put(LogicalDatastoreType.OPERATIONAL, initialDataIi, dataNormalizedNodes.get());
        try {
            writeOnlyTransaction.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("This should be not reached ", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DeviceWrapper [identifier=");
        builder.append(identifier);
        builder.append(", dataBroker=");
        builder.append(dataBroker);
        builder.append(", domDataBroker=");
        builder.append(domDataBroker);
        builder.append("]");
        return builder.toString();
    }
}
