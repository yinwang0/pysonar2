## PySonar2 - an advanced static analyzer for Python

To understand it, please refer to my blog posts:

    http://yinwang0.wordpress.com/2010/09/12/pysonar
    http://yinwang0.wordpress.com/2013/06/21/pysonar-slides


### How to build

PySonar 1.0 was part of Jython, and PySonar2 still depend on Jython's parser
(the situation may change soon). So you need to download Jython's source code
and compile PySonar2 with it.


1. Download Jython

        hg clone http://hg.python.org/jython

2. Checkout this repo, replace everything inside _src/org/python/indexer_ (which
   is PySonar 1.0) with the content of this repo

3. Delete the tests for the old indexer

        rm -rf tests/java/org/python/indexer

4. Build Jython

        ant jar-complete

5. Finished. PySonar2 is now inside _dist/jython.jar_.


### How to run?

PySonar2 is mainly designed as a library for Python IDEs and other tools, but
for your understanding of the library's usage, a demo program is built (most
credits go to Steve Yegge). To run it, use the following command line:

    java -classpath dist/jython.jar org.python.indexer.demos.HtmlDemo /usr/lib/python2.7 /usr/lib/python2.7

You should find some interactive HTML files inside the _html_ directory
generated after this process.

Note: PySonar2 doesn't need much memory to do analysis (1GB is probably enough),
but for generating the HTML files, you may need a lot of memory (~4GB for
Python 2.5 standard lib). This is due to the highlighting I added without using
more sophisticated ways of doing it. The situation may change soon.


### Copyright (BSD-style)

Copyright (c) 2013 Yin Wang

Copyright (c) 2009 Google Inc.

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
