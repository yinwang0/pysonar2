<img src="https://travis-ci.org/yinwang0/pysonar2.svg?branch=master">

# PySonar2 - a type inferencer for Python

PySonar2 is a type inferencer for Python. It performs whole-project interprocedural analysis to
infer types.

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>


### Get the code

    git clone https://github.com/yinwang0/pysonar2.git
    git checkout tags/v2.1.2


### How to build

    mvn package


### How to use

You can build a simple "code-browser" of the Python 2.7 standard library with
the following command line:

    java -jar target/pysonar-<version>.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.

Note that this is just a demo program. PySonar2 is not an end-user tool.
It is mainly designed as a library for Python IDEs, other developer
tools and code search engines, so its interface may not be as appealing as an
end-user tool.


### System requirements

* Python 2.7.x
* Python 3.x
* Java 8+
* maven



### Environment variables

PySonar2 uses CPython's ast package to parse Python code, so please make sure
you have `python` or `python3` installed and pointed to by the `PATH`
environment variable. If you have them in different names, please make symbol
links.

`PYTHONPATH` environment variable is used for locating the Python standard
libraries. It is important to point it to the correct Python library, for
example

    export PYTHONPATH=/usr/lib/python2.7

If this is not set up correctly, references to library code will not be found.



### Development

If you hope to contribute to PySonar2, please contact me first before making big changes.

For development, you can run the unit tests. PySonar2 has a test framework. You can run the
tests using this command:

    mvn test

If you modify the code or tests, you need to generate new expected results. Run these command lines:

    mvn package -DskipTests
    java -classpath target/pysonar-<version>.jar org.yinwang.pysonar.TestInference -generate tests

To write new tests, you just need to write relevant Python files, put them into a directory named 
`tests/testname.test`(test directory name must end with ".test"). Please look at the `tests` 
directory for examples.


### License

Apache 2.0 License. See LICENSE file.
