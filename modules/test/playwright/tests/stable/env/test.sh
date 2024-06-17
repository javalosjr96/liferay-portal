#!/bin/bash

CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})

echo CURRENT_DIR_NAME=${CURRENT_DIR_NAME}

function main {
  cd ${CURRENT_DIR_NAME}

  cd ant

  target="clean-database-upgrade-client"

  ant -f build-test-db-upgrade-client.xml "$1"

  echo "Task: $1 complete"
}

main "${@}"