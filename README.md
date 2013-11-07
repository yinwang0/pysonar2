## PySonar2 - an advanced static analyzer for Python

PySonar2 is a static analyzer for Python, which does global interprocedural analysis to infer types. To understand it, please refer to my blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar
- http://yinwang0.wordpress.com/2013/06/21/pysonar-slides


### How to build

    mvn clean package

### How to run?

PySonar2 is mainly designed as a library for Python IDEs and other tools, but
for your understanding of the library's capabilities, a demo program is built
(most credits go to Steve Yegge).

To run it, first you need Gson because PySonar2 uses Gson to deserialize the AST
output from CPython's `ast.parse`. You can get Gson <a
href="https://code.google.com/p/google-gson/downloads">here</a>. Maven should be
able to set this all up, but I don't know how. If you know please let me know.

Now you can try building an index of the Python 2.7 standard library with the
following command line:

    java -classpath target/pysonar.jar:<path-to-gson-2.2.4.jar> \
         org.yinwang.pysonar.demos.Demo /usr/lib/python2.7 ./html

You should find some interactive HTML files inside the _html_ directory
generated after this process.

Note: PySonar2 doesn't need much memory to do analysis (1GB is probably enough),
but for generating the HTML files, you may need a lot of memory (~4GB for
Python 2.5 standard lib). This is due to the highlighting I added without using
more sophisticated ways of doing it. The situation may change soon.


### Jython Branch

PySonar used to use Jython's parser and was part of Jython. If you want to try
the old version, please checkout the <a
href="https://github.com/yinwang0/pysonar2/tree/jython">jython branch</a>.



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
