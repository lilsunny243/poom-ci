#!/usr/bin/env bash

if [[ $# -eq 0 ]] ; then
    echo 'must provide a workspace as argument'
    exit 1
fi

WORKSPACE=$1
SRC=$(dirname $(readlink -f $0))
if [[ $# -gt 1 ]] ; then
    SRC=$(readlink -f $2)
    echo "running $0 on $SRC"
fi

rm -rf $WORKSPACE/logs
mkdir -p $WORKSPACE/logs

X="x-value"
Y="y-value"
Z="z-value"

STAGE=first

A
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

B
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 2 failure"
    exit $RESULT
fi

STAGE=second

C
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

echo "PIPELINE EXIT : $RESULT"
exit $RESULT