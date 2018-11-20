/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.ServiceEndpoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.routing.constrains.rev161014.routing.constraints.SoftConstraints;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceDeleteInput;

/**
 * Super class of {@link ServiceCreateInput} and {@link TempServiceCreateInput}.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class ServiceInput {
    private String serviceName;
    private String commonId;
    private ConnectionType connectionType;
    private SdncRequestHeader sdncRequestHeader;
    private HardConstraints hardConstraints;
    private SoftConstraints softConstraints;
    private ServiceEndpoint serviceAEnd;
    private ServiceEndpoint serviceZEnd;
    private String customer;
    private String customerContact;

    public ServiceInput(TempServiceCreateInput tempServiceCreateInput) {
        setServiceName(tempServiceCreateInput.getCommonId());
        setCommonId(tempServiceCreateInput.getCommonId());
        setConnectionType(tempServiceCreateInput.getConnectionType());
        setSdncRequestHeader(tempServiceCreateInput.getSdncRequestHeader());
        setHardConstraints(tempServiceCreateInput.getHardConstraints());
        setSoftConstraints(tempServiceCreateInput.getSoftConstraints());
        setServiceAEnd(tempServiceCreateInput.getServiceAEnd());
        setServiceZEnd(tempServiceCreateInput.getServiceZEnd());
        setCustomer(tempServiceCreateInput.getCustomer());
        setCustomerContact(tempServiceCreateInput.getCustomerContact());
    }

    public ServiceInput(ServiceCreateInput serviceCreateInput) {
        setServiceName(serviceCreateInput.getServiceName());
        setCommonId(serviceCreateInput.getCommonId());
        setConnectionType(serviceCreateInput.getConnectionType());
        setSdncRequestHeader(serviceCreateInput.getSdncRequestHeader());
        setHardConstraints(serviceCreateInput.getHardConstraints());
        setSoftConstraints(serviceCreateInput.getSoftConstraints());
        setServiceAEnd(serviceCreateInput.getServiceAEnd());
        setServiceZEnd(serviceCreateInput.getServiceZEnd());
        setCustomer(serviceCreateInput.getCustomer());
        setCustomerContact(serviceCreateInput.getCustomerContact());
    }

    public ServiceInput(ServiceDeleteInput serviceDeleteInput) {
        setServiceName(serviceDeleteInput.getServiceDeleteReqInfo().getServiceName());
        setSdncRequestHeader(serviceDeleteInput.getSdncRequestHeader());
    }

    public ServiceInput(TempServiceDeleteInput tempServiceDeleteInput) {
        String comId = tempServiceDeleteInput.getCommonId();
        setServiceName(comId);
        setCommonId(comId);
        setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId(comId).build());
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getCommonId() {
        return commonId;
    }

    public void setCommonId(String commonId) {
        this.commonId = commonId;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public SdncRequestHeader getSdncRequestHeader() {
        return sdncRequestHeader;
    }

    public void setSdncRequestHeader(SdncRequestHeader sdncRequestHeader) {
        this.sdncRequestHeader = sdncRequestHeader;
    }

    public ServiceEndpoint getServiceAEnd() {
        return serviceAEnd;
    }

    public void setServiceAEnd(ServiceEndpoint serviceAEnd) {
        this.serviceAEnd = serviceAEnd;
    }

    public ServiceEndpoint getServiceZEnd() {
        return serviceZEnd;
    }

    public void setServiceZEnd(ServiceEndpoint serviceZEnd) {
        this.serviceZEnd = serviceZEnd;
    }

    public HardConstraints getHardConstraints() {
        return hardConstraints;
    }

    public void setHardConstraints(HardConstraints hardConstraints) {
        this.hardConstraints = hardConstraints;
    }

    public SoftConstraints getSoftConstraints() {
        return softConstraints;
    }

    public void setSoftConstraints(SoftConstraints softConstraints) {
        this.softConstraints = softConstraints;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }
}
