#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
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
import test_utils_rfc8040  # nopep8


# pylint: disable=too-few-public-methods
class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class TransportPCEtesting(unittest.TestCase):

    processes = []
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()

    cr_serv_input_data = {
        "input": {
            "end-point": [
                {
                    "layer-protocol-name": "PHOTONIC_MEDIA",
                    "service-interface-point": {
                        "service-interface-point-uuid": "b1a0d883-32b8-3b0b-93d6-7ed074f6f107"
                    },
                    "administrative-state": "UNLOCKED",
                    "operational-state": "ENABLED",
                    "direction": "BIDIRECTIONAL",
                    "role": "SYMMETRIC",
                    "protection-role": "WORK",
                    "local-id": "SPDR-SA1-XPDR1",
                    "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "SPDR-SA1-XPDR1"
                        }
                    ]
                },
                {
                    "layer-protocol-name": "PHOTONIC_MEDIA",
                    "service-interface-point": {
                        "service-interface-point-uuid": "d1d6305e-179b-346f-b02d-8260aebe1ce8"
                    },
                    "administrative-state": "UNLOCKED",
                    "operational-state": "ENABLED",
                    "direction": "BIDIRECTIONAL",
                    "role": "SYMMETRIC",
                    "protection-role": "WORK",
                    "local-id": "SPDR-SC1-XPDR1",
                    "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "SPDR-SC1-XPDR1"
                        }
                    ]
                }
            ],
            "connectivity-constraint": {
                "service-layer": "PHOTONIC_MEDIA",
                "service-type": "POINT_TO_POINT_CONNECTIVITY",
                "service-level": "Some service-level",
                "requested-capacity": {
                    "total-size": {
                        "value": "100",
                        "unit": "GB"
                    }
                }
            },
            "state": "Some state"}}

    del_serv_input_data = {
        "input": {
            "service-id-or-name": "TBD"}}

    tapi_topo = {
        "tapi-topology:input": {"tapi-topology:topology-id-or-name": "TBD"}
    }

    @classmethod
    def setUpClass(cls):
        # pylint: disable=unsubscriptable-object
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
        cls.processes = test_utils_rfc8040.start_tpce()
        # TAPI feature is not installed by default in Karaf
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing tapi feature...")
            result = test_utils_rfc8040.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed = True
            print("Restarting OpenDaylight...")
            test_utils_rfc8040.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils_rfc8040.start_karaf()
            test_utils_rfc8040.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils_rfc8040.wait_until_log_contains(
                test_utils_rfc8040.KARAF_LOG, test_utils_rfc8040.KARAF_OK_START_MSG, time_to_wait=60)
        if cls.init_failed:
            print("tapi installation feature failed...")
            test_utils_rfc8040.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils_rfc8040.start_sims([('spdra', cls.NODE_VERSION),
                                                       ('roadma', cls.NODE_VERSION),
                                                       ('roadmc', cls.NODE_VERSION),
                                                       ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_spdrA(self):
        print("Connecting SPDRA")
        response = test_utils_rfc8040.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_02_connect_spdrC(self):
        print("Connecting SPDRC")
        response = test_utils_rfc8040.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_03_connect_rdmA(self):
        print("Connecting ROADMA")
        response = test_utils_rfc8040.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_04_connect_rdmC(self):
        print("Connecting ROADMC")
        response = test_utils_rfc8040.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_05_connect_sprdA_1_N1_to_roadmA_PP1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_spdrA_1_N1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_07_connect_sprdC_1_N1_to_roadmC_PP1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_spdrC_1_N1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_09_add_omsAttributes_ROADMA_ROADMC(self):
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
        response = test_utils_rfc8040.add_oms_attr_request(
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_10_add_omsAttributes_ROADMC_ROADMA(self):
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
        response = test_utils_rfc8040.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_11_check_otn_topology(self):
        response = test_utils_rfc8040.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 6, 'There should be 6 otn nodes')
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_12_check_openroadm_topology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 13, 'There should be 13 openroadm nodes')
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 22,
                         'There should be 22 openroadm links')

    def test_13_get_tapi_topology_details(self):
        self.tapi_topo["tapi-topology:input"]["tapi-topology:topology-id-or-name"] =\
            test_utils_rfc8040.T0_FULL_MULTILAYER_TOPO
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['output']['topology']['node']), 14, 'There should be 14 TAPI nodes')
        self.assertEqual(len(response['output']['topology']['link']), 15, 'There should be 15 TAPI links')

    def test_14_check_sip_details(self):
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-common', 'get-service-interface-point-list', None)
        self.assertEqual(len(response['output']['sip']), 60, 'There should be 60 service interface point')

