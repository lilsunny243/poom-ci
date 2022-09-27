#!/usr/bin/env bash

IMAGE=localhost:5000/codingmatters/poom-service-base-with-build-tools-and-docker-17-alpine:2.6.0-dev

#  -v /home/nel/workspaces/codingmatters-low-level/codingmatters-parent:/src \
#  -v /home/nel/workspaces/flexio-top-level/flexio-parent:/src \
docker run --rm \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v /home/nel/.docker:/.docker \
  -v /home/nel/workspaces/codingmatters-poom/flexio-commons:/src \
  -v /home/nel/.m2:/m2 \
  -w /wkdir \
  $IMAGE \
  mvn -f /src/pom.xml -Dmaven.repo.local=/m2 clean install "-Ddocker.client.impl=io.flexio.docker.cmd.CommandLineDockerClient" "-Ddocker.client.config.path=/.docker"
