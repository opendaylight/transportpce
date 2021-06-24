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

import json
import os
import sys
import re
import signal
import subprocess
import time

import psutil
import requests

import simulators

SIMS = simulators.SIMS

HONEYNODE_OK_START_MSG = "Netconf SSH endpoint started successfully at 0.0.0.0"
KARAF_OK_START_MSG = re.escape(
    "Blueprint container for bundle org.opendaylight.netconf.restconf")+".* was successfully created"
LIGHTY_OK_START_MSG = re.escape("lighty.io and RESTCONF-NETCONF started")

ODL_LOGIN = "admin"
ODL_PWD = "admin"
NODES_LOGIN = "admin"
NODES_PWD = "admin"
URL_CONFIG_NETCONF_TOPO = "{}/config/network-topology:network-topology/topology/topology-netconf/"
URL_CONFIG_ORDM_TOPO = "{}/config/ietf-network:networks/network/openroadm-topology/"
URL_CONFIG_OTN_TOPO = "{}/config/ietf-network:networks/network/otn-topology/"
URL_CONFIG_CLLI_NET = "{}/config/ietf-network:networks/network/clli-network/"
URL_CONFIG_ORDM_NET = "{}/config/ietf-network:networks/network/openroadm-network/"
URL_PORTMAPPING = "{}/config/transportpce-portmapping:network/nodes/"
URL_OPER_SERV_LIST = "{}/operational/org-openroadm-service:service-list/"
URL_GET_NBINOTIFICATIONS_SERV = "{}/operations/nbi-notifications:get-notifications-service/"
URL_SERV_CREATE = "{}/operations/org-openroadm-service:service-create"
URL_SERV_DELETE = "{}/operations/org-openroadm-service:service-delete"
URL_SERVICE_PATH = "{}/operations/transportpce-device-renderer:service-path"
URL_OTN_SERVICE_PATH = "{}/operations/transportpce-device-renderer:otn-service-path"
URL_CREATE_OTS_OMS = "{}/operations/transportpce-device-renderer:create-ots-oms"
URL_PATH_COMPUTATION_REQUEST = "{}/operations/transportpce-pce:path-computation-request"
URL_FULL_PORTMAPPING = "{}/config/transportpce-portmapping:network"

TYPE_APPLICATION_JSON = {'Content-Type': 'application/json', 'Accept': 'application/json'}
TYPE_APPLICATION_XML = {'Content-Type': 'application/xml', 'Accept': 'application/xml'}

CODE_SHOULD_BE_200 = 'Http status code should be 200'
CODE_SHOULD_BE_201 = 'Http status code should be 201'

SIM_LOG_DIRECTORY = os.path.join(os.path.dirname(os.path.realpath(__file__)), "log")
KARAF_LOG = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "..", "karaf", "target", "assembly", "data", "log", "karaf.log")

process_list = []


if "USE_ODL_ALT_RESTCONF_PORT" in os.environ:
    RESTCONF_BASE_URL = "http://localhost:" + os.environ['USE_ODL_ALT_RESTCONF_PORT'] + "/restconf"
else:
    RESTCONF_BASE_URL = "http://localhost:8181/restconf"

if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
    TPCE_LOG = 'odl.log'
else:
    TPCE_LOG = KARAF_LOG


def start_sims(sims_list):
    for sim in sims_list:
        print("starting simulator " + sim[0] + " in OpenROADM device version " + sim[1] + "...")
        log_file = os.path.join(SIM_LOG_DIRECTORY, SIMS[sim]['logfile'])
        process = start_honeynode(log_file, sim)
        if wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100):
            print("simulator for " + sim[0] + " started")
        else:
            print("simulator for " + sim[0] + " failed to start")
            shutdown_process(process)
            for pid in process_list:
                shutdown_process(pid)
            sys.exit(3)
        process_list.append(process)
    return process_list


