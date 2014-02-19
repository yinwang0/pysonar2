## PySonar2 - a type inferencer and indexer for Python

PySonar2 is a type inferencer and indexer for Python, which does sophisticated
interprocedural analysis to infer types. It is one of the underlying
technologies that powers the code search site <a
href="https://sourcegraph.com/github.com/rails/rails">Sourcegraph</a>.

To understand its properties, please refer to my blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar
- http://yinwang0.wordpress.com/2013/06/21/pysonar-slides

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>



### How to build

    mvn package



### System Requirements

* Python 2.7.x is recommended (Python <= 2.5 does not work, Python 2.6 works for
  some people but not all)

* Python 3.x if you have Python3 files

PySonar2 uses CPython interpreter to parse Python code, so please make sure you
have `python` or `python3` installed and pointed to by the `PATH` environment
variable. If you have them in different names, please make symbol links.

`PYTHONPATH` environment variable is used for locating the Python standard
libraries. It is important to point it to the correct Python library, for
example

    export PYTHONPATH=/usr/lib/python2.7

If this is not set up correctly, you may find suboptimal results.



### How to use

PySonar2 is mainly designed as a library for Python IDEs and other developer
tools, so its interface may not be as appealing as an end-user tool, but for
your understanding of the library's capabilities, a reasonably nice demo program
has been built.

Now you can build a simple "code-browser" of the Python 2.7 standard library
with the following command line:

    java -jar target/pysonar-2.0-SNAPSHOT.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



### Memory Usage

PySonar2 doesn't need much memory to do analysis. 1.5Gb is probably enough for
analyzing a medium sized project such as Python's standard library or Django.
But for generating the HTML files, you may need quite some memory (~2.5Gb for
Python 2.7 standard lib). This is due to the highlighting code is putting all
code and their HTML tags into the memory.



### License

Copyright (c) 2013 Yin Wang

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
