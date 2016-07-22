#!/usr/bin/env bash

baseDir=$(dirname "$0")

#For later implementation
function cleanup() {
echo "Inside Clean up.."
}

#For later implementation
function buildSamples() {
echo "Inside build samples.."
}

if [ "$1" = "build" ]
then
    cleanup
    buildSamples
else
    cleanup
    echo $baseDir
    $baseDir/excecute-tests.sh "$1"
fi

if [ ! "$?" = 0 ]
then
    echo "Test were not completed successfully"
    echo "Cleaning up.."
    cleanup
fi
