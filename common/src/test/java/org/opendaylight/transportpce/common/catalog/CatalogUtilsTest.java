/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.catalog;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OperationalModeCatalog;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogUtilsTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogUtilsTest.class);
    private static final String CATALOG_FILE = "src/test/resources/apidocCatalog12_0-OptSpecV5_1.json";

    private static OperationalModeCatalog omCatalog;
    private static Map<String, Double> outputImpairments = new HashMap<>();

    @BeforeAll
    static void setUp() throws InterruptedException,
        ExecutionException {
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
            .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader(CATALOG_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                .transformIntoNormalizedNode(reader).orElseThrow();
            omCatalog = (OperationalModeCatalog) getDataStoreContextUtil()
                .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                    .of(OperationalModeCatalog.QNAME), normalizedNode)
                .getValue();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = getDataBroker().newWriteOnlyTransaction();
            newWriteOnlyTransaction
                .put(LogicalDatastoreType.CONFIGURATION,
                    DataObjectIdentifier.builder(OperationalModeCatalog.class).build(),
                    omCatalog);
            newWriteOnlyTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load OpenROADM part of Operational Mode Catalog ", e);
            fail("Cannot load openROADM operational modes ");
        }
    }

    @Test
    void catalogPrimitivesTest() {
        NetworkTransactionService netTransServ = new NetworkTransactionImpl(getDataBroker());
        CatalogUtils catalogUtils = new CatalogUtils(netTransServ);
        assertEquals(
            CatalogConstant.MWWRCORE,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.ADD,
                StringConstants.SERVICE_TYPE_100GE_T),
            "Checking retrieval of Operational Mode from Node Type ADD");
        assertEquals(
            CatalogConstant.MWWRCORE,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.DROP,
                StringConstants.SERVICE_TYPE_100GE_T),
            "Checking retrieval of Operational Mode from Node Type DROP");
        assertEquals(
            CatalogConstant.MWMWCORE,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.EXPRESS,
                StringConstants.SERVICE_TYPE_100GE_T),
            "Checking retrieval of Operational Mode from Node Type EXPRESS");
        assertEquals(
            CatalogConstant.MWISTANDARD,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.AMP,
                StringConstants.SERVICE_TYPE_100GE_T),
            "Checking retrieval of Operational Mode from Node Type AMP");
        assertEquals(
            CatalogConstant.ORW100GSC,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_100GE_T),
            "Checking retrieval of Operational Mode from Node Type and service Type 100GE");
        assertEquals(
            CatalogConstant.ORW100GSC,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTU4),
            "Checking retrieval of Operational Mode from Node Type and service Type OTU4");
        assertEquals(
            CatalogConstant.ORW200GOFEC316GBD,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC2),
            "Checking retrieval of Operational Mode from Node Type and service Type OTUC2");
        assertEquals(
            CatalogConstant.ORW300GOFEC631GBD,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC3),
            "Checking retrieval of Operational Mode from Node Type and service Type OTUC3");
        assertEquals(
            CatalogConstant.ORW400GOFEC631GBD,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_400GE),
            "Checking retrieval of Operational Mode from Node Type and service Type 400GE");
        assertEquals(
            CatalogConstant.ORW400GOFEC631GBD,
            catalogUtils.getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC4),
            "Checking retrieval of Operational Mode from Node Type and service Type OTUC4");
        assertEquals(
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW100GSC),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 100G SC FEC");
        assertEquals(
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW100GOFEC316GBD),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 100G OFEC 31.6");
        assertEquals(
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW200GOFEC316GBD),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 200G OFEC 31.6");
        assertEquals(
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW200GOFEC631GBD),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 200G OFEC 63.1");
        assertEquals(
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW300GOFEC631GBD),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 300G OFEC 63.1 GBd");
        assertEquals(
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW400GOFEC631GBD),
            0.005,
            "Checking retrieval of channel spacing from Operational Mode 400G OFEC 63.1 Gbd");
        assertEquals(
            1345.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW100GSC, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 100GSCFEC ONSR Lin");
        assertEquals(
            450.7,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW100GOFEC316GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 100G OFEC 31.6 Gbauds ONSR Lin");
        assertEquals(
            450.7,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW200GOFEC316GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 200G OFEC 31.6 Gbauds ONSR Lin");
        assertEquals(
            450.7,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW200GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 200G OFEC 63.1 Gbauds ONSR Lin");
        assertEquals(
            450.7,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW300GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 300G OFEC 63.1 Gbauds ONSR Lin");
        assertEquals(
            450.7,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW400GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5,
            "Checking 400G OFEC 63.1 Gbauds ONSR Lin");
        assertEquals(
            0.0, catalogUtils.getPceTxTspParameters("SPE-non-existing-mode", CatalogConstant.MWWRCORE) * 1000000.0,
            0.0,
            "Checking ONSR Lin = 0 for non valid OM");
        assertEquals(
            -9996.9,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 18001.0, 0.0, 0.0, 20.0),
            0.5,
            "Checking 100GSCFEC RX margin OOR due to CD");
        assertEquals(
            -9996.9,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 0.0, 30.1, 0.0, 20.0),
            0.5,
            "Checking 100GSCFEC RX margin OOR due to PMD");
        assertEquals(
            0.0,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 0.0, 0.0, 6.0, 20.0),
            0.5,
            "Checking 100GSCFEC RX margin OOR due to PDL");
        assertEquals(
            0.0,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 17999.0, 29.9, 6.0, 20.0),
            0.05,
            "Checking 100GSCFEC RX margin in Range at max tolerated penalty");
        assertEquals(
            -9996.9,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 12001.0, 0.0, 0.0, 27.0),
            0.5,
            "Checking 400G OFEC 63.1 Gbauds RX margin OOR due to CD");
        assertEquals(
            -9996.9,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 0.0, 20.1, 0.0, 27.0),
            0.5,
            "Checking 400G OFEC 63.1 Gbauds RX margin OOR due to PMD");
        assertEquals(
            0.0,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 0.0, 0.0, 6.0, 27.0),
            0.5,
            "Checking 400G OFEC 63.1 Gbauds RX margin OOR due to PDL");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 11999.0, 19.9, 5.0, 28.0),
            0.05,
            "Checking 400G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 3999.0, 9.9, 2.0, 25.5),
            0.05,
            "Checking 400G OFEC 63.1 Gbauds RX margin in Range at intermediate tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 3999.0, 9.9, 1.0, 25.0),
            0.05,
            "Checking 400G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW300GOFEC631GBD, 17999.0, 24.9, 5.0, 25.0),
            0.05,
            "Checking 300G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW300GOFEC631GBD, 3999.0, 9.9, 1.0, 22.0),
            0.05,
            "Checking 300G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC631GBD, 23999.0, 24.9, 5.0, 21.0),
            0.05,
            "Checking 200G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC631GBD, 3999.0, 9.9, 1.0, 18.0),
            0.05,
            "Checking 200G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC316GBD, 23999.0, 29.9, 5.0, 24.5),
            0.05,
            "Checking 200G OFEC 31.6 Gbauds RX margin in Range at max tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC316GBD, 3999.0, 9.9, 1.0, 21.5),
            0.05,
            "Checking 200G OFEC 31.6 Gbauds RX margin in Range at min tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GOFEC316GBD, 47999.0, 29.9, 5.0, 16.0),
            0.05,
            "Checking 100G OFEC 31.6 Gbauds RX margin in Range at max tolerated penalty");
        assertEquals(
            0.5,
            catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GOFEC316GBD, 3999.0, 9.9, 1.0, 13.0),
            0.05,
            "Checking 100G OFEC 31.6 Gbauds RX margin in Range at min tolerated penalty");
        assertEquals(
            -9999.9,
            catalogUtils.getPceRxTspParameters("SPE-non-existing-mode", 0.0, 0.0, 0.0, 30.0),
            0.05,
            "Checking Margin negative for non valid OM");
        outputImpairments.put("CD", 1025.0);
        outputImpairments.put("DGD2", 18.0);
        outputImpairments.put("PDL2", 4.4);
        outputImpairments.put("ONSRLIN", 0.0016307685044580757);
        // check how to add Delta on an object<String, Double>
        assertEquals(
            outputImpairments,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.EXPRESS, CatalogConstant.MWMWCORE,
                -15.0, 1000.0, 9.0, 4.0, 0.001000, 50.0),
            "Checking ROADM Express path contribution to impairments ");
        outputImpairments.put("ONSRLIN", 0.0014729700859390747);
        assertEquals(
            outputImpairments,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.EXPRESS, CatalogConstant.MWMWCORE,
                -15.0, 1000.0, 9.0, 4.0, 0.001000, 87.5),
            "Checking ROADM Express path contribution to impairments with 87.5 GHz spacing");
        outputImpairments.put("ONSRLIN", 0.0015011872336272727);
        assertEquals(
            outputImpairments,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.ADD, CatalogConstant.MWWRCORE,
                -15.0, 1000.0, 9.0, 4.2, 0.001, 50.0),
            "Checking ROADM Add path contribution to impairments");
        outputImpairments.put("ONSRLIN", 0.0016307685044580757);
        assertEquals(
            outputImpairments,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.DROP, CatalogConstant.MWWRCORE,
                -15.0, 1000.0, 9.0, 4.2, 0.001, 50.0),
            "Checking ROADM Drop path contribution to impairments");
        outputImpairments.put("ONSRLIN", 0.0015010372326658581);
        assertEquals(
            outputImpairments,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP, CatalogConstant.MWISTANDARD,
                -15.0, 1025.0, 9.0, 4.36, 0.001, 50.0),
            "Checking Amp path contribution to impairments");
        assertEquals(
            true,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP, "ThisIsNotAValidMode",
                -15.0,1000.0, 0.0, 0.0, 0.001, 50.0).isEmpty(),
            "Checking empty map returned in case wrong Operational mode provided ");
        outputImpairments.put("ONSRLIN", 1.0);
        assertEquals(
            true,
            catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP, "OR-InvalidMode",
                -15.0, 1025.0, 18.0, 6.25, 0.001, 50.0).isEmpty(),
            "Checking empty map returned in case wrong Operational mode provided");
        assertEquals(
            0.000114266642501745,
            catalogUtils.calculateNLonsrContribution(2, 70, 87.5),
            0.000000005,
            "Checking Non Linear contribution calculation");
    }
}
