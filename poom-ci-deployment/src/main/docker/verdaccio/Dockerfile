FROM verdaccio/verdaccio

RUN npm install semver lodash express async request http-errors body-parser minimatch mkdirp @verdaccio/streams verdaccio-profile-api

USER verdaccio
COPY --chown=verdaccio config.yaml /verdaccio/conf/config.yaml
COPY --chown=verdaccio .htpasswd /verdaccio/conf/.htpasswd


