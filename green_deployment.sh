#!/bin/sh
cf push yoo-actuator-test-blue -n yoo-actuator-test-blue -p ./target/springbootpcfpas-0.0.1-SNAPSHOT.jar -f manifest.yml -i 1 -t 180 --no-start

# create a temp route we can use to test if the app is up and running
cf map-route yoo-actuator-test-blue cfapps.io -n yoo-actuator-test-blue-temp

cf start yoo-actuator-test-blue

# wait until new version is online
until $(curl --output /dev/null --silent --head --fail https://yoo-actuator-test-blue-temp.cfapps.io); do
    printf '.'
    sleep 5
done

# route traffic to the new app version
cf map-route yoo-actuator-test-blue ci-actuator.yooture.info
cf map-route yoo-actuator-test-blue cfapps.io -n yoo-actuator-test

# remove the old app version
cf unmap-route yoo-actuator-test-green cfapps.io -n yoo-actuator-test
cf unmap-route yoo-actuator-test-green ci-actuator.yooture.info
cf delete yoo-actuator-test-green -f
cf delete-orphaned-routes -f