# test create connectivity service from spdrA to spdrC for Photonic_media
    def test_15_create_connectivity_service_PhotonicMedia(self):
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services.pm))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'service-layer': 'PHOTONIC_MEDIA',
                        'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_16_get_service_PhotonicMedia(self):
        response = test_utils_rfc8040.get_ordm_serv_list_attr_request("services", str(self.uuid_services.pm))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.pm))
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# test create connectivity service from spdrA to spdrC for odu
    def test_17_create_connectivity_service_ODU(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["input"]["end-point"][0]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["input"]["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = "5efda776-f8de-3e0b-9bbd-2c702e210946"
        self.cr_serv_input_data["input"]["end-point"][1]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["input"]["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = "8116d0af-39fa-3df5-bed2-dd2cd5e8217d"
        self.cr_serv_input_data["input"]["connectivity-constraint"]["service-layer"] = "ODU"
        self.cr_serv_input_data["input"]["connectivity-constraint"]["service-level"] = self.uuid_services.pm

        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.odu = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("odu service uuid : {}".format(self.uuid_services.odu))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'service-layer': 'ODU',
                        'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_18_get_service_ODU(self):
        response = test_utils_rfc8040.get_ordm_serv_list_attr_request("services", str(self.uuid_services.odu))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.odu))
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# test create connectivity service from spdrA to spdrC for dsr
    def test_19_create_connectivity_service_DSR(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["input"]["end-point"][0]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["input"]["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
        self.cr_serv_input_data["input"]["end-point"][1]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["input"]["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = "25812ef2-625d-3bf8-af55-5e93946d1c22"
        self.cr_serv_input_data["input"]["connectivity-constraint"]["service-layer"] = "DSR"
        self.cr_serv_input_data["input"]["connectivity-constraint"]["requested-capacity"]["total-size"]["value"] = "10"
        self.cr_serv_input_data["input"]["connectivity-constraint"]["service-level"] = self.uuid_services.odu

        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.dsr = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("dsr service uuid : {}".format(self.uuid_services.dsr))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'service-layer': 'DSR',
                        'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1,
                                  **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # The sleep here is okey as the DSR service creation is very fast
        time.sleep(self.WAITING)

    def test_20_get_service_DSR(self):
        response = test_utils_rfc8040.get_ordm_serv_list_attr_request("services", str(self.uuid_services.dsr))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.dsr))
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_21_get_connectivity_service_list(self):
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-list', None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_service = response['output']['service']
        for ele in liste_service:
            if ele['uuid'] == self.uuid_services.pm:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'PHOTONIC_MEDIA')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 3, 'There should be 3 connections')
            elif ele['uuid'] == self.uuid_services.odu:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'ODU')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 1, 'There should be 1 connections')
            elif ele['uuid'] == self.uuid_services.dsr:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'DSR')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 2, 'There should be 2 connections')
            else:
                self.fail("get connectivity service failed")
        time.sleep(2)

    def test_22_delete_connectivity_service_DSR(self):
        self.del_serv_input_data["input"]["service-id-or-name"] = str(self.uuid_services.dsr)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_23_delete_connectivity_service_ODU(self):
        self.del_serv_input_data["input"]["service-id-or-name"] = str(self.uuid_services.odu)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_24_delete_connectivity_service_PhotonicMedia(self):
        self.del_serv_input_data["input"]["service-id-or-name"] = str(self.uuid_services.pm)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_25_get_no_tapi_services(self):
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-list', None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        # self.assertIn(
        #     {"error-type": "rpc", "error-tag": "operation-failed",
        #      "error-message": "No services exist in datastore",
        #      "error-info": "<severity>error</severity>"},
        #     res['errors']['error'])

    def test_26_get_no_openroadm_services(self):
        response = test_utils_rfc8040.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_27_disconnect_spdrA(self):
        response = test_utils_rfc8040.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_28_disconnect_spdrC(self):
        response = test_utils_rfc8040.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_29_disconnect_roadmA(self):
        response = test_utils_rfc8040.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_30_disconnect_roadmC(self):
        response = test_utils_rfc8040.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.no_content)


if __name__ == "__main__":
    unittest.main(verbosity=2)
