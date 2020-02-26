# those options are needed to remove JDK11 reflective access warnings at build and run time
# GROOVY_TURN_OFF_JAVA_WARNINGS=true cannot be passed directly
# see https://dev.to/erichelgeson/removing-illegal-reflective-access-warnings-in-grails-4-393o

export JDK_JAVA_OPTIONS="
 --add-opens=java.base/java.io=ALL-UNNAMED
 --add-opens=java.base/java.lang=ALL-UNNAMED
 --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
 --add-opens=java.base/java.lang.reflect=ALL-UNNAMED
 --add-opens=java.base/java.net=ALL-UNNAMED
 --add-opens=java.base/java.nio=ALL-UNNAMED
 --add-opens=java.base/java.nio.charset=ALL-UNNAMED
 --add-opens=java.base/java.nio.file=ALL-UNNAMED
 --add-opens=java.base/java.util=ALL-UNNAMED
 --add-opens=java.base/java.util.jar=ALL-UNNAMED
 --add-opens=java.base/java.util.stream=ALL-UNNAMED
 --add-opens=java.base/java.util.zip=ALL-UNNAMED
 --add-opens java.base/sun.nio.ch=ALL-UNNAMED
 --add-opens java.base/sun.nio.fs=ALL-UNNAMED
 -Xlog:disable"
# --illegal-access=permit #default option, can be set to warn to retrieve all details

