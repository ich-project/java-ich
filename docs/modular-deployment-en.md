# How to deploy java-ich after modularization

After modularization, java-ich is launched via shell script instead of typing command: `java -jar FullNode.jar`.

*`java -jar FullNode.jar` still works, but will be deprecated in future*.

## Download

```
git clone git@github.com:tronprotocol/java-ich.git
```

## Compile

Change to project directory and run:
```
./gradlew build
```
java-ich-1.0.0.zip will be generated in java-ich/build/distributions after compilation.

## Unzip

Unzip java-ich-1.0.0.zip
```
cd java-ich/build/distributions
unzip -o java-ich-1.0.0.zip
```
After unzip, two directories will be generated in java-ich: `bin` and `lib`, shell scripts are located in `bin`, jars are located in `lib`.

## Startup

Use the corresponding script to start java-ich according to the OS type, use `*.bat` on Windows, Linux demo is as below:
```
# default
java-ich-1.0.0/bin/FullNode

# using config file, there are some demo configs in java-ich/framework/build/resources
java-ich-1.0.0/bin/FullNode -c config.conf

# when startup with SR mode，add parameter: -w
java-ich-1.0.0/bin/FullNode -c config.conf -w
```

## JVM configuration

JVM options can also be specified, located in `bin/java-ich.vmoptions`:
```
# demo
-XX:+UseConcMarkSweepGC
-XX:+PrintGCDetails
-Xloggc:./gc.log
-XX:+PrintGCDateStamps
-XX:+CMSParallelRemarkEnabled
-XX:ReservedCodeCacheSize=256m
-XX:+CMSScavengeBeforeRemark
```