.. _transportpce-tox-guide:

TransportPCE Tox Guide
======================

What is tox?
------------

Tox is a tool written in Python to ease tests automation and dependencies management.
It provides a command line tool that run tests inside a Python virtual environment.
https://docs.python.org/3/glossary.html#term-virtual-environment
This means that it will not modify your local system settings and package or executable files
but uses a hidden folder (.tox) to install the required python dependencies via pip
(the package installer for Python https://pip.pypa.io/) before running tests.

You can fin more details about tox at https://tox.readthedocs.io/

This makes tox configuration and behavior very portable across GNU+Linux distributions
and others UNIX-like systems or environments.
Tox is often used as a front-end to Continuous Integration servers.

For instance, Linux Foundation globaljjb provides a gerrit-tox-verify profile that
is used by TransportPCE in OpenDaylight Jenkins CI.
https://globaljjbdocstest.readthedocs.io/en/latest/jjb/lf-python-jobs.html#tox-verify
https://git.opendaylight.org/gerrit/c/releng/builder/+/78656

Once installed in your local environment, the same test suite can be run locally by
simply running tox inside your project git clone local folder::

    $ sudo apt-get install tox
    $ tox

This will run every tox profiles listed in the envlist parameters of the tox.ini file.

Tox configuration
-----------------

Tox configuration is written in the tox.ini file at the root folder of the Git project.
Please read tox official documentation for more details.
The main parameters in the [tox] section is envlist that specifies which profiles to run
by default when tox is called without the option -e.
The option -e overrides this setting and allows to choose the profiles to run.
For example::

    $ tox -e gitlint

will only run the gitlint profile.
And::

    $ tox -e gitlint,checkbashisms

will run gitlint and checkbashisms profiles.

Profiles configuration are described in the [testenv] section.
Each profile specificities are usually configured in a subsection, for example the
subsection  [testenv:gitlint] for gitlint configuration.

Docs profiles
-------------

The docs profile itself is used to generate the documentation of the project with sphinx from the sources
written in the reStructuredText format a.k.a. RST and located in the docs folder.
More details can be found at https://www.sphinx-doc.org/ .

Sphinx is also used to check the validity of the URLs present in the sources by the docs-linkcheck profile.
False positive can be declared in the sphinx configuration file (usually docs/conf.py).

A third profile called spelling and based on sphinx and PyEnchant can also be used as a spellchecker.

Linter profiles
---------------

A few linter are also provided from tox.

* gitlint. Check that the last commit message is well formatted.
* pylint. Lint Python tests scripts
* autopep8. Autoformat Python tests scripts according to PEP8 standard rules.
* pyang. Lint YANG files.
* pyangformat. Autoformat YANG files.
* checkbashisms. Detect bashisms in shell scripts.


Pre-commit profiles
-------------------

Pre-commit is another wrapper for linters that relies on GitHook (https://pre-commit.com/).
These hooks can be run automatically when the command 'git commit' is called .
Its configuration can be found in the .pre-commit-config.yaml file at the Git project root.
It is particularly useful to address common programming issues such as triming trailing whitespaces or
removing tabs.

The pre-commit profiles allows to call pre-commit inside tox virtualenv without installing it on the
local system. This is also true to install/uninstall the corresponding Git hooks thanks to the tox profiles
pre-commit-install and pre-commit-uninstall.

Functional tests profiles
-------------------------

TransportPCE functional tests are Python scripts that allow to perform blackbox testing on a
controller instance.
Thery are split into several tox profiles.
Strictly spoken, only the following profiles performs these tests
* testsPCE. To evaluate the Path Computation behavior.
* tests121. To evaluate the support of OpenROADM devices version 1.2.1 .
* tests221. To evaluate the support of OpenROADM devices version 2.2.1 .
* tests71. To evaluate the support of OpenROADM devices version 7.1 .
* tests_hybrid. To evaluate the controller behavior in a mixed environment with several versions of OpenROADM devices.

Each of this profile depend on the buildcontroller profile, which is simply there to build the controller from sources.
They can also depend on sims121 or sims221 or sims71 profiles to download a simulator of openroadm devices when required.
