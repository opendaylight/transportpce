/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.catalog.CatalogConstant.CatalogNodeType;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.ImpairmentType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.amplifier.parameters.Amplifier;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.SpecificOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.openroadm.operational.modes.Amplifiers;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.openroadm.operational.modes.Roadms;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.openroadm.operational.modes.XpondersPluggables;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.openroadm.operational.modes.xponders.pluggables.XponderPluggableOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.specific.operational.modes.SpecificOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.catalog.specific.operational.modes.SpecificOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.add.parameters.Add;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.add.parameters.add.AddOpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.add.parameters.add.AddOpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.drop.parameters.Drop;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.drop.parameters.drop.OpenroadmOperationalMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.drop.parameters.drop.OpenroadmOperationalModeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.roadm.express.parameters.Express;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.transponder.parameters.Penalties;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.transponder.parameters.PenaltiesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.operational.mode.transponder.parameters.TXOOBOsnrKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.power.mask.MaskPowerVsPin;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526.power.mask.MaskPowerVsPinKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OperationalModeCatalog;
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

    private static final String OPMODE_MISMATCH_MSG =
        "Operational Mode {} passed to getPceRoadmAmpParameters does not correspond to an OpenROADM mode"
        + "Parameters for amplifier and/or ROADMs can not be derived from specific-operational-modes.";
    private static final Map<CatalogConstant.CatalogNodeType, String> CATALOGNODETYPE_OPERATIONMODEID_MAP = Map.of(
        CatalogConstant.CatalogNodeType.ADD, CatalogConstant.MWWRCORE,
        CatalogConstant.CatalogNodeType.DROP, CatalogConstant.MWWRCORE,
        CatalogConstant.CatalogNodeType.EXPRESS, CatalogConstant.MWMWCORE,
        CatalogConstant.CatalogNodeType.AMP, CatalogConstant.MWISTANDARD);
    private static final Map<String, String> TSP_DEFAULT_OM_MAP = Map.of(
        StringConstants.SERVICE_TYPE_100GE_T, CatalogConstant.ORW100GSC,
        StringConstants.SERVICE_TYPE_OTU4, CatalogConstant.ORW100GSC,
        StringConstants.SERVICE_TYPE_OTUC2,  CatalogConstant.ORW200GOFEC316GBD,
        StringConstants.SERVICE_TYPE_OTUC3, CatalogConstant.ORW300GOFEC631GBD,
        StringConstants.SERVICE_TYPE_OTUC4, CatalogConstant.ORW400GOFEC631GBD,
        StringConstants.SERVICE_TYPE_400GE, CatalogConstant.ORW400GOFEC631GBD);

    private final PenaltiesComparator penaltiesComparator = new PenaltiesComparator();
    private NetworkTransactionService networkTransactionService;

    public CatalogUtils(NetworkTransactionService networkTransactionService) {
        this.networkTransactionService = networkTransactionService;
    }

    /**
     * Following method returns default OperationalModeId for devices that do not
     * expose them.
     *
     * @param catalogNodeType
     *            identifies type of nodes in the catalog
     * @param serviceType
     *            allows for Xponder selecting default mode according to the rate
     *
     * @return a default operational mode that corresponds to initial specifications
     *
     */
    public String getPceOperationalModeFromServiceType(CatalogConstant.CatalogNodeType catalogNodeType,
            String serviceType) {
        if (CATALOGNODETYPE_OPERATIONMODEID_MAP.containsKey(catalogNodeType)) {
            return CATALOGNODETYPE_OPERATIONMODEID_MAP.get(catalogNodeType);
        }
        if (!catalogNodeType.equals(CatalogConstant.CatalogNodeType.TSP)) {
            LOG.warn("Unsupported catalogNodeType {}", catalogNodeType);
            return "";
        }
        if (!TSP_DEFAULT_OM_MAP.containsKey(serviceType)) {
            LOG.warn("Unsupported serviceType {} for TSP catalogNodeType", serviceType);
            return "";
        }
        return TSP_DEFAULT_OM_MAP.get(serviceType);
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
        double baudRate;
        double maxRollOff;
        if (operationalModeId.startsWith("OR")) {
            InstanceIdentifier<XponderPluggableOpenroadmOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(OpenroadmOperationalModes.class)
                .child(XpondersPluggables.class)
                .child(XponderPluggableOpenroadmOperationalMode.class,
                    new XponderPluggableOpenroadmOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<XponderPluggableOpenroadmOperationalMode> omOptional =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (omOptional.isEmpty()) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , empty list", omCatalogIid);
                    return 0.0;
                }
                XponderPluggableOpenroadmOperationalMode orTspOM = omOptional.orElseThrow();
                maxRollOff = orTspOM.getMaxRollOff() == null ? 0 : orTspOM.getMaxRollOff().doubleValue();
                baudRate = orTspOM.getBaudRate().doubleValue();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException("Operational mode not populated in Catalog : " + omCatalogIid + " :" + e);
            }
        } else {
            // In other cases, means the mode is a non OpenROADM specific Operational Mode
            InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
            try {
                var somOptional =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (somOptional.isEmpty()) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , empty list", omCatalogIid);
                    return 0.0;
                }
                SpecificOperationalMode speTspOM = somOptional.orElseThrow();
                maxRollOff = speTspOM.getMaxRollOff() == null ? 0 : speTspOM.getMaxRollOff().doubleValue();
                baudRate = speTspOM.getBaudRate().doubleValue();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException("Operational mode not populated in Catalog : " + omCatalogIid + " :" + e);
            }
        }
        if (maxRollOff == 0) {
            if (CatalogConstant.ORW100GSC.contentEquals(operationalModeId)) {
            // OR 100G SCFEC is the only case where rolloff factor is not mandatory in the catalog
                LOG.info("Operational Mode {} associated channel spacing is 50.0", operationalModeId);
                return 50.0;
            }
            LOG.warn("Missing rolloff factor (mandatory in Catalog) from Operational Mode {}: use default=0.2",
                operationalModeId);
            maxRollOff = 0.2;
        }
        double spacing = 12.5 * Math.ceil(baudRate * (1 + maxRollOff) / 12.5);
        LOG.info("Operational Mode {} associated channel spacing is {}", operationalModeId, spacing);
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
        RatioDB minOOBOsnrSingleChannelValue;
        RatioDB minOOBOsnrMultiChannelValue;
        if (operationalModeId.startsWith("OR")) {
            InstanceIdentifier<XponderPluggableOpenroadmOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(OpenroadmOperationalModes.class)
                .child(XpondersPluggables.class)
                .child(XponderPluggableOpenroadmOperationalMode.class,
                    new XponderPluggableOpenroadmOperationalModeKey(operationalModeId))
                .build();
            try {
                Optional<XponderPluggableOpenroadmOperationalMode> omOptional =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (omOptional.isEmpty()) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , empty list", omCatalogIid);
                    return 0.0;
                }
                orTspOM = omOptional.orElseThrow();
                LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orTspOM);
                TXOOBOsnrKey key = new TXOOBOsnrKey(addDropMuxOperationalModeId);
                if (orTspOM.getMinTXOsnr() != null) {
                    txOnsrLin = 1.0 / Math.pow(10.0, orTspOM.getMinTXOsnr().getValue().doubleValue() / 10.0);
                }
                if (orTspOM.nonnullTXOOBOsnr().get(key) == null) {
                    return txOnsrLin;
                }
                minOOBOsnrSingleChannelValue = orTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrSingleChannelValue();
                minOOBOsnrMultiChannelValue =  orTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrMultiChannelValue();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                        + omCatalogIid + " :" + e);
            }
        } else {
            // In other cases, means the mode is a non OpenROADM specific Operational Mode
            InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
            try {
                var somOptional =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                if (somOptional.isEmpty()) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , empty list", omCatalogIid);
                    return 0.0;
                }
                speTspOM = somOptional.orElseThrow();
                LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", speTspOM);
                TXOOBOsnrKey key = new TXOOBOsnrKey(addDropMuxOperationalModeId);
                if (speTspOM.getMinTXOsnr() != null) {
                    txOnsrLin = 1.0 / Math.pow(10.0, speTspOM.getMinTXOsnr().getValue().doubleValue() / 10.0);
                }
                if (speTspOM.nonnullTXOOBOsnr().get(key) == null) {
                    return txOnsrLin;
                }
                minOOBOsnrSingleChannelValue = speTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrSingleChannelValue();
                minOOBOsnrMultiChannelValue = speTspOM.nonnullTXOOBOsnr().get(key).getMinOOBOsnrMultiChannelValue();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist", omCatalogIid);
                throw new RuntimeException(
                    "readMdSal: Error reading from operational store, Operational Mode Catalog : " + omCatalogIid + " :"
                        + e);
            }
        }
        if (minOOBOsnrSingleChannelValue != null) {
            txOnsrLin += 1.0 / Math.pow(10.0, minOOBOsnrSingleChannelValue.getValue().doubleValue() / 10.0);
        }
        if (minOOBOsnrMultiChannelValue != null) {
            txOnsrLin += 1.0 / Math.pow(10.0, minOOBOsnrMultiChannelValue.getValue().doubleValue() / 10.0);
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
        double rxOsnrdB = 0.0;
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
                    orTspOM = omOptional.orElseThrow();
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
                    speTspOM = somOptional.orElseThrow();
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
            }
        }
        if (penaltiesMap == null) {
            LOG.error("Unable to calculate margin as penaltyMap can not be retrieved : Operational mode not populated");
            return -9999.9;
        }
        HashMap<String, Double> impairments = new HashMap<>();
        double penalty = getRxTspPenalty(calcCd, ImpairmentType.CDPsNm, penaltiesMap);
        impairments.put("CDpenalty", penalty);
        double totalPenalty = penalty;
        penalty = getRxTspPenalty(calcPmd, ImpairmentType.PMDPs, penaltiesMap);
        impairments.put("PMD Penalty", penalty);
        totalPenalty += penalty;
        // Calculation according to OpenROADM specification
        // penalty = getRxTspPenalty(calcPdl, ImpairmentType.PDLDB, penaltiesMap);
        // Calculation modified according to Julia's Tool
        penalty = calcPdl / 2;
        impairments.put("PDL penalty", penalty);
        totalPenalty += penalty;
        // TODO for Future work since at that time we have no way to calculate the following
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
        double margin = calcOsnrdB - totalPenalty - rxOsnrdB;
        LOG.info("According to RX TSP Specification and calculated impairments Margin is {} dB ", margin);
        if (margin < 0) {
            LOG.warn("Negative margin shall result in PCE rejecting the analyzed path");
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
        LOG.info("Penalty for {} is {} dB", impairmentType, penalty.getPenaltyValue().getValue().doubleValue());
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
     * @param cd
     *            accumulated chromatic dispersion across the line
     * @param dgd2
     *            Square of accumulated Group velocity dispersion across the line
     * @param pdl2
     *            Square of the accumulated Polarization Dependant Loss across the
     *            line
     * @param pwrIn
     *            Input power required to calculate OSNR contribution of the node =
     *            f(pwrIn)
     * @param onsrLin
     *            Linear Optical Noise to Signal Ratio resulting from the
     *            transmission on the line, that shall include the Non Linear
     *            contribution
     * @param spacing
     *            Interchannel spacing used for correction to calculate OSNR
     *            contribution of the node
     *
     * @return Impairment, a map that provides corrected values for all calculated
     *         parameters which includes the contribution of the node
     *         (CD/DGD2/PDL2/ONSRLin)
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */

    public Map<String, Double> getPceRoadmAmpParameters(CatalogConstant.CatalogNodeType catalogNodeType,
            String operationalModeId, double pwrIn, double cd, double dgd2, double pdl2,
            double onsrLin, double spacing) {
        double maxIntroducedCd;
        double maxIntroducedPdl;
        double maxIntroducedDgd;
        List<Double> osnrPolynomialFits;
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
                    var omOptional =
                        networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return new HashMap<>();
                    }
                    var orAddOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orAddOM);
                    maxIntroducedCd = orAddOM.getMaxIntroducedCd().doubleValue();
                    // As per current OpenROADM Spec
                    //maxIntroducedPdl = orAddOM.getMaxIntroducedPdl().getValue().doubleValue();
                    // Applying calculation as provided in Julia's tool
                    maxIntroducedPdl = Math.sqrt(0.2 * 0.2 + 0.4 * 0.4);
                    maxIntroducedDgd = orAddOM.getMaxIntroducedDgd().doubleValue();
                    osnrPolynomialFits = List.of(orAddOM.getIncrementalOsnr().getValue().doubleValue());
                } catch (InterruptedException | ExecutionException e) {
                    onsrLin = 1;
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid + " :" + e);
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
                    var omOptional =
                        networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid1).get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return new HashMap<>();
                    }
                    var orDropOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orDropOM);
                    maxIntroducedCd = orDropOM.getMaxIntroducedCd().doubleValue();
                    // As per current OpenROADM Spec
                    // maxIntroducedPdl = orDropOM.getMaxIntroducedPdl().getValue().doubleValue();
                    // Applying calculation as provided in Julia's tool
                    maxIntroducedPdl = Math.sqrt(0.2 * 0.2 + 0.4 * 0.4);
                    maxIntroducedDgd = orDropOM.getMaxIntroducedDgd().doubleValue();
                    osnrPolynomialFits = List.of(
                        orDropOM.getOsnrPolynomialFit().getD().doubleValue(),
                        orDropOM.getOsnrPolynomialFit().getC().doubleValue(),
                        orDropOM.getOsnrPolynomialFit().getB().doubleValue(),
                        orDropOM.getOsnrPolynomialFit().getA().doubleValue());
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid1);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid1 + " :" + e);
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
                            .org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.roadm.express.parameters.express.OpenroadmOperationalMode.class,
                        new org.opendaylight.yang.gen.v1.http
                            .org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.roadm.express.parameters.express.OpenroadmOperationalModeKey(
                                operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid2)
                        .get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return new HashMap<>();
                    }
                    var orExpressOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orExpressOM);
                    maxIntroducedCd = orExpressOM.getMaxIntroducedCd().doubleValue();
                    // As per current OpenROADM Spec
                    // maxIntroducedPdl = orExpressOM.getMaxIntroducedPdl().getValue().doubleValue();
                    // Applying calculation as provided in Julia's tool
                    maxIntroducedPdl = Math.sqrt(2 * 0.2 * 0.2 + 2 * 0.4 * 0.4);
                    maxIntroducedDgd = orExpressOM.getMaxIntroducedDgd().doubleValue();
                    osnrPolynomialFits = List.of(
                        orExpressOM.getOsnrPolynomialFit().getD().doubleValue(),
                        orExpressOM.getOsnrPolynomialFit().getC().doubleValue(),
                        orExpressOM.getOsnrPolynomialFit().getB().doubleValue(),
                        orExpressOM.getOsnrPolynomialFit().getA().doubleValue());
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid2);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid2 + " :" + e);
                }
                break;

            case AMP:
                var omCatalogIid3 = InstanceIdentifier
                    .builder(OperationalModeCatalog.class)
                    .child(OpenroadmOperationalModes.class)
                    .child(Amplifiers.class)
                    .child(Amplifier.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.amplifier.parameters.amplifier.OpenroadmOperationalMode.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.amplifier.parameters.amplifier.OpenroadmOperationalModeKey(
                                    operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid3)
                        .get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return new HashMap<>();
                    }
                    var orAmpOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orAmpOM);
                    maxIntroducedCd = orAmpOM.getMaxIntroducedCd().doubleValue();
                    // As per current OpenROADM Spec
                    // maxIntroducedPdl = orAmpOM.getMaxIntroducedPdl().getValue().doubleValue();
                    // Applying calculation as provided in Julia's tool
                    maxIntroducedPdl = 0.2;
                    maxIntroducedDgd = orAmpOM.getMaxIntroducedDgd().doubleValue();
                    osnrPolynomialFits = List.of(
                        orAmpOM.getOsnrPolynomialFit().getD().doubleValue(),
                        orAmpOM.getOsnrPolynomialFit().getC().doubleValue(),
                        orAmpOM.getOsnrPolynomialFit().getB().doubleValue(),
                        orAmpOM.getOsnrPolynomialFit().getA().doubleValue());
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid3);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid3 + " :" + e);
                }
                break;
            default:
                LOG.error("Unsupported catalogNodeType {}", catalogNodeType);
                return new HashMap<>();
        }
        cd += maxIntroducedCd;
        pdl2 += Math.pow(maxIntroducedPdl, 2.0);
        dgd2 += Math.pow(maxIntroducedDgd, 2.0);
        double pwrFact = 1;
        double contrib = 0;
        // We correct PwrIn to the value corresponding to a 50 GHz Bandwidth, because OpenROADM spec (polynomial fit)
        // is based on power in 50GHz Bandwidth
        pwrIn -= 10 * Math.log10(spacing / 50.0);
        if (catalogNodeType != CatalogNodeType.ADD) {
            // For add, incremental OSNR is defined for Noiseless input, BW Correction (contrib) does not apply
            contrib = 10 * Math.log10(spacing / 50.0);
        }
        for (double fit : osnrPolynomialFits) {
            contrib += pwrFact * fit;
            pwrFact *= pwrIn;
            // Using a for loop with multiplication instead of Math.pow optimizes the computation.
        }
        // Double is not strictly spoken a Mathematics commutative group because
        // computers design limits their bits representation size.
        // As a result, the order of arithmetic operation matters.
        // In a sum, smallest numbers should be introduced first for a maximum of
        // precision. In other words, the sum
        //    10 * Math.log10(spacing / 50.0)
        //    + osnrPolynomialFits.get(0)
        //    + osnrPolynomialFits.get(1) * pwrIn
        //    + osnrPolynomialFits.get(2) * Math.pow(pwrIn, 2)
        //    + osnrPolynomialFits.get(3) * Math.pow(pwrIn, 3)
        // is not equal to its reverse form
        //    osnrPolynomialFits.get(3) * Math.pow(pwrIn, 3)
        //    + osnrPolynomialFits.get(2) * Math.pow(pwrIn, 2)
        //    + osnrPolynomialFits.get(1) * pwrIn
        //    + osnrPolynomialFits.get(0)
        //    + 10 * Math.log10(spacing / 50.0)
        // and the more precise first form should be preferred here.
        onsrLin += Math.pow(10, -contrib / 10);
        Map<String, Double> impairments = new HashMap<>();
        impairments.put("CD", cd);
        impairments.put("DGD2", dgd2);
        impairments.put("PDL2", pdl2);
        impairments.put("ONSRLIN", onsrLin);
        LOG.info("Accumulated CD is {} ps, DGD is {} ps and PDL is {} dB", cd, Math.sqrt(dgd2), Math.sqrt(pdl2));
        LOG.info("Resulting OSNR is {} dB", 10 * Math.log10(1 / onsrLin));
        return impairments;
    }

    /**
     * This method calculates power that shall be applied at the output of ROADMs and
     * Amplifiers. It retrieves the mask-power-vs-Pin and calculates target output
     * power from the span loss
     *
     * @param catalogNodeType
     *            crossed node path type (ADD/EXPRESS/AMP)
     * @param operationalModeId
     *            operational-mode-Id of the Node (OpenROADM only)
     * @param spanLoss
     *            spanLoss at the output of the ROADM
     * @param powerCorrection
     *            correction to be applied to the calculated power according to fiber type
     * @param spacing
     *            Interchannel spacing used for correction to calculate output power
     * @return outputPower
     *         Corrected output power calculated according to channel spacing
     * @throws RuntimeException
     *             if operationalModeId is not described in the catalog
     */
    public double getPceRoadmAmpOutputPower(CatalogConstant.CatalogNodeType catalogNodeType,
            String operationalModeId, double spanLoss, double spacing, double powerCorrection) {
        double pout = 99999.0;
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
                    var omOptional =
                        networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return pout;
                    }
                    var orAddOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orAddOM);
                    var mask = orAddOM.getMaskPowerVsPin();
                    for (Map.Entry<MaskPowerVsPinKey, MaskPowerVsPin> pw : mask.entrySet()) {
                        if (spanLoss >= pw.getKey().getLowerBoundary().doubleValue()
                            && spanLoss <= pw.getKey().getUpperBoundary().doubleValue()) {
                            pout = pw.getValue().getC().doubleValue() * spanLoss + pw.getValue().getD().doubleValue()
                                + powerCorrection + 10 * Math.log10(spacing / 50.0);
                            LOG.info("Calculated target Output power is {} dB in {} Bandwidth", pout, spacing);
                            return pout;
                        }
                    }
                    LOG.info("Did not succeed in calculating target Output power, SpanLoss {} dB is out of range",
                        spanLoss);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid + " :" + e);
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
                            .org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.roadm.express.parameters.express.OpenroadmOperationalMode.class,
                        new org.opendaylight.yang.gen.v1.http
                            .org.openroadm.operational.mode.catalog.rev230526
                            .operational.mode.roadm.express.parameters.express.OpenroadmOperationalModeKey(
                                operationalModeId))
                    .build();
                try {
                    var omOptional = networkTransactionService
                        .read(LogicalDatastoreType.CONFIGURATION, omCatalogIid2)
                        .get();
                    if (omOptional.isEmpty()) {
                        LOG.error(OPMODE_MISMATCH_MSG, operationalModeId);
                        return pout;
                    }
                    var orExpressOM = omOptional.orElseThrow();
                    LOG.debug("readMdSal: Operational Mode Catalog: omOptional.isPresent = true {}", orExpressOM);
                    var mask = orExpressOM.getMaskPowerVsPin();
                    for (Map.Entry<MaskPowerVsPinKey, MaskPowerVsPin> pw : mask.entrySet()) {
                        if (spanLoss >= pw.getKey().getLowerBoundary().doubleValue()
                                && spanLoss <= pw.getKey().getUpperBoundary().doubleValue()) {
                            pout = pw.getValue().getC().doubleValue() * spanLoss + pw.getValue().getD().doubleValue()
                                + powerCorrection + 10 * Math.log10(spacing / 50.0);
                            LOG.info("Calculated target Output power is {} dB in {} Bandwidth", pout, spacing);
                            return pout;
                        }
                    }
                    LOG.info("Did not succeed in calculating target Output power, SpanLoss {} dB is out of range",
                        spanLoss);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("readMdSal: Error reading Operational Mode Catalog {} , Mode does not exist",
                        omCatalogIid2);
                    throw new RuntimeException(
                        "readMdSal: Error reading from operational store, Operational Mode Catalog : "
                            + omCatalogIid2 + " :" + e);
                }
                break;

            default:
                LOG.error("Unsupported catalogNodeType {}", catalogNodeType);
        }
        return pout;
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
        } else if (spacing > 112.5) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1625;
        } else if (spacing > 100.0) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1125;
        } else if (spacing > 87.5) {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO1000;
        } else {
            constanteC0 = CatalogConstant.NLCONSTANTC0UPTO875;
        }
        double nonLinearOnsrContributionLinDb = launchedPowerdB * CatalogConstant.NLCONSTANTC1
            + constanteC0 + CatalogConstant.NLCONSTANTCE * Math.exp(CatalogConstant.NLCONSTANTEX * spanLength);
        LOG.info(" OSNR Non Linear contribution is {} dB", nonLinearOnsrContributionLinDb);
        return Math.pow(10.0, -nonLinearOnsrContributionLinDb / 10);
    }

    public boolean isCatalogFilled() {
        var omCatalogIid = InstanceIdentifier
            .builder(OperationalModeCatalog.class)
            .child(OpenroadmOperationalModes.class)
            .child(Roadms.class)
            .child(Add.class)
            .child(AddOpenroadmOperationalMode.class, new AddOpenroadmOperationalModeKey(CatalogConstant.MWWRCORE))
            .build();
        try {
            if (networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get().isEmpty()) {
                LOG.error("Operational Mode catalog is not filled");
                return false;
            }
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("readMdSal: Error reading Operational Mode Catalog, catalog not filled");
            throw new RuntimeException(
                "readMdSal: Error reading from operational store, Operational Mode Catalog not filled" + e);
        }
    }

    /**
     * This method is to get central frequency granularity.
     * @param operationalModeId
     *            operational-mode-id of a specific-operational-mode
     * @return String central frequency
     */
    public String getCFGranularity(String operationalModeId) {
        FrequencyGHz centralFrequencyGranularity;
        InstanceIdentifier<SpecificOperationalMode> omCatalogIid = InstanceIdentifier
                .builder(OperationalModeCatalog.class)
                .child(SpecificOperationalModes.class)
                .child(SpecificOperationalMode.class, new SpecificOperationalModeKey(operationalModeId))
                .build();
        try {
            var somOptional =
                    networkTransactionService.read(LogicalDatastoreType.CONFIGURATION, omCatalogIid).get();
            if (somOptional.isEmpty()) {
                LOG.error("readMdSal: Error reading Specific Operational Mode Catalog {} , empty list", omCatalogIid);
                return null;
            }
            SpecificOperationalMode speTspOM = somOptional.orElseThrow();
            centralFrequencyGranularity = FrequencyGHz.getDefaultInstance(speTspOM.getCentralFrequencyGranularity()
                    .getValue().toString());

        } catch (InterruptedException | ExecutionException e) {
            LOG.error("readMdSal: Error reading Specific Operational Mode Catalog {} , Mode does not exist",
                    omCatalogIid);
            throw new RuntimeException("Operational mode not populated in Catalog : " + omCatalogIid + " :" + e);
        }
        return centralFrequencyGranularity.getValue().toString();
    }
}
