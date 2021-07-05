#!/bin/sh

if [ "$USE_LIGHTY" != "True" ]; then
    ./build_karaf_for_tests.sh
fi

for arg in $@; do
    if [ -z "$test_suite" ]; then
        test_suite=$1
    else
# Tox reinterprets "olm" and "pce" by "../olm" "../pce" because of the
# changedir directive and the presence of modules folders "olm" and pce"
# at project root.
        if [ "$arg" = "../olm" ]; then
            arglist=$arglist" olm"
        elif [ "$arg" = "../pce" ]; then
            arglist=$arglist" pce"
        else
            arglist=$arglist" "$arg
        fi
    fi
done

scriptlist=""
if [ -z "$arglist" ]; then
    scriptlist="transportpce_tests/$test_suite/test[0-9][0-9]_*.py"
else
    for test in $arglist; do
        scriptlist=$scriptlist" transportpce_tests/$test_suite/test[0-9][0-9]_$test.py"
    done
fi
if [ -z "$LAUNCHER" ]; then
    LAUNCHER="nosetests --with-xunit";
fi

for script in $scriptlist; do
    echo $LAUNCHER $(ls $script)
    $LAUNCHER $script || exit 1
done
