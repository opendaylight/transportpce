.. _transportpce-tox-guide:

TransportPCE tox Guide
======================

What is tox?
------------

`Tox` is a tool written in Python to ease tests automation and dependencies management.
It provides a command line tool that run tests inside a `Python virtual environment <https://docs.python.org/3/glossary.html#term-virtual-environment>`_.

This means that it will not modify your local system settings or package and executable files.
Instead, it uses a hidden folder (`.tox`) to install the required Python dependencies via pip
(`the package installer for Python <https://pip.pypa.io/>`_) before running tests.

You can fin more details about tox at https://github.com/tox-dev/tox .

This makes tox configuration and behavior very portable across GNU+Linux distributions
and others UNIX-like systems or environments.
`Tox` is often used as a front-end to Continuous Integration servers.

For instance, `Linux Foundation globaljjb <https://globaljjbdocstest.readthedocs.io/en/latest/jjb/lf-python-jobs.html#tox-verify>`_
provides a gerrit-tox-verify profile.
This profile is `used by TransportPCE <https://git.opendaylight.org/gerrit/c/releng/builder/+/78656>`_
in OpenDaylight Jenkins CI.

Once tox installed in your local environment,::

    $ sudo apt-get install tox

for example on Debian based systems or on Red Hat based systems::

    $ sudo yum install python-tox

the same test suite than the CI can be run locally by simply calling the tox shell command
inside your project git clone local folder::

    $ tox

This will run every tox profiles listed in the `envlist` parameter of the `tox.ini` file.

Tox configuration
-----------------

`Tox` configuration is written in the `tox.ini` file at the root folder of the Git project.
Please read `tox official documentation <https://tox.readthedocs.io/>`_ for more details.
For tox users, the most important parameter in the `[tox]` section is `envlist`.
It specifies which profiles to run by default (i.e. when tox is called without the option `-e`).
The option `-e` overrides this parameter and allows to choose which profiles to run.
For example::

    $ tox -e gitlint

will only run the `gitlint` profile.
And::

    $ tox -e gitlint,checkbashisms

will run the `gitlint` and `checkbashisms` profiles.

Profiles configuration are described in the `[testenv]` section.
Each profile specificities are usually configured in a subsection, for example the
subsection `[testenv:gitlint]` for the configuration of `Gitlint <https://jorisroovers.com/gitlint/>`_.

Docs profiles
-------------

The `docs` profile itself is used to generate the documentation of the project in HTML from the sources.
The sources are usually written in the reStructuredText format a.k.a. RST and located in the docs folder.
For this purpose, this profiles calls the sphinx Python package.
More details can be found at https://www.sphinx-doc.org/ .

`Sphinx` is also used to check the validity of the URLs present in the sources by the `docs-linkcheck` profile.
False positive can be declared in the sphinx configuration file (usually `docs/conf.py`).

A third profile called `spelling` and based on `Sphinx` and `PyEnchant <https://pyenchant.github.io/pyenchant/>`_
can also be used as a spellchecker.

Linter profiles
---------------

A few linter are also provided from tox.

* `gitlint`. Check that the last commit message is well formatted.
* `pylint`. Lint Python tests scripts
* `autopep8`. Autoformat Python tests scripts according to PEP8 standard rules.
* `pyang`. Lint YANG files.
* `pyangformat`. Autoformat YANG files.
* `checkbashisms`. Detect bashisms in shell scripts.


Pre-commit profiles
-------------------

`Pre-commit <https://pre-commit.com/>`_ is another wrapper for linters that relies on `Git Hooks <https://githooks.com/>_`.
These hooks can be run automatically when the command 'git commit' is called .
`Pre-commit` configuration can be found in the `.pre-commit-config.yaml` file at the Git project root.
It is particularly useful to address common programming issues such as triming trailing whitespaces or
removing tabs.

The `pre-commit` profiles allows to call pre-commit inside tox virtualenv without installing pre-commit
package in the local system, what is pretty convenient.
This is also true to install/uninstall the corresponding Git hooks in your Git folder thanks to the
profiles `pre-commit-install` and `pre-commit-uninstall`.

Functional tests profiles
-------------------------

TransportPCE functional tests are Python scripts that allow to perform blackbox testing on a
controller instance.
They do not need tox to be performed locally and can be called directly from the shell or with
a launcher such as `nosetests <https://nose.readthedocs.io/>`_
(often available in Linux distributions packages under the name `python-nose`).
Currently, they require the presence of `Honeynode simulators <https://gitlab.com/Orange-OpenSource/lfn/odl/honeynode-simulator>`_
and the modification of the controller default OLM timers to speed-up the tests.
They are also supposed to be called within the tests folder::

    $ cd tests/

These tests have been spread over several directories in `tranportpce_tests/` mostly named
on their corresponding OpenROADM device version.
Tests scripts files names are also numbered so that they are performed in a certain order.
To ease their integration in tox, a script launch_tests.sh can be used to call them.
For example, the following command::

    $ ./launch_tests.sh pce

will call by default all the tests in the folder tests/transportpce_tests/pce with nose.
And the command::

    $ ./launch_tests.sh 1.2.1 portmapping

is equivalent to::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test01_portmapping.py

Several tests can be listed in the arguments. For example::

    $ ./launch_tests.sh 1.2.1 portmapping topology

is equivalent to::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test01_portmapping.py
    $ nosetests --with-xunit transportpce_tests/1.2.1/test03_topology.py

Also, some shell environment variables can be used to modify their default behavior.
For example the commands::

    $ export LAUNCHER="python3"
    $ ./launch_tests.sh 2.2.1

