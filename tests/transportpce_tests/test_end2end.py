#!/usr/bin/env python
##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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


class TransportPCEFulltesting(unittest.TestCase):

    odl_process = None
    honeynode_process1 = None
    honeynode_process2 = None
    honeynode_process3 = None
    honeynode_process4 = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def __start_honeynode1(cls):
        executable = ("./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode1.log', 'w') as outfile:
                cls.honeynode_process1 = subprocess.Popen(
                    [executable, "17830", "sample_configs/ord_2.1/oper-ROADMA-full.xml"],
                    stdout=outfile)

    @classmethod
    def __start_honeynode2(cls):
        executable = ("./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode2.log', 'w') as outfile:
                cls.honeynode_process2 = subprocess.Popen(
                    [executable, "17831", "sample_configs/ord_2.1/oper-XPDRA.xml"],
                    stdout=outfile)

    @classmethod
    def __start_honeynode3(cls):
        executable = ("./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode3.log', 'w') as outfile:
                cls.honeynode_process3 = subprocess.Popen(
                    [executable, "17833", "sample_configs/ord_2.1/oper-ROADMC-full.xml"],
                    stdout=outfile)

    @classmethod
    def __start_honeynode4(cls):
        executable = ("./honeynode/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode4.log', 'w') as outfile:
                cls.honeynode_process4 = subprocess.Popen(
                    [executable, "17834", "sample_configs/ord_2.1/oper-XPDRC.xml"],
                    stdout=outfile)

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        print ("starting honeynode1")
        cls.__start_honeynode1()
        time.sleep(40)
        print ("starting honeynode2")
        cls.__start_honeynode2()
        time.sleep(40)
        print ("starting honeynode3")
        cls.__start_honeynode3()
        time.sleep(40)
        print ("starting honeynode4")
        cls.__start_honeynode4()
        time.sleep(40)
        print ("starting opendaylight")
        cls.__start_odl()
        time.sleep(80)

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
        for child in psutil.Process(cls.honeynode_process3.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process3.send_signal(signal.SIGINT)
        cls.honeynode_process3.wait()
        for child in psutil.Process(cls.honeynode_process4.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process4.send_signal(signal.SIGINT)
        cls.honeynode_process4.wait()
        print ("all processes killed")

    def setUp(self):  # instruction executed before each test method
        print ("execution of {}".format(self.id().split(".")[-1]))

#  connect netconf devices
    def test_01_connect_xpdrA(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRA"
              .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "XPDRA",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17831",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_02_connect_xpdrC(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRC"
              .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "XPDRC",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17834",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_03_connect_rdmA(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "ROADMA",
             "netconf-node-topology:username": "admin",
             "netconf-node-topology:password": "admin",
             "netconf-node-topology:host": "127.0.0.1",
             "netconf-node-topology:port": "17830",
             "netconf-node-topology:tcp-only": "false",
             "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
              auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_04_connect_rdmC(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMC"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "ROADMC",
             "netconf-node-topology:username": "admin",
             "netconf-node-topology:password": "admin",
             "netconf-node-topology:host": "127.0.0.1",
             "netconf-node-topology:port": "17833",
             "netconf-node-topology:tcp-only": "false",
             "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
              auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_05_connect_xprdA_N1_to_roadmA_PP1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRA",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "1",
                    "networkutils:rdm-node": "ROADMA",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP1-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_06_connect_xprdA_N2_to_roadmA_PP2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRA",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "2",
                    "networkutils:rdm-node": "ROADMA",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP2-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_07_connect_roadmA_PP1_to_xpdrA_N1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRA",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "1",
                    "networkutils:rdm-node": "ROADMA",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP1-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmA_PP2_to_xpdrA_N2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRA",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "2",
                    "networkutils:rdm-node": "ROADMA",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP2-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_09_connect_xprdC_N1_to_roadmC_PP1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRC",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "1",
                    "networkutils:rdm-node": "ROADMC",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP1-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_10_connect_xprdC_N2_to_roadmC_PP2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRC",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "2",
                    "networkutils:rdm-node": "ROADMC",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP2-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_11_connect_roadmC_PP1_to_xpdrC_N1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRC",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "1",
                    "networkutils:rdm-node": "ROADMC",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP1-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_12_connect_roadmC_PP2_to_xpdrC_N2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(self.restconf_baseurl)
        data = {
            "networkutils:input": {
                "networkutils:links-input": {
                    "networkutils:xpdr-node": "XPDRC",
                    "networkutils:xpdr-num": "1",
                    "networkutils:network-num": "2",
                    "networkutils:rdm-node": "ROADMC",
                    "networkutils:srg-num": "1",
                    "networkutils:termination-point-num": "SRG1-PP2-TXRX"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "POST", url, data=json.dumps(data),
             headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

#test service-create for Eth service from xpdr to xpdr
    def test_13_create_eth_service1(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "service1",
                "common-id": "ASATT1234567",
                "connection-type": "service",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service rendered successfully !',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(10)

    def test_14_get_eth_service1(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/service1"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_15_check_xc1_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'SRG1-PP1-TXRX-DEG1-TTP-TXRX-1',
              'wavelength-number': 1,
              'opticalControlMode': 'gainLoss',
              'target-output-power': -3.0},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'SRG1-PP1-TXRX-1'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'DEG1-TTP-TXRX-1'},
             res['roadm-connections'][0]['destination'])
        time.sleep(5)

    def test_16_check_xc1_ROADMC(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMC/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-1',
              'wavelength-number': 1,
              'opticalControlMode': 'gainLoss',
              'target-output-power': 2.0},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'SRG1-PP1-TXRX-1'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'DEG2-TTP-TXRX-1'},
             res['roadm-connections'][0]['destination'])
        time.sleep(5)

    def test_17_check_topo_XPDRA(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/XPDRA-XPDR1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-CLIENT1':
                self.assertEqual({u'index': 1}, ele['org-openroadm-network-topology:xpdr-client-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual({u'index': 1}, ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-CLIENT2':
                self.assertNotIn('wavelength', dict.keys(ele['org-openroadm-network-topology:xpdr-client-attributes']))
            if ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertNotIn('wavelength', dict.keys(ele['org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(3)

    def test_18_check_topo_ROADMA_SRG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-SRG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('used-wavelength', dict.keys(ele))
        time.sleep(3)

    def test_19_check_topo_ROADMA_DEG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-DEG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:ctp-attributes']['used-wavelengths'])
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:tx-ttp-attributes']['used-wavelengths'])
        time.sleep(3)

    def test_20_create_eth_service2(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "service2",
                "common-id": "ASATT1234567",
                "connection-type": "service",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service rendered successfully !',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(10)

    def test_21_get_eth_service2(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/service2"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service2')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_22_check_xc2_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/DEG1-TTP-TXRX-SRG1-PP2-TXRX-2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'DEG1-TTP-TXRX-SRG1-PP2-TXRX-2',
              'wavelength-number': 2,
              'opticalControlMode': 'power'},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'DEG1-TTP-TXRX-2'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'SRG1-PP2-TXRX-2'},
             res['roadm-connections'][0]['destination'])

    def test_23_check_topo_XPDRA(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/XPDRA-XPDR1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-CLIENT1':
                self.assertEqual({u'index': 1}, ele['org-openroadm-network-topology:xpdr-client-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual({u'index': 1}, ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-CLIENT2':
                self.assertEqual({u'index': 2}, ele['org-openroadm-network-topology:xpdr-client-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertEqual({u'index': 2}, ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
        time.sleep(10)

    def test_24_check_topo_ROADMA_SRG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-SRG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        self.assertNotIn({u'index': 2}, res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
                self.assertNotIn({u'index': 2}, ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertIn({u'index': 2}, ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
                self.assertNotIn({u'index': 1}, ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
            if ele['tp-id'] == 'SRG1-PP3-TXRX':
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(10)


    def test_25_check_topo_ROADMA_DEG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-DEG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        self.assertNotIn({u'index': 2}, res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:ctp-attributes']['used-wavelengths'])
                self.assertIn({u'index': 2}, ele['org-openroadm-network-topology:ctp-attributes']['used-wavelengths'])
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                self.assertIn({u'index': 1}, ele['org-openroadm-network-topology:tx-ttp-attributes']['used-wavelengths'])
                self.assertIn({u'index': 2}, ele['org-openroadm-network-topology:tx-ttp-attributes']['used-wavelengths'])
        time.sleep(10)

#     creation service test on a non-available resource
    def test_26_create_eth_service3(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "service3",
                "common-id": "ASATT1234567",
                "connection-type": "service",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "XPDRA",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "XPDRC",
                    "service-format": "Ethernet",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('No path available',
            res['output']['configuration-response-common']['response-message'])
        self.assertIn('500', res['output']['configuration-response-common']['response-code'])
        time.sleep(10)

# add a test that check the openroadm-service-list still only contains 2 elements

    def test_27_delete_eth_service3(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service3",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service \'service3\' does not exist in datastore',
            res['output']['configuration-response-common']['response-message'])
        self.assertIn('500', res['output']['configuration-response-common']['response-code'])
        time.sleep(1)

    def test_28_delete_eth_service1(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service1",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service delete was successful!',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(1)

    def test_29_delete_eth_service2(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service2",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service delete was successful!',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(1)

    def test_30_check_no_xc_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url, auth=('admin', 'admin'))
        res = response.json()
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertNotIn('roadm-connections', dict.keys(res['org-openroadm-device']))
        time.sleep(2)

    def test_31_check_topo_XPDRA(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/XPDRA-XPDR1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele[u'org-openroadm-network-topology:tp-type'] == 'XPONDER-CLIENT':
                self.assertNotIn('wavelength', dict.keys(ele['org-openroadm-network-topology:xpdr-client-attributes']))
            elif ele[u'org-openroadm-network-topology:tp-type'] == 'XPONDER-NETWORK':
                self.assertNotIn('wavelength', dict.keys(ele['org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(10)

    def test_32_check_topo_ROADMA_SRG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-SRG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        self.assertIn({u'index': 2}, res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                self.assertEqual({}, ele['org-openroadm-network-topology:pp-attributes'])
            elif ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertEqual({}, ele['org-openroadm-network-topology:pp-attributes'])
            else:
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(10)

    def test_33_check_topo_ROADMA_DEG1(self):
        url1 = ("{}/config/ietf-network:network/openroadm-topology/node/ROADMA-DEG1"
               .format(self.restconf_baseurl))
        response = requests.request(
             "GET", url1, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({u'index': 1}, res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        self.assertIn({u'index': 2}, res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                self.assertEqual({}, ele['org-openroadm-network-topology:ctp-attributes'])
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                self.assertEqual({}, ele['org-openroadm-network-topology:tx-ttp-attributes'])
        time.sleep(10)


# test service-create for Optical Channel (OC) service from srg-pp to srg-pp
    def test_34_create_oc_service1(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "service1",
                "common-id": "ASATT1234567",
                "connection-type": "roadm-line",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "ROADMA",
                    "service-format": "OC",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "ROADMC",
                    "service-format": "OC",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service rendered successfully !',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(10)

    def test_35_get_oc_service1(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/service1"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_36_check_xc1_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'SRG1-PP1-TXRX-DEG1-TTP-TXRX-1',
              'wavelength-number': 1,
              'opticalControlMode': 'gainLoss',
              'target-output-power': -3.0},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'SRG1-PP1-TXRX-1'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'DEG1-TTP-TXRX-1'},
             res['roadm-connections'][0]['destination'])
        time.sleep(7)

    def test_37_check_xc1_ROADMC(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMC/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-1',
              'wavelength-number': 1,
              'opticalControlMode': 'gainLoss',
              'target-output-power': 2.0},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'SRG1-PP1-TXRX-1'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'DEG2-TTP-TXRX-1'},
             res['roadm-connections'][0]['destination'])
        time.sleep(7)

    def test_38_create_oc_service2(self):
        url = ("{}/operations/org-openroadm-service:service-create"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-create",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-name": "service2",
                "common-id": "ASATT1234567",
                "connection-type": "roadm-line",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "ROADMA",
                    "service-format": "OC",
                    "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.3",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                            "lgx-port-name": "LGX Back.4",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "node-id": "ROADMC",
                    "service-format": "OC",
                    "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "rx-direction": {
                        "port": {
                            "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                            "port-type": "router",
                            "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.30",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        }
                    },
                    "optic-type": "gray"
                },
                "due-date": "2016-11-28T00:00:01Z",
                "operator-contact": "pw1234"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service rendered successfully !',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(10)

    def test_39_get_oc_service2(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/service2"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service2')
        self.assertEqual(
            res['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_40_check_xc2_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP2-TXRX-DEG1-TTP-TXRX-2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
             {'connection-number': 'SRG1-PP2-TXRX-DEG1-TTP-TXRX-2',
              'wavelength-number': 2,
              'opticalControlMode': 'gainLoss',
              'target-output-power': -3.0},
             res['roadm-connections'][0])
        self.assertDictEqual(
             {'src-if': 'SRG1-PP2-TXRX-2'},
             res['roadm-connections'][0]['source'])
        self.assertDictEqual(
             {'dst-if': 'DEG1-TTP-TXRX-2'},
             res['roadm-connections'][0]['destination'])
        time.sleep(2)

    def test_41_check_topo_ROADMA(self):
        self.test_24_check_topo_ROADMA_SRG1()
        self.test_25_check_topo_ROADMA_DEG1()
        time.sleep(3)


    def test_42_delete_oc_service1(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service1",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service delete was successful!',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(7)

    def test_43_delete_oc_service2(self):
        url = ("{}/operations/org-openroadm-service:service-delete"
              .format(self.restconf_baseurl))
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service2",
                    "tail-retention": "no"
                }
            }
        }
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service delete was successful!',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(1)

    def test_44_get_no_oc_services(self):
        print ("start test")
        url = ("{}/operational/org-openroadm-service:service-list"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual({u'service-list': {}}, res)
        time.sleep(1)

    def test_45_get_no_xc_ROADMA(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf"
               "/node/ROADMA/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn(['roadm-connections'][0], res['org-openroadm-device'])
        time.sleep(1)

    def test_46_check_topo_ROADMA(self):
        self.test_32_check_topo_ROADMA_SRG1()
        self.test_33_check_topo_ROADMA_DEG1()

    def test_47_loop_create_eth_service(self):
        for i in range(1,6):
            print ("trial number {}".format(i))
            print("eth service creation")
            self.test_13_create_eth_service1()
            print ("check xc in ROADMA")
            self.test_15_check_xc1_ROADMA()
            print ("check xc in ROADMC")
            self.test_16_check_xc1_ROADMC()
            print ("eth service deletion\n")
            self.test_28_delete_eth_service1()

    def test_48_loop_create_oc_service(self):
        url = ("{}/operational/org-openroadm-service:service-list/services/service1"
               .format(self.restconf_baseurl))
        response = requests.request("GET", url, auth=('admin', 'admin'))
        if response.status_code != 404:
            url = ("{}/operations/org-openroadm-service:service-delete"
                   .format(self.restconf_baseurl))
            data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
            },
                "service-delete-req-info": {
                    "service-name": "service1",
                    "tail-retention": "no"
            }
                }
            }
            headers = {'content-type': 'application/json'}
            requests.request("POST", url, data=json.dumps(data), headers=headers, auth=('admin', 'admin'))
            time.sleep(5)

        for i in range(1,6):
            print ("trial number {}".format(i))
            print("oc service creation")
            self.test_34_create_oc_service1()
            print ("check xc in ROADMA")
            self.test_36_check_xc1_ROADMA()
            print ("check xc in ROADMC")
            self.test_37_check_xc1_ROADMC()
            print ("oc service deletion\n")
            self.test_42_delete_oc_service1()


    def test_49_disconnect_XPDRA(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/XPDRA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)

    def test_50_disconnect_XPDRC(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/XPDRC"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)

    def test_51_disconnect_ROADMA(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)

    def test_52_disconnect_ROADMC(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMC"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)


if __name__ == "__main__":
    unittest.main(verbosity=2)
