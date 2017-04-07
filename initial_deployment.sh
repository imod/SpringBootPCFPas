#!/bin/sh
cf push yoo-actuator-test-green -n yoo-actuator-test-green -p ./target/springbootpcfpas-0.0.1-SNAPSHOT.jar -f manifest.yml -i 1 -t 180 --no-start
cf start yoo-actuator-test-green
cf map-route yoo-actuator-test-green ci-actuator.yooture.info
cf map-route yoo-actuator-test-green cfapps.io -n yoo-actuator-test
cf delete-orphaned-routes -f