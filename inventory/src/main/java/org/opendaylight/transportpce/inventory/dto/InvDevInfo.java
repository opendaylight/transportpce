/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.dto;

@SuppressWarnings({"checkstyle:ParameterName","checkstyle:MemberName"})
public class InvDevInfo {
    private String node_id;
    private String node_number;
    private String node_type;
    private String clli;
    private String vendor;
    private String model;
    private String serial_id;
    private String ipAddress;
    private String prefix_length;
    private String default_gateway;
    private String source;
    private String current_ipAddress;
    private String current_prefix_length;
    private String current_default_gateway;
    private String macAddress;
    private String software_version;
    private String template;
    private String current_datetime;
    private String latitude;
    private String longitude;
    private String max_degrees;
    private String max_srgs;
    private String sw_version;
    private String sw_validation_timer;
    private String activation_date_time;
    private String create_date;
    private String update_date;


    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }

    public String getNode_number() {
        return node_number;
    }

    public void setNode_number(String node_number) {
        this.node_number = node_number;
    }

    public String getNode_type() {
        return node_type;
    }

    public void setNode_type(String node_type) {
        this.node_type = node_type;
    }

    public String getClli() {
        return clli;
    }

    public void setClli(String clli) {
        this.clli = clli;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerial_id() {
        return serial_id;
    }

    public void setSerial_id(String serial_id) {
        this.serial_id = serial_id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getPrefix_length() {
        return prefix_length;
    }

    public void setPrefix_length(String prefix_length) {
        this.prefix_length = prefix_length;
    }

    public String getDefault_gateway() {
        return default_gateway;
    }

    public void setDefault_gateway(String default_gateway) {
        this.default_gateway = default_gateway;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCurrent_ipAddress() {
        return current_ipAddress;
    }

    public void setCurrent_ipAddress(String current_ipAddress) {
        this.current_ipAddress = current_ipAddress;
    }

    public String getCurrent_prefix_length() {
        return current_prefix_length;
    }

    public void setCurrent_prefix_length(String current_prefix_length) {
        this.current_prefix_length = current_prefix_length;
    }

    public String getCurrent_default_gateway() {
        return current_default_gateway;
    }

    public void setCurrent_default_gateway(String current_default_gateway) {
        this.current_default_gateway = current_default_gateway;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getSoftware_version() {
        return software_version;
    }

    public void setSoftware_version(String software_version) {
        this.software_version = software_version;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getCurrent_datetime() {
        return current_datetime;
    }

    public void setCurrent_datetime(String current_datetime) {
        this.current_datetime = current_datetime;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getMax_degrees() {
        return max_degrees;
    }

    public void setMax_degrees(String max_degrees) {
        this.max_degrees = max_degrees;
    }

    public String getMax_srgs() {
        return max_srgs;
    }

    public void setMax_srgs(String max_srgs) {
        this.max_srgs = max_srgs;
    }

    public String getSw_version() {
        return sw_version;
    }

    public void setSw_version(String sw_version) {
        this.sw_version = sw_version;
    }

    public String getSw_validation_timer() {
        return sw_validation_timer;
    }

    public void setSw_validation_timer(String sw_validation_timer) {
        this.sw_validation_timer = sw_validation_timer;
    }

    public String getActivation_date_time() {
        return activation_date_time;
    }

    public void setActivation_date_time(String activation_date_time) {
        this.activation_date_time = activation_date_time;
    }

    public String getCreate_date() {
        return create_date;
    }

    public void setCreate_date(String create_date) {
        this.create_date = create_date;
    }

    public String getUpdate_date() {
        return update_date;
    }

    public void setUpdate_date(String update_date) {
        this.update_date = update_date;
    }
}
