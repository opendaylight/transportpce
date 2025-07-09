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
# from builtins import True
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_generate_tapi_topo  # nopep8


# pylint: disable=too-few-public-methods
class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pmservicelist = list()
        self.oduservicelist = list()


class TransportPCEtest(unittest.TestCase):
    # SPDR-SA1-XPDR1 UUID is
    s1Auuid = "4e44bcc5-08d3-3fee-8fac-f021489e5a61"
    # SPDR-SA1-XPDR2 UUID is
    s2Auuid = "38c114ae-9c0e-3068-bb27-db2dbd81220b"
    # SPDR-SA1-XPDR3 UUID is
    s3Auuid = "4582e51f-2b2d-3b70-b374-86c463062710"
    # SPDR-SC1-XPDR1 UUID is
    s1Cuuid = "215ee18f-7869-3492-94d2-0f24ed0a3023"
    # SPDR-SC1-XPDR2 UUID is
    s2Cuuid = "d852c340-77db-3f9a-96e8-cb4de8e1004a"
    # SPDR-SC1-XPDR3 UUID is
    s3Cuuid = "c1f06957-c0b9-32be-8492-e278b2d4a3aa"
    # ROADM-A1-SRG1 UUID is
    rSRG1Auuid = "63a5b34b-02da-390f-835b-177e7bc7e1a8"
    # ROADM-A1-DEG2 UUID is
    rDEG2Auuid = "b3e98baf-ef7d-3814-8fd2-5c89d06214a7"
    # ROADM-C1-SRG1 UUID is
    rSRG1Cuuid = "12bcf05d-e3f8-3d76-8142-c5016f203b6d"
    # ROADM-C1-DEG1 UUID is
    rDEG1Cuuid = "9830675c-71a5-33a3-8c5c-1730dee9f4f0"

    # SIP+SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sAX1DSR1 = "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
    # SIP+SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sZX1DSR1 = "25812ef2-625d-3bf8-af55-5e93946d1c22"
    # SIP+SPDR-SA1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sAX1eODU1 = "b6421484-531f-3444-adfc-e11c503f1fab"
    # SIP+SPDR-SC1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sZX1eODU1 = "4511fca7-0cf7-3b27-a128-3b372d5e1fa8"
    # SIP+SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sAX1OTS1 = "38d81f55-1798-3520-ba16-08efa56630c4"
    # SIP+SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sZX1OTS1 = "97d9ba27-0efa-3010-8b98-b4d73240120c"
    # SIP+SPDR-SA1-XPDR1+iODU+XPDR1-NETWORK1 UUID IS
    sAX1iODU1 = "5efda776-f8de-3e0b-9bbd-2c702e210946"
    # SIP+SPDR-SC1-XPDR1+iODU+XPDR1-NETWORK1 UUID IS
    sZX1iODU1 = "8116d0af-39fa-3df5-bed2-dd2cd5e8217d"
    # LCP (NEP) SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1--NETWORK1 UUID IS
    nAX1OTS1 = "21efd6a4-2d81-3cdb-aabb-b983fb61904e"
    # LCP (NEP) SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    nZX1OTS1 = "ff10784b-3da2-3b88-88c3-27abc02b66fe"
    # LCP (NEP) SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    nAX1DSR1 = "c6cd334c-51a1-3995-bed3-5cf2b7445c04"
    # LCP (NEP) SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    nZX1DSR1 = "50b7521a-4a38-358f-9846-45c55813416a"
    # LCP (NEP) SPDR-SA1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    nAX1eODU1 = "72c6b97a-3944-3d88-9882-b7e688bb2772"
    # LCP (NEP) SPDR-SC1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    nZX1eODU1 = "99e1461c-4679-3dc5-9f59-b3054dce08a4"
    # LCP (NEP) SPDR-SA1-XPDR1+iODU+XPDR1-NETWORK1 UUID IS
    nAX1iODU1 = "2bdca70f-ef1e-3e56-b251-07eda88f31ba"
    # LCP (NEP) SPDR-SC1-XPDR1+iODU+XPDR1-NETWORK1 UUID IS
    nZX1iODU1 = "7b42f2cb-0fe8-33eb-9000-4f1ca262b384"

    # SIP+SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    sAX2DSR1 = "6980f38d-a3a0-31ab-85b3-b98f9537fc41"
    # SIP+SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    sZX2DSR1 = "43b47382-8192-36b3-89ff-641375bbf2c7"
    # SIP+SPDR-SA1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    sAX2eODU1 = "46486b63-9ca7-3854-ae86-6852a8ae12ad"
    # SIP+SPDR-SC1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    sZX2eODU1 = "0a24f848-55ac-3543-80ba-f74ead3a2064"
    # SIP+SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    sAX2OTS1 = "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
    # SIP+SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    sZX2OTS1 = "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
    # SIP+SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    sAX2iODU1 = "86ed195a-c926-334a-8a64-9418ca096579"
    # SIP+SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    sZX2iODU1 = "97598b05-b5a2-383b-a663-66290ba5d3e7"

    # LCP (NEP) SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2--NETWORK2 UUID IS
    nAX2OTS2 = "4e4bf439-b457-3260-865c-db716e2647c2"
    # LCP (NEP) SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2 UUID IS
    nZX2OTS2 = "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
    # LCP (NEP) SPDR-SA1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    nAX2DSR1 = "935d763e-297c-332f-8edc-65496a2a607c"
    # LCP (NEP) SPDR-SC1-XPDR2+DSR+XPDR2-CLIENT1 UUID IS
    nZX2DSR1 = "eddd56d6-0aa7-3583-9b1b-40470ed2cf96"
    # LCP (NEP) SPDR-SA1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    nAX2eODU1 = "871ac1d5-7b52-3e6f-b4eb-bc7cc6c7fa6a"
    # LCP (NEP) SPDR-SC1-XPDR2+eODU+XPDR2-CLIENT1 UUID IS
    nZX2eODU1 = "06336b5c-458b-395d-8f7f-167d24b15737"
    # LCP (NEP) SPDR-SA1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    nAX2iODU1 = "6f4777d4-41f0-3833-b811-68b90903d834"
    # LCP (NEP) SPDR-SC1-XPDR2+iODU+XPDR2-NETWORK1 UUID IS
    nZX2iODU1 = "8b82dac3-646c-301d-b455-b7a4802774f7"

    # SIP+SPDR-SA1-XPDR3+DSR+XPDR3-CLIENT1 UUID IS
    sAX3DSR1 = "2fd680f6-9aae-34b8-8a47-e01f3605f8e42fd680f6-9aae-34b8-8a47-e01f3605f8e4"
    # SIP+SPDR-SC1-XPDR3+DSR+XPDR3-CLIENT1 UUID IS
    sZX3DSR1 = "440b1bee-a312-31ba-ad46-24b8da1037b3"
    # SIP+SPDR-SA1-XPDR3+eODU+XPDR3-CLIENT1 UUID IS
    sAX3eODU1 = "ba18706f-e576-324f-91d3-916200203dd3"
    # SIP+SPDR-SC1-XPDR3+eODU+XPDR3-CLIENT1 UUID IS
    sZX3eODU1 = "8299a120-f2df-30d7-a9f5-a407f9968689"
    # SIP+SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    sAX3OTS1 = "858e8cbf-d264-3e6f-858e-cdb6257d3aa9"
    # SIP+SPDR-SC1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    sZX3OTS1 = "6431ec11-f109-39f6-9568-c037f0e006f3"
    # SIP+SPDR-SA1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    sAX3iODU1 = "3f395974-bb7a-3411-ac4c-f791752bdf06"
    # SIP+SPDR-SC1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    sZX3iODU1 = "2fe1c941-f9c9-34f4-86c4-8e97ae880ecd"
    # LCP (NEP) SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3--NETWORK1 UUID IS
    nAX3OTS1 = "339d9470-aef0-304f-9667-b685e1624653"
    # LCP (NEP) SPDR-SC1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    nZX3OTS1 = "c0dda59b-8af1-3996-ba32-8fc4b12d944a"
    # LCP (NEP) SPDR-SA1-XPDR3+DSR+XPDR3-CLIENT1 UUID IS
    nAX3DSR1 = "a69d96bf-4f0b-36a2-be27-b5c131dba1ce"
    # LCP (NEP) SPDR-SC1-XPDR3+DSR+XPDR3-CLIENT1 UUID IS
    nZX3DSR1 = "7ffb497d-03bd-3627-be54-0f2486623c8c"
    # LCP (NEP) SPDR-SA1-XPDR3+eODU+XPDR3-CLIENT1 UUID IS
    nAX3eODU1 = "4b19b632-02bc-3ae9-b52e-9dba731608a9"
    # LCP (NEP) SPDR-SC1-XPDR3+eODU+XPDR1-CLIENT1 UUID IS
    nZX3eODU1 = "671fe9af-4e0b-3441-b0e4-0393f0edceac"
    # LCP (NEP) SPDR-SA1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    nAX3iODU1 = "9f4f5e08-5590-30e4-a7ff-f9cad13090bc"
    # LCP (NEP) SPDR-SC1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    nZX3iODU1 = "abd4fb5d-7ec0-3fbc-884a-d6e6615f7981"

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
            "tx-direction": {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "rx-direction": {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "node-id": "SPDR-SA1-XPDR2+XPONDER"
        },
        "service-z-end": {
            "clli": "d852c340-77db-3f9a-96e8-cb4de8e1004a",
            "service-format": "OTU",
            "tx-direction": {
                "logical-connection-point": "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
            },
            "rx-direction": {
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

    # del_serv_input_data = {
    #     "sdnc-request-header": {
    #         "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
    #         "rpc-action": "service-delete",
    #         "request-system-id": "appname",
    #         "notification-url": "http://localhost:8585/NotificationServer/notify"},
    #     "service-delete-req-info": {
    #         "service-name": "TBD",
    #         "tail-retention": "no"}
    # }
    del_serv_input_data = {"uuid": "TBD"}

    processes = None
    WAITING = 10  # nominal value is 300
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()

    del_serv_input_data = {"uuid": "TBD"}

    tapi_topo = {"topology-id": "TBD"}

    @classmethod
    def setUpClass(cls):
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
        cls.processes = test_utils.start_tpce()
        # TAPI feature is not installed by default in Karaf
        if "NO_ODL_STARTUP" not in os.environ or "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing tapi feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed = True
        if cls.init_failed:
            print("tapi installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils_generate_tapi_topo.initSimulators()
        test_utils_generate_tapi_topo.connect_xpdrs()
        test_utils_generate_tapi_topo.connect_rdms()
        test_utils_generate_tapi_topo.interconnect_roadm_and_xpdr()
        test_utils_generate_tapi_topo.add_omsAttributes_to_ROADM2ROADM_links()
        # test_utils_generate_tapi_topo.create_services_on_infra()
        print("Entering PCE Test 05 to test PCE algo for TAPI")

    @classmethod
    def tearDownClass(cls):
        test_utils_generate_tapi_topo.cleanup_and_disconnect_devices()
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")
        test_utils.copy_karaf_log(cls.__name__)
        test_utils_generate_tapi_topo.uninstall_Tapi_Feature()
        print("Exiting PCE Test 05 to test PCE algo for TAPI")
        # test_utils_generate_tapi_topo.tearDownClass(cls)

    def setUp(self):
        time.sleep(2)

    # Test failed path computation
    def test_01_fail_path_computation(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.incomplete_path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Service Name is not set',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Path Computation success
    def test_02_successfull_path_computation_phtnc_service_spdr_x2(self):
        # Path Computation from NEP of SPDR-SA1-XPDR2-NETWORK2 to NEP of of SPDR-SC1-XPDR2-NETWORK2
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 PhotonicMedia: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])
        self.assertEqual(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'], 0)
        self.assertEqual(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-max-frequency'], '196.125')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['width'],
                         '40.0')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s2Auuid, self.s2Cuuid, self.rSRG1Auuid, self.rDEG2Auuid, self.rSRG1Cuuid, self.rDEG1Cuuid]
        link_to_find = ['79b23827-48eb-33ed-b110-fbeca32c4125', '7539f869-b93b-3371-8b5c-270717655db1',
                        'de09187f-5991-39c5-a43c-0f0aa83dcb99', '4172e87e-e7a7-31c1-aeff-c3601433345f',
                        '2f9d34e5-de00-3992-b6fd-6ba5c0e46bef', '0f58cca7-87ac-368e-a526-49e47227b917',
                        'd5be9a99-6663-3894-b328-7e835cb2ba4f', '984c8390-962d-392f-9a3f-9d5da3d37666',
                        'dabb9270-1855-3776-ae53-358f1ef085ef', '32a5955e-8d3e-3c37-a77a-2f6d8a1291e6']
        # in TP to find, include SGR1-PP4 of both ROADM A & C
        tp_to_find = [self.nAX2OTS2, self.nZX2OTS2, 'cd2619fb-1ae0-3785-a6fd-2b71f468cb6c',
                      'b2a2185e-1bd8-3fc1-8083-b50d08a1f21f']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN tps in PathDescription')
        time.sleep(self.WAITING)

    def test_03_create_connectivity_service_PhotonicMedia(self):
        # Service creation from SIP of SPDR-SA1-XPDR2-NETWORK1 to SIP of of SPDR-SC1-XPDR2-NETWORK1
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX2OTS1
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX2OTS1
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pmservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("the response after creation of servicephotonic-1 is  : {}".format(response['output']))
        print("photonic media service uuid : {}".format(self.uuid_services.pmservicelist[0]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        # 'service-layer': 'PHOTONIC_MEDIA',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        # 'connectivity-direction': 'BIDIRECTIONAL'
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SC1-XPDR2'}
        input_dict_3 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SA1-XPDR2'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(input_dict_2, response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(input_dict_3, response['output']['service']['end-point'][1]['name'][0])
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

    def test_05_successfull_path_computation_ODU4(self):
        self.path_computation_input_data["service-a-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX2iODU1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX2iODU1
        self.path_computation_input_data["service-z-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX2iODU1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX2iODU1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 ODU4: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])

        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        node_to_find = [self.s2Auuid, self.s2Cuuid]
        # tp to find are the iOTU CEP
        tp_to_find = ['b9dbee10-faa9-3947-94c1-3c023646a2df', 'bb58ebb0-ca7c-3518-8ffd-808abfca54e5']
        # client TP do not appear in path description : openROADM legacy
        # tp_to_find = [self.nAX2iODU1, self.nZX2iODU1]
        link_to_find = ['07df4edd-4408-310d-a820-5f34b0524900', '021f6a20-c7fb-3937-8b9f-c8ba5121692e']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN tps in PathDescription')
        time.sleep(self.WAITING)

    def test_06_create_connectivity_service_ODU4(self):
        self.cr_serv_input_data["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["name"][0]["value"] = "serviceOdu4-1"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX2iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = self.nAX2iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.nAX2iODU1
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX2iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = self.nZX2iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.nZX2iODU1
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.oduservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("digital_otn ODU service uuid : {}".format(self.uuid_services.oduservicelist[0]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        # 'service-layer': 'PHOTONIC_MEDIA',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        # 'connectivity-direction': 'BIDIRECTIONAL'
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SC1-XPDR2'}
        input_dict_3 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SA1-XPDR2'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(input_dict_2, response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(input_dict_3, response['output']['service']['end-point'][1]['name'][0])
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

    def test_08_successfull_path_computation_100GE_service__spdr_x2(self):
        # service-format set to other + service-rate set to 100 leads to 100GEs serviceType internally (switch)
        self.path_computation_input_data["service-a-end"]["service-format"] = "other"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        del self.path_computation_input_data["service-a-end"]["odu-service-rate"]
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX2DSR1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX2DSR1
        self.path_computation_input_data["service-z-end"]["service-format"] = "other"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        del self.path_computation_input_data["service-z-end"]["odu-service-rate"]
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX2DSR1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX2DSR1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 100GE: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s2Auuid, self.s2Cuuid]
        link_to_find = ['021f6a20-c7fb-3937-8b9f-c8ba5121692e', '01ee8fe0-4819-3171-9220-483c95c469ef']
        # client TP do not appear in path description : openROADM legacy
        # tp_to_find = [self.nAX2DSR1, self.nZX2DSR1]
        tp_to_find = ['eb1dfceb-9b93-38a4-8e34-e176cf5f5bf0', '7500a338-8f63-3751-8b89-0a61876f9d9a']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN links in PathDescription')
        print(response['output']['response-parameters']['path-description'])
        time.sleep(2)

    def test_09_successfull_path_computation_phtnc_service_spdr_x1(self):
        # Path Computation from NEP of SPDR-SA1-XPDR1-NETWORK1 to NEP of of SPDR-SC1-XPDR1-NETWORK1
        self.path_computation_input_data["service-name"] = "383790b1-ce29-4b7f-8462-1ca72d9db6ad"
        self.path_computation_input_data["service-a-end"]["clli"] = self.s1Auuid
        self.path_computation_input_data["service-a-end"]["service-format"] = "OTU"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-a-end"]["node-id"] = "SPDR-SA1-XPDR1+XPONDER"
        self.path_computation_input_data["service-a-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX1OTS1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX1OTS1
        self.path_computation_input_data["service-z-end"]["clli"] = self.s1Cuuid
        self.path_computation_input_data["service-z-end"]["service-format"] = "OTU"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["node-id"] = "SPDR-SC1-XPDR1+XPONDER"
        self.path_computation_input_data["service-z-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX1OTS1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX1OTS1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service2 PhotonicMedia: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])
# Following test line to be activated when changes associated with spectrum updates in topology are merged
        # self.assertEqual(response['output']['response-parameters']['path-description']
        #                  ['aToZ-direction']['aToZ-wavelength-number'], 0)
        # self.assertEqual(response['output']['response-parameters']['path-description']
        #                  ['aToZ-direction']['aToZ-max-frequency'], '196.125')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['width'],
                         '40.0')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s1Auuid, self.s1Cuuid, self.rSRG1Auuid, self.rDEG2Auuid, self.rSRG1Cuuid, self.rDEG1Cuuid]
        link_to_find = ['c72c7995-8f3a-30ee-940f-309880898d57', '7539f869-b93b-3371-8b5c-270717655db1',
                        'de09187f-5991-39c5-a43c-0f0aa83dcb99', 'ca501fad-f7a4-36dc-ab50-64c6a973e9b0',
                        '2f9d34e5-de00-3992-b6fd-6ba5c0e46bef', '19b7f180-ea7c-3b78-9e75-5107029cbdf9',
                        'd5be9a99-6663-3894-b328-7e835cb2ba4f', '984c8390-962d-392f-9a3f-9d5da3d37666',
                        'dabb9270-1855-3776-ae53-358f1ef085ef', '3b7db097-6186-30a0-9aaa-e58f0bd5227f']
        # in TP to find, include SGR1-PP1 of both ROADM A & C
        tp_to_find = [self.nAX1OTS1, self.nZX1OTS1, 'affbbabc-1f42-31ee-92fc-ea5311f0c2f9',
                      'abfc9b93-cfae-35a8-9ea9-7fb66b568927']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN tps in PathDescription')
        time.sleep(self.WAITING)

    def test_10_create_connectivity_service_PhotonicMedia2(self):
        # Service creation from SIP of SPDR-SA1-XPDR1-NETWORK1 to SIP of of SPDR-SC1-XPDR1-NETWORK1
        self.cr_serv_input_data["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["name"][0]["value"] = "servicephotonic-2"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX1OTS1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = self.s1Auuid
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = self.nAX1OTS1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.nAX1OTS1
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX1OTS1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = self.s1Cuuid
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = self.nZX1OTS1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.nZX1OTS1
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR1"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pmservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services.pmservicelist[1]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(input_dict_2, response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(input_dict_3, response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_11_get_OCh_servicephotonic2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicephotonic-2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'servicephotonic-2')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.WAITING)

    def test_12_successfull_path_computation_ODU4(self):
        self.path_computation_input_data["service-a-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX1iODU1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX1iODU1
        self.path_computation_input_data["service-z-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX1iODU1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX1iODU1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 ODU4: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])

        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        node_to_find = [self.s1Auuid, self.s1Cuuid]
        # tp to find are the iOTU CEP
        tp_to_find = ['a24840c1-a968-3e94-9689-0bc4e538528a', '37b8a89d-6bcd-359f-9432-9dd06e4ff462']
        # client TP do not appear in path description : openROADM legacy
        link_to_find = ['606ade82-de21-3e95-b2cd-cdb2b03ef78b', '13b8ac31-56d7-36e9-814b-5d91f10ced16']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Did not Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Did not Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Did notFound expected OTN tps in PathDescription')
        time.sleep(self.WAITING)

    def test_13_create_connectivity_service_ODU4_2(self):
        self.cr_serv_input_data["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["name"][0]["value"] = "serviceOdu4-2"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX1iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = self.nAX1iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.nAX1iODU1
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX1iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = self.nZX1iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.nZX1iODU1
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.oduservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("digital_otn ODU service uuid : {}".format(self.uuid_services.oduservicelist[1]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
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

    def test_14_get_ODU4_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceOdu4-2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        print("getODU4 Service Connection-type : {}".format(response['services'][0]['connection-type']))
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'serviceOdu4-2')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.WAITING)

    def test_15_successfull_path_computation_10GE_service__spdr_x1(self):
        self.path_computation_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "10"
        del self.path_computation_input_data["service-a-end"]["odu-service-rate"]
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX1DSR1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX1DSR1
        self.path_computation_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "10"
        del self.path_computation_input_data["service-z-end"]["odu-service-rate"]
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX1DSR1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX1DSR1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 10GE: {}".format(response))
        print(response['output']['response-parameters']['path-description'])
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 10)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s1Auuid, self.s1Cuuid]
        link_to_find = ['b90f7b96-4fe0-390c-8ef2-41942196f19e', 'd011697a-5660-39e3-ab35-a53175a484d8']
        # client TP do not appear in path description : openROADM legacy
        # tp_to_find = [self.nAX2DSR1, self.nZX2DSR1]
        tp_to_find = ['a2f84255-2775-34c9-ab0a-7c17a4249703', 'd6e08276-b5a9-3960-a3e1-14f8f72c280b']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN links in PathDescription')
        time.sleep(self.WAITING)

    def test_16_create_connectivity_service_PhotonicMedia3(self):
        # Service creation from SIP of SPDR-SA1-XPDR3-NETWORK1 to SIP of of SPDR-SC1-XPDR3-NETWORK1
        self.cr_serv_input_data["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["name"][0]["value"] = "servicephotonic-3"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX3OTS1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = self.s3Auuid
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = self.nAX3OTS1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.nAX3OTS1
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "PHOTONIC_MEDIA"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX3OTS1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = self.s3Cuuid
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = self.nZX3OTS1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.nZX3OTS1
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR3"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pmservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services.pmservicelist[2]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SC1-XPDR3'}
        input_dict_3 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SA1-XPDR3'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(input_dict_2, response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(input_dict_3, response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_17_get_OCh_servicephotonic3(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicephotonic-3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'servicephotonic-3')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.WAITING)

    def test_18_create_connectivity_service_ODU4_3(self):
        self.cr_serv_input_data["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["name"][0]["value"] = "serviceOdu4-3"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAX3iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = self.nAX3iODU1
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = self.nAX3iODU1
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DIGITAL_OTN"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZX3iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = self.nZX3iODU1
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = self.nZX3iODU1
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.oduservicelist.append(response['output']['service']['uuid'])
        # pylint: disable=consider-using-f-string
        print("digital_otn ODU service uuid : {}".format(self.uuid_services.oduservicelist[2]))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SC1-XPDR3'}
        input_dict_3 = {'value-name': 'OpenROADM node id', 'value': 'SPDR-SA1-XPDR3'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_19_get_ODU4_service3(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceOdu4-3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        print("getODU4 Service Connection-type : {}".format(response['services'][0]['connection-type']))
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'serviceOdu4-3')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(5)

    def test_20_successfull_path_computation_1GE_service__spdr_x3(self):

        self.path_computation_input_data["service-name"] = "383790b1-ce29-4b7f-8462-1ca72d9db6af"
        self.path_computation_input_data["service-a-end"]["clli"] = self.s3Auuid
        self.path_computation_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "1"
        self.path_computation_input_data["service-a-end"]["node-id"] = "SPDR-SA1-XPDR3+XPONDER"
        self.path_computation_input_data["service-a-end"]["tx-direction"]["logical-connection-point"] = self.nAX3DSR1
        self.path_computation_input_data["service-a-end"]["rx-direction"]["logical-connection-point"] = self.nAX3DSR1
        self.path_computation_input_data["service-z-end"]["clli"] = self.s3Cuuid
        self.path_computation_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "1"
        self.path_computation_input_data["service-z-end"]["node-id"] = "SPDR-SC1-XPDR3+XPONDER"
        self.path_computation_input_data["service-z-end"]["tx-direction"]["logical-connection-point"] = self.nZX3DSR1
        self.path_computation_input_data["service-z-end"]["rx-direction"]["logical-connection-point"] = self.nZX3DSR1
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        print("getServiceResponse Service1 1GE: {}".format(response))
        print(response['output']['response-parameters']['path-description'])
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 1)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s3Auuid, self.s3Cuuid]
        link_to_find = ['59a78027-0c53-3a7e-aa15-c9258c637bc0', '9e237eea-ce80-3490-9a66-34f7cbdac55f']
        # client TP do not appear in path description : openROADM legacy
        tp_to_find = ['74b4b605-379f-3700-bb46-97b578cb2c7d', 'ced2adb2-e1fd-338c-8b64-a02106d48b45']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected OTN Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected OTN tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected OTN links in PathDescription')
        time.sleep(2)

    def test_21_delete_connectivity_services(self):
        for i in range(0, 3):
            self.del_serv_input_data["uuid"] = str(self.uuid_services.oduservicelist[i])
            response = test_utils.transportpce_api_rpc_request(
                'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
            self.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
            time.sleep(self.WAITING)
            self.del_serv_input_data["uuid"] = str(self.uuid_services.pmservicelist[i])
            response = test_utils.transportpce_api_rpc_request(
                'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
            self.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
            time.sleep(self.WAITING)

    def test_22_get_no_tapi_services(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-list', None)
        self.assertEqual(response['status_code'], requests.codes.internal_server_error)
        self.assertIn(
            {"error-type": "rpc", "error-tag": "operation-failed",
             "error-message": "No services exist in datastore"},
            response['output']['errors']['error'])

    def find_resource_in_response(self, resource_list, response):
        result_list = []
        for resourceToFind in resource_list:
            res_found_in_atoz = False
            res_found_in_ztoa = False
            for atozElt in response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ']:
                # print(f"atozElt = {atozElt}")
                if ('tp-id' in atozElt['resource'].keys() and atozElt['resource']['tp-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
                elif ('node-id' in atozElt['resource'].keys() and atozElt['resource']['node-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
            for ztoaElt in response['output']['response-parameters']['path-description']['zToA-direction']['zToA']:
                # print(f"ztoaElt = {ztoaElt}")
                if ('tp-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['tp-id'] == resourceToFind
                        and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
                elif ('node-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['node-id'] == resourceToFind
                      and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
            if res_found_in_atoz and res_found_in_ztoa:
                result_list.append(True)
            else:
                result_list.append(False)
        if False in result_list:
            return False
        return True

    def find_link_in_response(self, link_list, response):
        result_list = []
        for resourceToFind in link_list:
            res_found_in_atoz = False
            res_found_in_ztoa = False
            for atozElt in response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ']:
                # print(f"atozElt = {atozElt}")
                if ('link-id' in atozElt['resource'].keys() and atozElt['resource']['link-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
            for ztoaElt in response['output']['response-parameters']['path-description']['zToA-direction']['zToA']:
                # print(f"ztoaElt = {ztoaElt}")
                if ('link-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['link-id'] == resourceToFind
                        and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
            if (res_found_in_atoz or res_found_in_ztoa) and not (res_found_in_atoz and res_found_in_ztoa):
                result_list.append(True)
            else:
                result_list.append(False)
        if False in result_list:
            return False
        return True


if __name__ == "__main__":
    unittest.main(verbosity=2)