are equivalent to::

    $ python3 transportpce_tests/2.2.1/test01_portmapping.py

And::

    $ export LAUNCHER="nosetests"
    $ export USE_LIGHTY="True"
    $ ./launch_tests.sh 7.1

is equivalent to::

    $ nosetests transportpce_tests/2.2.1/test01_portmapping.py

but will ask tests script to use the controller lighty.io build rather than Karaf.

These variables are also understood inside tox virtualenv thanks to the `passenv` parameter
configured in `tox.ini`.

Tox TransportPCE functional tests support is split into several tox profiles.
Strictly spoken, only the following profiles performs functional tests as described above:
* `testsPCE`. To evaluate the Path Computation behavior.
* `tests121`. To evaluate the support of OpenROADM devices version 1.2.1 .
* `tests221`. To evaluate the support of OpenROADM devices version 2.2.1 .
* `tests71`. To evaluate the support of OpenROADM devices version 7.1 .
* `tests_hybrid`. To evaluate the controller behavior in a mixed environment with several versions of OpenROADM devices.
* `gnpy`. To evaluate the controller behavior when used in conjunction with `GNPy <https://github.com/Telecominfraproject/oopt-gnpy>`_. Requires `docker <https://www.docker.com/>`_.
* `nbinotifications`. To evaluate the controller north-bound interface notifications support. Requires `docker <https://www.docker.com/>`_.

Each of this profile depends on the `buildcontroller` profile, which is simply
there to build the controller from sources and adapt OLM default timers.
They can also depend on `sims121` or `sims221` or `sims71` profiles to download
a simulator of OpenROADM devices if needed.

The `tox.ini` depend parameter allows tox to establish the most efficient tests
order strategy  when calling tox without the `-e` option.
This is particularly important when the parallelized mode is enabled.
If tox is called locally with the option `-e`, profiles not specified to this
option but listed in the depends parameters are simply ignored.
This means you have to specify manually the `buildcontroller` or `simsXXX` profiles if the controller was not build yet
or the sims were not downloaded previously. For example::

    $ tox -e buildcontroller,sims121,tests121

will build the controller and download simulators before running functional tests
for OpenROADM devices 1.2.1.
Once that done, you only need to list the others sims versions profiles before
lauching hybrid tests::

    $ tox -e sims221,sims71,tests_hybrid

Also the same way arguments can be passed to the launch_tests.sh script,
tests names can be passed as argument when calling the corresponding tox profiles.

For example:

    $  tox -e tests121 portmapping

will launch by default the following command inside tox virtual environment::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test01_portmapping.py

And::

    $  tox -e tests121 "portmapping topology"

will perform::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test01_portmapping.py
    $ nosetests --with-xunit transportpce_tests/1.2.1/test03_topology.py

Note the necessity to use quotes here when listing several test names.
If you need to test the portmapping behavior for all OpenROADM devices versions::

    $  tox -e tests121,tests221,tests71 portmapping

will perform::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test01_portmapping.py
    $ nosetests --with-xunit transportpce_tests/2.2.1/test01_portmapping.py
    $ nosetests --with-xunit transportpce_tests/7.1/test01_portmapping.py

Idem for OLM for only OpenROADM devices versions 1.2.1 and 2.2.1 ::

    $  tox -e tests121,tests221 olm

will perform::

    $ nosetests --with-xunit transportpce_tests/1.2.1/test05_olm.py
    $ nosetests --with-xunit transportpce_tests/2.2.1/test08_olm.py

Profiles parrallelization
-------------------------

Tox Profiles execution can be parallelized.
CI behavior can be configured from the `releng/builder` repository.
This is `the current configuration <https://git.opendaylight.org/gerrit/c/releng/builder/+/96557>`_
in TransportPCE CI.

Locally, tox jobs are not paralllized by default.
You have to use the `-p` option to specify the level of concurrency::

    $  tox -p

or::

    $  tox -p auto

or::

    $  tox -p 2

The default parameter "auto" is based on the number of CPU cores,
which is a bad idea for TransportPCE functional tests.
Their most critical ressource is RAM, mostly because of the need
to launch several simulators.
Unfortunately, "auto" is historically the only option available
in OpenDaylight CI configuration.
To palliate this problem, `tox.ini` current configuration uses the `depends` parameter
to artifically chain tests profiles and limit to only 2 the number of controller instances
run in parallel.

Also, the default display will change from the classical sequence mode.
You need to use the option `-o` to get it back.
This is the default configuration in the CI.

    $  tox -o -p 2

Running differents tests in parallel also creates concurrency access problems
to others ressources than RAM, mostly the port to listen to, and the log files.
To this sake, lighty.io and karaf build have be customized to configure
listening ports and log files from shell variables environment.
This variable environments are also understood by Python tests scripts and tox.

You can take a look at the following Gerrit changes for more details
https://git.opendaylight.org/gerrit/q/topic:%2522parallel%2522+project:transportpce
And particularly at
https://git.opendaylight.org/gerrit/c/transportpce/+/96696
and
https://git.opendaylight.org/gerrit/c/transportpce/+/96662
and
https://git.opendaylight.org/gerrit/c/transportpce/+/96663

As a consequence, if your computer environment has enough RAM and CPU cores,
it is perfectly possible to run in parallel 3 TranportPCE portmapping functional tests
for example with the following command::

    $  tox -p 3 -e buildcontroller,sims121,sims221,sims71,tests121,tests221,tests71 portmapping
