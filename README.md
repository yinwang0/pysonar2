## PySonar2 - a deep static analyzer for Python

PySonar2 is a static analyzer for Python, which does sophisticated
interprocedural analysis to infer types. To understand it, please refer to my
blog posts:

- http://yinwang0.wordpress.com/2010/09/12/pysonar
- http://yinwang0.wordpress.com/2013/06/21/pysonar-slides



### How to build

    mvn clean package



### If the build is broken

I haven't set up releases, so I push working copies of the builds that I don't
see problems so far to the `target` directory as a "stable" version. If the
current build is broken or buggy, please grab that snapshot instead. And of
course, filing an issue is appreciated.



### System Requirements

* Python 2.6 or 2.7 (Python <=2.5 does not work)
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
has been built (all features added by Steve Yegge, all bugs added by Yin Wang).

Now you can build a simple "code-browser" of the Python 2.7 standard library
with the following command line:

    java -jar target/pysonar-2.0-SNAPSHOT.jar /usr/lib/python2.7 ./html

This will take a few minutes. You should find some interactive HTML files inside
the _html_ directory after this process.



### Memory Usage

All serious static analysis tools require a lot of memory to run. PySonar2
doesn't need much memory to do analysis. 1GB is probably enough for analyzing a
medium sized project such as Python's standard library or Django. But for
generating the HTML files, you may need quite some memory (~2.5GB for Python 2.7
standard lib). This is due to the highlighting code is putting all code and
their HTML tags into the memory.


## How to contribute

PySonar2 is current a fast moving target. There are several big architectural
changes undergoing. Please contact me before you make any substantial changes,
otherwise it will unlikely I can merge back your changes. Thanks.


### Copyright (GPLv3)

Copyright (c) 2013 Yin Wang

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see <http://www.gnu.org/licenses/>.
