#!/usr/bin/env bash
set -e

docker run --cap-add CHECKPOINT_RESTORE --cap-add SYS_ADMIN --rm -p 8080:8085 --name reference-crac reference-crac:checkpoint
