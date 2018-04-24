#!/usr/bin/env sh

if [ ! -e "/var/archiva/conf/jetty.xml" ]
then
  echo "will initialize config dir with default files"
  cp -R  /opt/apache-archiva/conf/* /var/archiva/conf/
  chown -R archiva:archiva /var/archiva/conf/
  ls -l /var/archiva/conf/
fi


/opt/apache-archiva/bin/archiva "$@"