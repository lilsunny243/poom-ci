#!/usr/bin/env bash

if [[ $# -eq 0 ]] ; then
    echo 'must provide a workspace as argument'
    exit 1
fi

WORKSPACE=$1
SRC=$(dirname $(readlink -f $0))
mkdir -p $WORKSPACE/M2

rm -rf $WORKSPACE/logs
mkdir -p $WORKSPACE/logs

X="x-value"
Y="y-value"
Z="z-value"

STAGE=first
STAGE_OUT=$WORKSPACE/logs/$STAGE.stdout.log
STAGE_ERR=$WORKSPACE/logs/$STAGE.stderr.log

A > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

B > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 2 failure"
    exit $RESULT
fi

STAGE=second
STAGE_OUT=$WORKSPACE/logs/$STAGE.stdout.log
STAGE_ERR=$WORKSPACE/logs/$STAGE.stderr.log

C > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

echo "PIPELINE EXIT : $RESULT"
exit $RESULT