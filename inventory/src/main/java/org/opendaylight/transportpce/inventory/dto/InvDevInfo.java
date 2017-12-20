/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.dto;

public class InvDevInfo {
    private String nodeId;
    private String nodeNumber;
    private String nodeType;
    private String clli;
    private String vendor;
    private String model;
    private String serialId;
    private String ipAddress;
    private String prefixLength;
    private String defaultGateway;
    private String source;
    private String currentIpAddress;
    private String currentPrefixLength;
    private String currentDefaultGateway;
    private String macAddress;
    private String softwareVersion;
    private String template;
    private String currentDatetime;
    private String latitude;
    private String longitude;
    private String maxDegrees;
    private String maxSrgs;
    private String swVersion;
    private String swValidationTimer;
    private String activationDateTime;
    private String createDate;
    private String updateDate;


    public String getNodeId() {
        return this.nodeId;
    }

    public void setNode_id(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNode_number() {
        return this.nodeNumber;
    }

    public void setNode_number(String nodeNumber) {
        this.nodeNumber = nodeNumber;
    }

    public String getNode_type() {
        return this.nodeType;
    }

    public void setNode_type(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getClli() {
        return this.clli;
    }

    public void setClli(String clli) {
        this.clli = clli;
    }

    public String getVendor() {
        return this.vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return this.model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerial_id() {
        return this.serialId;
    }

    public void setSerial_id(String serialId) {
        this.serialId = serialId;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPrefix_length() {
        return this.prefixLength;
    }

    public void setPrefix_length(String prefixLength) {
        this.prefixLength = prefixLength;
    }

    public String getDefault_gateway() {
        return this.defaultGateway;
    }

    public void setDefault_gateway(String defaultGateway) {
        this.defaultGateway = defaultGateway;
    }

    public String getSource() {
        return this.source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCurrent_ipAddress() {
        return this.currentIpAddress;
    }

    public void setCurrent_ipAddress(String currentIpAddress) {
        this.currentIpAddress = currentIpAddress;
    }

    public String getCurrent_prefix_length() {
        return this.currentPrefixLength;
    }

    public void setCurrent_prefix_length(String currentPrefixLength) {
        this.currentPrefixLength = currentPrefixLength;
    }

    public String getCurrent_default_gateway() {
        return this.currentDefaultGateway;
    }

    public void setCurrent_default_gateway(String currentDefaultGateway) {
        this.currentDefaultGateway = currentDefaultGateway;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getSoftware_version() {
        return this.softwareVersion;
    }

    public void setSoftware_version(String softwareVersion) {
        this.softwareVersion = softwareVersion;
    }

    public String getTemplate() {
        return this.template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getCurrent_datetime() {
        return this.currentDatetime;
    }

    public void setCurrent_datetime(String currentDatetime) {
        this.currentDatetime = currentDatetime;
    }

    public String getLatitude() {
        return this.latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return this.longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getMax_degrees() {
        return this.maxDegrees;
    }

    public void setMax_degrees(String maxDegrees) {
        this.maxDegrees = maxDegrees;
    }

    public String getMax_srgs() {
        return this.maxSrgs;
    }

    public void setMax_srgs(String maxSrgs) {
        this.maxSrgs = maxSrgs;
    }

    public String getSw_version() {
        return this.swVersion;
    }

    public void setSw_version(String swVersion) {
        this.swVersion = swVersion;
    }

    public String getSw_validation_timer() {
        return this.swValidationTimer;
    }

    public void setSw_validation_timer(String swValidationTimer) {
        this.swValidationTimer = swValidationTimer;
    }

    public String getActivation_date_time() {
        return this.activationDateTime;
    }

    public void setActivation_date_time(String activationDateTime) {
        this.activationDateTime = activationDateTime;
    }

    public String getCreate_date() {
        return this.createDate;
    }

    public void setCreate_date(String createDate) {
        this.createDate = createDate;
    }

    public String getUpdate_date() {
        return this.updateDate;
    }

    public void setUpdate_date(String updateDate) {
        this.updateDate = updateDate;
    }
}
