#!/bin/sh

DOCKERFILE=Dockerfile.Payara5
BASEDIR=$(dirname "$0")

if [ -n "$1" ]; then
	DOCKERFILE=Dockerfile.$1
fi

docker build -t payaratest/war -f "$BASEDIR/$DOCKERFILE" "$BASEDIR"
