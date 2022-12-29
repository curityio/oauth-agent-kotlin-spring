#!/bin/bash

##############################################################
# Deploy the Curity Identity Server with the required settings
# This enables the OAuth Agent to be tested in isolation
##############################################################

RESTCONF_BASE_URL='https://localhost:6749/admin/api/restconf/data'
ADMIN_USER='admin'
ADMIN_PASSWORD='Password1'
IDENTITY_SERVER_TLS_NAME='Identity_Server_TLS'
PRIVATE_KEY_PASSWORD='Password1'

#
# Ensure that we are in the folder containing this script
#
cd "$(dirname "${BASH_SOURCE[0]}")"

#
# This is for Curity developers only
#
cp ./pre-commit ../../.git/hooks

#
# Check for a license file
#
if [ ! -f './license.json' ]; then
  echo "Please provide a license.json file in the test/idsvr folder"
  exit 1
fi

#
# Check for certificate files
#
if [ ! -f '../../certs/example.client.p12' ]; then
  echo "Please create development certificates before running this script"
  exit 1
fi

#
# Set an environment variable to reference the root CA used for the development setup
# This is passed through to the Docker Compose file and then to the config_backup.xml file
#
export FINANCIAL_GRADE_CLIENT_CA=$(openssl base64 -in "../../certs/example.ca.pem" | tr -d '\n')

#
# Run Docker to deploy the Curity Identity Server
#
docker compose --project-name tokenhandler up --detach --force-recreate --remove-orphans
if [ $? -ne 0 ]; then
  echo "Problem encountered starting Docker components"
  exit 1
fi

#
# Wait for the admin endpoint to become available
#
echo "Waiting for the Curity Identity Server ..."
while [ "$(curl -k -s -o /dev/null -w ''%{http_code}'' -u "$ADMIN_USER:$ADMIN_PASSWORD" "$RESTCONF_BASE_URL?content=config")" != "200" ]; do
  sleep 2
done
