#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member

import json
import os
# pylint: disable=wrong-import-order
import sys
import re
import signal
import subprocess
import time

import psutil
import requests

# pylint: disable=import-error
import simulators

SIMS = simulators.SIMS

HONEYNODE_OK_START_MSG = 'Netconf SSH endpoint started successfully at 0.0.0.0'
KARAF_OK_START_MSG = "Blueprint container for bundle org.opendaylight.netconf.restconf.* was successfully created"
LIGHTY_OK_START_MSG = re.escape("lighty.io and RESTCONF-NETCONF started")

ODL_LOGIN = 'admin'
ODL_PWD = 'admin'
NODES_LOGIN = 'admin'
NODES_PWD = 'admin'

TYPE_APPLICATION_JSON = {'Content-Type': 'application/json', 'Accept': 'application/json'}
TYPE_APPLICATION_XML = {'Content-Type': 'application/xml', 'Accept': 'application/xml'}

REQUEST_TIMEOUT = 10

CODE_SHOULD_BE_200 = 'Http status code should be 200'
CODE_SHOULD_BE_201 = 'Http status code should be 201'
T100GE = 'Transponder 100GE'
T0_MULTILAYER_TOPO = 'T0 - Multi-layer topology'
T0_FULL_MULTILAYER_TOPO = 'T0 - Full Multi-layer topology'

SIM_LOG_DIRECTORY = os.path.join(os.path.dirname(os.path.realpath(__file__)), 'log')

process_list = []

if 'USE_ODL_ALT_RESTCONF_PORT' in os.environ:
    RESTCONF_PORT = os.environ['USE_ODL_ALT_RESTCONF_PORT']
else:
    RESTCONF_PORT = 8181

RESTCONF_PATH_PREFIX = {'rfc8040': '/rests',
                        'draft-bierman02': '/restconf'}
if 'USE_LIGHTY' in os.environ and os.environ['USE_LIGHTY'] == 'True':
    RESTCONF_PATH_PREFIX['rfc8040'] = '/restconf'

if 'USE_ODL_RESTCONF_VERSION' in os.environ:
    RESTCONF_VERSION = os.environ['USE_ODL_RESTCONF_VERSION']
    if RESTCONF_VERSION not in RESTCONF_PATH_PREFIX:
        print('unsupported RESTCONF version ' + RESTCONF_VERSION)
        sys.exit(3)
else:
    RESTCONF_VERSION = 'rfc8040'

RESTCONF_BASE_URL = 'http://localhost:' + str(RESTCONF_PORT) + RESTCONF_PATH_PREFIX[RESTCONF_VERSION]

if 'USE_ODL_ALT_KARAF_INSTALL_DIR' in os.environ:
    KARAF_INSTALLDIR = os.environ['USE_ODL_ALT_KARAF_INSTALL_DIR']
else:
    KARAF_INSTALLDIR = 'karaf'

KARAF_LOG = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    '..', '..', '..', KARAF_INSTALLDIR, 'target', 'assembly', 'data', 'log', 'karaf.log')

if 'USE_LIGHTY' in os.environ and os.environ['USE_LIGHTY'] == 'True':
    TPCE_LOG = 'odl-' + str(os.getpid()) + '.log'
else:
    TPCE_LOG = KARAF_LOG

#
# Basic HTTP operations
#


def get_request(url):
    return requests.request(
        'GET', url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD),
        timeout=REQUEST_TIMEOUT)


def put_request(url, data):
    return requests.request(
        'PUT', url.format(RESTCONF_BASE_URL),
        data=json.dumps(data),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD),
        timeout=REQUEST_TIMEOUT)


def delete_request(url):
    return requests.request(
        'DELETE', url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD),
        timeout=REQUEST_TIMEOUT)


def post_request(url, data):
    if data:
        return requests.request(
            "POST", url.format(RESTCONF_BASE_URL),
            data=json.dumps(data),
            headers=TYPE_APPLICATION_JSON,
            auth=(ODL_LOGIN, ODL_PWD),
            timeout=REQUEST_TIMEOUT)
    return requests.request(
        "POST", url.format(RESTCONF_BASE_URL),
        headers=TYPE_APPLICATION_JSON,
        auth=(ODL_LOGIN, ODL_PWD),
        timeout=REQUEST_TIMEOUT)

#
# Process management
#


def start_sims(sims_list):
    for sim in sims_list:
        print('starting simulator ' + sim[0] + ' in OpenROADM device version ' + sim[1] + '...')
        log_file = os.path.join(SIM_LOG_DIRECTORY, SIMS[sim]['logfile'])
        process = start_honeynode(log_file, sim)
        if wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100):
            print('simulator for ' + sim[0] + ' started')
        else:
            print('simulator for ' + sim[0] + ' failed to start')
            shutdown_process(process)
            for pid in process_list:
                shutdown_process(pid)
            sys.exit(3)
        process_list.append(process)
    return process_list


