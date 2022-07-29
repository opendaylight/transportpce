/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OperationalModeCatalog;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogUtilsTest extends AbstractTest {
    private static final Logger LOG = LoggerFactory.getLogger(CatalogUtilsTest.class);
    private static final String CATALOG_FILE = "src/test/resources/apidocCatalogOptSpecV5_0.json";
    private static OperationalModeCatalog omCatalog;
    private static NetworkTransactionService netTransServ;
    private static Map<String, Double> outputImpairments = new HashMap<>();

    //
    @BeforeClass
    public static void setUp() throws InterruptedException,
        ExecutionException {
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
            .createWithDataStoreUtil(getDataStoreContextUtil());
        try (Reader reader = new FileReader(CATALOG_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter
                .transformIntoNormalizedNode(reader).get();
            omCatalog = (OperationalModeCatalog) getDataStoreContextUtil()
                .getBindingDOMCodecServices().fromNormalizedNode(YangInstanceIdentifier
                    .of(OperationalModeCatalog.QNAME), normalizedNode)
                .getValue();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = getDataBroker().newWriteOnlyTransaction();
            newWriteOnlyTransaction
                .put(LogicalDatastoreType.CONFIGURATION,
                    InstanceIdentifier.create(OperationalModeCatalog.class),
                    omCatalog);
            newWriteOnlyTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load OpenROADM part of Operational Mode Catalog ", e);
            fail("Cannot load openROADM operational modes ");
        }
    }

    @Test
    public void catalogPrimitivesTest() {
        RequestProcessor reqProc = new RequestProcessor(getDataBroker());
        netTransServ = new NetworkTransactionImpl(reqProc);
        CatalogUtils catalogUtils = new CatalogUtils(netTransServ);
        assertEquals("Checking retrieval of Operational Mode from Node Type ADD",
            CatalogConstant.MWWRCORE,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.ADD,
                StringConstants.SERVICE_TYPE_100GE_T));
        assertEquals("Checking retrieval of Operational Mode from Node Type DROP",
            CatalogConstant.MWWRCORE,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.DROP,
                StringConstants.SERVICE_TYPE_100GE_T));
        assertEquals("Checking retrieval of Operational Mode from Node Type EXPRESS",
            CatalogConstant.MWMWCORE,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.EXPRESS,
                StringConstants.SERVICE_TYPE_100GE_T));
        assertEquals("Checking retrieval of Operational Mode from Node Type AMP",
            CatalogConstant.MWISTANDARD,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.AMP,
                StringConstants.SERVICE_TYPE_100GE_T));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type 100GE",
            CatalogConstant.ORW100GSC,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_100GE_T));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type OTU4",
            CatalogConstant.ORW100GSC,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTU4));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type OTUC2",
            CatalogConstant.ORW200GOFEC316GBD,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC2));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type OTUC3",
            CatalogConstant.ORW300GOFEC631GBD,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC3));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type 400GE",
            CatalogConstant.ORW400GOFEC631GBD,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_400GE));
        assertEquals("Checking retrieval of Operational Mode from Node Type and service Type OTUC4",
            CatalogConstant.ORW400GOFEC631GBD,
            catalogUtils.getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType.TSP,
                StringConstants.SERVICE_TYPE_OTUC4));
        assertEquals("Checking retrieval of channel spacing from Operational Mode 100G SC FEC",
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW100GSC),0.005);
        assertEquals("Checking retrieval of channel spacing from Operational Mode 100G OFEC 31.6",
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW100GOFEC316GBD),0.005);
        assertEquals("Checking retrieval of channel spacing from Operational Mode 200G OFEC 31.6",
            50.0,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW200GOFEC316GBD),0.005);
        assertEquals("Checking retrieval of channel spacing from Operational Mode 200G OFEC 63.1",
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW200GOFEC631GBD),0.005);
        assertEquals("Checking retrieval of channel spacing from Operational Mode 300G OFEC 63.1 GBd",
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW300GOFEC631GBD),0.005);
        assertEquals("Checking retrieval of channel spacing from Operational Mode 400G OFEC 63.1 Gbd",
            87.5,
            catalogUtils.getPceTxTspChannelSpacing(CatalogConstant.ORW400GOFEC631GBD),0.005);
        assertEquals("Checking 100GSCFEC ONSR Lin",
            1345.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW100GSC, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking 100G OFEC 31.6 Gbauds ONSR Lin",
            1095.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW100GOFEC316GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking 200G OFEC 31.6 Gbauds ONSR Lin",
            1095.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW200GOFEC316GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking 200G OFEC 63.1 Gbauds ONSR Lin",
            1095.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW200GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking 300G OFEC 63.1 Gbauds ONSR Lin",
            1095.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW300GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking 400G OFEC 63.1 Gbauds ONSR Lin",
            1095.6,
            catalogUtils.getPceTxTspParameters(CatalogConstant.ORW400GOFEC631GBD, CatalogConstant.MWWRCORE) * 1000000.0,
            0.5);
        assertEquals("Checking ONSR Lin = 0 for non valid OM",
            0.0, catalogUtils.getPceTxTspParameters("SPE-non-existing-mode", CatalogConstant.MWWRCORE) * 1000000.0,
            0.0);
        assertEquals("Checking 100GSCFEC RX margin OOR due to CD",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 18001.0, 0.0, 0.0, 20.0), 0.5);
        assertEquals("Checking 100GSCFEC RX margin OOR due to PMD",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 0.0, 30.1, 0.0, 20.0), 0.5);
        assertEquals("Checking 100GSCFEC RX margin OOR due to PDL",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 0.0, 0.0, 6.1, 20.0), 0.5);
        assertEquals("Checking 100GSCFEC RX margin in Range at max tolerated penalty",
            3.0, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GSC, 17999.0, 29.9, 5.9, 20.0), 0.05);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin OOR due to CD",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 12001.0, 0.0, 0.0, 27.0),
            0.5);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin OOR due to PMD",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 0.0, 20.1, 0.0, 27.0),
            0.5);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin OOR due to PDL",
            -9996.9, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 0.0, 0.0, 4.1, 27.0),
            0.5);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 11999.0, 19.9, 3.9, 28.0),
            0.05);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin in Range at intermediate tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 3999.0, 9.9, 1.9, 25.5),
            0.05);
        assertEquals("Checking 400G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW400GOFEC631GBD, 3999.0, 9.9, 0.9, 25.0),
            0.05);
        assertEquals("Checking 300G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW300GOFEC631GBD, 17999.0, 24.9, 3.9, 25.0),
            0.05);
        assertEquals("Checking 300G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW300GOFEC631GBD, 3999.0, 9.9, 0.9, 22.0),
            0.05);
        assertEquals("Checking 200G OFEC 63.1 Gbauds RX margin in Range at max tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC631GBD, 23999.0, 24.9, 3.9, 21.0),
            0.05);
        assertEquals("Checking 200G OFEC 63.1 Gbauds RX margin in Range at min tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC631GBD, 3999.0, 9.9, 0.9, 18.0),
            0.05);
        assertEquals("Checking 200G OFEC 31.6 Gbauds RX margin in Range at max tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC316GBD, 23999.0, 29.9, 3.9, 24.5),
            0.05);
        assertEquals("Checking 200G OFEC 31.6 Gbauds RX margin in Range at min tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW200GOFEC316GBD, 3999.0, 9.9, 0.9, 21.5),
            0.05);
        assertEquals("Checking 100G OFEC 31.6 Gbauds RX margin in Range at max tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GOFEC316GBD, 47999.0, 29.9, 3.9, 16.0),
            0.05);
        assertEquals("Checking 100G OFEC 31.6 Gbauds RX margin in Range at min tolerated penalty",
            0.5, catalogUtils.getPceRxTspParameters(CatalogConstant.ORW100GOFEC316GBD, 3999.0, 9.9, 0.9, 13.0),
            0.05);
        assertEquals("Checking Margin negative for non valid OM",
            -9999.9, catalogUtils.getPceRxTspParameters("SPE-non-existing-mode", 0.0, 0.0, 0.0, 30.0), 0.05);
        outputImpairments.put("CD", 1025.0);
        outputImpairments.put("DGD2", 18.0);
        outputImpairments.put("PDL2", 6.25);
        outputImpairments.put("ONSRLIN", 0.0016307685044580744);
        // check how to add Delta on an object<String, Double>
        assertEquals("Checking ROADM Express path contribution to impairments ",
            outputImpairments, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.EXPRESS,
            CatalogConstant.MWMWCORE,-15.0, 1000.0, 9.0, 4.0, 0.001000, 50.0));
        outputImpairments.put("ONSRLIN", 0.0013604391454046139);
        assertEquals("Checking ROADM Express path contribution to impairments with 87.5 GHz spacing ",
            outputImpairments, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.EXPRESS,
            CatalogConstant.MWMWCORE,-15.0, 1000.0, 9.0, 4.0, 0.001000, 87.5));
        outputImpairments.put("ONSRLIN", 0.0015011872336272727);
        assertEquals("Checking ROADM Add path contribution to impairments ",
            outputImpairments, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.ADD,
            CatalogConstant.MWWRCORE, -15.0, 1000.0, 9.0, 4.0, 0.001, 50.0));
        outputImpairments.put("ONSRLIN", 0.0016307685044580744);
        assertEquals("Checking ROADM Drop path contribution to impairments ",
            outputImpairments, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.DROP,
            CatalogConstant.MWWRCORE, -15.0, 1000.0, 9.0, 4.0, 0.001, 50.0));
        outputImpairments.put("ONSRLIN", 0.0015010372326658573);
        assertEquals("Checking Amp path contribution to impairments ",
            outputImpairments, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP,
            CatalogConstant.MWISTANDARD, -15.0, 1025.0, 9.0, 5.76, 0.001, 50.0));
        assertEquals("Checking empty map returned in case wrong Operational mode provided  ",
            true, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP,
                "ThisIsNotAValidMode", -15.0,1000.0, 0.0, 0.0, 0.001, 50.0).isEmpty());
        outputImpairments.put("ONSRLIN", 1.0);
        assertEquals("Checking empty map returned in case wrong Operational mode provided  ",
            true, catalogUtils.getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType.AMP,
            "OR-InvalidMode", -15.0, 1025.0, 18.0, 6.25, 0.001, 50.0).isEmpty());
        assertEquals("Checking Non Linear contribution calculation  ", 0.000114266642501745,
            catalogUtils.calculateNLonsrContribution(2, 70, 87.5), 0.000000005);
    }
}