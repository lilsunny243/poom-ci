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

export WORKSPACE=$WORKSPACE
export SRC=$SRC

export MVN="docker run -it --rm -v $SRC:/src -v $WORKSPACE/.m2:/root/.m2 flexio-build-java mvn"

STAGE=build

$MVN clean install -DskipTests -Ddocker.resource.docker.url=http://172.17.0.1:2375
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

echo "$STAGE STAGE EXIT : $RESULT"
exit $RESULT