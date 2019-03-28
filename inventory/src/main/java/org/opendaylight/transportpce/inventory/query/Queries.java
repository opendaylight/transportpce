/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory.query;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

@SuppressWarnings("checkstyle:LineLength")
public final class Queries {

    private static final String DEVICE_INFO_INSERT =
            "INSERT INTO %sinv_dev_info (node_id, node_number, node_type, clli, vendor, model, serial_id, ipAddress, prefix_length, default_gateway, "
            + "source, current_ipAddress, current_prefix_length, current_default_gateway, macAddress, software_version, openroadm_version, "
            + "template, current_datetime, geo_latitude, geo_longitude, max_degrees, max_srgs, max_num_bin_15min_historical_pm, "
            + "max_num_bin_24hour_historical_pm, sw_version, sw_validation_timer, activation_date_time, create_date, update_date) VALUES "
            + "(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_SHELF_INSERT = "INSERT INTO %sinv_dev_shelf"
            + " (node_id, shelf_name, shelf_type, rack, shelf_position, administrative_state, vendor, model, serial_id, type, product_code, manufacture_date, clei, hardware_version, operational_state, equipment_state, due_date, "
            + "create_date, update_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_SHELF_SLOT_INSERT = "INSERT INTO %sinv_dev_shelf_slot "
            + " (node_id, shelf_name, slot_name, label, provisioned_circuit_pack, slot_status, create_date, update_date)  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_CP_INSERT = "INSERT INTO %sinv_dev_circuit_pack"
            + " (node_id, circuit_pack_name, circuit_pack_type, circuit_pack_product_code, administrative_state, vendor, model, serial_id, type, "
            + " product_code, manufacture_date, clei, hardware_version, operational_state, cpc_type, cpc_extension, equipment_state, circuit_pack_mode, shelf, slot, "
            + " subSlot, due_date, pcp_circuit_pack_name, pcp_cp_slot_name, create_date, update_date) "
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    private static final String DEVICE_CP_PORT_INSERT = "INSERT INTO %sinv_dev_cp_ports "
            + " (node_id, circuit_pack_name, port_name, port_type, port_qual, port_wavelength_type, port_direction, label, circuit_id, "
            + "administrative_state, operational_state, logical_connection_point, partner_port_circuit_pack_name, partner_port_port_name, parent_port_circuit_pack_name, "
            + "parent_port_port_name, roadm_port_port_power_capability_min_rx, roadm_port_port_power_capability_min_tx, roadm_port_port_power_capability_max_rx, "
            + "roadm_port_port_power_capability_max_tx, roadm_port_capable_wavelengths, roadm_port_available_wavelengths, roadm_port_used_wavelengths, "
            + "transponder_port_port_power_capability_min_rx, transponder_port_port_power_capability_min_tx, transponder_port_port_power_capability_max_rx, "
            + "transponder_port_port_power_capability_max_tx, transponder_port_capable_wavelengths, otdr_port_launch_cable_length, otdr_port_port_direction, "
            + "ila_port_port_power_capability_mix_rx, ila_port_port_power_capability_mix_tx, ila_port_port_power_capability_max_rx, ila_port_port_power_capability_max_tx, "
            + "create_date, update_date)  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_CP_SLOT_INSERT = "INSERT INTO %sinv_dev_cp_slots "
            + " (node_id, circuit_pack_name, slot_name, label, provisioned_circuit_pack, slot_status, slot_type, create_date, update_date)  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERFACES_INSERT = "INSERT INTO %sinv_dev_interfaces "
            + " (node_id, name, description, type, administrative_state, operational_state, circuit_id, supporting_interface, supporting_circuit_pack_name, "
            + "supporting_port, ethernet_speed, ethernet_fec, ethernet_duplex, ethernet_mtu, ethernet_auto_negotiation, ethernet_curr_speed, "
            + "ethernet_curr_duplex, mci_mcttp_min_freq, mci_mcttp_max_freq, mci_mcttp_center_freq, mci_mcttp_slot_width, mci_nmc_ctp_frequency, mci_nmc_ctp_width, "
            + "och_rate, och_frequency, och_width, och_wavelength_number, och_modulation_format, och_transmit_power, ots_fiber_type, ots_span_loss_receive, "
            + "ots_span_loss_transmit, ots_ingress_span_loss_aging_margin, ots_eol_max_load_pin, odu_rate, odu_function, odu_monitoring_mode, odu_no_oam_function, "
            + "odu_proactive_delay_measurement_enabled, odu_poa_trib_port_number, odu_tx_sapi, odu_tx_dapi, odu_tx_operator, odu_accepted_sapi, odu_accepted_dapi, "
            + "odu_accepted_operator, odu_expected_sapi, odu_expected_dapi, odu_tim_act_enabled, odu_tim_detect_mode, odu_degm_intervals, odu_degthr_percentage, "
            + "opu_payload_type, opu_rx_payload_type, opu_exp_payload_type, opu_payload_interface, maint_testsignal_enabled, maint_testsignal_testpattern, "
            + "maint_testsignal_type, maint_testsignal_biterrors, maint_testsignal_biterrorsterminal, maint_testsignal_syncseconds, "
            + "maint_testsignal_syncsecondsterminal, otu_rate, otu_fec, otu_tx_sapi, otu_tx_dapi, otu_tx_operator, otu_accepted_sapi, otu_accepted_dapi, "
            + "otu_accepted_operator, otu_expected_sapi, otu_expected_dapi, otu_tim_act_enabled, otu_tim_detect_mode, otu_degm_intervals, otu_degthr_percentage, "
            + "otu_maint_loopback_enabled, otu_maint_type, mt_otu_rate, mt_otu_fec, mt_otu_maint_loopback, mt_otu_enabled, mt_otu_type, create_date, update_date)  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
            + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERFACE_ODU_OPU_MIS_TX_INSERT = "INSERT INTO %sinv_dev_interface_odu_opu_tx_msi "
            + " (node_id, interface_name, trib_slot, odtu_type, trib_port, trib_port_payload, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERFACE_ODU_OPU_MIS_RX_INSERT = "INSERT INTO %sinv_dev_interface_odu_opu_rx_msi "
            + " (node_id, interface_name, trib_slot, odtu_type, trib_port, trib_port_payload, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERFACE_ODU_OPU_MIS_EXP_INSERT = "INSERT INTO %sinv_dev_interface_odu_opu_exp_msi "
            + " (node_id, interface_name, trib_slot, odtu_type, trib_port, trib_port_payload, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERFACE_TCM_INSERT = "INSERT INTO %sinv_dev_interface_odu_tcm "
            + " (node_id, interface_name, layer, monitoring_mode, ltc_act_enabled, proactive_delay_measurement_enabled, tcm_direction, tx_sapi, tx_dapi, tx_operator, accepted_sapi,"
            + " accepted_dapi, accepted_operator, expected_sapi, expected_dapi, tim_act_enabled, tim_detect_mode, degm_intervals, degthr_percentage, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

