### PySonar2 - a type inferencer and indexer for Python

PySonar2 is a type inferencer and indexer for Python, which performs
sophisticated interprocedural analysis to infer types. It is one of the
underlying technologies that power the code search engine <a
href="http://www.sourcegraph.com">Sourcegraph</a>, where it has been used to
index hundreds of thousands of open source Python repositories, producing a
globally connected network of Python code. An older version of PySonar is used
internally at Google, producing high-quality semantic code index for millions of
lines of Python code.

To understand its properties, please refer to my blog post:

- http://yinwang0.wordpress.com/2010/09/12/pysonar

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>



#### How to build

    mvn package



#### How to use

PySonar2 is mainly designed as a library for Python IDEs, other developer
tools and code search engines, so its interface may not be as appealing as an
end-user tool, but for your understanding of the library's capabilities, a
reasonably nice demo program has been built.

You can build a simple "code-browser" of the Python 2.7 standard library with
the following command line:

    java -jar target/pysonar-<version>.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



#### System requirements

* Python 2.7.x
* Python 3.x if you have Python3 files
* Java 8
* maven



##### Environment variables

PySonar2 uses CPython's ast package to parse Python code, so please make sure
you have `python` or `python3` installed and pointed to by the `PATH`
environment variable. If you have them in different names, please make symbol
links.

`PYTHONPATH` environment variable is used for locating the Python standard
libraries. It is important to point it to the correct Python library, for
example

    export PYTHONPATH=/usr/lib/python2.7

If this is not set up correctly, you may find suboptimal results.



##### Memory usage

PySonar2 doesn't need much memory to do analysis compared to other static
analysis tool of its class. 1.5Gb is probably enough for analyzing a medium
sized project such as Python's standard library or Django. But for generating
the HTML files, you may need quite some memory (~2.5Gb for Python 2.7 standard
lib). This is due to the highlighting code is putting all code and their HTML
tags into the memory.



#### License

Apache License. See LICENSE file.
