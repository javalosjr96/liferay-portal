#!/bin/bash

CURRENT_DIR_NAME=$(dirname ${BASH_SOURCE[0]})

echo CURRENT_DIR_NAME=${CURRENT_DIR_NAME}

function main {
  cd ${CURRENT_DIR_NAME}

  cd ant

  ls -a

  echo "Database Upgrade Client Reset"
}

main "${@}"