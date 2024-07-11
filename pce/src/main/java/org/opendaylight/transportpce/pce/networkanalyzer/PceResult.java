/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.external._interface.characteristics.SupportedOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.external._interface.characteristics.SupportedOperationalModesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceResult {
    private static final Logger LOG = LoggerFactory.getLogger(PceResult.class);
    private String calcMessage = "503 Calculator Unavailable";
    private boolean calcStatus = false;
    private String responseCode = ResponseCodes.RESPONSE_FAILED;
    private long resultWavelength = GridConstant.IRRELEVANT_WAVELENGTH_NUMBER;
    private List<OpucnTribSlotDef> resultTribSlotDefList;
    private String serviceType = "";
    private BigDecimal minFreq;
    private BigDecimal maxFreq;

    // for now it is constant returned as received from A-end
    private long rate = -1;
    private  ServiceFormat serviceFormat = ServiceFormat.OC;
    private Map<SupportedOperationalModesKey, SupportedOperationalModes> supportedOperationalModes;

    public enum LocalCause {
        NONE, TOO_HIGH_LATENCY, OUT_OF_SPEC_OSNR, NO_PATH_EXISTS, INT_PROBLEM, HD_NODE_INCLUDE;
    }

    private LocalCause localCause = LocalCause.NONE;

    private AToZDirection atozdirection = null;
    private ZToADirection ztoadirection = null;

    public void setRC(String rc) {
        switch (rc) {
            case ResponseCodes.RESPONSE_OK :
                calcMessage = "Path is calculated by PCE";
                calcStatus = true;
                responseCode = ResponseCodes.RESPONSE_OK;
                break;
            case ResponseCodes.RESPONSE_FAILED :
                responseCode = ResponseCodes.RESPONSE_FAILED;
                calcStatus = false;
                calcMessage = "No path available by PCE";
                break;
            default:
                LOG.error("setRC: RespondeCodes unknown");
        }
    }

    public String toString() {
        return ("[" + calcMessage + "] code:[" + responseCode + "] wavelength=" + resultWavelength + " localCause="
                + localCause + " rate=" + rate + " serviceType = " + serviceType);
    }

    public boolean getStatus() {
        return calcStatus;
    }

    public String getMessage() {
        return calcMessage;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public long getResultWavelength() {
        return resultWavelength;
    }

    public void setResultWavelength(long resultWavelength) {
        this.resultWavelength = resultWavelength;
    }

    public AToZDirection getAtoZDirection() {
        return atozdirection;
    }

    public ZToADirection getZtoADirection() {
        return ztoadirection;
    }

    public void setAtoZDirection(AToZDirection atozDirection) {
        this.atozdirection = atozDirection;
    }

    public void setZtoADirection(ZToADirection ztoaDirection) {
        this.ztoadirection = ztoaDirection;
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public ServiceFormat getServiceFormat() {
        return serviceFormat;
    }

    public void setServiceFormat(ServiceFormat serviceFormat) {
        this.serviceFormat = serviceFormat;
    }

    public LocalCause getLocalCause() {
        return localCause;
    }

    public void setLocalCause(LocalCause lc) {
        this.localCause = lc;
    }

    public void setCalcMessage(String calcMessage) {
        this.calcMessage = calcMessage;
    }

    public List<OpucnTribSlotDef> getResultTribPortTribSlot() {
        return resultTribSlotDefList;
    }

    public void setResultTribPortTribSlot(List<OpucnTribSlotDef> resultTribPortTribSlot) {
        this.resultTribSlotDefList = resultTribPortTribSlot;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Get the minimal frequency.
     * @return the minFreq.
     */
    public BigDecimal getMinFreq() {
        return minFreq;
    }

    /**
     * Set the minimal frequency.
     * @param minFreq the minFreq to set.
     */
    public void setMinFreq(BigDecimal minFreq) {
        this.minFreq = minFreq;
    }

    /**
     * Get the maximal frequency.
     * @return the maxFreq.
     */
    public BigDecimal getMaxFreq() {
        return maxFreq;
    }

    /**
     * Set the maximal frequency.
     * @param maxFreq the maxFreq to set.
     */
    public void setMaxFreq(BigDecimal maxFreq) {
        this.maxFreq = maxFreq;
    }

    /**
     * Set operational mode given the PCE.
     * @param supportedOperationalModes Operational mode ID given the PCE.
     */
    public void setSupportedOperationalModes(Map<SupportedOperationalModesKey, SupportedOperationalModes>
                                                     supportedOperationalModes) {
        this.supportedOperationalModes = supportedOperationalModes;
    }

    public Map<SupportedOperationalModesKey, SupportedOperationalModes> getSupportedOperationalModes() {
        return supportedOperationalModes;
    }

    public AToZDirection setAtoZOpticalOperationalMode(AToZDirection atozDirection, String opticalOperationalMode) {
        LOG.info("This is oom in pceresult {}", opticalOperationalMode);
        AToZDirectionBuilder atozDirectionBuilder = new AToZDirectionBuilder(atozDirection)
                .setOpticalOperationalMode(opticalOperationalMode);
        return atozDirectionBuilder.build();
    }

    public ZToADirection setZtoAOpticalOperationalMode(ZToADirection ztoaDirection, String opticalOperationalMode) {
        ZToADirectionBuilder ztoaDirectionBuilder = new ZToADirectionBuilder(ztoaDirection)
                .setOpticalOperationalMode(opticalOperationalMode);
        return ztoaDirectionBuilder.build();
    }

}
