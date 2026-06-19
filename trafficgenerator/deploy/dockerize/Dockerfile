FROM eclipse-temurin:17-jre-jammy
WORKDIR /trafficgenerator

# copy jars. 'other' jars are log4j, json, ...
COPY ./build/target/*.jar /trafficgenerator/lib/
COPY ./build/other_jars/*.jar /trafficgenerator/lib/

# java class path. Docker will export these
# NOTE: the trafficgenerator-release.jar must be soft-linked to the actual release version (eg, -2.7.0.0.jar) using 'ln -s ... so that it can be referenced in the CLASSPATH'
RUN ln -s /trafficgenerator/lib/trafficgenerator-2.7.0.0-SNAPSHOT.jar /trafficgenerator/lib/trafficgenerator-release.jar
ENV CLASSPATH="/trafficgenerator/lib/trafficgenerator-release.jar:/trafficgenerator/lib/log4j-api-2.17.1.jar:/trafficgenerator/lib/json-20220320.jar:/trafficgenerator/lib/log4j-core-2.17.1.jar"

# Options provided by trafficgenerator 
# '-a' == account/site name
# '-d' == data dir path
# '-l' == message level (info/debug/warn/error)
# '-r' == realm (staging | prod ) (default staging)
# '-t' == testData (true | false) (for pixel) (default true)
# '-e' == env type ('dev' | 'qa' | 'release') (default dev)
# '-p' == pixelDebug (true | false) (for pixel debug via Event manager) (default false)
# '-c' == use curated_searchterms (defined in journeydata/CuratedSearchterms.tsv - this file must exist)

# NOTE: mount ./data via "docker run -mount ..." command. It in turn contains individual sandbox_* configs etc
# Each sandbox's data (eg, search-terms, widget info, ...) are kep separate from actual code.
# This docker image contains only the code, not sandbox data so that accts can be 
# added/deleted w/o changing the docker image
ENTRYPOINT ["java", "com.bloomreach.trafficgenerator.Generator", "-d", "./data", "-r", "production", "-e", "release", "-t", "false", "-p", "false", "-l", "warn"]

# default 'app'. The '-a' option must be overridden via command line argument
CMD ["-a", "UNDEFINED"]