def start_tpce():
    print("starting OpenDaylight...")
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        process = start_lighty()
        start_msg = LIGHTY_OK_START_MSG
    else:
        process = start_karaf()
        start_msg = KARAF_OK_START_MSG
    if wait_until_log_contains(TPCE_LOG, start_msg, time_to_wait=120):
        print("OpenDaylight started !")
    else:
        print("OpenDaylight failed to start !")
        shutdown_process(process)
        for pid in process_list:
            shutdown_process(pid)
        sys.exit(1)
    process_list.append(process)
    return process_list


def start_karaf():
    print("starting KARAF TransportPCE build...")
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "karaf", "target", "assembly", "bin", "karaf")
    with open('odl.log', 'w') as outfile:
        return subprocess.Popen(
            ["sh", executable, "server"], stdout=outfile, stderr=outfile, stdin=None)


def start_lighty():
    print("starting LIGHTY.IO TransportPCE build...")
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "lighty", "target", "tpce",
        "clean-start-controller.sh")
    with open('odl.log', 'w') as outfile:
        return subprocess.Popen(
            ["sh", executable], stdout=outfile, stderr=outfile, stdin=None)


def install_karaf_feature(feature_name: str):
    print("installing feature " + feature_name)
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "karaf", "target", "assembly", "bin", "client")
    return subprocess.run([executable],
                          input='feature:install ' + feature_name + '\n feature:list | grep '
                          + feature_name + ' \n logout \n',
                          universal_newlines=True, check=False)


def get_request(url):
    return requests.request(
        "GET", url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))


def post_request(url, data):
    if data:
        print(json.dumps(data))
        return requests.request(
            "POST", url.format(RESTCONF_BASE_URL),
            data=json.dumps(data),
            headers=TYPE_APPLICATION_JSON,
            auth=(ODL_LOGIN, ODL_PWD))

    return requests.request(
        "POST", url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))


def post_xmlrequest(url, data):
    if data:
        return requests.request(
            "POST", url.format(RESTCONF_BASE_URL),
            data=data,
            headers=TYPE_APPLICATION_XML,
            auth=(ODL_LOGIN, ODL_PWD))
    return None


def put_request(url, data):
    return requests.request(
        "PUT", url.format(RESTCONF_BASE_URL),
        data=json.dumps(data),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))


def put_xmlrequest(url, data):
    return requests.request(
        "PUT", url.format(RESTCONF_BASE_URL),
        data=data,
        headers=TYPE_APPLICATION_XML,
        auth=(ODL_LOGIN, ODL_PWD))

def put_jsonrequest(url, data):
    return requests.request(
        "PUT", url.format(RESTCONF_BASE_URL),
        data=data,
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))

def rawput_request(url, data):
    return requests.request(
        "PUT", url.format(RESTCONF_BASE_URL),
        data=data,
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))

def rawpost_request(url, data):
    return requests.request(
        "POST", url.format(RESTCONF_BASE_URL),
        data=data,
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))


def delete_request(url):
    return requests.request(
        "DELETE", url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD))


def mount_device(node_id, sim):
    url = URL_CONFIG_NETCONF_TOPO + "node/" + node_id
    body = {"node": [{
        "node-id": node_id,
        "netconf-node-topology:username": NODES_LOGIN,
        "netconf-node-topology:password": NODES_PWD,
        "netconf-node-topology:host": "127.0.0.1",
        "netconf-node-topology:port": SIMS[sim]['port'],
        "netconf-node-topology:tcp-only": "false",
        "netconf-node-topology:pass-through": {}}]}
    response = put_request(url, body)
    if wait_until_log_contains(TPCE_LOG, re.escape("Triggering notification stream NETCONF for node " + node_id), 60):
        print("Node " + node_id + " correctly added to tpce topology", end='... ', flush=True)
    else:
        print("Node " + node_id + " still not added to tpce topology", end='... ', flush=True)
        if response.status_code == requests.codes.ok:
            print("It was probably loaded at start-up", end='... ', flush=True)
        # TODO an else-clause to abort test would probably be nice here
    return response


def unmount_device(node_id):
    url = URL_CONFIG_NETCONF_TOPO + "node/" + node_id
    response = delete_request(url)
    if wait_until_log_contains(TPCE_LOG, re.escape("onDeviceDisConnected: " + node_id), 60):
        print("Node " + node_id + " correctly deleted from tpce topology", end='... ', flush=True)
    else:
        print("Node " + node_id + " still not deleted from tpce topology", end='... ', flush=True)
    return response


