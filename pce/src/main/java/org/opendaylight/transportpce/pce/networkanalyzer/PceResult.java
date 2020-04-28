/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.networkanalyzer;

import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ZToADirection;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PceResult {
    private static final Logger LOG = LoggerFactory.getLogger(PceResult.class);
    private String calcMessage = "503 Calculator Unavailable";
    private boolean calcStatus = false;
    private String responseCode = ResponseCodes.RESPONSE_FAILED;
    private long resultWavelength = -1;
    private Map<String, Uint16> resultTribPort;
    private Map<String, List<Uint16>> resultTribSlot;
    private Integer resultTribSlotNb = -1;
    private String serviceType = "";

    // for now it is constant returned as received from A-end
    private long rate = -1;
    private  ServiceFormat serviceFormat = ServiceFormat.OC;

    public enum LocalCause {
        NONE, TOO_HIGH_LATENCY, OUT_OF_SPEC_OSNR, NO_PATH_EXISTS, INT_PROBLEM, HD_NODE_INCLUDE;
    }

    private LocalCause localCause = LocalCause.NONE;

    private AToZDirection atozdirection = null;
    private ZToADirection ztoadirection = null;

    public PceResult() {
    }

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

    public Map<String, Uint16> getResultTribPort() {
        return resultTribPort;
    }

    public void setResultTribPort(Map<String, Uint16> resultTribPort) {
        this.resultTribPort = resultTribPort;
    }

    public Map<String, List<Uint16>> getResultTribSlot() {
        return resultTribSlot;
    }

    public void setResultTribSlot(Map<String, List<Uint16>> resultTribSlot) {
        this.resultTribSlot = resultTribSlot;
    }

    public int getResultTribSlotNb() {
        return resultTribSlotNb;
    }

    public void setResultTribSlotNb(int resultTribSlotNb) {
        this.resultTribSlotNb = resultTribSlotNb;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

}
