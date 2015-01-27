#!/bin/bash
BASEDIR="/Users/ae1925/javautil"
MULTICDIR="/Users/ae1925/javautil/src/dk/itu/jesl/multic"
JAVAFILES="$( find $MULTICDIR -name '*.java' -print )"
javac -Xlint:unchecked -sourcepath $BASEDIR/src -classpath $BASEDIR/bin -d $BASEDIR/bin $JAVAFILES