def start_tpce():
    print('starting OpenDaylight...')
    if 'USE_LIGHTY' in os.environ and os.environ['USE_LIGHTY'] == 'True':
        process = start_lighty()
        start_msg = LIGHTY_OK_START_MSG
    else:
        process = start_karaf()
        start_msg = KARAF_OK_START_MSG
    if wait_until_log_contains(TPCE_LOG, start_msg, time_to_wait=300):
        print('OpenDaylight started !')
    else:
        print('OpenDaylight failed to start !')
        shutdown_process(process)
        for pid in process_list:
            shutdown_process(pid)
        sys.exit(1)
    process_list.append(process)
    return process_list


def start_karaf():
    print('starting KARAF TransportPCE build...')
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        '..', '..', '..', KARAF_INSTALLDIR, 'target', 'assembly', 'bin', 'karaf')
    with open('odl.log', 'w', encoding='utf-8') as outfile:
        return subprocess.Popen(
            ['sh', executable, 'server'], stdout=outfile, stderr=outfile, stdin=None)


def start_lighty():
    print('starting LIGHTY.IO TransportPCE build...')
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        '..', '..', '..', 'lighty', 'target', 'tpce',
        'clean-start-controller.sh')
    with open(TPCE_LOG, 'w', encoding='utf-8') as outfile:
        return subprocess.Popen(
            ['sh', executable], stdout=outfile, stderr=outfile, stdin=None)


def install_karaf_feature(feature_name: str):
    print('installing feature ' + feature_name)
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        '..', '..', '..', KARAF_INSTALLDIR, 'target', 'assembly', 'bin', 'client')
    return subprocess.run([executable, '-b'],
                          input='feature:install ' + feature_name + '\n feature:list | grep '
                          + feature_name + ' \n logout \n',
                          universal_newlines=True, check=False)


def shutdown_process(process):
    if process is not None:
        for child in psutil.Process(process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        process.send_signal(signal.SIGINT)


def start_honeynode(log_file: str, sim):
    executable = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                              '..', '..', 'honeynode', sim[1], 'honeynode-simulator', 'honeycomb-tpce')
    sample_directory = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                    '..', '..', 'sample_configs', 'openroadm', sim[1])
    if os.path.isfile(executable):
        with open(log_file, 'w', encoding='utf-8') as outfile:
            return subprocess.Popen(
                [executable, SIMS[sim]['port'], os.path.join(sample_directory, SIMS[sim]['configfile'])],
                stdout=outfile, stderr=outfile)
    return None


def wait_until_log_contains(log_file, regexp, time_to_wait=60):
    # pylint: disable=lost-exception
    # pylint: disable=consider-using-with
    stringfound = False
    filefound = False
    line = None
    try:
        with TimeOut(seconds=time_to_wait):
            while not os.path.exists(log_file):
                time.sleep(0.2)
            filelogs = open(log_file, 'r', encoding='utf-8')
            filelogs.seek(0, 2)
            filefound = True
            print("Searching for pattern '" + regexp + "' in " + os.path.basename(log_file), end='... ', flush=True)
            compiled_regexp = re.compile(regexp)
            while True:
                line = filelogs.readline()
                if compiled_regexp.search(line):
                    print('Pattern found!', end=' ')
                    stringfound = True
                    break
                if not line:
                    time.sleep(0.1)
    except TimeoutError:
        print('Pattern not found after ' + str(time_to_wait), end=' seconds! ', flush=True)
    except PermissionError:
        print('Permission Error when trying to access the log file', end=' ... ', flush=True)
    finally:
        if filefound:
            filelogs.close()
        else:
            print('log file does not exist or is not accessible... ', flush=True)
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

#
# Basic NetCONF device operations
#


def mount_device(node: str, sim: str):
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}',
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}'}
    body = {'node': [{
        'node-id': node,
        'netconf-node-topology:username': NODES_LOGIN,
        'netconf-node-topology:password': NODES_PWD,
        'netconf-node-topology:host': '127.0.0.1',
        'netconf-node-topology:port': SIMS[sim]['port'],
        'netconf-node-topology:tcp-only': 'false',
        'netconf-node-topology:pass-through': {}}]}
    response = put_request(url[RESTCONF_VERSION].format('{}', node), body)
    if wait_until_log_contains(TPCE_LOG, 'Triggering notification stream NETCONF for node ' + node, 180):
        print('Node ' + node + ' correctly added to tpce topology', end='... ', flush=True)
    else:
        print('Node ' + node + ' still not added to tpce topology', end='... ', flush=True)
        if response.status_code == requests.codes.ok:
            print('It was probably loaded at start-up', end='... ', flush=True)
        # TODO an else-clause to abort test would probably be nice here
    return response


