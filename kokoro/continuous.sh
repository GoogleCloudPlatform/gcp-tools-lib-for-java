#!/bin/bash

# Fail on any error.
set -e
# Display commands to stderr.
set -x

cd github/appengine-plugins-core
sudo /opt/google-cloud-sdk/bin/gcloud components update
sudo /opt/google-cloud-sdk/bin/gcloud components install app-engine-java
mvn clean install -B -U