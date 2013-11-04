PECOFF4J
========

This is a fork of http://sourceforge.net/projects/pecoff4j/
Initial sources were in CVS, so I imported them from tarball created on 04 Nov 2013 at 22.04 GMT


License
=======
Initial sources are under [Common Public License v1.0](http://www.eclipse.org/legal/cpl-v10.html)




I used https://github.com/jonnyzzz/intellij-ant-maven to simplify IDEA dependencies



Building
========

First call ```ant -f build/fetch.xml fetch``` to make all dependencies fetched,
next use IntelliJ IDEA to compule project. Build artifact to have library jars created