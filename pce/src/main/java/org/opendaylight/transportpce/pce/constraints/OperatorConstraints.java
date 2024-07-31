/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.constraints;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.controller.customization.rev230526.controller.parameters.SpectrumFilling;
import org.opendaylight.yang.gen.v1.http.org.openroadm.controller.customization.rev230526.controller.parameters.spectrum.filling.SpectrumFillingRules;
import org.opendaylight.yang.gen.v1.http.org.openroadm.controller.customization.rev230526.controller.parameters.spectrum.filling.SpectrumFillingRulesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ControllerBehaviourSettings;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Class to handle Operator Constraints associated with Specific Engineering rules
 * as they are defined in the controller-behaviour-settings container of the service
 * Data-Store.
 *
 */

public class OperatorConstraints {

    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(OperatorConstraints.class);
    private static final String SPECTRUM_LOG_MSG =
        "Specific Spectrum filling Rules have been defined for {} the spectrum range {} - {}";
    private NetworkTransactionService networkTransaction;

    public OperatorConstraints(NetworkTransactionService networkTransaction) {

        this.networkTransaction = networkTransaction;

    }

    public BitSet getBitMapConstraint(String customerName) {
        BitSet referenceBitSet = new BitSet(GridConstant.EFFECTIVE_BITS);
        referenceBitSet.set(0, GridConstant.EFFECTIVE_BITS, true);
        DataObjectIdentifier<SpectrumFilling> sfIID = DataObjectIdentifier.builder(ControllerBehaviourSettings.class)
                .child(SpectrumFilling.class)
                .build();

        try {
            if (networkTransaction.read(LogicalDatastoreType.CONFIGURATION, sfIID).get().isPresent()) {
                SpectrumFilling spectrumConstraint = networkTransaction
                    .read(LogicalDatastoreType.CONFIGURATION, sfIID)
                    .get().orElseThrow();
                if (spectrumConstraint.getSpectrumFillingRules().isEmpty()) {
                    return referenceBitSet;
                }
                if (customerName == null) {
                    for (Map.Entry<SpectrumFillingRulesKey, SpectrumFillingRules> rule:
                            spectrumConstraint.getSpectrumFillingRules().entrySet()) {
                        var spectrumRangeOfAppl = rule.getValue().getSpectrumRangeOfAppliance();
                        var dedicatedCustomer = spectrumRangeOfAppl.getDedicatedCustomer();
                        if (dedicatedCustomer == null || dedicatedCustomer.isEmpty()) {
                            continue;
                        }
                        // Spectrum portion is dedicated to some customers that do not include this one
                        FrequencyTHz startFreq = spectrumRangeOfAppl.getStartEdgeFrequency();
                        FrequencyTHz stopFreq = spectrumRangeOfAppl.getStopEdgeFrequency();
                        referenceBitSet.set(
                            GridUtils.getIndexFromFrequency(startFreq.getValue()),
                            GridUtils.getIndexFromFrequency(stopFreq.getValue()),
                            false);
                        LOG.info(SPECTRUM_LOG_MSG,
                            "other customers, preventing the customer from using", startFreq, stopFreq);
                    }
                    return referenceBitSet;
                }

                for (Map.Entry<SpectrumFillingRulesKey, SpectrumFillingRules> rule:
                        spectrumConstraint.getSpectrumFillingRules().entrySet()) {
                    var spectrumRangeOfAppl = rule.getValue().getSpectrumRangeOfAppliance();
                    FrequencyTHz startFreq = spectrumRangeOfAppl.getStartEdgeFrequency();
                    FrequencyTHz stopFreq = spectrumRangeOfAppl.getStopEdgeFrequency();
                    var nonAuthorizedCustomer = spectrumRangeOfAppl.getNonAuthorizedCustomer();
                    if (nonAuthorizedCustomer != null && nonAuthorizedCustomer.contains(customerName)) {
                        //Customer shall not be put in this spectrum portion
                        referenceBitSet.set(
                            GridUtils.getIndexFromFrequency(startFreq.getValue()),
                            GridUtils.getIndexFromFrequency(stopFreq.getValue()),
                            false);
                        LOG.info(SPECTRUM_LOG_MSG,
                            "customer " + customerName + ", exluding it from", startFreq, stopFreq);
                        continue;
                    }
                    var dedicatedCustomer = spectrumRangeOfAppl.getDedicatedCustomer();
                    if (dedicatedCustomer == null || dedicatedCustomer.isEmpty()) {
                        continue;
                    }
                    if (dedicatedCustomer.contains(customerName)) {
                        // Spectrum portion is dedicated to customers including this one
                        referenceBitSet.set(
                            GridUtils.getIndexFromFrequency(startFreq.getValue()),
                            GridUtils.getIndexFromFrequency(stopFreq.getValue()),
                            true);
                        LOG.info(SPECTRUM_LOG_MSG,
                            "customer " + customerName + ", to dedicate", startFreq, stopFreq + " to it");
                        continue;
                    }
                    // Spectrum portion is dedicated to some customers that do not include this one
                    referenceBitSet.set(
                        GridUtils.getIndexFromFrequency(startFreq.getValue()),
                        GridUtils.getIndexFromFrequency(stopFreq.getValue()),
                        false);
                    LOG.info(SPECTRUM_LOG_MSG,
                        "other customers, preventing the customer from using", startFreq, stopFreq);
                }
                return referenceBitSet;
            }
        } catch (InterruptedException | ExecutionException e1) {
            LOG.error("Exception caught handling Spectrum filling Rules ", e1.getCause());
        }
        LOG.info("Did not succeed finding any Specific Spectrum filling Rules defined in Configuration Datastore");
        return referenceBitSet;
    }
}
