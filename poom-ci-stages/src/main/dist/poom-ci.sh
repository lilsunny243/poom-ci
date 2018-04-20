#!/usr/bin/env bash

SCRIPTDIR=$(dirname $(readlink -f $0))
echo "running poom-ci script version ${project.version} in $SCRIPTDIR"

java -cp $SCRIPTDIR/${project.artifactId}-${project.version}-uber.jar org.codingmatters.poom.ci.pipeline.GeneratePipelineScript "$@"