#!/usr/bin/env python

##############################################################################
# Copyright (c) 2025 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import os
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
# pylint: disable=too-few-public-methods

class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None

class UuidServices2:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None

class UuidServices3:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None

# class TransportPCEtesting(unittest.TestCase):

processes = None
WAITING = 20  # nominal value is 300
NODE_VERSION = '2.2.1'
uuid_services = UuidServices()
uuid_services2 = UuidServices2()
uuid_services3 = UuidServices3()
del_serv_input_data = {"uuid": "TBD"}
tapi_topo = {"topology-id": "TBD"}

def initSimulators():
    print("Entering TestUtils setupClass starting simulators")
    print("Node version is {}".format(NODE_VERSION))
    processes = test_utils.start_sims([('spdra', NODE_VERSION),
                                       ('roadma', NODE_VERSION),
                                       ('roadmc', NODE_VERSION),
                                       ('xpdra', NODE_VERSION),
                                       ('xpdrc', NODE_VERSION),
                                       ('spdrc', NODE_VERSION)])

def connect_xpdrs():
    
    print("Connecting SPDRA")
    response = test_utils.mount_device("SPDR-SA1", ('spdra', NODE_VERSION))
    time.sleep(2)
    print("Connecting SPDRC")
    response = test_utils.mount_device("SPDR-SC1", ('spdrc', NODE_VERSION))
    time.sleep(2)
    print("Connecting XPDRA")
    response = test_utils.mount_device("XPDR-A1", ('xpdra', NODE_VERSION))
    time.sleep(2)
    print("Connecting XPDRC")
    response = test_utils.mount_device("XPDR-C1", ('xpdrc', NODE_VERSION))
    time.sleep(2)

def connect_rdms():
    print("Connecting ROADMA")
    response = test_utils.mount_device("ROADM-A1", ('roadma', NODE_VERSION))
    if response.status_code in [200, 201, 202, 403]:
        print("Connecting ROADMA failed")
    time.sleep(2)
    print("Connecting ROADMC")
    response = test_utils.mount_device("ROADM-C1", ('roadmc', NODE_VERSION))
    if response.status_code in [200, 201, 202, 403]:
        print("Connecting ROADMC failed")
    time.sleep(2)

def interconnect_roadm_and_xpdr():
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRA1-X1 to ROADMA1 failed")
    else:
        print("Connection from SPDRA1-X1 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X1 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X2 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X2 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X2 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X3 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X3 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X3 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X3 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from XPDRA1-X1 to ROADMA1 failed")
    else:
        print("Connection from XPDRA1-X1 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to XPDRA1-X1 failed")
    else:
        print("Connection from ROADMA1 to XPDRA1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X1 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X1 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X1 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X2 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X2 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X2 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X3 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X3 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X3 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X3 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from XPDRC1-X1 to ROADMC1 failed")
    else:
        print("Connection from XPDRC1-X1 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to XPDRC1-X1 failed")
    else:
        print("Connection from ROADMC1 to XPDRC1-X1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSA1-X2 to ROADMA1 failed")
    else:
        print("Connection from SPDRSA1-X2 to ROADMA1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMA1 to SPDRSA1-X2 failed")
    else:
        print("Connection from ROADMA1 to SPDRSA1-X2 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-xpdr-rdm-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Xponder Roadm Link created successfully'
            not in response["output"]["result"]):
        print("Connection from SPDRSC1-X2 to ROADMC1 failed")
    else:
        print("Connection from SPDRSC1-X2 to ROADMC1 succeeded")
    time.sleep(2)
    response = test_utils.transportpce_api_rpc_request(
        'transportpce-networkutils', 'init-rdm-xpdr-links',
        {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                         'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
    if (response['status_code'] != requests.codes.ok or 'Roadm Xponder links created successfully'
            not in response["output"]["result"]):
        print("Connection from ROADMC1 to SPDRSC1-X2 failed")
    else:
        print("Connection from ROADMC1 to SPDRSC1-X2 succeeded")
    time.sleep(2)

def add_omsAttributes_to_ROADM2ROADM_links():
    # Config ROADMA-ROADMC oms-attributes
    data = {"span": {
        "auto-spanloss": "true",
        "spanloss-base": 11.4,
        "spanloss-current": 12,
        "engineered-spanloss": 12.2,
        "link-concatenation": [{
            "SRLG-Id": 0,
            "fiber-type": "smf",
            "SRLG-length": 100000,
            "pmd": 0.5}]}}
    response = test_utils.add_oms_attr_request(
        "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
    if (response.status_code != requests.codes.created):
        print("OMS attributes add on link ROADMA-ROADMC failed")
    else:
        print("OMS attributes added on link ROADMA-ROADMC")
    # Config ROADMC-ROADMA oms-attributes
    data = {"span": {
        "auto-spanloss": "true",
        "spanloss-base": 11.4,
        "spanloss-current": 12,
        "engineered-spanloss": 12.2,
        "link-concatenation": [{
            "SRLG-Id": 0,
            "fiber-type": "smf",
            "SRLG-length": 100000,
            "pmd": 0.5}]}}
    response = test_utils.add_oms_attr_request(
        "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
    if (response.status_code != requests.codes.created):
        print("OMS attributes add on link ROADMC-ROADMA failed")
    else:
        print("OMS attributes added on link ROADMC-ROADMA")

def create_services_on_infra(self):
    create_connectivity_service_PhotonicMedia(self)
    get_service_PhotonicMedia(self)
    # self.create_connectivity_service2_PhotonicMedia()
    # self.get_service2_PhotonicMedia()
    create_connectivity_service3_PhotonicMedia(self)
    get_service3_PhotonicMedia(self)
    create_connectivity_service_ODU(self)
    get_service_ODU(self)
    create_connectivity_service3_ODU(self)
    get_service3_ODU(self)
    create_connectivity_service_DSR(self)
    get_service_DSR(self)

def create_connectivity_service_PhotonicMedia(self):
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAOTS
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZOTS
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services.pm = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("photonic media service uuid : {}".format(tpcetest.uuid_services.pm))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                    # 'service-layer': 'PHOTONIC_MEDIA',
                    'layer-protocol-name': 'PHOTONIC_MEDIA',
                    # 'connectivity-direction': 'BIDIRECTIONAL'
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR1'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR1'}

    tpcetest.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

def get_service_PhotonicMedia(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services.pm))
    print("getServiceResponse Service1 PhotonicMedia: {}".format(response))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services.pm))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    time.sleep(1)

