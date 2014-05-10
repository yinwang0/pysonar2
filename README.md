### PySonar2 - a type inferencer and indexer for Python

PySonar2 is a type inferencer and indexer for Python, which does sophisticated
interprocedural analysis to infer types. It is one of the underlying
technologies that power the code search site <a
href="https://sourcegraph.com/github.com/rails/rails">Sourcegraph</a>.

To understand its properties, please refer to my blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar
- http://yinwang0.wordpress.com/2013/06/21/pysonar-slides

<a href="http://www.yinwang.org/resources/demos/pysonar2/email/header.py.html">
<img src="http://www.yinwang.org/images/pysonar2.gif" width="70%">
</a>



#### How to build

    mvn package



#### System Requirements

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



#### How to use

PySonar2 is mainly designed as a library for Python IDEs and other developer
tools, so its interface may not be as appealing as an end-user tool, but for
your understanding of the library's capabilities, a reasonably nice demo program
has been built.

Now you can build a simple "code-browser" of the Python 2.7 standard library
with the following command line:

    java -jar target/pysonar-2.0-SNAPSHOT.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



#### Memory Usage

PySonar2 doesn't need much memory to do analysis. 1.5Gb is probably enough for
analyzing a medium sized project such as Python's standard library or Django.
But for generating the HTML files, you may need quite some memory (~2.5Gb for
Python 2.7 standard lib). This is due to the highlighting code is putting all
code and their HTML tags into the memory.



#### License (GNU AGPLv3)

PySonar - a type inferencer and indexer for Python

Copyright (c) 2013-2014 Yin Wang

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.


### Support


After significant development effort, PySonar2 is now stable and has been used
to process hundreds of thousands of Python repositories, millions of lines of
code. The total research & development time by me single-handedly was just a few
months. A similar project developed by a team of programmers of some big company
would take years.

Because of its extremely high code quality, very little maintenance time and
cost is needed, and because of the extremely short development time, I don't get
paid much for developing it, and thus must do some other work to buy my bread.
This is sad. So your donations are graciously appreciated.


<form action="https://www.paypal.com/cgi-bin/webscr" method="post" target="_top">
<input type="hidden" name="cmd" value="_s-xclick">
<input type="hidden" name="encrypted" value="-----BEGIN PKCS7-----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-----END PKCS7-----
">
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0" name="submit" alt="PayPal - The safer, easier way to pay online!">
<img alt="" border="0" src="https://www.paypalobjects.com/en_US/i/scr/pixel.gif" width="1" height="1">
</form>