def unmount_device(node: str):
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}',
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}'}
    response = delete_request(url[RESTCONF_VERSION].format('{}', node))
    if wait_until_log_contains(TPCE_LOG, re.escape("onDeviceDisConnected: " + node), 180):
        print('Node ' + node + ' correctly deleted from tpce topology', end='... ', flush=True)
    else:
        print('Node ' + node + ' still not deleted from tpce topology', end='... ', flush=True)
    return response


def check_device_connection(node: str):
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}?content=nonconfig',
           'draft-bierman02': '{}/operational/network-topology:network-topology/topology/topology-netconf/node/{}'}
    response = get_request(url[RESTCONF_VERSION].format('{}', node))
    res = response.json()
    return_key = {'rfc8040': 'network-topology:node',
                  'draft-bierman02': 'node'}
    if return_key[RESTCONF_VERSION] in res.keys():
        connection_status = res[return_key[RESTCONF_VERSION]][0]['netconf-node-topology:connection-status']
    else:
        connection_status = res['errors']['error'][0]
    return {'status_code': response.status_code,
            'connection-status': connection_status}


def check_node_request(node: str):
    # pylint: disable=line-too-long
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}/yang-ext:mount/org-openroadm-device:org-openroadm-device?content=config',  # nopep8
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}/yang-ext:mount/org-openroadm-device:org-openroadm-device'}  # nopep8
    response = get_request(url[RESTCONF_VERSION].format('{}', node))
    res = response.json()
    return_key = {'rfc8040': 'org-openroadm-device:org-openroadm-device',
                  'draft-bierman02': 'org-openroadm-device'}
    if return_key[RESTCONF_VERSION] in res.keys():
        response_attribute = res[return_key[RESTCONF_VERSION]]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            'org-openroadm-device': response_attribute}


def check_node_attribute_request(node: str, attribute: str, attribute_value: str):
    # pylint: disable=line-too-long
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}={}?content=nonconfig',  # nopep8
           'draft-bierman02': '{}/operational/network-topology:network-topology/topology/topology-netconf/node/{}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}/{}'}  # nopep8
    response = get_request(url[RESTCONF_VERSION].format('{}', node, attribute, attribute_value))
    res = response.json()
    return_key = {'rfc8040': 'org-openroadm-device:' + attribute,
                  'draft-bierman02': attribute}
    if return_key[RESTCONF_VERSION] in res.keys():
        response_attribute = res[return_key[RESTCONF_VERSION]]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            attribute: response_attribute}


def check_node_attribute2_request(node: str, attribute: str, attribute_value: str, attribute2: str):
    # pylint: disable=line-too-long
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}={}/{}?content=config',  # nopep8
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}/{}/{}'}  # nopep8
    response = get_request(url[RESTCONF_VERSION].format('{}', node, attribute, attribute_value, attribute2))
    res = response.json()
    if attribute2 in res.keys():
        response_attribute = res[attribute2]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            attribute2: response_attribute}


def del_node_attribute_request(node: str, attribute: str, attribute_value: str):
    # pylint: disable=line-too-long
    url = {'rfc8040': '{}/data/network-topology:network-topology/topology=topology-netconf/node={}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}={}',  # nopep8
           'draft-bierman02': '{}/config/network-topology:network-topology/topology/topology-netconf/node/{}/yang-ext:mount/org-openroadm-device:org-openroadm-device/{}/{}'}  # nopep8
    response = delete_request(url[RESTCONF_VERSION].format('{}', node, attribute, attribute_value))
    return response

#
# Portmapping operations
#


def post_portmapping(payload: str):
    url = {'rfc8040': '{}/data/transportpce-portmapping:network',
           'draft-bierman02': '{}/config/transportpce-portmapping:network'}
    json_payload = json.loads(payload)
    response = post_request(url[RESTCONF_VERSION].format('{}'), json_payload)
    return {'status_code': response.status_code}


def del_portmapping():
    url = {'rfc8040': '{}/data/transportpce-portmapping:network',
           'draft-bierman02': '{}/config/transportpce-portmapping:network'}
    response = delete_request(url[RESTCONF_VERSION].format('{}'))
    return {'status_code': response.status_code}


