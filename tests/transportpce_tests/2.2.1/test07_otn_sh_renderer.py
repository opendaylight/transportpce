#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import time
import requests
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.get_netconf_oper_request("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_connect_SPDR_SC1(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.get_netconf_oper_request("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_03_service_create_OTU4(self):
        url = "{}/operations/transportpce-renderer:service-implementation-request"
        data = {
            "transportpce-renderer:input": {
                "transportpce-renderer:service-name": "SPDRA-SPDRC-OTU4-ODU4",
                "transportpce-renderer:connection-type": "infrastructure",
                "transportpce-renderer:service-handler-header": {
                    "transportpce-renderer:request-id": "abcd12-efgh34"
                },
                "transportpce-renderer:service-a-end": {
                    "transportpce-renderer:service-format": "OTU",
                    "transportpce-renderer:otu-service-rate": "org-openroadm-otn-common-types:OTU4",
                    "transportpce-renderer:clli": "nodeSA",
                    "transportpce-renderer:node-id": "SPDR-SA1"

                },
                "transportpce-renderer:service-z-end": {
                    "transportpce-renderer:service-format": "OTU",
                    "transportpce-renderer:otu-service-rate": "org-openroadm-otn-common-types:OTU4",
                    "transportpce-renderer:clli": "nodeSC",
                    "transportpce-renderer:node-id": "SPDR-SC1"
                },
                "transportpce-renderer:path-description": {
                    "aToZ-direction": {
                        "rate": 100,
                        "transportpce-renderer:modulation-format": "dp-qpsk",
                        "aToZ-wavelength-number": 1,
                        "aToZ": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": ""
                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": ""
                                }
                            }
                        ],
                        "transportpce-renderer:aToZ-min-frequency": 196.075,
                        "transportpce-renderer:aToZ-max-frequency": 196.125
                    },
                    "transportpce-renderer:zToA-direction": {
                        "transportpce-renderer:zToA-wavelength-number": "1",
                        "transportpce-renderer:rate": "100",
                        "transportpce-renderer:modulation-format": "dp-qpsk",
                        "zToA": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": ""
                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": ""
                                }
                            }
                        ],
                        "transportpce-renderer:zToA-min-frequency": 196.075,
                        "transportpce-renderer:zToA-max-frequency": 196.125
                    }
                }
            }
        }
        response = test_utils.post_request(url, data)
        time.sleep(3)
        print(response.json())
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]["response-message"])

    # Test OCH-OTU interfaces on SPDR-A1
    def test_04_check_interface_och(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_05_check_interface_OTU(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': 'AMf1n5hK6Xkk',
                        'expected-sapi': 'AMf1n5hK6Xkk',
                        'tx-sapi': 'H/OelLynehI=',
                        'expected-dapi': 'H/OelLynehI=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    # Test OCH-OTU interfaces on SPDR-C1
    def test_06_check_interface_och(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_07_check_interface_OTU(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'H/OelLynehI=',
                        'tx-sapi': 'AMf1n5hK6Xkk',
                        'expected-dapi': 'AMf1n5hK6Xkk',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    # Test creation of ODU4 service
    def test_08_service_create_ODU4(self):
        url = "{}/operations/transportpce-renderer:service-implementation-request"

        data = {
            "transportpce-renderer:input": {
                "transportpce-renderer:service-name":
                "SPDRA-SPDRC-OTU4-ODU4",
                "transportpce-renderer:connection-type": "infrastructure",
                "transportpce-renderer:service-handler-header": {
                    "transportpce-renderer:request-id": "abcd12-efgh34"
                },
                "transportpce-renderer:service-a-end": {
                    "transportpce-renderer:service-format": "ODU",
                    "transportpce-renderer:odu-service-rate":
                    "org-openroadm-otn-common-types:ODU4",
                    "transportpce-renderer:clli": "nodeSA",
                    "transportpce-renderer:node-id": "SPDR-SA1"

                },
                "transportpce-renderer:service-z-end": {
                    "transportpce-renderer:service-format": "ODU",
                    "transportpce-renderer:odu-service-rate":
                    "org-openroadm-otn-common-types:ODU4",
                    "transportpce-renderer:clli": "nodeSC",
                    "transportpce-renderer:node-id": "SPDR-SC1"
                },
                "transportpce-renderer:path-description": {
                    "aToZ-direction": {
                        "rate": 100,
                        "aToZ": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": ""
                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": ""
                                }
                            }
                        ]
                    },
                    "transportpce-renderer:zToA-direction": {
                        "transportpce-renderer:rate": "100",
                        "zToA": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": ""
                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": ""
                                }
                            }
                        ]
                    }
                }
            }
        }
        response = test_utils.post_request(url, data)
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]
                      ["response-message"])

    # Test ODU4 interfaces on SPDR-A1 and SPDR-C1
    def test_09_check_interface_ODU4(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'expected-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'AMf1n5hK6Xkk',
                        'tx-dapi': 'AMf1n5hK6Xkk',
                        'tx-sapi': 'H/OelLynehI='}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_10_check_interface_ODU4(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'tx-sapi': 'AMf1n5hK6Xkk',
                        'tx-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'H/OelLynehI=',
                        'expected-dapi': 'AMf1n5hK6Xkk'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # Test creation of 10G service
    def test_11_service_create_10GE(self):
        url = "{}/operations/transportpce-renderer:service-implementation-request"

        data = {
            "transportpce-renderer:input": {
                "transportpce-renderer:service-name": "SPDRA-SPDRC-10G",
                "transportpce-renderer:connection-type": "service",
                "transportpce-renderer:service-handler-header": {
                    "transportpce-renderer:request-id": "abcd12-efgh34"
                },
                "transportpce-renderer:service-a-end": {
                    "transportpce-renderer:service-format": "Ethernet",
                    "transportpce-renderer:service-rate": "10",
                    "transportpce-renderer:clli": "nodeSA",
                    "transportpce-renderer:node-id": "SPDR-SA1"
                },
                "transportpce-renderer:service-z-end": {
                    "transportpce-renderer:service-format": "Ethernet",
                    "transportpce-renderer:service-rate": "10",
                    "transportpce-renderer:clli": "nodeSC",
                    "transportpce-renderer:node-id": "SPDR-SC1"
                },
                "transportpce-renderer:path-description": {
                    "aToZ-direction": {
                        "rate": 10,
                        "min-trib-slot": "1.1",
                        "max-trib-slot": "1.8",
                        "aToZ": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-CLIENT1"

                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-CLIENT1"
                                }
                            }
                        ]
                    },
                    "transportpce-renderer:zToA-direction": {
                        "rate": "10",
                        "min-trib-slot": "1.1",
                        "max-trib-slot": "1.8",
                        "zToA": [
                            {
                                "id": "0",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-CLIENT1"
                                }
                            },
                            {
                                "id": "1",
                                "resource": {
                                    "tp-node-id": "SPDR-SC1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "2",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-NETWORK1"
                                }
                            },
                            {
                                "id": "3",
                                "resource": {
                                    "tp-node-id": "SPDR-SA1-XPDR1",
                                    "tp-id": "XPDR1-CLIENT1"

                                }
                            }
                        ]
                    }
                }
            }
        }

        response = test_utils.post_request(url, data)
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]
                      ["response-message"])

    # Test the interfaces on SPDR-A1
    def test_12_check_interface_10GE_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_13_check_interface_ODU2E_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_ODU2E_connection(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1",
            "odu-connection/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G'},
                             res['odu-connection'][0]['source'])

    def test_15_check_interface_ODU2E_NETWORK(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    # Test the interfaces on SPDR-C1
    def test_16_check_interface_ODU2E_NETWORK(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_17_check_interface_10GE_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_18_check_interface_ODU2E_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_19_check_ODU2E_connection(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1",
            "odu-connection/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G'},
                             res['odu-connection'][0]['source'])

    def test_20_check_interface_ODU2E_NETWORK(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    # TODO: Delete the services (OTU, ODU, LO-ODU)
    # TODO: Delete interfaces (SPDR-A1, SPDR-C1)

    def test_21_disconnect_SPDR_SA1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_22_disconnect_SPDR_SC1(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
