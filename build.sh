# !/bin/bash

echo Downloading Jython...
hg clone http://hg.python.org/jython

echo Removing PySonar 1.0...
rm -rf jython/tests/java/org/python/indexer
rm -rf jython/src/org/python/indexer/*

echo Moving PySonar2 into src/org/python/indexer
mv * jython/src/org/python/indexer

echo Buidling
cd jython

# check ant dependency on mac os
if [ "$(uname)" == "Darwin" ]; then
    echo "Checking if ant is installed or not.";
    ant -version > /dev/null;
    if [ "$?" -ne 0 ]; then
        brew install ant;
        if [ $? -ne 0 ]; then 
            echo "Please install brew manually, see http://brew.sh";
            exit 1;
        fi 
    else
        echo "ant is installed";
    fi
fi
ant jar-complete

echo Please find PySonar2 inside 'dist/jython.jar'
echo To run demo:
echo java -classpath dist/jython.jar org.python.indexer.demos.HtmlDemo /usr/lib/python2.7 /usr/lib/python2.7