def connect_xpdr_to_rdm_request(xpdr_node: str, xpdr_num: str, network_num: str,
                                rdm_node: str, srg_num: str, termination_num: str):
    url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links"
    data = {
        "networkutils:input": {
            "networkutils:links-input": {
                "networkutils:xpdr-node": xpdr_node,
                "networkutils:xpdr-num": xpdr_num,
                "networkutils:network-num": network_num,
                "networkutils:rdm-node": rdm_node,
                "networkutils:srg-num": srg_num,
                "networkutils:termination-point-num": termination_num
            }
        }
    }
    return post_request(url, data)


def connect_rdm_to_xpdr_request(xpdr_node: str, xpdr_num: str, network_num: str,
                                rdm_node: str, srg_num: str, termination_num: str):
    url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links"
    data = {
        "networkutils:input": {
            "networkutils:links-input": {
                "networkutils:xpdr-node": xpdr_node,
                "networkutils:xpdr-num": xpdr_num,
                "networkutils:network-num": network_num,
                "networkutils:rdm-node": rdm_node,
                "networkutils:srg-num": srg_num,
                "networkutils:termination-point-num": termination_num
            }
        }
    }
    return post_request(url, data)


def check_netconf_node_request(node: str, suffix: str):
    url = URL_CONFIG_NETCONF_TOPO + (
        "node/" + node + "/yang-ext:mount/org-openroadm-device:org-openroadm-device/" + suffix
    )
    return get_request(url)


def get_netconf_oper_request(node: str):
    url = "{}/operational/network-topology:network-topology/topology/topology-netconf/node/" + node
    return get_request(url)


def get_ordm_topo_request(suffix: str):
    url = URL_CONFIG_ORDM_TOPO + suffix
    return get_request(url)


def add_oms_attr_request(link: str, attr):
    url = URL_CONFIG_ORDM_TOPO + (
        "ietf-network-topology:link/" + link + "/org-openroadm-network-topology:OMS-attributes/span"
    )
    return put_request(url, attr)


def del_oms_attr_request(link: str):
    url = URL_CONFIG_ORDM_TOPO + (
        "ietf-network-topology:link/" + link + "/org-openroadm-network-topology:OMS-attributes/span"
    )
    return delete_request(url)


def get_clli_net_request():
    return get_request(URL_CONFIG_CLLI_NET)


def get_ordm_net_request():
    return get_request(URL_CONFIG_ORDM_NET)


def get_otn_topo_request():
    return get_request(URL_CONFIG_OTN_TOPO)


def del_link_request(link: str):
    url = URL_CONFIG_ORDM_TOPO + ("ietf-network-topology:link/" + link)
    return delete_request(url)


def del_node_request(node: str):
    url = URL_CONFIG_CLLI_NET + ("node/" + node)
    return delete_request(url)


def portmapping_request(suffix: str):
    url = URL_PORTMAPPING + suffix
    return get_request(url)


def get_notifications_service_request(attr):
    return post_request(URL_GET_NBINOTIFICATIONS_SERV, attr)


def get_service_list_request(suffix: str):
    url = URL_OPER_SERV_LIST + suffix
    return get_request(url)


def service_create_request(attr):
    return post_request(URL_SERV_CREATE, attr)


