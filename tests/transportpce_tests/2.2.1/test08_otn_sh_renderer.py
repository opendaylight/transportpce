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
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils_rfc8040  # nopep8


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils_rfc8040.start_tpce()
        cls.processes = test_utils_rfc8040.start_sims([('spdra', cls.NODE_VERSION),
                                                       ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils_rfc8040.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils_rfc8040.check_device_connection("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_02_connect_SPDR_SC1(self):
        response = test_utils_rfc8040.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils_rfc8040.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils_rfc8040.check_device_connection("SPDR-SC1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_03_service_create_OTU4(self):
        response = test_utils_rfc8040.device_renderer_service_implementation_request(
            {
                'service-name': 'SPDRA-SPDRC-OTU4-ODU4',
                'connection-type': 'infrastructure',
                'service-handler-header': {
                    'request-id': 'abcd12-efgh34'
                },
                'service-a-end': {
                    'service-format': 'OTU',
                    'otu-service-rate': 'org-openroadm-otn-common-types:OTU4',
                    'clli': 'nodeSA',
                    'node-id': 'SPDR-SA1'

                },
                'service-z-end': {
                    'service-format': 'OTU',
                    'otu-service-rate': 'org-openroadm-otn-common-types:OTU4',
                    'clli': 'nodeSC',
                    'node-id': 'SPDR-SC1'
                },
                'path-description': {
                    'aToZ-direction': {
                        'rate': 100,
                        'modulation-format': 'dp-qpsk',
                        'aToZ-wavelength-number': 1,
                        'aToZ': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': ''
                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': ''
                                }
                            }
                        ],
                        'aToZ-min-frequency': 196.075,
                        'aToZ-max-frequency': 196.125
                    },
                    'zToA-direction': {
                        'zToA-wavelength-number': '1',
                        'rate': '100',
                        'modulation-format': 'dp-qpsk',
                        'zToA': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': ''
                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': ''
                                }
                            }
                        ],
                        'zToA-min-frequency': 196.075,
                        'zToA-max-frequency': 196.125
                    }
                }
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Operation Successful',
                      response['output']['configuration-response-common']['response-message'])

    # Test OCH-OTU interfaces on SPDR-A1
    def test_04_check_interface_och(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-761:768")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])
        self.assertIn(
            response['interface'][0]['org-openroadm-optical-channel-interfaces:och'],
            [{'frequency': '196.1000', 'rate': 'org-openroadm-common-types:R100G',
              'transmit-power': '-5', 'modulation-format': 'dp-qpsk'},
             {'frequency': 196.1, 'rate': 'org-openroadm-common-types:R100G',
              'transmit-power': -5, 'modulation-format': 'dp-qpsk'}])

    def test_05_check_interface_OTU(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # Test OCH-OTU interfaces on SPDR-C1
    def test_06_check_interface_och(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SC1", "interface", "XPDR1-NETWORK1-761:768")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])
        self.assertIn(
            response['interface'][0]['org-openroadm-optical-channel-interfaces:och'],
            [{'frequency': '196.1000', 'rate': 'org-openroadm-common-types:R100G',
              'transmit-power': '-5', 'modulation-format': 'dp-qpsk'},
             {'frequency': 196.1, 'rate': 'org-openroadm-common-types:R100G',
              'transmit-power': -5, 'modulation-format': 'dp-qpsk'}])

    def test_07_check_interface_OTU(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SC1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # Test creation of ODU4 service
    def test_08_service_create_ODU4(self):
        response = test_utils_rfc8040.device_renderer_service_implementation_request(
            {
                'service-name':
                'SPDRA-SPDRC-OTU4-ODU4',
                'connection-type': 'infrastructure',
                'service-handler-header': {
                    'request-id': 'abcd12-efgh34'
                },
                'service-a-end': {
                    'service-format': 'ODU',
                    'odu-service-rate':
                    'org-openroadm-otn-common-types:ODU4',
                    'clli': 'nodeSA',
                    'node-id': 'SPDR-SA1'

                },
                'service-z-end': {
                    'service-format': 'ODU',
                    'odu-service-rate':
                    'org-openroadm-otn-common-types:ODU4',
                    'clli': 'nodeSC',
                    'node-id': 'SPDR-SC1'
                },
                'path-description': {
                    'aToZ-direction': {
                        'rate': 100,
                        'aToZ': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': ''
                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': ''
                                }
                            }
                        ]
                    },
                    'zToA-direction': {
                        'rate': '100',
                        'zToA': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': ''
                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': ''
                                }
                            }
                        ]
                    }
                }
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Operation Successful',
                      response['output']['configuration-response-common']['response-message'])

    # Test ODU4 interfaces on SPDR-A1 and SPDR-C1
    def test_09_check_interface_ODU4(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_10_check_interface_ODU4(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SC1", "interface", "XPDR1-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # Test creation of 10G service
    def test_11_service_create_10GE(self):
        response = test_utils_rfc8040.device_renderer_service_implementation_request(
            {
                'service-name': 'SPDRA-SPDRC-10G',
                'connection-type': 'service',
                'service-handler-header': {
                    'request-id': 'abcd12-efgh34'
                },
                'service-a-end': {
                    'service-format': 'Ethernet',
                    'service-rate': '10',
                    'clli': 'nodeSA',
                    'node-id': 'SPDR-SA1'
                },
                'service-z-end': {
                    'service-format': 'Ethernet',
                    'service-rate': '10',
                    'clli': 'nodeSC',
                    'node-id': 'SPDR-SC1'
                },
                'path-description': {
                    'aToZ-direction': {
                        'rate': 10,
                        'min-trib-slot': '1.1',
                        'max-trib-slot': '1.8',
                        'aToZ': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-CLIENT1'

                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-CLIENT1'
                                }
                            }
                        ]
                    },
                    'zToA-direction': {
                        'rate': '10',
                        'min-trib-slot': '1.1',
                        'max-trib-slot': '1.8',
                        'zToA': [
                            {
                                'id': '0',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-CLIENT1'
                                }
                            },
                            {
                                'id': '1',
                                'resource': {
                                    'tp-node-id': 'SPDR-SC1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '2',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-NETWORK1'
                                }
                            },
                            {
                                'id': '3',
                                'resource': {
                                    'tp-node-id': 'SPDR-SA1-XPDR1',
                                    'tp-id': 'XPDR1-CLIENT1'

                                }
                            }
                        ]
                    }
                }
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Operation Successful',
                      response['output']['configuration-response-common']['response-message'])

    # Test the interfaces on SPDR-A1
    def test_12_check_interface_10GE_CLIENT(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'], 10000)

    def test_13_check_interface_ODU2E_CLIENT(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '03', 'exp-payload-type': '03'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_ODU2E_connection(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SA1",
            "odu-connection", "XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
            'direction': 'bidirectional'
        }
        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G'},
                             response['odu-connection'][0]['source'])

    def test_15_check_interface_ODU2E_NETWORK(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    # Test the interfaces on SPDR-C1
    def test_16_check_interface_ODU2E_NETWORK(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SC1", "interface", "XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_17_check_interface_10GE_CLIENT(self):
        response = test_utils_rfc8040.check_node_attribute_request("SPDR-SC1", "interface", "XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'], 10000)

    def test_18_check_interface_ODU2E_CLIENT(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SC1", "interface", "XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '03', 'exp-payload-type': '03'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_19_check_ODU2E_connection(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SC1",
            "odu-connection", "XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G',
            'direction': 'bidirectional'
        }
        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G'},
                             response['odu-connection'][0]['source'])

    def test_20_check_interface_ODU2E_NETWORK(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "SPDR-SC1", "interface", "XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    # TODO: Delete the services (OTU, ODU, LO-ODU)
    # TODO: Delete interfaces (SPDR-A1, SPDR-C1)

    def test_21_disconnect_SPDR_SA1(self):
        response = test_utils_rfc8040.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_22_disconnect_SPDR_SC1(self):
        response = test_utils_rfc8040.unmount_device("SPDR-SC1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