def get_portmapping_node_attr(node: str, attr: str, value: str):
    # pylint: disable=consider-using-f-string
    url = {'rfc8040': '{}/data/transportpce-portmapping:network/nodes={}',
           'draft-bierman02': '{}/config/transportpce-portmapping:network/nodes/{}'}
    target_url = url[RESTCONF_VERSION].format('{}', node)
    if attr is not None:
        target_url = (target_url + '/{}').format('{}', attr)
        if value is not None:
            suffix = {'rfc8040': '={}', 'draft-bierman02': '/{}'}
            target_url = (target_url + suffix[RESTCONF_VERSION]).format('{}', value)
    else:
        attr = 'nodes'
    response = get_request(target_url)
    res = response.json()
    return_key = {'rfc8040': 'transportpce-portmapping:' + attr,
                  'draft-bierman02': attr}
    if return_key[RESTCONF_VERSION] in res.keys():
        return_output = res[return_key[RESTCONF_VERSION]]
    else:
        return_output = res['errors']['error'][0]
    return {'status_code': response.status_code,
            attr: return_output}

#
# Topology operations
#


def get_ietf_network_request(network: str, content: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}?content={}',
           'draft-bierman02': '{}/{}/ietf-network:networks/network/{}'}
    if RESTCONF_VERSION in ('rfc8040'):
        format_args = ('{}', network, content)
    elif content == 'config':
        format_args = ('{}', content, network)
    else:
        format_args = ('{}', 'operational', network)
    response = get_request(url[RESTCONF_VERSION].format(*format_args))
    if bool(response):
        res = response.json()
        return_key = {'rfc8040': 'ietf-network:network',
                      'draft-bierman02': 'network'}
        networks = res[return_key[RESTCONF_VERSION]]
    else:
        networks = None
    return {'status_code': response.status_code,
            'network': networks}


def put_ietf_network(network: str, payload: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}',
           'draft-bierman02': '{}/config/ietf-network:networks/network/{}'}
    json_payload = json.loads(payload)
    response = put_request(url[RESTCONF_VERSION].format('{}', network), json_payload)
    return {'status_code': response.status_code}


def del_ietf_network(network: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}',
           'draft-bierman02': '{}/config/ietf-network:networks/network/{}'}
    response = delete_request(url[RESTCONF_VERSION].format('{}', network))
    return {'status_code': response.status_code}


def get_ietf_network_link_request(network: str, link: str, content: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/ietf-network-topology:link={}?content={}',
           'draft-bierman02': '{}/{}/ietf-network:networks/network/{}/ietf-network-topology:link/{}'}
    if RESTCONF_VERSION in ('rfc8040'):
        format_args = ('{}', network, link, content)
    elif content == 'config':
        format_args = ('{}', content, network, link)
    else:
        format_args = ('{}', 'operational', network, link)
    response = get_request(url[RESTCONF_VERSION].format(*format_args))
    res = response.json()
    return_key = {'rfc8040': 'ietf-network-topology:link',
                  'draft-bierman02': 'ietf-network-topology:link'}
    link = res[return_key[RESTCONF_VERSION]][0]
    return {'status_code': response.status_code,
            'link': link}


def del_ietf_network_link_request(network: str, link: str, content: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/ietf-network-topology:link={}?content={}',
           'draft-bierman02': '{}/{}/ietf-network:networks/network/{}/ietf-network-topology:link/{}'}
    if RESTCONF_VERSION in ('rfc8040'):
        format_args = ('{}', network, link, content)
    elif content == 'config':
        format_args = ('{}', content, network, link)
    else:
        format_args = ('{}', 'operational', network, link)
    response = delete_request(url[RESTCONF_VERSION].format(*format_args))
    return response


def add_oms_attr_request(link: str, oms_attr: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/ietf-network-topology:link={}',
           'draft-bierman02': '{}/config/ietf-network:networks/network/{}/ietf-network-topology:link/{}'}
    url2 = url[RESTCONF_VERSION] + '/org-openroadm-network-topology:OMS-attributes/span'
    network = 'openroadm-topology'
    response = put_request(url2.format('{}', network, link), oms_attr)
    return response


def del_oms_attr_request(link: str,):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/ietf-network-topology:link={}',
           'draft-bierman02': '{}/config/ietf-network:networks/network/{}/ietf-network-topology:link/{}'}
    url2 = url[RESTCONF_VERSION] + '/org-openroadm-network-topology:OMS-attributes/span'
    network = 'openroadm-topology'
    response = delete_request(url2.format('{}', network, link))
    return response


