#!/usr/bin/env python

##############################################################################
#Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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

class TransportPCEtesting(unittest.TestCase):

    testtools_process1 = None
    testtools_process2 = None
    testtools_process3 = None
    testtools_process4 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def __start_testtools(cls):
        executable = ("./netconf/netconf/tools/netconf-testtool/target/"
                      "netconf-testtool-1.3.1-executable.jar")
        if os.path.isfile(executable):
            if not os.path.exists("transportpce_tests/log"):
                os.makedirs("transportpce_tests/log")
            if os.path.isfile("./transportpce_tests/log/response.log"):
                os.remove("transportpce_tests/log/response.log")
            with open('transportpce_tests/log/testtools_ROADMA.log', 'w') as outfile1:
                cls.testtools_process1 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/nodes_config/sample-config-ROADMA.xml","--starting-port","17831"],
                    stdout=outfile1)
            with open('transportpce_tests/log/testtools_ROADMB.log', 'w') as outfile2:
                cls.testtools_process2 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/nodes_config/sample-config-ROADMB.xml","--starting-port","17832"],
                    stdout=outfile2)
            with open('transportpce_tests/log/testtools_ROADMC.log', 'w') as outfile3:
                cls.testtools_process3 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/nodes_config/sample-config-ROADMC.xml","--starting-port","17833"],
                    stdout=outfile3)
            with open('transportpce_tests/log/testtools_XPDRA.log', 'w') as outfile4:
                cls.testtools_process4 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/nodes_config/sample-config-XPDRA.xml","--starting-port","17830"],
                    stdout=outfile4)

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('transportpce_tests/log/odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(60)

    @classmethod
    def tearDownClass(cls):
        cls.testtools_process1.send_signal(signal.SIGINT)
        cls.testtools_process1.wait()
        cls.testtools_process2.send_signal(signal.SIGINT)
        cls.testtools_process2.wait()
        cls.testtools_process3.send_signal(signal.SIGINT)
        cls.testtools_process3.wait()
        cls.testtools_process4.send_signal(signal.SIGINT)
        cls.testtools_process4.wait()
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()
        print('End of the tear down class')

    def setUp(self):
        time.sleep(10)

    def test_01_connect_ROADMA(self):
        #Config ROADMA
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "ROADMA",
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
        time.sleep(30)

    def test_02_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'],'NodeA')

    def test_03_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMA')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'],'clli-network')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'],'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:node-type'],'ROADM')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'],'2')

    def test_04_getLinks_OpenroadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Tests related to links
        self.assertEqual(len(res['network'][0]['ietf-network-topology:link']),6)
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        time.sleep(1)

    def test_05_getNodes_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        #Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
            outfile1.write(str(len(res['network'][0]['node'])))
        self.assertEqual(len(res['network'][0]['node']),3)
        #Tests related to nodes
        #Test related to SRG1
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMA-SRG1')
        #failed tests
        self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
        self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][0]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network-topology:node-type'],'SRG')
        #Test related to DEG2
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'ROADMA-DEG2')
        self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][1]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network-topology:node-type'],'DEGREE')
        #Test related to DEG1
        self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMA-DEG1')
        self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][2]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network-topology:node-type'],'DEGREE')

    def test_06_connect_XPDRA(self):
         url = ("{}/config/network-topology:"
                 "network-topology/topology/topology-netconf/node/XPDRA"
                .format(self.restconf_baseurl))
         data = {"node": [{
              "node-id": "XPDRA",
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
         time.sleep(30)

    def test_07_getClliNetwork(self):
         url = ("{}/config/ietf-network:network/clli-network"
               .format(self.restconf_baseurl))
         headers = {'content-type': 'application/json'}
         response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
         self.assertEqual(response.status_code, requests.codes.ok)
         res = response.json()
         self.assertEqual(res['network'][0]['node'][0]['node-id'],'NodeA')
         self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'],'NodeA')

    def test_08_getOpenRoadmNetwork(self):
         url = ("{}/config/ietf-network:network/openroadm-network"
               .format(self.restconf_baseurl))
         headers = {'content-type': 'application/json'}
         response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
         self.assertEqual(response.status_code, requests.codes.ok)
         res = response.json()
         nbNode=len(res['network'][0]['node'])
         self.assertEqual(nbNode,2)
         self.assertEqual(res['network'][0]['node'][0]['node-id'],'XPDRA')
         self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'],'clli-network')
         self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'],'NodeA')
         self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:node-type'],'XPONDER')
         self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'],'1')

    def test_09_getNodes_OpenRoadmTopology(self):
         url = ("{}/config/ietf-network:network/openroadm-topology"
               .format(self.restconf_baseurl))
         headers = {'content-type': 'application/json'}
         response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
         res = response.json()
         #Tests related to nodes
         self.assertEqual(response.status_code, requests.codes.ok)
         with open('./transportpce_tests/log/response.log', 'a') as outfile1:
             outfile1.write(str(len(res['network'][0]['node'])))
         nbNode=len(res['network'][0]['node'])
         self.assertEqual(nbNode,4)
         #Tests related to XPDRA nodes
         self.assertEqual(res['network'][0]['node'][1]['node-id'],'XPDRA-XPDR1')
         self.assertEqual(len(res['network'][0]['node'][1]['ietf-network-topology:termination-point']),2)
         self.assertEqual({'tp-id': 'XPDR1-CLIENT1', 'org-openroadm-network-topology:tp-type': 'XPONDER-CLIENT',
                             'org-openroadm-network-topology:xpdr-network-attributes': {
                             'tail-equipment-id': 'XPDR1-NETWORK1'}},
                        res['network'][0]['node'][1]['ietf-network-topology:termination-point'][0])
         self.assertEqual({'tp-id': 'XPDR1-NETWORK1', 'org-openroadm-network-topology:tp-type': 'XPONDER-NETWORK',
                        'org-openroadm-network-topology:xpdr-client-attributes': {'tail-equipment-id': 'XPDR1-CLIENT1'}},
                        res['network'][0]['node'][1]['ietf-network-topology:termination-point'][1])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
             res['network'][0]['node'][1]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network-topology:node-type'],'XPONDER')
         #Tests related to ROADMA nodes
         #Test related to SRG1
         self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMA-SRG1')
         #To be integrate in the effective tests
         self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
         self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
             res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][0]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network-topology:node-type'],'SRG')
         #Test related to DEG2
         self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMA-DEG2')
         self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][2]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network-topology:node-type'],'DEGREE')
         #Test related to DEG1
         self.assertEqual(res['network'][0]['node'][3]['node-id'],'ROADMA-DEG1')
         self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][3]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][3]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][3]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][3]['org-openroadm-network-topology:node-type'],'DEGREE')

    #Connect the tail XPDRA to ROADMA and vice versa
    def test_10_connect_tail_xpdr_rdm(self):
         #Connect the tail: XPDRA to ROADMA
        url = ("{}/operations/networkutils:init-xpdr-rdm-links"
                .format(self.restconf_baseurl))
        data = {"networkutils:input": {
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
              "POST", url, data=json.dumps(data), headers=headers,
              auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)

    def test_11_connect_tail_rdm_xpdr(self):
         #Connect the tail: ROADMA to XPDRA
         url = ("{}/operations/networkutils:init-rdm-xpdr-links"
                 .format(self.restconf_baseurl))
         data = {"networkutils:input": {
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
               "POST", url, data=json.dumps(data), headers=headers,
               auth=('admin', 'admin'))
         self.assertEqual(response.status_code, requests.codes.ok)
         time.sleep(10)

    def test_12_getLinks_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Tests related to links
        self.assertEqual(len(res['network'][0]['ietf-network-topology:link']),8)
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['link-id'],'XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['org-openroadm-network-topology:link-type'],'XPONDER-OUTPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['link-id'],'ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['org-openroadm-network-topology:link-type'],'XPONDER-INPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['org-openroadm-network-topology:link-type'],'ADD-LINK')

    def test_13_connect_ROADMC(self):
        #Config ROADMC
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
        time.sleep(30)

    def test_14_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'NodeC')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'],'NodeC')
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'NodeA')
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-clli-network:clli'],'NodeA')

    def test_15_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,3)
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'XPDRA')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'],'clli-network')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'],'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:node-type'],'XPONDER')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'],'1')
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'ROADMA')
        self.assertEqual(res['network'][0]['node'][1]['supporting-node'][0]['network-ref'],'clli-network')
        self.assertEqual(res['network'][0]['node'][1]['supporting-node'][0]['node-ref'],'NodeA')
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network:node-type'],'ROADM')
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network:model'],'2')
        self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMC')
        self.assertEqual(res['network'][0]['node'][2]['supporting-node'][0]['network-ref'],'clli-network')
        self.assertEqual(res['network'][0]['node'][2]['supporting-node'][0]['node-ref'],'NodeC')
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network:node-type'],'ROADM')
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network:model'],'2')

    def test_16_getROADMLinkOpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Tests related to links
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,16)
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['link-id'],'XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['org-openroadm-network-topology:link-type'],'XPONDER-OUTPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['link-id'],'ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][8]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][8]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][9]['link-id'],'ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][9]['org-openroadm-network-topology:link-type'],'ROADM-TO-ROADM')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][10]['link-id'],'ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][10]['org-openroadm-network-topology:link-type'],'XPONDER-INPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][11]['link-id'],'ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][11]['org-openroadm-network-topology:link-type'],'ROADM-TO-ROADM')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][12]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][12]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][13]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][13]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][14]['link-id'],'ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][14]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][15]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][15]['org-openroadm-network-topology:link-type'],'ADD-LINK')

    def test_17_getNodes_OpenRoadmTopology(self):
         url = ("{}/config/ietf-network:network/openroadm-topology"
               .format(self.restconf_baseurl))
         headers = {'content-type': 'application/json'}
         response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
         res = response.json()
         #Tests related to nodes
         self.assertEqual(response.status_code, requests.codes.ok)
         nbNode=len(res['network'][0]['node'])
         self.assertEqual(nbNode,7)
         #************************Tests related to XPDRA nodes
         self.assertEqual(res['network'][0]['node'][4]['node-id'],'XPDRA-XPDR1')
         self.assertEqual(len(res['network'][0]['node'][4]['ietf-network-topology:termination-point']),2)
         self.assertEqual({'tp-id': 'XPDR1-CLIENT1', 'org-openroadm-network-topology:tp-type': 'XPONDER-CLIENT',
                             'org-openroadm-network-topology:xpdr-network-attributes': {
                             'tail-equipment-id': 'XPDR1-NETWORK1'}},
                        res['network'][0]['node'][4]['ietf-network-topology:termination-point'][0])
         self.assertEqual({'tp-id': 'XPDR1-NETWORK1', 'org-openroadm-network-topology:tp-type': 'XPONDER-NETWORK',
                        'org-openroadm-network-topology:xpdr-client-attributes': {'tail-equipment-id': 'XPDR1-CLIENT1'}},
                        res['network'][0]['node'][4]['ietf-network-topology:termination-point'][1])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
             res['network'][0]['node'][4]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][4]['org-openroadm-network-topology:node-type'],'XPONDER')
         #************************Tests related to ROADMA nodes
         #Test related to SRG1
         self.assertEqual(res['network'][0]['node'][3]['node-id'],'ROADMA-SRG1')
         #To be integrate in the effective tests
         self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
         self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
             res['network'][0]['node'][3]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][3]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][3]['org-openroadm-network-topology:node-type'],'SRG')
         #Test related to DEG2
         self.assertEqual(res['network'][0]['node'][5]['node-id'],'ROADMA-DEG2')
         self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][5]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][5]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][5]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][5]['org-openroadm-network-topology:node-type'],'DEGREE')
         #Test related to DEG1
         self.assertEqual(res['network'][0]['node'][6]['node-id'],'ROADMA-DEG1')
         self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][6]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][6]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
             res['network'][0]['node'][6]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][6]['org-openroadm-network-topology:node-type'],'DEGREE')
         #************************Tests related to ROADMC nodes
         #Test related to SRG1
         self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMC-SRG1')
         #To be integrate in the effective tests
         self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
         self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
             res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
             res['network'][0]['node'][0]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network-topology:node-type'],'SRG')
         #Test related to DEG1
         self.assertEqual(res['network'][0]['node'][1]['node-id'],'ROADMC-DEG1')
         self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
             res['network'][0]['node'][1]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network-topology:node-type'],'DEGREE')
         #Test related to DEG2
         self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMC-DEG2')
         self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
             res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
         self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
             res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
         self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
             res['network'][0]['node'][2]['supporting-node'])
         self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network-topology:node-type'],'DEGREE')

    def test_18_connect_ROADMB(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMB"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "ROADMB",
             "netconf-node-topology:username": "admin",
             "netconf-node-topology:password": "admin",
             "netconf-node-topology:host": "127.0.0.1",
             "netconf-node-topology:port": "17832",
             "netconf-node-topology:tcp-only": "false",
             "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(30)

    def test_19_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'NodeC')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'],'NodeC')
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'NodeA')
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-clli-network:clli'],'NodeA')
        self.assertEqual(res['network'][0]['node'][2]['node-id'],'NodeB')
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-clli-network:clli'],'NodeB')

    def test_20_verifyDegree(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Tests related to links
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        listR2RLink=['ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX','ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX',
           'ROADMA-DEG2-DEG2-TTP-TXRXtoROADMB-DEG1-DEG1-TTP-TXRX','ROADMC-DEG1-DEG1-TTP-TXRXtoROADMB-DEG2-DEG2-TTP-TXRX',
           'ROADMB-DEG1-DEG1-TTP-TXRXtoROADMA-DEG2-DEG2-TTP-TXRX','ROADMB-DEG2-DEG2-TTP-TXRXtoROADMC-DEG1-DEG1-TTP-TXRX']
        for i in range(0,nbLink):
            if res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'] == 'ROADM-TO-ROADM':
                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
                find= link_id in listR2RLink
                self.assertEqual(find, True)
                listR2RLink.remove(link_id)
        self.assertEqual(len(listR2RLink),0)

    def test_21_verifyOppositeLinkTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Write the response in the log
        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
            outfile1.write(str(res))
        #Tests related to links
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,26)
        for i in range(0,nbLink):
            link_id=res['network'][0]['ietf-network-topology:link'][i]['link-id']
            link_type=res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']
            link_src=res['network'][0]['ietf-network-topology:link'][i]['source']['source-node']
            link_dest=res['network'][0]['ietf-network-topology:link'][i]['destination']['dest-node']
            oppLink_id=res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-opposite-links:opposite-link']
            #Find the opposite link
            url_oppLink="{}/config/ietf-network:network/openroadm-topology/ietf-network-topology:link/"+oppLink_id
            url = (url_oppLink.format(self.restconf_baseurl))
            headers = {'content-type': 'application/json'}
            response_oppLink = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
            self.assertEqual(response_oppLink.status_code, requests.codes.ok)
            res_oppLink = response_oppLink.json()
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['org-openroadm-opposite-links:opposite-link'],link_id)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['source']['source-node'],link_dest)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['destination']['dest-node'],link_src)
            oppLink_type=res_oppLink['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type']
            if link_type=='ADD-LINK':
                self.assertEqual(oppLink_type, 'DROP-LINK')
            elif link_type=='DROP-LINK':
                self.assertEqual(oppLink_type, 'ADD-LINK')
            elif link_type=='EXPRESS-LINK':
                self.assertEqual(oppLink_type, 'EXPRESS-LINK')
            elif link_type=='ROADM-TO-ROADM':
                self.assertEqual(oppLink_type, 'ROADM-TO-ROADM')
            elif link_type=='XPONDER-INPUT':
                self.assertEqual(oppLink_type, 'XPONDER-OUTPUT')
            elif link_type=='XPONDER-OUTPUT':
                self.assertEqual(oppLink_type, 'XPONDER-INPUT')
        time.sleep(5)

    def test_22_disconnect_ROADMB(self):
        #Delete in the topology-netconf
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMB"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the clli-network
        url = ("{}/config/ietf-network:network/clli-network/node/NodeB"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/ROADMB"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_23_disconnect_ROADMC(self):
        #Delete in the topology-netconf
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMC"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the clli-network
        url = ("{}/config/ietf-network:network/clli-network/node/NodeC"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/ROADMC"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_24_getLinks_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        #Write the response in the log
        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
            outfile1.write(str(res))
        #Tests related to links
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,8)
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['link-id'],'XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['org-openroadm-network-topology:link-type'],'XPONDER-OUTPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['link-id'],'ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['org-openroadm-network-topology:link-type'],'XPONDER-INPUT')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][6]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][7]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        for i in range(0,nbLink-1):
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'ROADM-TO-ROADM')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG1-CTP-TXRX')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')

    def test_25_getNodes_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        #Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
            outfile1.write(str(len(res['network'][0]['node'])))
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,4)
        #Tests related to nodes
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'XPDRA-XPDR1')
        self.assertEqual(len(res['network'][0]['node'][1]['ietf-network-topology:termination-point']),4)
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
             res['network'][0]['node'][1]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network-topology:node-type'],'XPONDER')
        #Test related to SRG1
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMA-SRG1')
        self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
        self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][0]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network-topology:node-type'],'SRG')
        #Test related to DEG2
        self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMA-DEG2')
        self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'        ][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][2]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network-topology:node-type'],'DEGREE')
        #Test related to DEG1
        self.assertEqual(res['network'][0]['node'][3]['node-id'],'ROADMA-DEG1')
        self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'][0]['node'][3]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][3]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][3]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][3]['org-openroadm-network-topology:node-type'],'DEGREE')
        #Test related to SRG1 of ROADMC
        for i in range(0,nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-SRG1')
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-DEG1')
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-DEG2')

    def test_26_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,2)
        for i in range(0,nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC')

    def test_27_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,1)
        for i in range(0,nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][1]['org-openroadm-clli-network:clli'],'NodeC')

    def test_28_disconnect_XPDRA(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRA"
              .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/XPDRA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_29_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,1)
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'],'NodeA')

    def test_30_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,1)
        for i in range(0,nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'XPDRA')

    def test_31_getNodes_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        #Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,3)
        #Tests related to nodes
        #Test related to SRG1
        self.assertEqual(res['network'][0]['node'][0]['node-id'],'ROADMA-SRG1')
        self.assertEqual(len(res['network'][0]['node'][0]['ietf-network-topology:termination-point']),17)
        self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
            res['network'][0]['node'][0]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][0]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network-topology:node-type'],'SRG')
        #Test related to DEG2
        self.assertEqual(res['network'][0]['node'][1]['node-id'],'ROADMA-DEG2')
        self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][1]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][1]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][1]['org-openroadm-network-topology:node-type'],'DEGREE')
        #Test related to DEG1
        self.assertEqual(res['network'][0]['node'][2]['node-id'],'ROADMA-DEG1')
        self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
            res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
            res['network'][0]['node'][2]['ietf-network-topology:termination-point'])
        self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
            res['network'][0]['node'][2]['supporting-node'])
        self.assertEqual(res['network'][0]['node'][2]['org-openroadm-network-topology:node-type'],'DEGREE')

    def test_32_disconnect_ROADM_XPDRA_link(self):
        #Link-1
        url = ("{}/config/ietf-network:network/openroadm-topology/ietf-network-topology:"
               "link/XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX"
                .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Link-2
        url = ("{}/config/ietf-network:network/openroadm-topology/ietf-network-topology:"
               "link/ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1"
                .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_33_getLinks_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,6)
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][0]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][1]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['link-id'],'ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][2]['org-openroadm-network-topology:link-type'],'DROP-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][3]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['link-id'],'ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][4]['org-openroadm-network-topology:link-type'],'EXPRESS-LINK')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['link-id'],'ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX')
        self.assertEqual(res['network'][0]['ietf-network-topology:link'][5]['org-openroadm-network-topology:link-type'],'ADD-LINK')
        for i in range(0,nbLink-1):
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'XPONDER-OUTPUT')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'XPONDER-INPUT')

    def test_34_disconnect_ROADMA(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the clli-network
        url = ("{}/config/ietf-network:network/clli-network/node/NodeA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(5)

    def test_35_getClliNetwork(self):
        url = ("{}/config/ietf-network:network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_36_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_37_getOpenRoadmTopology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

if __name__ == "__main__":
    #logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