    private static final String DEVICE_PROTOCOL_INSERT =
            "INSERT INTO %sinv_dev_proto_lldp"
                + "(node_id,"
                + "adminstatus,"
                + "msgtxinterval,"
                + "msgtxholdmultiplier,"
                + "create_date,"
                + "update_date )"
                + "values ( ?,"
                + "?,"
                + "?,"
                + "?,"
                + "?,"
                + "? )";

    private static final String DEVICE_PROTOCOL_PORT_CONFIG_INSERT = "INSERT INTO %sinv_dev_proto_lldp_port_config "
            + " (node_id, ifname, adminstatus, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ? )";


    private static final String DEVICE_PROTOCOL_LLDP_NBR_LIST_INSERT = "INSERT INTO %sinv_dev_proto_lldp_nbr_lst "
            + " (node_id, ifname, remotesysname, remotemgmtaddresssubtype, remotemgmtaddress, remoteportidsubtype, remoteportid, remotechassisidsubtype, remotechassisid, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


    private static final String DEVICE_PROTOCOL_RSTP_INSERT = "INSERT INTO %sinv_dev_proto_rstp "
            + " (node_id, bridge_name, bridge_priority, shutdown, hold_time, hello_time, max_age, forward_delay, transmit_hold_count, "
            + "root_bridge_port, root_path_cost, root_bridge_priority, root_bridge_id, root_hold_time, root_hello_time, root_max_age, "
            + "root_forward_delay, bridge_id, topo_change_count, time_since_topo_change, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_PROTOCOL_RSTP_BRIDGEPORT_INSERT = "INSERT INTO %sinv_dev_proto_rstp_bridge_port "
            + " (node_id, bridge_name, ifname, cost, priority, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_PROTOCOL_RSTP_BRIDGEPORT_ATTR_INSERT = "INSERT INTO %sinv_dev_proto_rstp_bridge_port_attr "
            + " (node_id, bridge_name, ifname, bridge_port_state, bridge_port_role, bridge_port_id, open_edge_bridge_port, designated_bridge_port, designated_bridgeid, create_date, update_date  )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String DEVICE_INTERNAL_LINK_INSERT = "INSERT INTO %sinv_dev_internal_link "
            + " (node_id, internal_link_name, source_circuit_pack_name, source_port_name, destination_circuit_pack_name, destination_port_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_EXTERNAL_LINK_INSERT = "INSERT INTO %sinv_dev_external_link "
            + " (node_id, external_link_name, source_node_id, source_circuit_pack_name, source_port_name, destination_node_id, destination_circuit_pack_name, destination_port_name, create_date, update_date)  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_PHYSICAL_LINK_INSERT = "INSERT INTO %sinv_dev_physical_link "
            + " (node_id, physical_link_name, source_circuit_pack_name, source_port_name, destination_circuit_pack_name, destination_port_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_DEGREE_INSERT = "INSERT INTO %sinv_dev_degree "
            + " (node_id, degree_number, max_wavelengths, otdr_port_circuit_pack_name, otdr_port_port_name, mc_capabilities_slot_width_granularity, mc_capabilities_center_freq_granularity, mc_capabilities_min_slots, mc_capabilities_max_slots, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_DEGREE_CIRCUITPACK_INSERT = "INSERT INTO %sinv_dev_degree_circuit_packs "
            + " (node_id, degree_number, cktpk_index, circuit_pack_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_CONNECTION_PORT_INSERT = "INSERT INTO %sinv_dev_degree_connection_ports "
            + " (node_id, degree_number, conn_port_index, circuit_pack_name, port_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_SHARED_RISK_GROUP_INSERT = "INSERT INTO %sinv_dev_srg "
            + " (node_id, max_add_drop_ports, current_provisioned_add_drop_ports, srg_number, wavelength_duplication, mc_cap_slot_width_granularity, mc_cap_center_freq_granularity, mc_cap_min_slots, mc_cap_max_slots, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_SRG_CIRCUITPACK_INSERT = "INSERT INTO %sinv_dev_srg_circuit_pack "
            + " (node_id, srg_number, ckptk_index, circuit_pack_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_ROADM_CONNECTIONS_INSERT = "INSERT INTO %sinv_dev_roadm_connections "
            + " (node_id, connection_name, connection_number, wavelength_number, opticalcontrolmode, target_output_power, src_if, dst_if, create_date, update_date  )  "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_CONNECTION_MAP_INSERT = "INSERT INTO %sinv_dev_connection_map "
            + " (node_id, connection_map_number, source_circuit_pack_name, source_port_name, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DEVICE_WAVELENGTH_INSERT = "INSERT INTO %sinv_dev_wavelength_map "
            + " (node_id, wavelength_number, center_frequency, wavelength, create_date, update_date )  "
            + "VALUES (?, ?, ?, ?, ?, ?)";


