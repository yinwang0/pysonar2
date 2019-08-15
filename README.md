<img src="https://travis-ci.org/yinwang0/pysonar2.svg?branch=master">

# PySonar2 - an advanced semantic indexer for Python

PySonar2 is a semantic indexer library for Python, designed for batch processing of large code
bases.

To create high accuracy and quality index of Python, which is very dynamic language, PySonar2
performs (costly) whole-project interprocedural analysis to infer types of variables, parameters and
functions. Because of this, PySonar2 generally produces better index than Python IDEs (such as
PyCharm etc.), while at the same time sacraficing real-time indexing abilities of IDEs.

PySonar2 has been the underlying indexing engine for several large-scale code navigation services,
such as Google's internal Code Search service, Sourcegraph.com and Insight.io (now part of
Elasticsearch). It has been used to index millions of lines of Python code.

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>


### How to build

    mvn package


### Demo

To have a feel of what PySonar2 produce, you can build a simple code browser of the Python 2.7
standard library with the following command line:

    java -jar target/pysonar-<version>.jar /usr/lib/python2.7 ./html

This may take a few minutes depending on your machine. You should find some interactive HTML files
inside the _html_ directory after this process. You can move your mouse on the variables and click
on them to jump to definitions etc.

Note that this is just a simple demo program based on the library. PySonar2 is not meant to be an
end-user tool. It is mainly designed as a library for Python IDEs, developer tools and code search
engines, so its interface may not be as appealing as an end-user tool.

If you have problems with it, please feel free to contact me.


### System requirements

* Python 2.7.x
* Python 3.x
* Java 8+
* maven


### Environment variables

PySonar2 uses CPython's built-in `ast` package to parse Python code, so please make sure you have
`python` or `python3` installed and pointed to by the `PATH` environment variable. If you have them
in different names, please make symbol links.

`PYTHONPATH` environment variable is used for locating the Python standard libraries. It is
important to point it to the correct Python library, for example

    export PYTHONPATH=/usr/lib/python2.7

If this is not set up correctly, references to library code will not be found.


### Contribute

You are welcome to make code contributions.

Because of the highly complex and unpublished theory behind PySonar2, things may go wrong easily
with even an innocent-looking change. If you hope to contribute to PySonar2, please discuss with me
first before making significant changes, otherwise I may not be able to review your changes.

For basic verification, you can run the unit tests. PySonar2 has a basic test framework. You can run
the tests using this command:

    mvn test

If you modify the code or tests, you need to generate new expected results. Run these command lines:

    mvn package -DskipTests
    java -classpath target/pysonar-<version>.jar org.yinwang.pysonar.TestInference -generate tests

To write new tests, you just need to write relevant Python code demonstrating your change, put them
into a directory named `tests/testname.test`(test directory name must end with ".test"). Please look
at the `tests` directory for examples.

Please don't expect the tests to catch all bugs. Be very careful :)


### License

Apache 2.0 License. See LICENSE file.
