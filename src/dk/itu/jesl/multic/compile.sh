#!/bin/bash
BASEDIR="/Users/jesl/javautil"
MULTICDIR="/Users/jesl/javautil/src/dk/itu/jesl/multic"
JAVAFILES="$( find $MULTICDIR -name '*.java' -print )"
javac -Xlint:unchecked -sourcepath $BASEDIR/src -classpath $BASEDIR/bin -d $BASEDIR/bin $JAVAFILES