    private static final String SERVICE_PATH_LIST =
            "INSERT INTO %sinv_ser_pathlist ( path_name ) values (?)";

    private static final String SERVICE_UPDATE = "UPDATE %sinv_ser_main "
            + "set "
            + "common_id = ? "
            + "sdnc_req_header_request_id = ? "
            + "sdnc_req_header_rpc_action = ? "
            + "sdnc_req_header_notification_url = ? "
            + "sdnc_req_header_request_system_id = ? "
            + "connection_type = ? "
            + "lifecycle_state = ? "
            + "administrative_state = ? "
            + "operational_state = ? "
            + "serv_condition = ? "
            + "a_end_service_format = ? "
            + "a_end_service_rate = ? "
            + "a_end_clli = ? "
            + "a_end_node_id = ? "
            + "a_end_tx_port_device_name = ? "
            + "a_end_tx_port_type = ? "
            + "a_end_tx_port_name = ? "
            + "a_end_tx_port_rack = ? "
            + "a_end_tx_port_shelf = ? "
            + "a_end_tx_port_slot = ? "
            + "a_end_tx_port_sub_slot = ? "
            + "a_end_tx_lgx_device_name = ? "
            + "a_end_tx_lgx_port_name = ? "
            + "a_end_tx_lgx_port_rack = ? "
            + "a_end_tx_lgx_port_shelf = ? "
            + "a_end_tx_tail_rnode_id = ? "
            + "a_end_tx_xport_cp_name = ? "
            + "a_end_tx_xport_port_name = ? "
            + "a_end_tx_tail_roadm_port_aid = ? "
            + "a_end_tx_tail_roadm_port_rack_loc = ? "
            + "a_end_rx_port_device_name = ? "
            + "a_end_rx_port_type = ? "
            + "a_end_rx_port_name = ? "
            + "a_end_rx_port_rack = ? "
            + "a_end_rx_port_shelf = ? "
            + "a_end_rx_port_slot = ? "
            + "a_end_rx_port_sub_slot = ? "
            + "a_end_rx_lgx_device_name = ? "
            + "a_end_rx_lgx_port_name = ? "
            + "a_end_rx_lgx_port_rack = ? "
            + "a_end_rx_lgx_port_shelf = ? "
            + "a_end_rx_tail_rnode_id = ? "
            + "a_end_rx_xport_cp_name = ? "
            + "a_end_rx_xport_port_name = ? "
            + "a_end_rx_tail_roadm_port_aid = ? "
            + "a_end_rx_tail_roadm_port_rack_loc = ? "
            + "a_end_optic_type = ? "
            + "a_end_router_node_id = ? "
            + "a_end_router_ip_address = ? "
            + "a_end_router_url = ? "
            + "a_end_user_label = ? "
            + "z_end_service_format = ? "
            + "z_end_service_rate = ? "
            + "z_end_clli = ? "
            + "z_end_node_id = ? "
            + "z_end_tx_port_device_name = ? "
            + "z_end_tx_port_type = ? "
            + "z_end_tx_port_name = ? "
            + "z_end_tx_port_rack = ? "
            + "z_end_tx_port_shelf = ? "
            + "z_end_tx_port_slot = ? "
            + "z_end_tx_port_sub_slot = ? "
            + "z_end_tx_lgx_device_name = ? "
            + "z_end_tx_lgx_port_name = ? "
            + "z_end_tx_lgx_port_rack = ? "
            + "z_end_tx_lgx_port_shelf = ? "
            + "z_end_tx_tail_rnode_id = ? "
            + "z_end_tx_xport_cp_name = ? "
            + "z_end_tx_xport_port_name = ? "
            + "z_end_tx_tail_roadm_port_aid = ? "
            + "z_end_tx_tail_roadm_port_rack_loc = ? "
            + "z_end_rx_port_device_name = ? "
            + "z_end_rx_port_type = ? "
            + "z_end_rx_port_name = ? "
            + "z_end_rx_port_rack = ? "
            + "z_end_rx_port_shelf = ? "
            + "z_end_rx_port_slot = ? "
            + "z_end_rx_port_sub_slot = ? "
            + "z_end_rx_lgx_device_name = ? "
            + "z_end_rx_lgx_port_name = ? "
            + "z_end_rx_lgx_port_rack = ? "
            + "z_end_rx_lgx_port_shelf = ? "
            + "z_end_rx_tail_rnode_id = ? "
            + "z_end_rx_xport_cp_name = ? "
            + "z_end_rx_xport_port_name = ? "
            + "z_end_rx_tail_roadm_port_aid = ? "
            + "z_end_rx_tail_roadm_port_rack_loc = ? "
            + "z_end_optic_type = ? "
            + "z_end_router_node_id = ? "
            + "z_end_router_ip_address = ? "
            + "z_end_router_url = ? "
            + "z_end_user_label "
            + "due_date = ? "
            + "end_date = ? "
            + "nc_code = ? "
            + "nci_code = ? "
            + "secondary_nci_code = ? "
            + "customer = ? "
            + "customer_contact = ? "
            + "operator_contact = ? "
            + "latency = ? "
            + "fiber_span_srlgs = ? "
            + "supp_serv_name = ? "
            + "update_date"
            + " where service_name = ? ";