def get_ietf_network_node_request(network: str, node: str, content: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/node={}?content={}',
           'draft-bierman02': '{}/{}/ietf-network:networks/network/{}/node/{}'}
    if RESTCONF_VERSION in ('rfc8040'):
        format_args = ('{}', network, node, content)
    elif content == 'config':
        format_args = ('{}', content, network, node)
    else:
        format_args = ('{}', 'operational', network, node)
    response = get_request(url[RESTCONF_VERSION].format(*format_args))
    if bool(response):
        res = response.json()
        return_key = {'rfc8040': 'ietf-network:node',
                      'draft-bierman02': 'node'}
        node = res[return_key[RESTCONF_VERSION]][0]
    else:
        node = None
    return {'status_code': response.status_code,
            'node': node}


def del_ietf_network_node_request(network: str, node: str, content: str):
    url = {'rfc8040': '{}/data/ietf-network:networks/network={}/node={}?content={}',
           'draft-bierman02': '{}/{}/ietf-network:networks/network/{}/node/{}'}
    if RESTCONF_VERSION in ('rfc8040'):
        format_args = ('{}', network, node, content)
    elif content == 'config':
        format_args = ('{}', content, network, node)
    else:
        format_args = ('{}', 'operational', network, node)
    response = delete_request(url[RESTCONF_VERSION].format(*format_args))
    return response


#
# Service list operations
#


def get_ordm_serv_list_request():
    url = {'rfc8040': '{}/data/org-openroadm-service:service-list?content=nonconfig',
           'draft-bierman02': '{}/operational/org-openroadm-service:service-list/'}
    response = get_request(url[RESTCONF_VERSION])
    res = response.json()
    return_key = {'rfc8040': 'org-openroadm-service:service-list',
                  'draft-bierman02': 'service-list'}
    if return_key[RESTCONF_VERSION] in res.keys():
        response_attribute = res[return_key[RESTCONF_VERSION]]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            'service-list': response_attribute}


def get_ordm_serv_list_attr_request(attribute: str, value: str):
    url = {'rfc8040': '{}/data/org-openroadm-service:service-list/{}={}?content=nonconfig',
           'draft-bierman02': '{}/operational/org-openroadm-service:service-list/{}/{}'}
    format_args = ('{}', attribute, value)
    response = get_request(url[RESTCONF_VERSION].format(*format_args))
    res = response.json()
    return_key = {'rfc8040': 'org-openroadm-service:' + attribute,
                  'draft-bierman02': attribute}
    if return_key[RESTCONF_VERSION] in res.keys():
        response_attribute = res[return_key[RESTCONF_VERSION]]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            attribute: response_attribute}


def get_serv_path_list_attr(attribute: str, value: str):
    url = {'rfc8040': '{}/data/transportpce-service-path:service-path-list/{}={}?content=nonconfig',
           'draft-bierman02': '{}/operational/transportpce-service-path:service-path-list/{}/{}'}
    response = get_request(url[RESTCONF_VERSION].format('{}', attribute, value))
    res = response.json()
    return_key = {'rfc8040': 'transportpce-service-path:' + attribute,
                  'draft-bierman02': attribute}
    if return_key[RESTCONF_VERSION] in res.keys():
        response_attribute = res[return_key[RESTCONF_VERSION]]
    else:
        response_attribute = res['errors']['error'][0]
    return {'status_code': response.status_code,
            attribute: response_attribute}


#
# TransportPCE internal API RPCs
#


def prepend_dict_keys(input_dict: dict, prefix: str):
    return_dict = {}
    for key, value in input_dict.items():
        newkey = prefix + key
        if isinstance(value, dict):
            return_dict[newkey] = prepend_dict_keys(value, prefix)
            # TODO: perhaps some recursion depth limit or another solution has to be considered here
            # even if recursion depth is given by the input_dict argument
            # direct (self-)recursive functions may carry unwanted side-effects such as ressource consumptions
        else:
            return_dict[newkey] = value
    return return_dict


def transportpce_api_rpc_request(api_module: str, rpc: str, payload: dict):
    # pylint: disable=consider-using-f-string
    url = "{}/operations/{}:{}".format('{}', api_module, rpc)
    if payload is None:
        data = None
    elif RESTCONF_VERSION == 'draft-bierman02':
        data = prepend_dict_keys({'input': payload}, api_module + ':')
    else:
        data = {'input': payload}
    response = post_request(url, data)
    if response.status_code == requests.codes.no_content:
        return_output = None
    else:
        res = response.json()
        return_key = {'rfc8040': api_module + ':output',
                      'draft-bierman02': 'output'}
        if response.status_code == requests.codes.internal_server_error:
            return_output = res
        else:
            return_output = res[return_key[RESTCONF_VERSION]]
    return {'status_code': response.status_code,
            'output': return_output}
