#!/usr/bin/env bash

SCRIPTDIR=$(dirname $(readlink -f $0))
HERE=$(pwd)

if [ $# -eq 0 ]
  then
    echo "usage: $0 <docker-compose arguments>"
fi

COORDINATES="${project.groupId}:${project.artifactId}:${project.version}"

cd $SCRIPTDIR
echo "driving compose for $COORDINATES from $(pwd)"

docker-compose "$@"

cd $HERE