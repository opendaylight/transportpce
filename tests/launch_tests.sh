#!/bin/sh

show_help() {
    cat <<'EOF'
Usage:
  launch_tests.sh <test-suite> [test-name ...] [--stop-at <test-method>]
  launch_tests.sh -h | --help

Description:
  Execute transportpce functional tests selected by suite and optional
  test file name fragments.

  If no test names are given, all matching test files in the suite are run.

  If --stop-at is provided, tests are collected in execution order and run
  only up to and including the specified test method.

Arguments:
  <test-suite>
      Test suite directory under transportpce_tests/
      (e.g. tapi, olm, pce, renderer)

  <test-name>
      Optional test file suffix (without numeric prefix or .py).
      Example:
        abstracted_topology
      matches:
        transportpce_tests/<test-suite>/test01_abstracted_topology.py

Options:
  --stop-at <test-method>
      Stop execution after the specified test method has completed.

  -h, --help
      Show this help text and exit.

Notes:
  When using tox, arguments must be passed after '--':
    tox -e tests_tapi -- <args>

Examples:
  Run all tests in the tapi suite:
    tox -e tests_tapi -- tapi

  Run a specific test file:
    tox -e tests_tapi -- tapi abstracted_topology

  Run tests up to and including a specific method:
    tox -e tests_tapi -- tapi abstracted_topology \
      --stop-at test_12_connect_roadma_pp1_to_xpdra_n1
EOF
}

if [ -n "$USE_ODL_ALT_KARAF_ENV" ]; then
    echo "using environment variables from $USE_ODL_ALT_KARAF_ENV"
    . "$USE_ODL_ALT_KARAF_ENV"
fi

test_suite=""
arglist=""
stop_at=""

while [ $# -gt 0 ]; do
    case "$1" in
        -h|--help)
            show_help
            exit 0
            ;;
        --stop-at)
            if [ $# -lt 2 ] || [ -z "$2" ]; then
                echo "Error: --stop-at requires a test method name" >&2
                echo "Try --help for usage." >&2
                exit 1
            fi
            stop_at="$2"
            shift 2
            ;;
        *)
            if [ -z "$test_suite" ]; then
                test_suite="$1"
            else
                if [ "$1" = "../olm" ]; then
                    arglist="$arglist olm"
                elif [ "$1" = "../pce" ]; then
                    arglist="$arglist pce"
                elif [ "$1" = "../renderer" ]; then
                    arglist="$arglist renderer"
                else
                    arglist="$arglist $1"
                fi
            fi
            shift
            ;;
    esac
done

if [ -z "$test_suite" ]; then
    echo "Error: missing test suite" >&2
    echo "Try --help for usage." >&2
    exit 1
fi

scriptlist=""
if [ -z "$arglist" ]; then
    scriptlist="transportpce_tests/$test_suite/test[0-9][0-9]_*.py"
else
    for test_name in $arglist; do
        scriptlist="$scriptlist transportpce_tests/$test_suite/test[0-9][0-9]_$test_name.py"
    done
fi

if [ -z "$LAUNCHER" ]; then
    if command -v pytest-3 >/dev/null 2>&1; then
        LAUNCHER="pytest-3"
    elif command -v pytest >/dev/null 2>&1; then
        LAUNCHER="pytest"
    else
        LAUNCHER="python3 -m pytest"
    fi

    if [ -d "allure-report" ]; then
        LAUNCHER="$LAUNCHER --alluredir=allure-report/"
    fi

    LAUNCHER="$LAUNCHER -q"
fi

selected_tests=""
stop_reached=0

for script in $scriptlist; do
    resolved_script=$(ls $script 2>/dev/null) || exit 1

    if [ -z "$stop_at" ]; then
        selected_tests="$selected_tests $resolved_script"
        continue
    fi

    collected=$($LAUNCHER --collect-only "$resolved_script" 2>/dev/null) || exit 1

    old_ifs=$IFS
    IFS='
'
    for nodeid in $collected; do
        case "$nodeid" in
            *"::test_"*)
                selected_tests="$selected_tests $nodeid"
                method_name=${nodeid##*::}
                if [ "$method_name" = "$stop_at" ]; then
                    stop_reached=1
                    break
                fi
                ;;
        esac
    done
    IFS=$old_ifs

    if [ "$stop_reached" -eq 1 ]; then
        break
    fi
done

if [ -n "$stop_at" ] && [ "$stop_reached" -eq 0 ]; then
    echo "Requested stop test '$stop_at' was not found" >&2
    exit 1
fi

echo "$LAUNCHER $selected_tests"
# shellcheck disable=SC2086
$LAUNCHER $selected_tests || exit 1