def create_connectivity_service2_PhotonicMedia(self):
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AOTS
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZOTS
    self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR2"
    self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR2"
    self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR2"
    self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR2"
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services2.pm = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("photonic media service uuid : {}".format(tpcetest.uuid_services2.pm))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    'layer-protocol-name': 'PHOTONIC_MEDIA',
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR2'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR2'}

    tpcetest.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

def get_service2_PhotonicMedia(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services2.pm))
    print("getServiceResponse Service2 PhotonicMedia: {}".format(response))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services2.pm))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    time.sleep(1)

def create_connectivity_service3_PhotonicMedia(self):
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s3AOTS
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s3ZOTS
    self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR3"
    self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR3"
    self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR3"
    self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR3"
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services3.pm = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("photonic media service uuid : {}".format(tpcetest.uuid_services3.pm))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    'layer-protocol-name': 'PHOTONIC_MEDIA',
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR3'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR3'}

    tpcetest.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

def get_service3_PhotonicMedia(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services3.pm))
    print("getServiceResponse Service3 PhotonicMedia: {}".format(response))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services3.pm))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    time.sleep(1)

# test create connectivity service from spdrA to spdrC for odu
def create_connectivity_service_ODU(self):
    # pylint: disable=line-too-long
    self.cr_serv_input_data["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAeODU
    self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZeODU
    self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR1"
    self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR1"
    self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR1"
    self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR1"
#        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "ODU"
    self.cr_serv_input_data["connectivity-constraint"]["service-level"] = tpcetest.uuid_services.pm

    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services.odu = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("odu service uuid : {}".format(tpcetest.uuid_services.odu))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                    'layer-protocol-name': 'ODU',
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR1'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR1'}

    tpcetest.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

def get_service_ODU(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services.odu))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services.odu))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    time.sleep(1)

# test create connectivity service from spdrA to spdrC for odu
def create_connectivity_service3_ODU(self):
    # pylint: disable=line-too-long
    self.cr_serv_input_data["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s3AiODU
    self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "ODU"
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s3ZiODU
    self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR3"
    self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR3"
    self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR3"
    self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR3"
#        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "ODU"
    self.cr_serv_input_data["connectivity-constraint"]["service-level"] = tpcetest.uuid_services3.pm

    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services3.odu = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("odu service uuid : {}".format(tpcetest.uuid_services3.odu))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                    'layer-protocol-name': 'ODU',
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR3'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR3'}

    tpcetest.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

def get_service3_ODU(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services3.odu))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services3.odu))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')

# test create connectivity service from spdrA to spdrC for dsr
def create_connectivity_service_DSR(self):
    # pylint: disable=line-too-long
    self.cr_serv_input_data["layer-protocol-name"] = "DSR"
    self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DSR"
    self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sADSR
    self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DSR"
    self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZDSR
    self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR1"
    self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR1"
    self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR1"
    self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR1"
