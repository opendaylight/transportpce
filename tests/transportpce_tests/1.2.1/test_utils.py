import os
import subprocess

sims = {
    'xpdra': {'port': '17830', 'configfile': 'oper-XPDRA.xml'},
    'roadma': {'port': '17831', 'configfile': 'oper-ROADMA.xml'},
    'roadmb': {'port': '17832', 'configfile': 'oper-ROADMB.xml'},
    'roadmc': {'port': '17833', 'configfile': 'oper-ROADMC.xml'},
    'xpdrc': {'port': '17834', 'configfile': 'oper-XPDRC.xml'},
    'roadma-full': {'port': '17821', 'configfile': 'oper-ROADMA-full.xml'},
    'roadmc-full': {'port': '17823', 'configfile': 'oper-ROADMC-full.xml'}
}

honeynode_executable = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "1.2.1", "honeynode-simulator", "honeycomb-tpce")
samples_directory = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "1.2.1")


def start_sim(sim):
    if os.path.isfile(honeynode_executable):
        with open(sim+'.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, sims[sim]['port'], os.path.join(samples_directory, sims[sim]['configfile'])],
                stdout=outfile, stderr=outfile)


def start_tpce():
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        print("starting LIGHTY.IO TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "lighty", "target", "tpce",
            "clean-start-controller.sh")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable], stdout=outfile, stderr=outfile, stdin=None)
    else:
        print("starting KARAF TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "karaf", "target", "assembly", "bin", "karaf")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable, "server"], stdout=outfile, stderr=outfile, stdin=None)
