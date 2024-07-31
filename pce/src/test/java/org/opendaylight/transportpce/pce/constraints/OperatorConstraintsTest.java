/*
 * Copyright © 2024 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.concurrent.ExecutionException;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.graph.PceGraphTest;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.transportpce.test.converter.DataObjectConverter;
import org.opendaylight.transportpce.test.converter.JSONDataObjectConverter;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.controller.customization.rev230526.controller.parameters
//.SpectrumFilling;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.controller.customization.rev230526.controller.parameters
//.spectrum.filling.SpectrumFillingRules;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ControllerBehaviourSettings;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.data.codec.spi.BindingDOMCodecServices;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ExtendWith(MockitoExtension.class)
public class OperatorConstraintsTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(PceGraphTest.class);
    private NetworkTransactionImpl networkTransaction;
    private static final String SPEC_FILLING_FILE = "src/test/resources/spectrum-filling-rule1.json";
//    private static SpectrumFillingRules sfRule;
    private static ControllerBehaviourSettings cbSettings;
    private OperatorConstraints opConstraints;
    @Mock
    private BindingDOMCodecServices bindingDOMCodecServices;
    private DataBroker dataBroker;

    @BeforeEach
    void setUp() throws InterruptedException, ExecutionException {
        this.dataBroker = getNewDataBroker();
        networkTransaction = new NetworkTransactionImpl(this.dataBroker);
        opConstraints = new OperatorConstraints(networkTransaction);
        PceTestUtils.writeNetworkInDataStore(this.dataBroker);
        DataObjectConverter dataObjectConverter = JSONDataObjectConverter
            .createWithDataStoreUtil(getDataStoreContextUtil());
        // The Spectrum filling rules associated with CustomerProfileLamda1 is populated from a file in the Data Store
        try (Reader reader = new FileReader(SPEC_FILLING_FILE, StandardCharsets.UTF_8)) {
            NormalizedNode normalizedNode = dataObjectConverter.transformIntoNormalizedNode(reader).orElseThrow();
            cbSettings = (ControllerBehaviourSettings) getDataStoreContextUtil()
                .getBindingDOMCodecServices()
                .fromNormalizedNode(
                    YangInstanceIdentifier.of(ControllerBehaviourSettings.QNAME), normalizedNode)
                .getValue();
            DataObjectIdentifier<ControllerBehaviourSettings> sfIID = DataObjectIdentifier
                    .builder(ControllerBehaviourSettings.class)
//                    .child(SpectrumFilling.class)
//                    .child(SpectrumFillingRules.class)
                    .build();
            @NonNull
            WriteTransaction newWriteOnlyTransaction = dataBroker.newWriteOnlyTransaction();
            newWriteOnlyTransaction.put(LogicalDatastoreType.CONFIGURATION, sfIID, cbSettings);
            newWriteOnlyTransaction.commit().get();
        } catch (IOException e) {
            LOG.error("Cannot load Spectrum-Filling-Rules in Service DS ", e);
            fail("Cannot load Spectrum-Filling-Rules");
        }
    }


    @Test
    void checkSpecttrumFilling() {
        BitSet referenceBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        referenceBitSet.set(0, GridConstant.EFFECTIVE_BITS, false);
        referenceBitSet.set(0, 8, true);
        assertEquals(referenceBitSet, opConstraints.getBitMapConstraint("CustomerProfileLamda1"));
        referenceBitSet.set(8, GridConstant.EFFECTIVE_BITS, true);
        referenceBitSet.set(0, 8, false);
        assertEquals(referenceBitSet, opConstraints.getBitMapConstraint("otherCustomer"));
    }

}