#        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "DSR"
    self.cr_serv_input_data["connectivity-constraint"]["requested-capacity"]["total-size"]["value"] = "10"
    self.cr_serv_input_data["connectivity-constraint"]["service-level"] = tpcetest.uuid_services.odu

    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    time.sleep(self.WAITING)
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.uuid_services.dsr = response['output']['service']['uuid']
    # pylint: disable=consider-using-f-string
    print("dsr service uuid : {}".format(tpcetest.uuid_services.dsr))

    input_dict_1 = {'administrative-state': 'LOCKED',
                    'lifecycle-state': 'PLANNED',
                    'operational-state': 'DISABLED',
                    # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                    'layer-protocol-name': 'DSR',
                    'direction': 'BIDIRECTIONAL'
                    }
    input_dict_2 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SC1-XPDR1'}
    input_dict_3 = {'value-name': 'OpenROADM node id',
                    'value': 'SPDR-SA1-XPDR1'}

    tpcetest.assertDictEqual(dict(input_dict_1,
                              **response['output']['service']),
                         response['output']['service'])
    tpcetest.assertDictEqual(dict(input_dict_2,
                              **response['output']['service']['end-point'][0]['name'][0]),
                         response['output']['service']['end-point'][0]['name'][0])
    tpcetest.assertDictEqual(dict(input_dict_3,
                              **response['output']['service']['end-point'][1]['name'][0]),
                         response['output']['service']['end-point'][1]['name'][0])
    # The sleep here is okey as the DSR service creation is very fast
#        time.sleep(self.WAITING)

def get_service_DSR(self):
    response = test_utils.get_ordm_serv_list_attr_request("services", str(tpcetest.uuid_services.dsr))
    tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    tpcetest.assertEqual(response['services'][0]['administrative-state'], 'inService')
    tpcetest.assertEqual(response['services'][0]['service-name'], str(tpcetest.uuid_services.dsr))
    tpcetest.assertEqual(response['services'][0]['connection-type'], 'service')
    tpcetest.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    time.sleep(1)
    print("Time to retrieve topology : 100 seconds... Hurry up")
    time.sleep(100)

def delete_created_tapi_services(self):
    delete_connectivity_service_DSR(self)
    delete_connectivity_service_ODU(self)
    delete_connectivity_service3_ODU(self)
    delete_connectivity_service3_PhotonicMedia(self)
    delete_connectivity_service2_PhotonicMedia(self)
    delete_connectivity_service_PhotonicMedia(self)
    get_no_tapi_services()

def delete_connectivity_service_DSR(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services.dsr)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def delete_connectivity_service_ODU(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services.odu)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def delete_connectivity_service3_ODU(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services3.odu)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def delete_connectivity_service_PhotonicMedia(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services.pm)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def delete_connectivity_service3_PhotonicMedia(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services3.pm)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def delete_connectivity_service2_PhotonicMedia(self):
    self.del_serv_input_data["uuid"] = str(tpcetest.uuid_services2.pm)
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
    tpcetest.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
    time.sleep(self.WAITING)

def get_no_tapi_services(self):
    response = test_utils.transportpce_api_rpc_request(
        'tapi-connectivity', 'get-connectivity-service-list', None)
    tpcetest.assertEqual(response['status_code'], requests.codes.internal_server_error)
    tpcetest.assertIn(
        {"error-type": "rpc", "error-tag": "operation-failed",
         "error-message": "No services exist in datastore"},
        response['output']['errors']['error'])

def cleanup_and_desinstall_tapi_feature():
    disconnect_spdrA_C()
    disconnect_roadmA_C()
    uninstall_Tapi_Feature()

# def test_47_get_no_openroadm_services(self):
#     response = test_utils.get_ordm_serv_list_request()
#     tpcetest.assertEqual(response['status_code'], requests.codes.conflict)

def disconnect_spdrA_C():
    response = test_utils.unmount_device("SPDR-SA1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SA1 failed")
        response = test_utils.unmount_device("SPDR-SC1")
    if response.status_code not in [204]:
        print("disconnection of SPDR-SC1 failed")

def disconnect_roadmA_C():
    response = test_utils.unmount_device("ROADM-A1")
    if response.status_code not in [204]:
        print("disconnection of ROADM-A1 failed")
    response = test_utils.unmount_device("ROADM-C1")
    if response.status_code not in [204]:
        print("disconnection of ROADM-C1 failed")

def uninstall_Tapi_Feature():
    test_utils.uninstall_karaf_feature("odl-transportpce-tapi")
    time.sleep(5)
    response = test_utils.get_ietf_network_request('otn-topology', 'config')
    if (requests.codes.ok in response['status_code']):
        print("Tapi Feature uninstallation :")
    # tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    # print("Tapi Feature uninstalled")
    if('node' not in response['network'][0] and 'ietf-network-topology:link' not in response['network'][0]):
         print(" otn topology empty")
    # tpcetest.assertNotIn('node', response['network'][0])
    # tpcetest.assertNotIn('ietf-network-topology:link', response['network'][0])
    response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
    if ((requests.codes.ok in response['status_code']) and
            ('node' not in response['network'][0] and 'ietf-network-topology:link' not in response['network'][0])):
    # tpcetest.assertEqual(response['status_code'], requests.codes.ok)
    # tpcetest.assertNotIn('node', response['network'][0])
    # tpcetest.assertNotIn('ietf-network-topology:link', response['network'][0])
        print("OpenROADM topology also empty")
        print("Confirm Tapi Feature correctly uninstalled")


if __name__ == "__main__":
    unittest.main(verbosity=2)
