# PySonar2 - a type inferencer for Python

PySonar2 is a type inferencer and indexer for Python, which performs interprocedural analysis to
infer types.

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>

### Checkout code

    git clone https://github.com/yinwang0/pysonar2.git
    git checkout tags/v2.1.1


### How to build

    mvn package


### How to use

PySonar2 is mainly designed as a library for Python IDEs, other developer
tools and code search engines, so its interface may not be as appealing as an
end-user tool, but for your understanding of the library's capabilities, a
reasonably nice demo program has been built.

You can build a simple "code-browser" of the Python 2.7 standard library with
the following command line:

    java -jar target/pysonar-<version>.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



### System requirements

* Python 2.7.x
* Python 3.x (if your code uses Python3)
* Java 8
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

If you hope to improve certain things in PySonar2, please contact me first. For development, you can run the unit tests.
PySonar2 has a test framework. You can run the tests using this command line after a successful build.

    java -classpath target/pysonar-<version>.jar org.yinwang.pysonar.Test tests

If you modified the code, to generate new expected test results, please run this command line (adding `-exp` to the command):

    java -classpath target/pysonar-<version>.jar org.yinwang.pysonar.Test tests -exp

To write new tests, you just need to write relevant Python files, put them into a directory named `test/your-test.test`.
Please look at the `tests` directory for examples.


### License

Apache 2.0 License. See LICENSE file.
