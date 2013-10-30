#!/bin/bash

echo Downloading Jython...
hg clone http://hg.python.org/jython

echo Removing PySonar 1.0...
rm -rf jython/tests/java/org/python/indexer
rm -rf jython/src/org/python/indexer/*

echo Moving PySonar2 into src/org/python/indexer
mv * jython/src/org/python/indexer

echo Buidling
cd jython
ant jar-complete

echo Please find PySonar2 inside 'dist/jython.jar'
echo To run demo:
echo java -classpath dist/jython.jar org.python.indexer.demos.HtmlDemo /usr/lib/python2.7 /usr/lib/python2.7

