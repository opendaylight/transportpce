/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.ImpairmentType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.amplifier.parameters.Amplifier;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.Amplifiers;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.Roadms;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.XpondersPluggables;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.specific.operational.modes.SpecificOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.specific.operational.modes.SpecificOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.common.amplifier.drop.parameters.OpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.common.amplifier.drop.parameters.OpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.add.parameters.Add;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.add.parameters.add.AddOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.add.parameters.add.AddOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.drop.parameters.Drop;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.roadm.express.parameters.Express;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.transponder.parameters.Penalties;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.transponder.parameters.PenaltiesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.transponder.parameters.TXOOBOsnrKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.OperationalModeCatalog;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for Catalog. Following methods are used to retrieve parameters
 * from the specification catalog. They point to either openROADM or specific
 * operational modes. They provide to the PCE, the OLM and the Renderer, the
 * required parameters to calculate impairments and set output power levels
 * according to the specifications.
 *
 */
public class CatalogUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogUtils.class);
    private final PenaltiesComparator penaltiesComparator = new PenaltiesComparator();
    private NetworkTransactionService networkTransactionService;

    public CatalogUtils(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Following method returns default OperationalModeId for devices that do not expose them.
     *
     * @param catalogNodeType
     *            identifies type of nodes in the catalog
     * @param serviceType
     *            allows for Xponder selecting default mode according to the rate
     *
     * @return a default operational mode that corresponds to initial specifications
     *
     */
    public String getPceTxTspOperationalModeFromServiceType(CatalogConstant.CatalogNodeType catalogNodeType,
            String serviceType) {
        String operationalModeId = "";

        switch (catalogNodeType) {
            case ADD:
            case DROP:
                operationalModeId = CatalogConstant.MWWRCORE;
                break;
            case EXPRESS:
                operationalModeId = CatalogConstant.MWMWCORE;
                break;
            case AMP:
                operationalModeId = CatalogConstant.MWISTANDARD;
                break;
            case TSP:
                if (StringConstants.SERVICE_TYPE_100GE_T.contentEquals(serviceType)
                        || StringConstants.SERVICE_TYPE_OTU4.contentEquals(serviceType)) {
                    operationalModeId = CatalogConstant.ORW100GSC;
                }
                if (StringConstants.SERVICE_TYPE_OTUC2.contentEquals(serviceType)) {
                    operationalModeId = CatalogConstant.ORW200GOFEC316GBD;
                }
                if (StringConstants.SERVICE_TYPE_OTUC3.contentEquals(serviceType)) {
                    operationalModeId = CatalogConstant.ORW300GOFEC631GBD;
                }
                if ((StringConstants.SERVICE_TYPE_OTUC4.contentEquals(serviceType))
                        || (StringConstants.SERVICE_TYPE_400GE.contentEquals(serviceType))) {
                    operationalModeId = CatalogConstant.ORW400GOFEC631GBD;
                }
                break;
            default:
                LOG.warn("Unsupported catalogNodeType {}", catalogNodeType);
                break;
        }
        return operationalModeId;
    }

    /**
     * This method retrieves channel-spacing associated with a Xponder TX.
     *
     * @param operationalModeId
     *            operational-mode-Id of the Xponder (OR or Specific)
     *
     * @return the channel spacing used to correct OSNR contribution values from
     *         ROADMs and amplifiers
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */

    public double getPceTxTspChannelSpacing(String operationalModeId) {
        double spacing = 50.0;
        XponderPluggableOpenroadmOperationalMode orTspOM = null;
        SpecificOperationalMode speTspOM = null;

        if (operationalModeId.startsWith("OR")) {
            InstanceIdentifier<XponderPluggableOpenroadmOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(OpenroadmOperationalModes.class)
                .child(XpondersPluggables.class)
                .child(XponderPluggableOpenroadmOperationalMode.class,
                    new XponderPluggableOpenroadmOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<XponderPluggableOpenroadmOperationalMode> omOptional = networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (omOptional.isPresent()) {
                    orTspOM = omOptional.get();
                    if (!(orTspOM.getMaxRollOff() == null || (orTspOM.getMaxRollOff().doubleValue() == 0))) {
                        spacing = (Math.ceil(
                            orTspOM.getBaudRate().doubleValue() * (1 + orTspOM.getMaxRollOff().doubleValue()) / 12.5))
                            * 12.5;
                        LOG.info("Operational Mode {} associated channel spacing is {}",
                            operationalModeId, spacing);
                    } else {
                        if (CatalogConstant.ORW100GSC.contentEquals(operationalModeId)) {
                            LOG.info("Operational Mode {} associated channel spacing is {}",
                                operationalModeId, spacing);

                        } else {
                            LOG.info("Did not succeed in retrieving chanel spacing from Operational Mode {}",
                                operationalModeId);
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "Operational mode not populated in Catalog : "
                        + omCatalogIid + " :" + e);
            } finally {
                networkTransactionService.close();
            }

        } else {
            // In other cases, means the mode is a non OpenROADM specific Operational Mode
            InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
            try {
                var somOptional = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid)
                    .get();
                if (somOptional.isPresent()) {
                    speTspOM = somOptional.get();
                    if (!((speTspOM.getMaxRollOff() == null) || (speTspOM.getMaxRollOff().doubleValue() == 0))) {
                        spacing = (Math.ceil(
                            speTspOM.getBaudRate().doubleValue() * (1 + speTspOM.getMaxRollOff().doubleValue()) / 12.5))
                            * 12.5;
                        LOG.info("Operational Mode {} associated channel spacing is {}",
                            operationalModeId, spacing);
                    } else {
                        LOG.info("Did not succeed in retrieving chanel spacing from Operational Mode {}",
                            operationalModeId);
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "Operational mode not populated in Catalog : "
                        + omCatalogIid + " :" + e);
            } finally {
                networkTransactionService.close();
            }
        }
        return spacing;
    }

    /**
     * This method retrieves performance parameters associated with a Xponder TX.
     *
     * @param operationalModeId
     *            operational-mode-Id of the Xponder (OR or Specific)
     * @param addDropMuxOperationalModeId
     *            operational-mode-Id of the Add-Drop bloc the XponderTX is
     *            associated to (conditions TX-OOB OSNR value)
     *
     * @return the linear Optical Noise to signal Ratio
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */
    public double getPceTxTspParameters(String operationalModeId, String addDropMuxOperationalModeId) {
        double txOnsrLin = 0.0;
        XponderPluggableOpenroadmOperationalMode orTspOM = null;
        SpecificOperationalMode speTspOM = null;

        if (operationalModeId.startsWith("OR")) {
            InstanceIdentifier<XponderPluggableOpenroadmOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(OpenroadmOperationalModes.class)
                .child(XpondersPluggables.class)
                .child(XponderPluggableOpenroadmOperationalMode.class,
                    new XponderPluggableOpenroadmOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<XponderPluggableOpenroadmOperationalMode> omOptional = networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (omOptional.isPresent()) {
                    orTspOM = omOptional.get();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orTspOM);
                    TXOOBOsnrKey key = new TXOOBOsnrKey(addDropMuxOperationalModeId);
                    if (orTspOM.getMinTXOsnr() != null) {
                        txOnsrLin = 1.0 / (Math.pow(10.0, (orTspOM.getMinTXOsnr().getValue().doubleValue() / 10.0)));
                    }
                    if (orTspOM.nonnullTXOOBOsnr().get(key) != null
                            && orTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrSingleChannelValue() != null) {
                        // To 1/(Xponder Min TX OSNR lin) Add 1/(Xponder TX OOB OSNR Single channel lin)
                        txOnsrLin = txOnsrLin + 1.0 / (Math.pow(10.0, (orTspOM.nonnullTXOOBOsnr().get(key)
                            .getMinOOBOsnrSingleChannelValue().getValue().doubleValue() / 10.0)));
                    }
                    if (orTspOM.getTXOOBOsnr() != null && orTspOM.nonnullTXOOBOsnr().get(key)
                            .getMinOOBOsnrMultiChannelValue() != null) {
                        // To resulting 1/(OSNR lin) Add 1/(Xponder TX OOB OSNR Multi channel lin)
                        // contribution
                        txOnsrLin = txOnsrLin + 1.0 / (Math.pow(10.0, (orTspOM.nonnullTXOOBOsnr().get(key)
                            .getMinOOBOsnrMultiChannelValue().getValue().doubleValue() / 10.0)));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : " + omCatalogIid + " :"
                        + e);
            } finally {
                networkTransactionService.close();
            }

        } else {
            // In other cases, means the mode is a non OpenROADM specific Operational Mode
            InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
            try {
                var somOptional = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid)
                    .get();
                if (somOptional.isPresent()) {
                    speTspOM = somOptional.get();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", speTspOM);
                    TXOOBOsnrKey key = new TXOOBOsnrKey(addDropMuxOperationalModeId);
                    if (speTspOM.getMinTXOsnr() != null) {
                        txOnsrLin = 1.0 / (Math.pow(10.0, (speTspOM.getMinTXOsnr().getValue().doubleValue() / 10.0)));
                    }
                    if (speTspOM.nonnullTXOOBOsnr().get(key) != null
                            && speTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrSingleChannelValue() != null) {
                        // Add to 1/(Transponder Min TX OSNR lin) 1/(Transponder TX OOB OSNR Single
                        // channel lin)
                        txOnsrLin = txOnsrLin + 1.0 / (Math.pow(10.0, (speTspOM.nonnullTXOOBOsnr().get(key)
                            .getMinOOBOsnrSingleChannelValue().getValue().doubleValue() / 10.0)));
                    }
                    if (speTspOM.nonnullTXOOBOsnr().get(key) != null
                            && speTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrMultiChannelValue() != null) {
                        // Add to resulting 1/(OSNR lin) 1/(Transponder TX OOB OSNR Multi channel lin)
                        // contribution
                        txOnsrLin = txOnsrLin + 1.0 / (Math.pow(10.0, (speTspOM.nonnullTXOOBOsnr().get(key)
                            .getMinOOBOsnrMultiChannelValue().getValue().doubleValue() / 10.0)));
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : " + omCatalogIid + " :"
                        + e);
            } finally {
                networkTransactionService.close();
            }
        }
        return txOnsrLin;
    }

    /**
     * This method retrieves performance parameters associated with a Xponder RX.
     * It calls getRxTspPenalty to evaluate the penalty associated with CD/PMD/PDL
     * It compares expected OSNR with the OSNR resulting from the line degradation,
     * and finally calculates and return the resulting margin.
     *
     * @param operationalModeId
     *            operational-mode-Id of the Xponder (OR or Specific)
     * @param calcCd
     *            accumulated chromatic dispersion across the line
     * @param calcPmd
     *            accumulated Polarization mode dispersion across the line
     * @param calcPdl
     *            accumulated Polarization Dependant Loss across the line
     * @param calcOsnrdB
     *            Optical Signal to Noise Ratio (dB)resulting from the transmission
     *            on the line, that shall include the Non Linear contribution
     *
     * @return the margin on the service path
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */
    public double getPceRxTspParameters(String operationalModeId, double calcCd, double calcPmd,
            double calcPdl, double calcOsnrdB) {
        HashMap<String, Double> impairments = new HashMap<>();
        double totalPenalty = 0.0;
        double penalty ;
        double rxOsnrdB = 0.0;
        double margin = -9999.9;
        XponderPluggableOpenroadmOperationalMode orTspOM = null;
        SpecificOperationalMode speTspOM = null;
        Map<PenaltiesKey, Penalties> penaltiesMap = null;
        if (operationalModeId.startsWith("OR")) {
            var omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(OpenroadmOperationalModes.class)
                .child(XpondersPluggables.class)
                .child(XponderPluggableOpenroadmOperationalMode.class,
                    new XponderPluggableOpenroadmOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<XponderPluggableOpenroadmOperationalMode> omOptional = networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (omOptional.isPresent()) {
                    orTspOM = omOptional.get();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orTspOM);
                    if (orTspOM.getMinRXOsnrTolerance() != null) {
                        rxOsnrdB = orTspOM.getMinRXOsnrTolerance().getValue().doubleValue();
                    }
                    penaltiesMap = orTspOM.getPenalties();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : " + omCatalogIid + " :"
                        + e);
            } finally {
                networkTransactionService.close();
            }
        } else {
            // In other cases, means the mode is a non OpenROADM specific Operational Mode
            // InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
            var omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<SpecificOperationalMode> somOptional = networkTransactionService
                    .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (somOptional.isPresent()) {
                    speTspOM = somOptional.get();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", speTspOM);
                    if (speTspOM.getMinRXOsnrTolerance() != null) {
                        rxOsnrdB = speTspOM.getMinRXOsnrTolerance().getValue().doubleValue();
                    }
                    penaltiesMap = speTspOM.getPenalties();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : " + omCatalogIid + " :"
                        + e);
            } finally {
                networkTransactionService.close();
            }
        }
        if (penaltiesMap != null) {
            penalty = getRxTspPenalty(calcCd, ImpairmentType.CDPsNm, penaltiesMap);
            impairments.put("CDpenalty", penalty);
            totalPenalty = totalPenalty + penalty;
            penalty = getRxTspPenalty(calcPmd, ImpairmentType.PMDPs, penaltiesMap);
            impairments.put("PMD Penalty", penalty);
            totalPenalty = totalPenalty + penalty;
            penalty = getRxTspPenalty(calcPdl, ImpairmentType.PDLDB, penaltiesMap);
            impairments.put("PDL penalty", penalty);
            totalPenalty = totalPenalty + penalty;
            // For Future work since at that time we have no way to calculate the following
            // parameters,even if penalties are defined in the OpenROADM specifications
            //
            // impairments.put("Colorless Drop Adjacent Xtalk Penalty", getRxTspPenalty(TBD,
            // ImpairmentType.ColorlessDropAdjacentChannelCrosstalkGHz, penalitiesMap));
            // impairments.put("XTalk total Power Penalty", getRxTspPenalty(TBD,
            // ImpairmentType.CrossTalkTotalPowerDB, penalitiesMap));
            // impairments.put("Power penalty", getRxTspPenalty(TBD,
            // ImpairmentType.PowerDBm, penalitiesMap));
            LOG.info("Penalty resulting from CD, PMD and PDL is {} dB with following contributions {}",
                totalPenalty, impairments);
            margin = calcOsnrdB - totalPenalty - rxOsnrdB;
            LOG.info("According to RX TSP Specification and calculated impairments Margin is {} dB ", margin);
            if (margin < 0) {
                LOG.info("Negative margin shall result in PCE rejecting the analyzed path");
            }
        } else {
            LOG.info("Unable to calculate margin as penaltyMap can not be retrieved : Operational mode not populated");
        }
        return margin;
    }

    /**
     * This generic method is called from getPceRxTspParameters to provide the
     * Penalties associated with CD, PMD and DGD for Xponder. It scans a penalty
     * list that includes penalty values corresponding to an interval between an
     * upper and a lower boundary for each of the above parameters.
     *
     * @param impairmentType
     *            : the type of impairment (CD/PMD/DGD)
     * @param calculatedParameter
     *            calculated accumulated value on the line for the impairment
     * @param penaltiesMap
     *            the global map of penalties retrieved by getPceRxTspParameters
     *            from the Xponder operational mode
     *
     * @return the penalty associated with accumulated impairment if it is in the
     *         range specified in the table, a value that will lead to reject the
     *         path if this is not the case
     */

    private double getRxTspPenalty(double calculatedParameter, ImpairmentType impairmentType,
        Map<PenaltiesKey, Penalties> penalitiesMap) {
        Penalties penalty = penalitiesMap.values().stream()
            // We only keep penalties corresponding to the calculated Parameter
            .filter(val -> val.getParameterAndUnit().getName().equals(impairmentType.getName()))
            // we sort it according to the comparator (based on up-to-boundary)
            .sorted(penaltiesComparator)
            // keep only items for which up to boundary is greater than calculatedParameter
            .filter(val -> val.getUpToBoundary().doubleValue() >= calculatedParameter)
            // takes the immediate greater or equal value
            .findFirst().orElse(null);

        if (penalty == null) {
            //means a boundary that is greater than calculatedParameter couldn't be found
            // Out of specification!
            return 9999.9;
        }
        // In spec, return penalty associated with calculatedParameter
        return penalty.getPenaltyValue().getValue().doubleValue();
    }

    /**
     * This method retrieves performance parameters associated with ROADMs and
     * Amplifiers. It calculates the contribution of the node to the degradation of
     * the signal for CD, DGD, PDL, and OSNR which is calculated through a
     * polynomial fit described in the catalog. It finally corrects the accumulated
     * values for these parameters and return them.
     *
     * @param catalogNodeType
     *            crossed node path type (ADD/DROP/EXPRESS/AMP)
     * @param operationalModeId
     *            operational-mode-Id of the Node (OpenROADM only)
     * @param calcCd
     *            accumulated chromatic dispersion across the line
     * @param calcDgd2
     *            Square of accumulated Group velocity dispersion across the line
     * @param calcPdl2
     *            Square of the accumulated Polarization Dependant Loss across the
     *            line
     * @param pwrIn
     *            Input power required to calculate OSNR contribution of the node =
     *            f(pwrIn)
     * @param calcOnsrLin
     *            Linear Optical Noise to Signal Ratio resulting from the
     *            transmission on the line, that shall include the Non Linear
     *            contribution
     *
     * @return Impairment, a map that provides corrected values for all calculated
     *         parameters which includes the contribution of the node
     *         (CD/DGD2/PDL2/ONSRLin)
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */

    public Map<String, Double> getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType catalogNodeType,
        String operationalModeId, double pwrIn, double calcCd, double calcDgd2, double calcPdl2,
        double calcOnsrLin, double spacing) {

        Map<String, Double> impairments = new HashMap<>();
        double pdl2 = calcPdl2;
        double dgd2 = calcDgd2;
        double cd = calcCd;
        double onsrLin = calcOnsrLin;
        boolean supportedMode = true;

        switch (catalogNodeType) {
            case ADD:
                var omCatalogIid = InstanceIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .child(Roadms.class)
                    .child(Add.class)
                    .child(AddOpenroadmOperationalMode.class, new AddOpenroadmOperationalModeKey(operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService.read(LogicalDatastoreType.CONFIGURATION,
                        omCatalogIid).get();
                    if (omOptional.isPresent()) {
                        var orAddOM = omOptional.get();
                        LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orAddOM);
                        networkTransactionService.close();
                        onsrLin = onsrLin
                            + Math.pow(10, (-orAddOM.getIncrementalOsnr().getValue().doubleValue()
                            - Math.log10(spacing / 50.0)) / 10.0);
                        cd = cd + orAddOM.getMaxIntroducedCd().doubleValue();
                        pdl2 = pdl2 + Math.pow(orAddOM.getMaxIntroducedPdl().getValue().doubleValue(), 2.0);
                        dgd2 = dgd2 + Math.pow(orAddOM.getMaxIntroducedDgd().doubleValue(), 2.0);
                    } else {
                        supportedMode = false;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    onsrLin = 1;
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid + " :" + e);
                } finally {
                    networkTransactionService.close();
                }
                break;

            case DROP:
                var omCatalogIid1 = InstanceIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .child(Roadms.class)
                    .child(Drop.class)
                    .child(OpenroadmOperationalMode.class, new OpenroadmOperationalModeKey(operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid1)
                        .get();
                    if (omOptional.isPresent()) {
                        var orDropOM = omOptional.get();
                        LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orDropOM);
                        networkTransactionService.close();
                        cd = cd + orDropOM.getMaxIntroducedCd().doubleValue();
                        pdl2 = pdl2 + Math.pow(orDropOM.getMaxIntroducedPdl().getValue().doubleValue(), 2.0);
                        dgd2 = dgd2 + Math.pow(orDropOM.getMaxIntroducedDgd().doubleValue(), 2);
                        onsrLin = onsrLin + Math.pow(10,
                            -(orDropOM.getOsnrPolynomialFit().getA().doubleValue() * Math.pow(pwrIn, 3)
                                + orDropOM.getOsnrPolynomialFit().getB().doubleValue() * Math.pow(pwrIn, 2)
                                + orDropOM.getOsnrPolynomialFit().getC().doubleValue() * pwrIn
                                + orDropOM.getOsnrPolynomialFit().getD().doubleValue()
                                + 10 * Math.log10(spacing / 50.0)) / 10);
                    } else {
                        supportedMode = false;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    onsrLin = 1;
                    supportedMode = false;
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid1);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid1 + " :" + e);
                } finally {
                    networkTransactionService.close();
                }
                break;

            case EXPRESS:
                var omCatalogIid2 = InstanceIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .child(Roadms.class)
                    .child(Express.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http
                        .org.openroadm.operational.mode.catalog.rev211210
                        .operational.mode.roadm.express.parameters.express.OpenroadmOperationalMode.class,
                        new org.opendaylight.yang.gen.v1.http
                        .org.openroadm.operational.mode.catalog.rev211210
                        .operational.mode.roadm.express.parameters.express.OpenroadmOperationalModeKey(
                            operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid2)
                        .get();
                    if (omOptional.isPresent()) {
                        var orExpressOM = omOptional.get();
                        LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}",
                            orExpressOM);
                        cd = cd + orExpressOM.getMaxIntroducedCd().doubleValue();
                        pdl2 = pdl2 + Math.pow(orExpressOM.getMaxIntroducedPdl().getValue().doubleValue(), 2.0);
                        dgd2 = dgd2 + Math.pow(orExpressOM.getMaxIntroducedDgd().doubleValue(), 2.0);
                        onsrLin = onsrLin + Math.pow(10,
                            -(orExpressOM.getOsnrPolynomialFit().getA().doubleValue() * Math.pow(pwrIn, 3)
                                + orExpressOM.getOsnrPolynomialFit().getB().doubleValue() * Math.pow(pwrIn, 2)
                                + orExpressOM.getOsnrPolynomialFit().getC().doubleValue() * pwrIn
                                + orExpressOM.getOsnrPolynomialFit().getD().doubleValue()
                                + 10 * Math.log10(spacing / 50.0)) / 10);
                    } else {
                        supportedMode = false;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    onsrLin = 1;
                    supportedMode = false;
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid2);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid2 + " :" + e);
                } finally {
                    networkTransactionService.close();
                }
                break;

            case AMP:
                var omCatalogIid3 = InstanceIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .child(Amplifiers.class)
                    .child(Amplifier.class)
                    .child(OpenroadmOperationalMode.class, new OpenroadmOperationalModeKey(operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid3)
                        .get();
                    if (omOptional.isPresent()) {
                        var orAmpOM = omOptional.get();
                        LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orAmpOM);
                        networkTransactionService.close();
                        cd = cd + orAmpOM.getMaxIntroducedCd().doubleValue();
                        pdl2 = pdl2 + Math.pow(orAmpOM.getMaxIntroducedPdl().getValue().doubleValue(), 2.0);
                        dgd2 = dgd2 + Math.pow(orAmpOM.getMaxIntroducedDgd().doubleValue(), 2.0);
                        onsrLin = onsrLin + Math.pow(10,
                            -(orAmpOM.getOsnrPolynomialFit().getA().doubleValue() * Math.pow(pwrIn, 3)
                                + orAmpOM.getOsnrPolynomialFit().getB().doubleValue() * Math.pow(pwrIn, 2)
                                + orAmpOM.getOsnrPolynomialFit().getC().doubleValue() * pwrIn
                                + orAmpOM.getOsnrPolynomialFit().getD().doubleValue()
                                + 10 * Math.log10(spacing / 50.0)) / 10);

                    } else {
                        supportedMode = false;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    onsrLin = 1;
                    supportedMode = false;
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid3);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid3 + " :" + e);
                } finally {
                    networkTransactionService.close();
                }
                break;
            default:
                LOG.warn("Unsupported catalogNodeType {}", catalogNodeType);
                break;
        }

        if (supportedMode) {
            impairments.put("CD", cd);
            impairments.put("DGD2", dgd2);
            impairments.put("PDL2", pdl2);
            impairments.put("ONSRLIN", onsrLin);

            LOG.info("Accumulated CD is {} ps, DGD2 is {} ps and PDL2 is {} dB", cd, Math.sqrt(dgd2), Math.sqrt(pdl2));
            LOG.info("Resulting OSNR is {} dB", 10 * Math.log10(1 / onsrLin));

        } else {
            LOG.error("Operational Mode {} passed to getPceRoadmAmpParameters does not correspond to an OpenROADM mode"
                + "Parameters for amplifier and/or ROADMs can not be derived from specific-operational-modes.",
                operationalModeId);
        }

        return impairments;
    }

    /**
     * Non linear contribution computation.
     * Public method calculating non linear contribution among the path from
     * launched power and span length Formula comes from
     * OpenROADM_OSNR_Calculation_20220610 Tool The resulting contribution shall be
     * calculated for each fiber span and summed up
     * @param launchedPowerdB
     *            The power launched in the span (shall account for Optical Distribution
     *            Frame loss)
     * @param spanLength
     *            Length of the span in km
     * @param spacing
     *            OpenROADM power and osnr contribution calculations are based on
     *            spacing between channels : the Media Channel (MC) width
     *
     * @return nonLinearOnsrContributionLin
     *         The inverse of the NL OSNR contribution converted from dB to linear value
     */
    public double calculateNLonsrContribution(double launchedPowerdB, double spanLength, double spacing) {
        double constanteC0 = 0 ;
        if (spacing > 162.5) {
            constanteC0 = CatalogConstant.NLCONSTANTC0GT1625;
        }
        else if (spacing > 112.5) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1625;
        }
        else if (spacing > 100.0) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1125;
        }
        else if (spacing > 87.5) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1000;
        }
        else {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO875;
        }

        double nonLinearOnsrContributionLin = Math.pow(10.0, -(launchedPowerdB * CatalogConstant.NLCONSTANTC1
            + constanteC0
            + CatalogConstant.NLCONSTANTCE * Math.exp(CatalogConstant.NLCONSTANTEX * spanLength)) / 10);
        LOG.info(" OSNR Non Linear contribution is {} dB", launchedPowerdB * CatalogConstant.NLCONSTANTC1
            + constanteC0
            + CatalogConstant.NLCONSTANTCE * Math.exp(CatalogConstant.NLCONSTANTEX * spanLength));
        return nonLinearOnsrContributionLin;
    }

}