FROM eclipse-temurin:17-jre-jammy
WORKDIR /trafficgenerator

# copy jars
COPY ./build/target/*.jar /trafficgenerator/lib/
COPY ./build/other_jars/*.jar /trafficgenerator/lib/
COPY ./check_image.sh .

# copy run scripts (eg, deployrun_release.sh) -- not needed when trafficGenerator is run using 'docker run'
# COPY ./deploy/cloud/scripts/*.sh /trafficgenerator

# java class path. Docker will export these
ENV CLASSPATH="/trafficgenerator/lib/trafficgenerator-2.3.7.3-X-SNAPSHOT.jar:/trafficgenerator/lib/log4j-api-2.17.1.jar:/trafficgenerator/lib/json-20220320.jar:/trafficgenerator/lib/log4j-core-2.17.1.jar"

# ./data must have been mounted before running docker run...
# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)
# '-r' == realm (staging | prod ) (default staging)
# '-t' == testData (true | false) (for pixel) (default true)
# '-e' == env type ('dev' | 'qa' | 'release') (default dev)
# '-p' == pixelDebug (true | false) (for pixel debug via Event manager) (default false)

### -- default staging
#ENTRYPOINT ["java", "com.bloomreach.trafficgenerator.Generator", "-d", "./data", "-r", "staging", "-e", "dev", "-t", "true", "-p", "false", "-l", "debug"]

### -- default prod
ENTRYPOINT ["java", "com.bloomreach.trafficgenerator.Generator", "-d", "./data", "-r", "prod", "-e", "release", "-t", "false", "-p", "false", "-l", "warn"]

# default 'app'. Must be overridden via command line argument
CMD ["-a", "UNDEFINED"]

