#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest
import logging
import test_utils


class TransportPCEtesting(unittest.TestCase):

    honeynode_process1 = None
    honeynode_process2 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def setUpClass(cls):
        cls.honeynode_process1 = test_utils.start_sim('spdrav1')
        time.sleep(30)
        cls.honeynode_process2 = test_utils.start_sim('spdrav2')
        time.sleep(30)
        cls.odl_process = test_utils.start_tpce()
        time.sleep(60)
        print("opendaylight started")

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

        for child in psutil.Process(cls.honeynode_process1.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process1.send_signal(signal.SIGINT)
        cls.honeynode_process1.wait()

        for child in psutil.Process(cls.honeynode_process2.pid).children():
          child.send_signal(signal.SIGINT)
          child.wait()
        cls.honeynode_process2.send_signal(signal.SIGINT)
        cls.honeynode_process2.wait()

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "SPDR-SA1",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17845",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(10)
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_connect_SPDR_SC1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SC1"
               .format(self.restconf_baseurl))
        data = {"node": [{
          "node-id": "SPDR-SC1",
          "netconf-node-topology:username": "admin",
          "netconf-node-topology:password": "admin",
          "netconf-node-topology:host": "127.0.0.1",
          "netconf-node-topology:port": "17846",
          "netconf-node-topology:tcp-only": "false",
          "netconf-node-topology:pass-through": {}}]}

        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(10)
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SC1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_03_service_create_OTU4(self):
        url = "{}/operations/transportpce-renderer:service-" \
              "implementation-request".format(self.restconf_baseurl)
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
                        "tp-id": "XPDR1-NETWORK1"
                      }
                    },
                    {
                      "id": "1",
                      "resource": {
                        "tp-node-id": "SPDR-SC1-XPDR1",
                        "tp-id": "XPDR1-NETWORK1"
                      }
                    }
                  ]
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
                        "tp-id": "XPDR1-NETWORK1"
                      }
                    },
                    {
                      "id": "1",
                      "resource": {
                        "tp-node-id": "SPDR-SA1-XPDR1",
                        "tp-id": "XPDR1-NETWORK1"
                      }
                    }
                  ]
                }
              }
            }
          }
        response = test_utils.post_request(url, data, 'admin', 'admin')
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]["response-message"])

    # TODO: Test OCH-OTU interfaces on SPDR-A1
    def test_04_check_interface_och(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        # assertDictContainsSubset is deprecated
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   } ,**res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_05_check_interface_OTU(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': '-768660269',
                         'expected-sapi': '-768660269',
                         'tx-sapi': '819687633',
                         'expected-dapi': '819687633',
                         'rate': 'org-openroadm-otn-common-types:OTU4',
                         'fec': 'scfec'
                       }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    # TODO: Test OCH-OTU interfaces on SPDR-C1
    def test_06_check_interface_och(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        # assertDictContainsSubset is deprecated
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   } ,**res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_07_check_interface_OTU(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON, auth=('admin',
                                                                        'admin')
        )
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': '819687633',
                        'expected-sapi': '819687633',
                        'tx-sapi': '-768660269',
                        'expected-dapi': '-768660269',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    # TODO: Test creation of ODU4 service
    def test_08_service_create_OTU4(self):
        url = "{}/operations/transportpce-renderer:service-implementation-request"\
          .format(self.restconf_baseurl)
        data = {
          "transportpce-renderer:input": {
            "transportpce-renderer:service-name": "SPDRA-SPDRC-OTU4-ODU4",
            "transportpce-renderer:connection-type": "infrastructure",
            "transportpce-renderer:service-handler-header": {
              "transportpce-renderer:request-id": "abcd12-efgh34"
            },
            "transportpce-renderer:service-a-end": {
              "transportpce-renderer:service-format": "ODU",
              "transportpce-renderer:odu-service-rate": "org-openroadm-otn-common-types:ODU4",
              "transportpce-renderer:clli": "nodeSA",
              "transportpce-renderer:node-id": "SPDR-SA1"

            },
            "transportpce-renderer:service-z-end": {
              "transportpce-renderer:service-format": "ODU",
              "transportpce-renderer:odu-service-rate": "org-openroadm-otn-common-types:ODU4",
              "transportpce-renderer:clli": "nodeSC",
              "transportpce-renderer:node-id": "SPDR-SC1"
            },
            "transportpce-renderer:path-description": {
              "aToZ-direction": {
                "rate": 100,
                "aToZ": [
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
                  }
                ]
              },
              "transportpce-renderer:zToA-direction": {
                "transportpce-renderer:rate": "100",
                "zToA": [
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
                  }
                ]
              }
            }
          }
        }
        response = test_utils.post_request(url, data, 'admin', 'admin')
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]
                      ["response-message"])

    # TODO: Test ODU4 interfaces on SPDR-A1 and SPDR-C1
    def test_09_check_interface_ODU4(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU4"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # TODO: This will change, when SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                                  ),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_09_check_interface_ODU4(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU4"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # TODO: This will change, when SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                                  ),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # TODO: Test creation of 10G service
    def test_10_service_create_10GE(self):
        url = "{}/operations/transportpce-renderer:service-implementation-request" \
          .format(self.restconf_baseurl)
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
              "transportpce-renderer:rate": "10",
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

        response = test_utils.post_request(url, data, 'admin', 'admin')
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Operation Successful',
                      res["output"]["configuration-response-common"]
                      ["response-message"])


    # TODO: Test the interfaces on SPDR-A1
    def test_11_check_interface_10GE_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    def test_12_check_interface_ODU2E_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        print(res)
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

    def test_17_check_ODU2E_connection(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "odu-connection/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    # TODO: Test the interfaces on SPDR-C1
    def test_13_check_interface_ODU2E_NETWORK(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    def test_14_check_interface_10GE_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    def test_15_check_interface_ODU2E_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    def test_16_check_interface_ODU2E_NETWORK(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
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

    def test_17_check_ODU2E_connection(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SC1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "odu-connection/XPDR1-CLIENT1-ODU2e-SPDRA-SPDRC-10G-x-XPDR1-NETWORK1-ODU2e-SPDRA-SPDRC-10G"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=('admin', 'admin'))
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

    # TODO: Delete the services



if __name__ == "__main__":
    unittest.main(verbosity=2)
