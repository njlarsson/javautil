#!/bin/bash
DIR="$( cd "$( dirname "$0" )" && pwd )"
JAVAFILES="$( find $DIR -name '*.java' -print )"
TESTCLASSES="$( find . -name '*.java' -print | cut -d/ -f2- | cut -d. -f1 | sed 's/\//./g' | sed 's/^/dk.itu.jesl.hash./g' )"
javac -cp /Users/jesl/javautil/src:/Users/jesl/javautil/test:/Users/jesl/jars/junit-4.10.jar  -Xlint:unchecked *.java && \
java   -Djava.util.logging.config.file=logging.properties -cp /Users/jesl/javautil/src:/Users/jesl/javautil/test:/Users/jesl/jars/junit-4.10.jar org.junit.runner.JUnitCore $TESTCLASSES