    private static final String SERVICE_EVENT_INSERT =
            "INSERT INTO %sinv_ser_events"
            + "(event,"
            + "event_key,"
            + "event_value,"
            + "change_key,"
            + "change_orig_val,"
            + "change_new_val,"
            + "event_date )"
            + "values ( ?"
            + "?"
            + "?"
            + "?"
            + "?"
            + "?"
            + "? )";

    private static final String SERVICE_INSERT =
            "INSERT INTO %sinv_ser_main "
                    + "( service_name, "
                    + "common_id, "
                    + "sdnc_req_header_request_id, "
                    + "sdnc_req_header_rpc_action, "
                    + "sdnc_req_header_notification_url, "
                    + "sdnc_req_header_request_system_id, "
                    + "connection_type, "
                    + "lifecycle_state, "
                    + "administrative_state, "
                    + "operational_state, "
                    + "serv_condition, "
                    + "a_end_service_format, "
                    + "a_end_service_rate, "
                    + "a_end_clli, "
                    + "a_end_node_id, "
                    + "a_end_tx_port_device_name, "
                    + "a_end_tx_port_type, "
                    + "a_end_tx_port_name, "
                    + "a_end_tx_port_rack, "
                    + "a_end_tx_port_shelf, "
                    + "a_end_tx_port_slot, "
                    + "a_end_tx_port_sub_slot, "
                    + "a_end_tx_lgx_device_name, "
                    + "a_end_tx_lgx_port_name, "
                    + "a_end_tx_lgx_port_rack, "
                    + "a_end_tx_lgx_port_shelf, "
                    + "a_end_tx_tail_rnode_id, "
                    + "a_end_tx_xport_cp_name, "
                    + "a_end_tx_xport_port_name, "
                    + "a_end_tx_tail_roadm_port_aid, "
                    + "a_end_tx_tail_roadm_port_rack_loc, "
                    + "a_end_rx_port_device_name, "
                    + "a_end_rx_port_type, "
                    + "a_end_rx_port_name, "
                    + "a_end_rx_port_rack, "
                    + "a_end_rx_port_shelf, "
                    + "a_end_rx_port_slot, "
                    + "a_end_rx_port_sub_slot, "
                    + "a_end_rx_lgx_device_name, "
                    + "a_end_rx_lgx_port_name, "
                    + "a_end_rx_lgx_port_rack, "
                    + "a_end_rx_lgx_port_shelf, "
                    + "a_end_rx_tail_rnode_id, "
                    + "a_end_rx_xport_cp_name, "
                    + "a_end_rx_xport_port_name, "
                    + "a_end_rx_tail_roadm_port_aid, "
                    + "a_end_rx_tail_roadm_port_rack_loc, "
                    + "a_end_optic_type, "
                    + "a_end_router_node_id, "
                    + "a_end_router_ip_address, "
                    + "a_end_router_url, "
                    + "a_end_user_label, "
                    + "z_end_service_format, "
                    + "z_end_service_rate, "
                    + "z_end_clli, "
                    + "z_end_node_id, "
                    + "z_end_tx_port_device_name, "
                    + "z_end_tx_port_type, "
                    + "z_end_tx_port_name, "
                    + "z_end_tx_port_rack, "
                    + "z_end_tx_port_shelf, "
                    + "z_end_tx_port_slot, "
                    + "z_end_tx_port_sub_slot, "
                    + "z_end_tx_lgx_device_name, "
                    + "z_end_tx_lgx_port_name, "
                    + "z_end_tx_lgx_port_rack, "
                    + "z_end_tx_lgx_port_shelf, "
                    + "z_end_tx_tail_rnode_id, "
                    + "z_end_tx_xport_cp_name, "
                    + "z_end_tx_xport_port_name, "
                    + "z_end_tx_tail_roadm_port_aid, "
                    + "z_end_tx_tail_roadm_port_rack_loc, "
                    + "z_end_rx_port_device_name, "
                    + "z_end_rx_port_type, "
                    + "z_end_rx_port_name, "
                    + "z_end_rx_port_rack, "
                    + "z_end_rx_port_shelf, "
                    + "z_end_rx_port_slot, "
                    + "z_end_rx_port_sub_slot, "
                    + "z_end_rx_lgx_device_name, "
                    + "z_end_rx_lgx_port_name, "
                    + "z_end_rx_lgx_port_rack, "
                    + "z_end_rx_lgx_port_shelf, "
                    + "z_end_rx_tail_rnode_id, "
                    + "z_end_rx_xport_cp_name, "
                    + "z_end_rx_xport_port_name, "
                    + "z_end_rx_tail_roadm_port_aid, "
                    + "z_end_rx_tail_roadm_port_rack_loc, "
                    + "z_end_optic_type, "
                    + "z_end_router_node_id, "
                    + "z_end_router_ip_address, "
                    + "z_end_router_url, "
                    + "z_end_user_label, "
                    /*+ "hcon_cust_code, "
                    + "hcon_gen_div_existing_serv, "
                    + "hcon_gen_div_app_site, "
                    + "hcon_gen_div_app_node, "
                    + "hcon_gen_div_app_srlg, "
                    + "hcon_gen_excl_fiber_bundle, "
                    + "hcon_gen_excl_site, "
                    + "hcon_gen_excl_node_id, "
                    + "hcon_gen_excl_supp_serv_name, "
                    + "hcon_gen_incl_fiber_bundle, "
                    + "hcon_gen_incl_site, "
                    + "hcon_gen_incl_node_id, "
                    + "hcon_gen_incl_supp_serv_name, "
                    + "hcon_gen_max_latency, "
                    + "hcon_corout_existing_serv, "
                    + "scon_cust_code, "
                    + "scon_gen_div_existing_serv, "
                    + "scon_gen_div_app_site, "
                    + "scon_gen_div_app_node, "
                    + "scon_gen_div_app_srlg, "
                    + "scon_gen_excl_fiber_bundle, "
                    + "scon_gen_excl_site, "
                    + "scon_gen_excl_node_id, "
                    + "scon_gen_excl_supp_serv_name, "
                    + "scon_gen_incl_fiber_bundle, "
                    + "scon_gen_incl_site, "
                    + "scon_gen_incl_node_id, "
                    + "scon_gen_incl_supp_serv_name, "
                    + "scon_gen_max_latency, "
                    + "scon_corout_existing_serv, " */
                    + "due_date, "
                    + "end_date, "
                    + "nc_code, "
                    + "nci_code, "
                    + "secondary_nci_code, "
                    + "customer, "
                    + "customer_contact, "
                    + "operator_contact, "
                    + "latency, "
                    + "fiber_span_srlgs, "
                    + "supp_serv_name, "
                    + "create_date, "
                    + "update_date) "
                    + "values ( "
                    /*+ "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, " */
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?, "
                    + "?) ";


