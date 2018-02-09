#!/usr/bin/env bash

if [[ $# -eq 0 ]] ; then
    echo 'must provide a workspace as argument'
    exit 1
fi

WORKSPACE=$1
SRC=$(dirname $(readlink -f $0))

rm -rf $WORKSPACE/logs
mkdir -p $WORKSPACE/logs

MVN="docker run -it --rm -v $SRC:/src -v $WORKSPACE/.m2:/root/.m2 flexio-build-java mvn"

STAGE=build
STAGE_OUT=$WORKSPACE/logs/$STAGE.stdout.log
STAGE_ERR=$WORKSPACE/logs/$STAGE.stderr.log

$MVN clean install -DskipTests -Ddocker.resource.docker.url=http://172.17.0.1:2375 > >(tee -a $STAGE_OUT) 2> >(tee -a $STAGE_ERR >&2)
RESULT=$?
if [ "$RESULT" -ne 0 ]
then
    echo "stage $STAGE exec 1 failure"
    exit $RESULT
fi

echo "$STAGE STAGE EXIT : $RESULT"
exit $RESULT