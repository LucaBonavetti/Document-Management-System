#!/bin/bash

# Delete old generated sources if they already exist to avoid residual files
rm -rf ./out/

#
# Spring Generator Configuration-Parameters reference, see https://openapi-generator.tech/docs/generators/spring/
#
docker run \
  --name openapi-gen \
  -v ${PWD}:/local \
  openapitools/openapi-generator-cli generate \
  -i /local/openapi.yaml \
  -g spring \
  -p pocoModels=true \
  -p useSeperateModelProject=true \
  --artifact-id todomanager-rest \
  --group-id at.fhtw.swen \
  --additional-properties useTags=true \
  --package-name at.fhtw.swen.todomanager \
  --api-package at.fhtw.swen.todomanager.controller \
  --model-package at.fhtw.swen.todomanager.services.dto \
  --additional-properties configPackage=at.fhtw.swen.todomanager.config \
  --additional-properties basePackage=at.fhtw.swen.todomanager.services \
  --additional-properties useSpringBoot3=true \
  --additional-properties useJakartaEe=true \
  -o /tmp/out/

docker cp openapi-gen:/tmp/out/. ./out/

docker rm -f openapi-gen