    private static final String SERVICE_HCON_CREATE = "insert into @inv_ser_hcon (service_name, customer_code, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_HCON_DIVERSITY_CREATE = "insert into @inv_ser_hcon_diversity_service (service_name, existing_service, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_HCON_DIVERSITY_CAPABILITY_CREATE = "insert into @inv_ser_hcon_diversity_service_capability (service_name, site, node, srlg, create_date, update_date)"
            + "values (?, ?, ?, ?, ?) ";

    private static final String SERVICE_HCON_COMPONENT_CREATE = "insert into @inv_ser_hcon_components (service_name, application, component, value, create_date, update_date)"
            + "values (?, ?, ?, ?, ?, ?) ";

    private static final String SERVICE_HCON_COROUTING_CREATE = "insert into @inv_ser_hcon_corouting (service_name, existing_service, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_SCON_CREATE = "insert into @inv_ser_scon (service_name, customer_code, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_SCON_DIVERSITY_CREATE = "insert into @inv_ser_scon_diversity_service (service_name, existing_service, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_SCON_DIVERSITY_CAPABILITY_CREATE = "insert into @inv_ser_scon_diversity_service_capability (service_name, site, node, srlg, create_date, update_date)"
            + "values (?, ?, ?, ?, ?) ";

    private static final String SERVICE_SCON_COMPONENT_CREATE = "insert into @inv_ser_scon_components (service_name, application, component, value, create_date, update_date)"
            + "values (?, ?, ?, ?, ?, ?) ";

    private static final String SERVICE_SCON_COROUTING_CREATE = "insert into @inv_ser_scon_corouting (service_name, existing_service, create_date, update_date)"
            + "values (?, ?, ?, ?) ";

    private static final String SERVICE_FIBERSPAN_SRLG_CREATE = "insert into inv_ser_fiber_span_srlgs values (service_name, fiber_span_srlg, create_date, update_date) "
            + "values(?, ?, ?, ?)";

    private static final String SERVICE_EQUIPMENT_SRG_CREATE = "insert into inv_ser_equipment_srgs values (service_name, srg_number, create_date, update_date) "
            + "values(?, ?, ?, ?)";

    private static final String SERVICE_SUPPORTING_SVC_CREATE = "insert into inv_ser_supporting_service values (service_name, supporting_service_name, create_date, update_date) "
            + "values(?, ?, ?, ?)";

    private static final String SERVICE_DELETE = "DELETE FROM %sinv_ser_main WHERE service_name = ?";


    private Queries() {
        // util class
    }

    public static Query getQuery() {
        return new Query();
    }

    public static final class Query {
        private String sql;
        private String schema;

        private Query() {
            this.schema = "";
        }

        public Query withSchema(String schema0) {
            this.schema = schema0;
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

        public Query deviceCPSlotInsert() {
            this.sql = DEVICE_CP_SLOT_INSERT;
            return this;
        }

        public Query deviceCPPortInsert() {
            this.sql = DEVICE_CP_PORT_INSERT;
            return this;
        }

        public Query deviceInterfacesInsert() {
            this.sql = DEVICE_INTERFACES_INSERT;
            return this;
        }

        public Query deviceInterfaceTcmInsert() {
            this.sql = DEVICE_INTERFACE_TCM_INSERT;
            return this;
        }

        public Query deviceInterfaceOtnOduTxMsiInsert() {
            this.sql = DEVICE_INTERFACE_ODU_OPU_MIS_TX_INSERT;
            return this;
        }

        public Query deviceInterfaceOtnOduRxMsiInsert() {
            this.sql = DEVICE_INTERFACE_ODU_OPU_MIS_RX_INSERT;
            return this;
        }

        public Query deviceInterfaceOtnOduExpMsiInsert() {
            this.sql = DEVICE_INTERFACE_ODU_OPU_MIS_EXP_INSERT;
            return this;
        }

        public Query deviceProtocolInsert() {
            this.sql = DEVICE_PROTOCOL_INSERT;
            return this;
        }

        public Query deviceProtocolPortConfigInsert() {
            this.sql = DEVICE_PROTOCOL_PORT_CONFIG_INSERT;
            return this;
        }

        public Query deviceProtocolLldpNbrlistInsert() {
            this.sql = DEVICE_PROTOCOL_LLDP_NBR_LIST_INSERT;
            return this;
        }

        public Query deviceProtocolRstpInsert() {
            this.sql = DEVICE_PROTOCOL_RSTP_INSERT;
            return this;
        }

        public Query deviceProtocolRstpBridgePortInsert() {
            this.sql = DEVICE_PROTOCOL_RSTP_BRIDGEPORT_INSERT;
            return this;
        }

        public Query deviceProtocolRstpBridgePortAttrInsert() {
            this.sql = DEVICE_PROTOCOL_RSTP_BRIDGEPORT_ATTR_INSERT;
            return this;
        }

        public Query deviceInternalLinkInsert() {
            this.sql = DEVICE_INTERNAL_LINK_INSERT;
            return this;
        }

        public Query deviceExternalLinkInsert() {
            this.sql = DEVICE_EXTERNAL_LINK_INSERT;
            return this;
        }

        public Query devicePhysicalLinkInsert() {
            this.sql = DEVICE_PHYSICAL_LINK_INSERT;
            return this;
        }


        public Query deviceDegreeInsert() {
            this.sql = DEVICE_DEGREE_INSERT;
            return this;
        }

        public Query deviceDegreeCircuitPackInsert() {
            this.sql = DEVICE_DEGREE_CIRCUITPACK_INSERT;
            return this;
        }

        public Query deviceDegreeConnectionPortInsert() {
            this.sql = DEVICE_CONNECTION_PORT_INSERT;
            return this;
        }


        public Query deviceSharedRiskGroupInsert() {
            this.sql = DEVICE_SHARED_RISK_GROUP_INSERT;
            return this;
        }


        public Query deviceSrgCircuitPackInsert() {
            this.sql = DEVICE_SRG_CIRCUITPACK_INSERT;
            return this;
        }


        public Query deviceRoadmConnectionsInsert() {
            this.sql = DEVICE_ROADM_CONNECTIONS_INSERT;
            return this;
        }


        public Query deviceConnectionMapInsert() {
            this.sql = DEVICE_CONNECTION_MAP_INSERT;
            return this;
        }


        public Query deviceWavelengthInsert() {
            this.sql = DEVICE_WAVELENGTH_INSERT;
            return this;
        }

        public Query serviceCreate() {
            this.sql = SERVICE_INSERT;
            return this;
        }

        public Query serviceEventCreate() {
            this.sql = SERVICE_EVENT_INSERT;
            return this;
        }

        public Query serviceUpdate() {
            this.sql = SERVICE_UPDATE;
            return this;
        }

        public Query serviceDelete() {
            this.sql = SERVICE_DELETE;
            return this;
        }

        public Query servicePathListCreate() {
            this.sql = SERVICE_PATH_LIST;
            return this;
        }

        public Query serviceHConCreate() {
            this.sql = SERVICE_HCON_CREATE;
            return this;
        }

        public Query serviceHConDiversityCreate() {
            this.sql = SERVICE_HCON_DIVERSITY_CREATE;
            return this;
        }

        public Query serviceHConDiversityCapabilityCreate() {
            this.sql = SERVICE_HCON_DIVERSITY_CAPABILITY_CREATE;
            return this;
        }

        public Query serviceHConComponentCreate() {
            this.sql = SERVICE_HCON_COMPONENT_CREATE;
            return this;
        }

        public Query serviceHConCoroutingCreate() {
            this.sql = SERVICE_HCON_COROUTING_CREATE;
            return this;
        }

        public Query serviceSConDiversityCreate() {
            this.sql = SERVICE_SCON_DIVERSITY_CREATE;
            return this;
        }

        public Query serviceSConDiversityCapabilityCreate() {
            this.sql = SERVICE_SCON_DIVERSITY_CAPABILITY_CREATE;
            return this;
        }

        public Query serviceSConComponentCreate() {
            this.sql = SERVICE_SCON_COMPONENT_CREATE;
            return this;
        }

        public Query serviceSConCoroutingCreate() {
            this.sql = SERVICE_SCON_COROUTING_CREATE;
            return this;
        }

        public Query serviceFiberspanSrlgCreate() {
            this.sql = SERVICE_FIBERSPAN_SRLG_CREATE;
            return this;
        }

        public Query serviceEquipmentSrgCreate() {
            this.sql = SERVICE_EQUIPMENT_SRG_CREATE;
            return this;
        }

        public Query serviceSupportingSvcCreate() {
            this.sql = SERVICE_SUPPORTING_SVC_CREATE;
            return this;
        }

        public String get() {
            Preconditions.checkArgument(!Strings.isNullOrEmpty(this.sql), "No query selected");
            return String.format(this.sql, this.schema.concat("."));
        }
    }
}
