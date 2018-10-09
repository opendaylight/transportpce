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
                      "netconf-testtool-1.5.0-executable.jar")
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
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(100)

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
        time.sleep(30)

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
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,6)
        expressLink=['ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX','ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX']
        addLink=['ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX','ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX']
        dropLink=['ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX','ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX']
        for i in range(0,nbLink):
            linkId = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='EXPRESS-LINK'):
                find= linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='ADD-LINK'):
                find= linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='DROP-LINK'):
                find= linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink),0)
        self.assertEqual(len(addLink),0)
        self.assertEqual(len(dropLink),0)

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
        nbNode=len(res['network'][0]['node'])
        self.assertEqual(nbNode,3)
        listNode=['ROADMA-SRG1','ROADMA-DEG1','ROADMA-DEG2']
        for i in range(0,nbNode):
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                          res['network'][0]['node'][i]['supporting-node'])
            nodeType=res['network'][0]['node'][i]['org-openroadm-network-topology:node-type']
            nodeId=res['network'][0]['node'][i]['node-id']
            if(nodeId=='ROADMA-SRG1'):
                #Test related to SRG1
                self.assertEqual(nodeType,'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif(nodeId=='ROADMA-DEG1'):
                #Test related to DEG1
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif(nodeId=='ROADMA-DEG2'):
                #Test related to DEG2
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode),0)

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
         for i in range(0,nbNode):
             self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['network-ref'],'clli-network')
             self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'],'NodeA')
             nodeId=res['network'][0]['node'][i]['node-id']
             if(nodeId=='XPDRA'):
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:node-type'],'XPONDER')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'],'1')
             elif(nodeId=='ROADMA'):
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:node-type'],'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'],'2')
             else:
                self.assertFalse(True)

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
         listNode=['XPDRA-XPDR1','ROADMA-SRG1','ROADMA-DEG1','ROADMA-DEG2']
         for i in range(0,nbNode):
             nodeType=res['network'][0]['node'][i]['org-openroadm-network-topology:node-type']
             nodeId=res['network'][0]['node'][i]['node-id']
             #Tests related to XPDRA nodes
             if(nodeId=='XPDRA-XPDR1'):
                 self.assertEqual(nodeType,'XPONDER')
                 self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),2)
                 self.assertEqual({'tp-id': 'XPDR1-CLIENT1', 'org-openroadm-network-topology:tp-type': 'XPONDER-CLIENT',
                                   'org-openroadm-network-topology:xpdr-network-attributes': {'tail-equipment-id': 'XPDR1-NETWORK1'}},
                                  res['network'][0]['node'][i]['ietf-network-topology:termination-point'][0])
                 self.assertEqual({'tp-id': 'XPDR1-NETWORK1', 'org-openroadm-network-topology:tp-type': 'XPONDER-NETWORK',
                                   'org-openroadm-network-topology:xpdr-client-attributes': {'tail-equipment-id': 'XPDR1-CLIENT1'}},
                                  res['network'][0]['node'][i]['ietf-network-topology:termination-point'][1])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMA-SRG1'):
                 #Test related to SRG1
                 self.assertEqual(nodeType,'SRG')
                 self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
                 self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMA-DEG1'):
                #Test related to DEG1
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
             elif(nodeId=='ROADMA-DEG2'):
                #Test related to DEG2
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
             else:
                self.assertFalse(True)
         self.assertEqual(len(listNode),0)

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
        nbLink=len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink,8)
        expressLink=['ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX','ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX']
        addLink=['ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX','ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX',]
        dropLink=['ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX','ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX']
        XPDR_IN=['ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1']
        XPDR_OUT=['XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX']
        for i in range(0,nbLink):
            nodeType=res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']
            linkId=res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if(nodeType=='EXPRESS-LINK'):
                find= linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif(nodeType=='ADD-LINK'):
                find= linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif(nodeType=='DROP-LINK'):
                find= linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            elif(nodeType=='XPONDER-INPUT'):
                find= linkId in XPDR_IN
                self.assertEqual(find, True)
                XPDR_IN.remove(linkId)
            elif(nodeType=='XPONDER-OUTPUT'):
                find= linkId in XPDR_OUT
                self.assertEqual(find, True)
                XPDR_OUT.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink),0)
        self.assertEqual(len(addLink),0)
        self.assertEqual(len(dropLink),0)
        self.assertEqual(len(XPDR_IN),0)
        self.assertEqual(len(XPDR_OUT),0)

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
        nbNode=len(res['network'][0]['node'])
        listNode=['NodeA','NodeC']
        for i in range(0,nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            find= nodeId in listNode
            self.assertEqual(find, True)
            if(nodeId=='NodeA'):
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'],'NodeA')
            else:
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'],'NodeC')
            listNode.remove(nodeId)

        self.assertEqual(len(listNode),0)

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
        listNode=['XPDRA','ROADMA','ROADMC']
        for i in range(0,nbNode):
            self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['network-ref'],'clli-network')
            nodeId=res['network'][0]['node'][i]['node-id']
            if(nodeId=='XPDRA'):
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'],'NodeA')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:node-type'],'XPONDER')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'],'1')
                listNode.remove(nodeId)
            elif(nodeId=='ROADMA'):
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'],'NodeA')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:node-type'],'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'],'2')
                listNode.remove(nodeId)
            elif(nodeId=='ROADMC'):
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'],'NodeC')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:node-type'],'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'],'2')
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode),0)

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
        expressLink=['ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX','ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX',
                     'ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX','ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX']
        addLink=['ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX','ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX',
                 'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX','ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX']
        dropLink=['ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX','ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX',
                  'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX','ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX']
        R2RLink=['ROADMA-DEG1-DEG1-TTP-TXRXtoROADMC-DEG2-DEG2-TTP-TXRX','ROADMC-DEG2-DEG2-TTP-TXRXtoROADMA-DEG1-DEG1-TTP-TXRX']
        XPDR_IN=['ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1']
        XPDR_OUT=['XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX']
        for i in range(0,nbLink):
            nodeType=res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']
            linkId=res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if(nodeType=='EXPRESS-LINK'):
                find= linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif(nodeType=='ADD-LINK'):
                find= linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif(nodeType=='DROP-LINK'):
                find= linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            elif(nodeType=='ROADM-TO-ROADM'):
                find= linkId in R2RLink
                self.assertEqual(find, True)
                R2RLink.remove(linkId)
            elif(nodeType=='XPONDER-INPUT'):
                find= linkId in XPDR_IN
                self.assertEqual(find, True)
                XPDR_IN.remove(linkId)
            elif(nodeType=='XPONDER-OUTPUT'):
                find= linkId in XPDR_OUT
                self.assertEqual(find, True)
                XPDR_OUT.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink),0)
        self.assertEqual(len(addLink),0)
        self.assertEqual(len(dropLink),0)
        self.assertEqual(len(R2RLink),0)
        self.assertEqual(len(XPDR_IN),0)
        self.assertEqual(len(XPDR_OUT),0)

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
         listNode=['XPDRA-XPDR1',
                   'ROADMA-SRG1','ROADMA-DEG1','ROADMA-DEG2',
                   'ROADMC-SRG1','ROADMC-DEG1','ROADMC-DEG2']
         #************************Tests related to XPDRA nodes
         for i in range(0,nbNode):
             nodeId=res['network'][0]['node'][i]['node-id']
             if(nodeId=='XPDRA-XPDR1'):
                 #Test related to XPDR1
                 self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),2)
                 self.assertEqual({'tp-id': 'XPDR1-CLIENT1', 'org-openroadm-network-topology:tp-type': 'XPONDER-CLIENT',
                                   'org-openroadm-network-topology:xpdr-network-attributes': {'tail-equipment-id': 'XPDR1-NETWORK1'}},
                                  res['network'][0]['node'][i]['ietf-network-topology:termination-point'][0])
                 self.assertEqual({'tp-id': 'XPDR1-NETWORK1', 'org-openroadm-network-topology:tp-type': 'XPONDER-NETWORK',
                                   'org-openroadm-network-topology:xpdr-client-attributes': {'tail-equipment-id': 'XPDR1-CLIENT1'}},
                                  res['network'][0]['node'][i]['ietf-network-topology:termination-point'][1])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'XPONDER')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMA-SRG1'):
                 #Test related to SRG1
                 self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
                 self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'SRG')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMA-DEG1'):
                 #Test related to DEG1
                 self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'DEGREE')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMA-DEG2'):
                 #Test related to DEG2
                 self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'DEGREE')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMC-SRG1'):
                 #Test related to SRG1
                 self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
                 self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'SRG')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMC-DEG1'):
                 #Test related to DEG1
                 self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'DEGREE')
                 listNode.remove(nodeId)
             elif(nodeId=='ROADMC-DEG2'):
                 #Test related to DEG2
                 self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                               res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                 self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC'},
                               res['network'][0]['node'][i]['supporting-node'])
                 self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network-topology:node-type'],'DEGREE')
                 listNode.remove(nodeId)
             else:
                self.assertFalse(True)
         self.assertEqual(len(listNode),0)

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
        nbNode=len(res['network'][0]['node'])
        listNode=['NodeA','NodeB','NodeC']
        for i in range(0,nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            find= nodeId in listNode
            self.assertEqual(find, True)
            if(nodeId=='NodeA'):
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'],'NodeA')
            elif(nodeId=='NodeB'):
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'],'NodeB')
            else:
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'],'NodeC')
            listNode.remove(nodeId)

        self.assertEqual(len(listNode),0)

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

#    def test_24_getLinks_OpenRoadmTopology(self):
#        url = ("{}/config/ietf-network:network/openroadm-topology"
#               .format(self.restconf_baseurl))
#        headers = {'content-type': 'application/json'}
#        response = requests.request(
#             "GET", url, headers=headers, auth=('admin', 'admin'))
#        self.assertEqual(response.status_code, requests.codes.ok)
#        res = response.json()
#        #Write the response in the log
#        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
#            outfile1.write(str(res))
#        #Tests related to links
#        nbLink=len(res['network'][0]['ietf-network-topology:link'])
#        self.assertEqual(nbLink,8)
#        expressLink=['ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX','ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX']
#        addLink=['ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX','ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX',]
#        dropLink=['ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX','ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX']
#        XPDR_IN=['ROADMA-SRG1-SRG1-PP1-TXRXtoXPDRA-XPDR1-XPDR1-NETWORK1']
#        XPDR_OUT=['XPDRA-XPDR1-XPDR1-NETWORK1toROADMA-SRG1-SRG1-PP1-TXRX']
#        for i in range(0,nbLink):
#            nodeType=res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']
#            linkId=res['network'][0]['ietf-network-topology:link'][i]['link-id']
#            if(nodeType=='EXPRESS-LINK'):
#                find= linkId in expressLink
#                self.assertEqual(find, True)
#                expressLink.remove(linkId)
#            elif(nodeType=='ADD-LINK'):
#                find= linkId in addLink
#                self.assertEqual(find, True)
#                addLink.remove(linkId)
#            elif(nodeType=='DROP-LINK'):
#                find= linkId in dropLink
#                self.assertEqual(find, True)
#                dropLink.remove(linkId)
#            elif(nodeType=='XPONDER-INPUT'):
#                find= linkId in XPDR_IN
#                self.assertEqual(find, True)
#                XPDR_IN.remove(linkId)
#            elif(nodeType=='XPONDER-OUTPUT'):
#                find= linkId in XPDR_OUT
#                self.assertEqual(find, True)
#                XPDR_OUT.remove(linkId)
#            else:
#                self.assertFalse(True)
#        self.assertEqual(len(expressLink),0)
#        self.assertEqual(len(addLink),0)
#        self.assertEqual(len(dropLink),0)
#        self.assertEqual(len(XPDR_IN),0)
#        self.assertEqual(len(XPDR_OUT),0)
#
#        for i in range(0,nbLink):
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'ROADM-TO-ROADM')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-SRG1-SRG1-CP-TXRXtoROADMC-DEG2-DEG1-CTP-TXRX')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG2-CTP-TXRXtoROADMC-SRG1-SRG1-CP-TXRX')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG1-DEG1-CTP-TXRXtoROADMC-DEG2-DEG2-CTP-TXRX')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['link-id'],'ROADMC-DEG2-DEG2-CTP-TXRXtoROADMC-DEG1-DEG1-CTP-TXRX')
#
#    def test_25_getNodes_OpenRoadmTopology(self):
#        url = ("{}/config/ietf-network:network/openroadm-topology"
#              .format(self.restconf_baseurl))
#        headers = {'content-type': 'application/json'}
#        response = requests.request(
#            "GET", url, headers=headers, auth=('admin', 'admin'))
#        res = response.json()
#        #Tests related to nodes
#        self.assertEqual(response.status_code, requests.codes.ok)
#        with open('./transportpce_tests/log/response.log', 'a') as outfile1:
#            outfile1.write(str(len(res['network'][0]['node'])))
#        nbNode=len(res['network'][0]['node'])
#        self.assertEqual(nbNode,4)
#        listNode=['XPDRA-XPDR1','ROADMA-SRG1','ROADMA-DEG1','ROADMA-DEG2']
#        for i in range(0,nbNode):
#            nodeType=res['network'][0]['node'][i]['org-openroadm-network-topology:node-type']
#            nodeId=res['network'][0]['node'][i]['node-id']
#            #Tests related to XPDRA nodes
#            if(nodeId=='XPDRA-XPDR1'):
#                self.assertEqual(nodeType,'XPONDER')
#                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),2)
#                self.assertEqual({'tp-id': 'XPDR1-CLIENT1', 'org-openroadm-network-topology:tp-type': 'XPONDER-CLIENT',
#                                  'org-openroadm-network-topology:xpdr-network-attributes': {
#                                      'tail-equipment-id': 'XPDR1-NETWORK1'}},
#                                 res['network'][0]['node'][i]['ietf-network-topology:termination-point'][0])
#                self.assertEqual({'tp-id': 'XPDR1-NETWORK1', 'org-openroadm-network-topology:tp-type': 'XPONDER-NETWORK',
#                                  'org-openroadm-network-topology:xpdr-client-attributes': {'tail-equipment-id': 'XPDR1-CLIENT1'}},
#                                 res['network'][0]['node'][i]['ietf-network-topology:termination-point'][1])
#                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA'},
#                    res['network'][0]['node'][i]['supporting-node'])
#                listNode.remove(nodeId)
#            elif(nodeId=='ROADMA-SRG1'):
#                #Test related to SRG1
#                self.assertEqual(nodeType,'SRG')
#                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
#                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
#                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
#                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
#                              res['network'][0]['node'][i]['supporting-node'])
#                listNode.remove(nodeId)
#            elif(nodeId=='ROADMA-DEG1'):
#                #Test related to DEG1
#                self.assertEqual(nodeType,'DEGREE')
#                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
#                    res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
#                    res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
#                    res['network'][0]['node'][i]['supporting-node'])
#                listNode.remove(nodeId)
#            elif(nodeId=='ROADMA-DEG2'):
#                #Test related to DEG2
#                self.assertEqual(nodeType,'DEGREE')
#                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
#                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
#                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
#                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
#                              res['network'][0]['node'][i]['supporting-node'])
#                listNode.remove(nodeId)
#            else:
#                self.assertFalse(True)
#        self.assertEqual(len(listNode),0)
#        #Test related to SRG1 of ROADMC
#        for i in range(0,nbNode):
#            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-SRG1')
#            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-DEG1')
#            self.assertNotEqual(res['network'][0]['node'][i]['node-id'],'ROADMC-DEG2')

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
        for i in range(0,nbNode):
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
        listNode=['ROADMA-SRG1','ROADMA-DEG1','ROADMA-DEG2']
        for i in range(0,nbNode):
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA'},
                          res['network'][0]['node'][i]['supporting-node'])
            nodeType=res['network'][0]['node'][i]['org-openroadm-network-topology:node-type']
            nodeId=res['network'][0]['node'][i]['node-id']
            if(nodeId=='ROADMA-SRG1'):
                #Test related to SRG1
                self.assertEqual(nodeType,'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']),17)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-network-topology:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif(nodeId=='ROADMA-DEG1'):
                #Test related to DEG1
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif(nodeId=='ROADMA-DEG2'):
                #Test related to DEG2
                self.assertEqual(nodeType,'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-network-topology:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode),0)

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

#    def test_33_getLinks_OpenRoadmTopology(self):
#        url = ("{}/config/ietf-network:network/openroadm-topology"
#              .format(self.restconf_baseurl))
#        headers = {'content-type': 'application/json'}
#        response = requests.request(
#            "GET", url, headers=headers, auth=('admin', 'admin'))
#        self.assertEqual(response.status_code, requests.codes.ok)
#        res = response.json()
#        nbLink=len(res['network'][0]['ietf-network-topology:link'])
#        self.assertEqual(nbLink,6)
#        expressLink=['ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX','ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX']
#        addLink=['ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG2-DEG2-CTP-TXRX','ROADMA-SRG1-SRG1-CP-TXRXtoROADMA-DEG1-DEG1-CTP-TXRX']
#        dropLink=['ROADMA-DEG1-DEG1-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX','ROADMA-DEG2-DEG2-CTP-TXRXtoROADMA-SRG1-SRG1-CP-TXRX']
#        for i in range(0,nbLink):
#            if (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='EXPRESS-LINK'):
#                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
#                find= link_id in expressLink
#                self.assertEqual(find, True)
#                expressLink.remove(link_id)
#            elif (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='ADD-LINK'):
#                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
#                find= link_id in addLink
#                self.assertEqual(find, True)
#                addLink.remove(link_id)
#            elif (res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type']=='DROP-LINK'):
#                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
#                find= link_id in dropLink
#                self.assertEqual(find, True)
#                dropLink.remove(link_id)
#            else:
#                self.assertFalse(True)
#        self.assertEqual(len(expressLink),0)
#        self.assertEqual(len(addLink),0)
#        self.assertEqual(len(dropLink),0)
#        for i in range(0,nbLink):
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'XPONDER-OUTPUT')
#            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-network-topology:link-type'],'XPONDER-INPUT')

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

#    def test_37_getOpenRoadmTopology(self):
#        url = ("{}/config/ietf-network:network/openroadm-topology"
#              .format(self.restconf_baseurl))
#        headers = {'content-type': 'application/json'}
#        response = requests.request(
#            "GET", url, headers=headers, auth=('admin', 'admin'))
#        self.assertEqual(response.status_code, requests.codes.ok)
#        res = response.json()
#        self.assertNotIn('node', res['network'][0])
#        self.assertNotIn('ietf-network-topology:link', res['network'][0])

if __name__ == "__main__":
    #logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
