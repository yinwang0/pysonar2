## PySonar2 - a deep static analyzer for Python

PySonar2 is a static analyzer for Python, which does sophisticated
interprocedural analysis to infer types. To understand it, please refer to my
blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar
- http://yinwang0.wordpress.com/2013/06/21/pysonar-slides



### How to build

    mvn clean package



### Configuration

PySonar2 uses CPython interpreter to parse Python code, so please make sure you
have `python` or `python3` installed and pointed to by the `PATH` environment
variable.

`PYTHONPATH` environment variable is used for locating the Python standard
libraries. It is important to point it to the correct Python library, for
example

    export PYTHONPATH=/usr/lib/python2.7

If this is not set up correctly, you may find suboptimal results.



### How to use

PySonar2 is mainly designed as a library for Python IDEs and other developer
tools, so its interface may not be as appealing as an end-user tool, but for
your understanding of the library's capabilities, a reasonably nice demo program
has been built (all features added by Steve Yegge, all bugs added by Yin Wang).

Now you can build a simple "code-browser" of the Python 2.7 standard library
with the following command line:

    java -jar target/pysonar-2.0-SNAPSHOT.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



### Memory Usage

All serious static analysis tools require a lot of memory to run. PySonar2
doesn't need much memory to do analysis. 1GB is probably enough for analyzing a
medium sized project such as the standard library or Django. But for generating
the HTML files, you may need a lot of memory (~4GB for Python 2.5 standard lib).
This is due to the highlighting code I added to the demo not using sophisticated
ways of doing it. The situation may change soon.



### Jython Branch

PySonar used to use Jython's parser and was part of Jython. If you want to try
that version, please checkout the <a
href="https://github.com/yinwang0/pysonar2/tree/jython">jython branch</a>. You
may also want to look at <a
href="http://hg.python.org/jython/file/11776cd9765b/src/org/python/indexer">PySonar 1.0
code</a> inside Jython project. But keep in mind that the new code here is much
better, and those old versions are no longer supported or developed by me.



### Copyright (BSD-style)

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
