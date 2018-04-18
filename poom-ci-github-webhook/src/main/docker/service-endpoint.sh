#!/usr/bin/env sh

echo "starting service..."
export SERVICE_HOST=$(hostname -i)
java -cp "/var/service/lib/*:/var/service/config" "$@"
echo "service stopped"