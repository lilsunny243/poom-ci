FROM verdaccio/verdaccio:4.0.2

USER root
RUN npm install commander semver lodash express async request http-errors body-parser minimatch mkdirp @verdaccio/streams verdaccio-profile-api

COPY --chown=verdaccio:root config.yaml /verdaccio/conf/config.yaml
COPY --chown=verdaccio:root .htpasswd /verdaccio/conf/.htpasswd
USER verdaccio
