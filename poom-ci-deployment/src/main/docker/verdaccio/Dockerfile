FROM verdaccio/verdaccio:4.0.2

RUN npm install semver lodash express async request http-errors body-parser minimatch mkdirp @verdaccio/streams verdaccio-profile-api

USER verdaccio
COPY --chown=verdaccio config.yaml /verdaccio/conf/config.yaml
COPY --chown=verdaccio .htpasswd /verdaccio/conf/.htpasswd


CMD [ "chown -R vrdaccio:verdaccio /verdaccio &&\
exec guso verdaccio:verdacio  $VERDACCIO_APPDIR/bin/verdaccio --config /verdaccio/conf/config.yaml --listen $VERDACCIO_PROTOCOL://0.0.0.0:${VERDACCIO_PORT}" ]