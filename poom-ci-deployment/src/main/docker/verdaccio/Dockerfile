FROM verdaccio/verdaccio:4.0.2

RUN mkdir -p /opt/verdaccio
RUN chown -R verdaccio /opt/verdaccio

RUN npm install semver lodash express async request http-errors body-parser minimatch mkdirp @verdaccio/streams verdaccio-profile-api

USER verdaccio
COPY --chown=verdaccio config.yaml /verdaccio/conf/config.yaml
COPY --chown=verdaccio .htpasswd /verdaccio/conf/.htpasswd