def service_delete_request(servicename: str,
                           requestid="e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                           notificationurl="http://localhost:8585/NotificationServer/notify"):
    attr = {"input": {
        "sdnc-request-header": {
            "request-id": requestid,
            "rpc-action": "service-delete",
            "request-system-id": "appname",
            "notification-url": notificationurl},
        "service-delete-req-info": {
            "service-name": servicename,
            "tail-retention": "no"}}}
    return post_request(URL_SERV_DELETE, attr)


def service_path_request(operation: str, servicename: str, wavenumber: str, nodes, centerfreq: str,
                         slotwidth: int, minfreq: float, maxfreq: float, lowerslotnumber: int,
                         higherslotnumber: int):
    attr = {"renderer:input": {
        "renderer:service-name": servicename,
        "renderer:wave-number": wavenumber,
        "renderer:modulation-format": "dp-qpsk",
        "renderer:operation": operation,
        "renderer:nodes": nodes,
        "renderer:center-freq": centerfreq,
        "renderer:width": slotwidth,
        "renderer:min-freq": minfreq,
        "renderer:max-freq": maxfreq,
        "renderer:lower-spectral-slot-number": lowerslotnumber,
        "renderer:higher-spectral-slot-number": higherslotnumber}}
    return post_request(URL_SERVICE_PATH, attr)


def otn_service_path_request(operation: str, servicename: str, servicerate: str, servicetype: str, nodes,
                             eth_attr=None):
    attr = {"service-name": servicename,
            "operation": operation,
            "service-rate": servicerate,
            "service-type": servicetype,
            "nodes": nodes}
    if eth_attr:
        attr.update(eth_attr)
    return post_request(URL_OTN_SERVICE_PATH, {"renderer:input": attr})


def create_ots_oms_request(nodeid: str, lcp: str):
    attr = {"input": {
        "node-id": nodeid,
        "logical-connection-point": lcp}}
    return post_request(URL_CREATE_OTS_OMS, attr)


def path_computation_request(requestid: str, servicename: str, serviceaend, servicezend,
                             hardconstraints=None, softconstraints=None, metric="hop-count", other_attr=None):
    attr = {"service-name": servicename,
            "resource-reserve": "true",
            "service-handler-header": {"request-id": requestid},
            "service-a-end": serviceaend,
            "service-z-end": servicezend,
            "pce-metric": metric}
    if hardconstraints:
        attr.update({"hard-constraints": hardconstraints})
    if softconstraints:
        attr.update({"soft-constraints": softconstraints})
    if other_attr:
        attr.update(other_attr)
    return post_request(URL_PATH_COMPUTATION_REQUEST, {"input": attr})


def shutdown_process(process):
    if process is not None:
        for child in psutil.Process(process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        process.send_signal(signal.SIGINT)


def start_honeynode(log_file: str, sim):
    executable = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                              "..", "..", "honeynode", sim[1], "honeynode-simulator", "honeycomb-tpce")
    sample_directory = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                    "..", "..", "sample_configs", "openroadm", sim[1])
    if os.path.isfile(executable):
        with open(log_file, 'w') as outfile:
            return subprocess.Popen(
                [executable, SIMS[sim]['port'], os.path.join(sample_directory, SIMS[sim]['configfile'])],
                stdout=outfile, stderr=outfile)
    return None


def wait_until_log_contains(log_file, regexp, time_to_wait=20):
    # pylint: disable=lost-exception
    stringfound = False
    filefound = False
    line = None
    try:
        with TimeOut(seconds=time_to_wait):
            while not os.path.exists(log_file):
                time.sleep(0.2)
            filelogs = open(log_file, 'r')
            filelogs.seek(0, 2)
            filefound = True
            print("Searching for pattern '" + regexp + "' in " + os.path.basename(log_file), end='... ', flush=True)
            compiled_regexp = re.compile(regexp)
            while True:
                line = filelogs.readline()
                if compiled_regexp.search(line):
                    print("Pattern found!", end=' ')
                    stringfound = True
                    break
                if not line:
                    time.sleep(0.1)
    except TimeoutError:
        print("Pattern not found after " + str(time_to_wait), end=" seconds! ", flush=True)
    except PermissionError:
        print("Permission Error when trying to access the log file", end=" ... ", flush=True)
    finally:
        if filefound:
            filelogs.close()
        else:
            print("log file does not exist or is not accessible... ", flush=True)
        return stringfound


class TimeOut:
    def __init__(self, seconds=1, error_message='Timeout'):
        self.seconds = seconds
        self.error_message = error_message

    def handle_timeout(self, signum, frame):
        raise TimeoutError(self.error_message)

    def __enter__(self):
        signal.signal(signal.SIGALRM, self.handle_timeout)
        signal.alarm(self.seconds)

    def __exit__(self, type, value, traceback):
        # pylint: disable=W0622
        signal.alarm(0)
