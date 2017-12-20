/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.query;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class Queries {

    private static final String DEVICE_INFO_INSERT =
        "INSERT INTO %sinv_dev_info "
        + "(node_id, node_number, node_type, clli, vendor, model, serial_id, ipAddress, prefix_length, "
        + "default_gateway, source, current_ipAddress, current_prefix_length, current_default_gateway, macAddress, "
        + "software_version, template, current_datetime, latitude, longitude, max_degrees, max_srgs, sw_version, "
        + "sw_validation_timer, activation_date_time, create_date, update_date) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_SHELF_INSERT =
        "INSERT INTO %sinv_dev_shelf "
        + "(node_id, shelf_name, shelf_type, rack, shelf_position, administrative_state, vendor, model, serial_id, "
        + "type, product_code, manufacture_date, clei, hardware_version, operational_state, equipment_state, due_date, "
        + "create_date, update_date) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_SHELF_SLOT_INSERT =
        "INSERT INTO %sinv_dev_shelf_slot "
        + "(node_id, shelf_name, slot_name, label, provisioned_circuit_pack, create_date, update_date) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_CP_INSERT =
        "INSERT INTO %sinv_dev_circuit_pack "
        + "(node_id, circuitPackName, circuitPackType, circuitPackProductCode, administrative_state, vendor, model, "
        + "serial_id, type, product_code, manufacture_date, clei, hardware_version, operational_state, "
        + "equipment_state, shelf, slot, subSlo, cpCatType, extension, due_date, parentCpName, parentCpSlotName) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SERVICE_INSERT =
        "INSERT INTO %sinv_ser_main "
        + "(service_name, common_id, sdnc_req_header_request_id, sdnc_req_header_rpc_action, "
        + "sdnc_req_header_notification_url, sdnc_req_header_request_system_id, connection_type, lifecycle_state, "
        + "administrative_state, operational_state, serv_condition, a_end_service_format, a_end_service_rate, "
        + "a_end_clli, a_end_node_id, a_end_tx_port_device_name, a_end_tx_port_type, a_end_tx_port_name, "
        + "a_end_tx_port_rack, a_end_tx_port_shelf, a_end_tx_port_slot, a_end_tx_port_sub_slot, "
        + "a_end_tx_lgx_device_name, a_end_tx_lgx_port_name, a_end_tx_lgx_port_rack, a_end_tx_lgx_port_shelf, "
        + "a_end_tx_tail_rnode_id, a_end_tx_xport_cp_name, a_end_tx_xport_port_name, a_end_tx_tail_roadm_port_aid, "
        + "a_end_tx_tail_roadm_port_rack_loc, a_end_rx_port_device_name, a_end_rx_port_type, a_end_rx_port_name, "
        + "a_end_rx_port_rack, a_end_rx_port_shelf, a_end_rx_port_slot, a_end_rx_port_sub_slot, "
        + "a_end_rx_lgx_device_name, a_end_rx_lgx_port_name, a_end_rx_lgx_port_rack, a_end_rx_lgx_port_shelf, "
        + "a_end_rx_tail_rnode_id, a_end_rx_xport_cp_name, a_end_rx_xport_port_name, a_end_rx_tail_roadm_port_aid, "
        + "a_end_rx_tail_roadm_port_rack_loc, a_end_optic_type, a_end_router_node_id, a_end_router_ip_address, "
        + "a_end_router_url, a_end_user_label, z_end_service_format, z_end_service_rate, z_end_clli, z_end_node_id, "
        + "z_end_tx_port_device_name, z_end_tx_port_type, z_end_tx_port_name, z_end_tx_port_rack, z_end_tx_port_shelf, "
        + "z_end_tx_port_slot, z_end_tx_port_sub_slot, z_end_tx_lgx_device_name, z_end_tx_lgx_port_name, "
        + "z_end_tx_lgx_port_rack, z_end_tx_lgx_port_shelf, z_end_tx_tail_rnode_id, z_end_tx_xport_cp_name, "
        + "z_end_tx_xport_port_name, z_end_tx_tail_roadm_port_aid, z_end_tx_tail_roadm_port_rack_loc, "
        + "z_end_rx_port_device_name, z_end_rx_port_type, z_end_rx_port_name, z_end_rx_port_rack, z_end_rx_port_shelf, "
        + "z_end_rx_port_slot, z_end_rx_port_sub_slot, z_end_rx_lgx_device_name, z_end_rx_lgx_port_name, "
        + "z_end_rx_lgx_port_rack, z_end_rx_lgx_port_shelf, z_end_rx_tail_rnode_id, z_end_rx_xport_cp_name, "
        + "z_end_rx_xport_port_name, z_end_rx_tail_roadm_port_aid, z_end_rx_tail_roadm_port_rack_loc, "
        + "z_end_optic_type, z_end_router_node_id, z_end_router_ip_address, z_end_router_url, z_end_user_label, "
        + "due_date, end_date, nc_code, nci_code, secondary_nci_code, customer, customer_contact, operator_contact, "
        + "latency, fiber_span_srlgs, supp_serv_name, create_date, update_date) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SERVICE_DELETE =
        "DELETE FROM %sinv_ser_main WHERE service_name = ?";

    private Queries() {
        // util class
    }

    public static Query getQuery() {
        return new Query();
    }

    public static class Query {
        private String sql;
        private String schema;

        private Query() {
            this.schema = "";
        }

        public Query withSchema(String schema) {
            this.schema = schema;
            return this;
        }

        public Query deviceInfoInsert() {
            this.sql = DEVICE_INFO_INSERT;
            return this;
        }

        public Query deviceShelfInsert() {
            this.sql = DEVICE_SHELF_INSERT;
            return this;
        }

        public Query deviceShelfSlotInsert() {
            this.sql = DEVICE_SHELF_SLOT_INSERT;
            return this;
        }

        public Query deviceCircuitPackInsert() {
            this.sql = DEVICE_CP_INSERT;
            return this;
        }

        public Query serviceCreate() {
            this.sql = SERVICE_INSERT;
            return this;
        }

        public Query serviceDelete() {
            this.sql = SERVICE_DELETE;
            return this;
        }

        public String get() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(this.sql), "No query selected");
            return String.format(this.sql, this.schema.concat("."));
        }
    }
}
