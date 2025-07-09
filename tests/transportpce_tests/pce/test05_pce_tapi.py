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
    # # SIP+SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    # sAOTS = "38d81f55-1798-3520-ba16-08efa56630c4"
    # # SIP+SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    # sZOTS = "97d9ba27-0efa-3010-8b98-b4d73240120c"
    # SIP+SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    s2ADSR = "6980f38d-a3a0-31ab-85b3-b98f9537fc41"
    # SIP+SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    s2ZDSR = "43b47382-8192-36b3-89ff-641375bbf2c7"
    # SIP+SPDR-SA1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    s2AeODU = "46486b63-9ca7-3854-ae86-6852a8ae12ad"
    # SIP+SPDR-SC1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    s2ZeODU = "0a24f848-55ac-3543-80ba-f74ead3a2064"
    # SIP+SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2AOTS = "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
    # SIP+SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2ZOTS = "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
    # SIP+SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    s2AiODU = "86ed195a-c926-334a-8a64-9418ca096579"
    # SIP+SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    s2ZiODU = "97598b05-b5a2-383b-a663-66290ba5d3e7"
    # # SIP+SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    # s3AOTS = "858e8cbf-d264-3e6f-858e-cdb6257d3aa9"
    # # SIP+SPDR-SC1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    # s3ZOTS = "6431ec11-f109-39f6-9568-c037f0e006f3"
    # # SIP+SPDR-SA1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    # s3AiODU = "3f395974-bb7a-3411-ac4c-f791752bdf06"
    # # SIP+SPDR-SC1-XPDR3+iODU+XPDR3-NETWORK UUID IS
    # s3ZiODU = "2fe1c941-f9c9-34f4-86c4-8e97ae880ecd"
    # LCP (NEP) SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    n2ADSR = "935d763e-297c-332f-8edc-65496a2a607c"
    # LCP (NEP) SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    n2ZDSR = "eddd56d6-0aa7-3583-9b1b-40470ed2cf96"
    # LCP (NEP) SPDR-SA1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    n2AeODU = "871ac1d5-7b52-3e6f-b4eb-bc7cc6c7fa6a"
    # LCP (NEP) SPDR-SC1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    n2ZeODU = "06336b5c-458b-395d-8f7f-167d24b15737"
    # LCP (NEP) SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    n2AiODU = "6f4777d4-41f0-3833-b811-68b90903d834"
    # LCP (NEP) SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    n2ZiODU = "8b82dac3-646c-301d-b455-b7a4802774f7"
    # LCP (CEP) CEP+SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    c2AiODU = ""
    # LCP (CEP) CEP+SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    c2ZiODU = ""

    incomplete_path_computation_input_data = {
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "pce-routing-metric": "hop-count"
    }

    path_computation_input_data = {
        "service-name": "1a58e7bd-c997-48eb-9967-fa54afb02f47",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "clli": "38c114ae-9c0e-3068-bb27-db2dbd81220b",
            "service-format": "OTU",
            "tx-direction" : {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "rx-direction" : {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "node-id": "SPDR-SA1-XPDR2+XPONDER"
        },
        "service-z-end": {
            "clli": "d852c340-77db-3f9a-96e8-cb4de8e1004a",
            "service-format": "OTU",
            "tx-direction" : {
                "logical-connection-point": "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
            },
            "rx-direction" : {
                "logical-connection-point": "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
            },
            "node-id": "SPDR-SC1-XPDR2+XPONDER"
        },
        "pce-routing-metric": "hop-count"
    }

    # cr_serv_input_data is used for tapi-connectivity service creation. However, at that stage we create service
    # using OR PCE and OR renderer (in a first step, as TAPI Renderer is not available). Thus to trigger path
    # through OR PCE, considering initial request through TAPI API will be converted to an OR service-create, we need : 
    #     1) To use a service name that iS NOT a UUID but a string
    #     2) To use as the local-id, the name of the node in the OPENROADM topology
    cr_serv_input_data = {
        "name": [
            {
                "value-name": "service-name",
                "value": "servicephotonic-1"
            }
        ],
        "end-point": [
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
                },
                "connection-end-point": [
                    {
                        "topology-uuid": "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                        "node-uuid": "38c114ae-9c0e-3068-bb27-db2dbd81220b",
                        "node-edge-point-uuid": "ff69716d-325d-35fc-baa9-0c4bcd992bbd",
                        "connection-end-point-uuid": "a4411c09-2913-334d-b652-8837b8e9c5f8"
                    }
                ],
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "SPDR-SA1-XPDR2",
                "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "SPDR-SA1-XPDR2"
                        }
                ]
            },
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
                },
                "connection-end-point": [
                    {
                        "topology-uuid": "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                        "node-uuid": "d852c340-77db-3f9a-96e8-cb4de8e1004a",
                        "node-edge-point-uuid": "8557ddb5-4ee9-33e0-ba0f-adf713868625",
                        "connection-end-point-uuid": "54100e71-96c1-3bd4-bf3c-9cd9cdfdb78d"
                    }
                ],
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "SPDR-SC1-XPDR2",
                "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "SPDR-SC1-XPDR2"
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

    cr_serv_input_data2 = {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-create",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"
        },
        "service-name": "service1",
        "common-id": "ASATT1234567",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "SPDR-SA1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSA",
            "tx-direction": [{
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "SPDR-SA1-XPDR2-NETWORK1",
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "SPDR-SA1-XPDR2-NETWORK1",
                },
                "index": 0
            }],
            "optic-type": "dwdm"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "SPDR-SC1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSC",
            "tx-direction": [{
                "port": {
                    "port-device-name": "SPDR-SC1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "SPDR-SC1-XPDR2-NETWORK1",
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "SPDR-SC1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "SPDR-SC1-XPDR2-NETWORK1",
                },
                "index": 0
            }],
            "optic-type": "dwdm"
        },
        "due-date": "2016-11-28T00:00:01Z",
        "operator-contact": "pw1234"
    }

    del_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-delete",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"},
        "service-delete-req-info": {
            "service-name": "TBD",
            "tail-retention": "no"}
    }
    processes = []
    WAITING = 300  # nominal value is 300
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
    def setUpClass(cls):
        test_utils_generate_tapi_topo.initSimulators()
        test_utils_generate_tapi_topo.connect_xpdrs()
        test_utils_generate_tapi_topo.connect_rdms()
        test_utils_generate_tapi_topo.interconnect_roadm_and_xpdr()
        test_utils_generate_tapi_topo.add_omsAttributes_to_ROADM2ROADM_links()
        #test_utils_generate_tapi_topo.create_services_on_infra()
        print("Entering PCE Test 05 to test PCE algo for TAPI")

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        # test_utils_generate_tapi_topo.delete_created_tapi_services(self)
        #test_utils_generate_tapi_topo.cleanup_and_desinstall_tapi_feature()
        print("Exiting PCE Test 05 to test PCE algo for TAPI")
        #test_utils_generate_tapi_topo.tearDownClass(cls)

    def setUp(self):
        time.sleep(2)

    
    # Test failed path computation
    # def test_01_fail_path_computation(self):
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.incomplete_path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Service Name is not set',
    #                   response['output']['configuration-response-common']['response-message'])
    #     time.sleep(2)
    # # Path Computation success
    # def test_02_successfull_path_computation_phtnc_service_spdr_x2(self):
    #     self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
    #     self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     print("getServiceResponse Service1 PhotonicMedia: {}".format(response))
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     print(response['output']['response-parameters']['path-description'])
    #     self.assertEqual(0, response['output']['response-parameters']['path-description']
    #                      ['aToZ-direction']['aToZ-wavelength-number'])
    #     time.sleep(2)

    # def test_03_create_OCh_service_PhotonicMedia(self):
    #     response = test_utils.transportpce_api_rpc_request(
    #         'org-openroadm-service', 'service-create', self.cr_serv_input_data2)
    #     time.sleep(self.WAITING)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('PCE calculation in progress',
    #                   response['output']['configuration-response-common']['response-message'])
        # time.sleep(self.WAITING)

    def test_03_create_connectivity_service_PhotonicMedia(self):
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AOTS
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZOTS
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services.pm))

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

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_04_get_OCh_servicephotonic1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicephotonic-1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'servicephotonic-1')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.WAITING)

    # def test_05_successfull_path_computation_ODU4(self):
    #     self.path_computation_input_data["service-a-end"]["service-format"] = "ODU"
    #     self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
    #     self.path_computation_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
    #     self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.n2AiODU
    #     self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.n2AiODU
    #     self.path_computation_input_data["service-z-end"]["service-format"] = "ODU"
    #     self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
    #     self.path_computation_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
    #     self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.n2ZiODU
    #     self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.n2ZiODU
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     print("getServiceResponse Service1 ODU4: {}".format(response))
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
    #                   response['output']['configuration-response-common']['response-message'])
    #     print(response['output']['response-parameters']['path-description'])
    #     time.sleep(2)
    
    def test_06_create_connectivity_service_ODU4(self):
        self.cr_serv_input_data["name"][0]["value"] = "serviceOdu4-1"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AiODU
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"]= self.n2AiODU
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.n2AiODU
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZiODU
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"]= self.n2ZiODU
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.n2ZiODU
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("digital_otn ODU service uuid : {}".format(self.uuid_services.pm))
    
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
    
        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)
    
    def test_07_get_ODU4_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceOdu4-1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        print("getODU4 Service Connection-type : {}".format(response['services'][0]['connection-type']))
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'serviceOdu4-1')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    # def test_06_create_connectivity_service_ODU4(self):
    #     self.cr_serv_input_data["name"][0]["value"] = "serviceOdu4-1"
    #     self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DIGITAL_OTN"
    #     self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AeODU
    #     self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"]= self.n2AeODU
    #     self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.n2AeODU
    #     self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZeODU
    #     self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"]= self.n2ZeODU
    #     self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.n2ZeODU
    #     response = test_utils.transportpce_api_rpc_request(
    #         'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    #     time.sleep(self.WAITING)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.uuid_services.pm = response['output']['service']['uuid']
    #     # pylint: disable=consider-using-f-string
    #     print("digital_otn ODU service uuid : {}".format(self.uuid_services.pm))
    #
    #     input_dict_1 = {'administrative-state': 'LOCKED',
    #                     'lifecycle-state': 'PLANNED',
    #                     'operational-state': 'DISABLED',
    #                     # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
    #                     # 'service-layer': 'PHOTONIC_MEDIA',
    #                     'layer-protocol-name': 'PHOTONIC_MEDIA',
    #                     # 'connectivity-direction': 'BIDIRECTIONAL'
    #                     'direction': 'BIDIRECTIONAL'
    #                     }
    #     input_dict_2 = {'value-name': 'OpenROADM node id',
    #                     'value': 'SPDR-SC1-XPDR1'}
    #     input_dict_3 = {'value-name': 'OpenROADM node id',
    #                     'value': 'SPDR-SA1-XPDR1'}
    #
    #     self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
    #                          response['output']['service'])
    #     self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
    #                          response['output']['service']['end-point'][0]['name'][0])
    #     self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
    #                          response['output']['service']['end-point'][1]['name'][0])
    #     # If the gate fails is because of the waiting time not being enough
    #     time.sleep(self.WAITING)
    #
    # def test_07_get_ODU4_service1(self):
    #     response = test_utils.get_ordm_serv_list_attr_request("services", "serviceOdu4-1")
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     print("getODU4 Service Connection-type : {}".format(response['services'][0]['connection-type']))
    #     self.assertEqual(response['services'][0]['administrative-state'], 'inService')
    #     self.assertEqual(response['services'][0]['service-name'], 'serviceOdu4-1')
    #     self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
    #     self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
    #     time.sleep(1)

    def test_08_successfull_path_computation_100GE_service__spdr_x2(self):
        self.path_computation_input_data["service-a-end"]["service-format"] = "other"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        del self.path_computation_input_data["service-a-end"]["odu-service-rate"]
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.n2ADSR
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.n2ADSR
        self.path_computation_input_data["service-z-end"]["service-format"] = "other"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        del self.path_computation_input_data["service-z-end"]["odu-service-rate"]
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.n2ZDSR
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.n2ZDSR
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 100GE: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])
        time.sleep(2)

    # def test_03_create_connectivity_service_PhotonicMedia(self):
    #     # self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAOTS
    #     # self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZOTS
    #     response = test_utils.transportpce_api_rpc_request(
    #         'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
    #     time.sleep(self.WAITING)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.uuid_services.pm = response['output']['service']['uuid']
    #     # pylint: disable=consider-using-f-string
    #     print("photonic media service uuid : {}".format(self.uuid_services.pm))
    #
    #     input_dict_1 = {'administrative-state': 'LOCKED',
    #                     'lifecycle-state': 'PLANNED',
    #                     'operational-state': 'DISABLED',
    #                     # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
    #                     # 'service-layer': 'PHOTONIC_MEDIA',
    #                     'layer-protocol-name': 'PHOTONIC_MEDIA',
    #                     # 'connectivity-direction': 'BIDIRECTIONAL'
    #                     'direction': 'BIDIRECTIONAL'
    #                     }
    #     input_dict_2 = {'value-name': 'OpenROADM node id',
    #                     'value': 'SPDR-SC1-XPDR1'}
    #     input_dict_3 = {'value-name': 'OpenROADM node id',
    #                     'value': 'SPDR-SA1-XPDR1'}
    #
    #     self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
    #                          response['output']['service'])
    #     self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
    #                          response['output']['service']['end-point'][0]['name'][0])
    #     self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
    #                          response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
    #        time.sleep(self.WAITING)

    # Path Computation success
    # def test_03_path_computation_rdm_bi(self):
    #     self.path_computation_input_data["service-name"] = "6c6532ac-0b91-4a11-8321-9976e64c9a70"
    #     self.path_computation_input_data["service-a-end"]["node-id"] = "ROADM-A1+PHOTONIC_MEDIA"
    #     self.path_computation_input_data["service-a-end"]["clli"] = "3b726367-6f2d-3e3f-9033-d99b61459075"
    #     self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = "121893e2-53f0-3f90-b8fe-87960bc6e75d1CCC"
    #     self.path_computation_input_data["service-a-end"]["node-id"] = "ROADM-C1+PHOTONIC_MEDIA"
    #     self.path_computation_input_data["service-a-end"]["clli"] = "4986dca9-2d59-3d79-b306-e11802bcf1e6"
    #     self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = "b2a2185e-1bd8-3fc1-8083-b50d08a1f21f"
    #     response = test_utils.transportpce_api_rpc_request('transportpce-pce',
    #                                                        'path-computation-request',
    #                                                        self.path_computation_input_data)
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertIn('Path is calculated',
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
