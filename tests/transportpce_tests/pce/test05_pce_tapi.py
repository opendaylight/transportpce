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
import test_utils_generate_tapi_topo


# pylint: disable=too-few-public-methods
class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None

class TransportPCEtest(unittest.TestCase):
    # SIP+SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sAOTS = "38d81f55-1798-3520-ba16-08efa56630c4"
    # SIP+SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sZOTS = "97d9ba27-0efa-3010-8b98-b4d73240120c"
    # SIP+SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sADSR = "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
    # SIP+SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sZDSR = "25812ef2-625d-3bf8-af55-5e93946d1c22"
    # SIP+SPDR-SA1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sAeODU = "b6421484-531f-3444-adfc-e11c503f1fab"
    # SIP+SPDR-SC1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sZeODU = "4511fca7-0cf7-3b27-a128-3b372d5e1fa8"
    # SIP+SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2AOTS = "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
    # SIP+SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2ZOTS = "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
    # SIP+SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    s3AOTS = "858e8cbf-d264-3e6f-858e-cdb6257d3aa9"
    # SIP+SPDR-SC1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    s3ZOTS = "6431ec11-f109-39f6-9568-c037f0e006f3"
    # SIP+SPDR-SA1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    s3AiODU = "3f395974-bb7a-3411-ac4c-f791752bdf06"
    # SIP+SPDR-SC1-XPDR3+iODU+XPDR3-NETWORK UUID IS
    s3ZiODU = "2fe1c941-f9c9-34f4-86c4-8e97ae880ecd"

    path_computation_input_data = {
        "service-name": "1a58e7bd-c997-48eb-9967-fa54afb02f47",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "service-rate": "400",
            "clli": "SPDR-SA2",
            "service-format": "OC",
            "tx-direction" : {
                "logical-connection-point": "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
            },
            "rx-direction" : {
                "logical-connection-point": "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
            },
            "node-id": "SPDR-SA2"
        },
        "service-z-end": {
            "service-rate": "400",
            "clli": "SPDR-SC2",
            "service-format": "OC",
            "tx-direction" : {
                "logical-connection-point": "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
            },
            "rx-direction" : {
                "logical-connection-point": "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
            },
            "node-id": "SPDR-SC2"
        },
        "pce-routing-metric": "hop-count"
    }

    cr_serv_input_data = {
        "end-point": [
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
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
                    "service-interface-point-uuid": "25812ef2-625d-3bf8-af55-5e93946d1c22"
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
            "service-type": "POINT_TO_POINT_CONNECTIVITY",
            "service-level": "Some service-level",
            "requested-capacity": {
                "total-size": {
                    "value": "100",
                    "unit": "tapi-common:CAPACITY_UNIT_GBPS"
                }
            }
        },
        "topology-constraint": [
            {
                "local-id": "localIdTopoConstraint",
                "name": [
                    {
                            "value-name": "Dumb constraint",
                            "value": "for debug1"
                    }
                ]
            }
        ],
        "state": "LOCKED",
        "layer-protocol-name": "PHOTONIC_MEDIA"}

    processes = []
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()
    # # SIP+SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    # s2AOTS = "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
    # # SIP+SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    # s2ZOTS = "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
    # # SIP+SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    del_serv_input_data = {"uuid": "TBD"}
    
    tapi_topo = {"topology-id": "TBD"}


    @classmethod
    def setUpClass(self):
        test_utils_generate_tapi_topo.connect_xpdrs(self)
        test_utils_generate_tapi_topo.connect_rdms(self)
        test_utils_generate_tapi_topo.interconnect_roadm_and_xpdr(self)
        test_utils_generate_tapi_topo.add_omsAttributes_to_ROADM2ROADM_links(self)
        # test_utils_generate_tapi_topo.create_services_on_infra(self)
        print("Entering PCE Test 05 to test PCE algo for TAPI")

    @classmethod
    def tearDownClass(self):
        # pylint: disable=not-an-iterable
        # test_utils_generate_tapi_topo.delete_created_tapi_services(self)
        test_utils_generate_tapi_topo.cleanup_and_desinstall_tapi_feature(self)
        print("Entering PCE Test 05 to test PCE algo for TAPI")
        test_utils_generate_tapi_topo.tearDownClass(cls)

    def setUp(self):
        time.sleep(2)


    # Path Computation success
    def test_01_path_computation_spdr_x2(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])
        self.assertEqual(5, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        # self.assertEqual(5, response['output']['response-parameters']['path-description']
        #                  ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(2)

    # Path Computation success
    def test_02_path_computation_rdm_bi(self):
        self.path_computation_input_data["service-name"] = "6c6532ac-0b91-4a11-8321-9976e64c9a70"
        self.path_computation_input_data["service-a-end"]["node-id"] = "ROADMA01"
        self.path_computation_input_data["service-a-end"]["clli"] = "ROADMA01"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = "cd2619fb-1ae0-3785-a6fd-2b71f468cb6c"
        self.path_computation_input_data["service-a-end"]["node-id"] = "ROADMC01"
        self.path_computation_input_data["service-a-end"]["clli"] = "ROADMC01"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = "b2a2185e-1bd8-3fc1-8083-b50d08a1f21f"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    #
    # # Test failed path computation
    # def test_14_fail_path_computation(self):
    #     del self.path_computation_input_data["service-name"]
    #     del self.path_computation_input_data["service-a-end"]
    #     del self.path_computation_input_data["service-z-end"]
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Service Name is not set',
    #                   response['output']['configuration-response-common']['response-message'])
    #     time.sleep(2)
    #
    # # Test1 success path computation
    # def test_15_success1_path_computation(self):
    #     self.path_computation_input_data["service-name"] = "service 1"
    #     self.path_computation_input_data["service-a-end"] = {"service-format": "Ethernet", "service-rate": "100",
    #                                                          "clli": "ORANGE2", "node-id": "XPONDER-2-2"}
    #     self.path_computation_input_data["service-z-end"] = {"service-format": "Ethernet", "service-rate": "100",
    #                                                          "clli": "ORANGE1", "node-id": "XPONDER-1-2"}
    #     self.path_computation_input_data["hard-constraints"] = {"customer-code": ["Some customer-code"],
    #                                                             "co-routing": {
    #                                                                 "service-identifier-list": [{
    #2                                                                  "service-identifier": "Some existing-service"}]
    #     }}
    #     self.path_computation_input_data["soft-constraints"] = {"customer-code": ["Some customer-code"],
    #                                                             "co-routing": {
    #                                                                 "service-identifier-list": [{
    #2                                                                  "service-identifier": "Some existing-service"}]
    #     }}
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #
    #     time.sleep(4)
    #
    # # Test2 success path computation with path description
    # def test_16_success2_path_computation(self):
    #     self.path_computation_input_data["service-a-end"]["node-id"] = "XPONDER-1-2"
    #     self.path_computation_input_data["service-a-end"]["clli"] = "ORANGE1"
    #     self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-3-2"
    #     self.path_computation_input_data["service-z-end"]["clli"] = "ORANGE3"
    #     del self.path_computation_input_data["hard-constraints"]
    #     del self.path_computation_input_data["soft-constraints"]
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     self.assertEqual(5, response['output']['response-parameters']['path-description']
    #                      ['aToZ-direction']['aToZ-wavelength-number'])
    #     self.assertEqual(5, response['output']['response-parameters']['path-description']
    #                      ['zToA-direction']['zToA-wavelength-number'])
    #     time.sleep(4)
    #
    # # Test3 success path computation with hard-constraints exclude
    # def test_17_success3_path_computation(self):
    #     self.path_computation_input_data["hard-constraints"] = {"exclude":
    #                                                             {"node-id": ["OpenROADM-2-1", "OpenROADM-2-2"]}}
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     self.assertEqual(9, response['output']['response-parameters']['path-description']
    #                      ['aToZ-direction']['aToZ-wavelength-number'])
    #     self.assertEqual(9, response['output']['response-parameters']['path-description']
    #                      ['zToA-direction']['zToA-wavelength-number'])
    #     time.sleep(4)
    #
    # # Path computation before deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    # def test_18_path_computation_before_oms_attribute_deletion(self):
    #     self.path_computation_input_data["service-a-end"]["node-id"] = "XPONDER-2-2"
    #     self.path_computation_input_data["service-a-end"]["clli"] = "ORANGE2"
    #     self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-1-2"
    #     self.path_computation_input_data["service-z-end"]["clli"] = "ORANGE1"
    #     del self.path_computation_input_data["hard-constraints"]
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     path_depth = len(response['output']['response-parameters']['path-description']
    #                      ['aToZ-direction']['aToZ'])
    #     self.assertEqual(31, path_depth)
    #     link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state": "inService"}
    #     find = False
    #     for i in range(0, path_depth):
    #         resource_i = (response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
    #                       ['resource'])
    #         if resource_i == link:
    #             find = True
    #     self.assertEqual(find, True)
    #     time.sleep(4)
    #
    # # Delete oms-attribute in the link :openroadm1-3 to openroadm1-2
    # def test_19_delete_oms_attribute_in_openroadm13toopenroadm12_link(self):
    #     response = test_utils.del_oms_attr_request("OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2")
    #     self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
    #     time.sleep(2)
    #
    # # Path computation after deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    # def test_20_path_computation_after_oms_attribute_deletion(self):
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     path_depth = len(response['output']['response-parameters']['path-description']
    #                      ['aToZ-direction']['aToZ'])
    #     self.assertEqual(47, path_depth)
    #     link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state": "inService"}
    #     find = False
    #     for i in range(0, path_depth):
    #         resource_i = (response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
    #                       ['resource'])
    #         if resource_i == link:
    #             find = True
    #     self.assertNotEqual(find, True)
    #     time.sleep(5)
#
#
#
#
#
#     def create_connectivity_service2_PhotonicMedia(self):
#2      self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AOTS
#2      self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZOTS
#         self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR2"
#         self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR2"
#         self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR2"
#         self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR2"
#         response = test_utils.transportpce_api_rpc_request(
#             'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
#         time.sleep(self.WAITING)
#         self.assertEqual(response['status_code'], requests.codes.ok)
#         self.uuid_services2.pm = response['output']['service']['uuid']
#         # pylint: disable=consider-using-f-string
#         print("photonic media service uuid : {}".format(self.uuid_services2.pm))
#
#         input_dict_1 = {'administrative-state': 'LOCKED',
#                         'lifecycle-state': 'PLANNED',
#                         'operational-state': 'DISABLED',
#                         'layer-protocol-name': 'PHOTONIC_MEDIA',
#                         'direction': 'BIDIRECTIONAL'
#                         }
#         input_dict_2 = {'value-name': 'OpenROADM node id',
#                         'value': 'SPDR-SC1-XPDR2'}
#         input_dict_3 = {'value-name': 'OpenROADM node id',
#                         'value': 'SPDR-SA1-XPDR2'}
#
#         self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
#                              response['output']['service'])
#         self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
#                              response['output']['service']['end-point'][0]['name'][0])
#         self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
#                              response['output']['service']['end-point'][1]['name'][0])
#         # If the gate fails is because of the waiting time not being enough
# #        time.sleep(self.WAITING)
#
#     def get_service2_PhotonicMedia(self):
#         response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services2.pm))
#         self.assertEqual(response['status_code'], requests.codes.ok)
#         self.assertEqual(response['services'][0]['administrative-state'], 'inService')
#         self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services2.pm))
#         self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
#         self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
#         time.sleep(1)



if __name__ == "__main__":
    unittest.main(verbosity=2)
