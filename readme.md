salt-channel
============

This repo contains [the specification](files/spec/spec-salt-channel-v1.md) and the 
Java reference implementation of 
*Salt Channel* - a simple, light-weight secure channel protocol based on 
[TweetNaCl](http://tweetnacl.cr.yp.to/) by Bernstein et al.
Salt Channel is "Powered by Curve25519".

Salt Channel is simpler than TLS and works well on small embedded processors.
It has a lower handshake overhead. See [this comparison](files/salt-channel-vs-tls-2017-05.md).
Salt Channel *always* uses mutual authentication and forward secrecy.
The protocol supports secret client IDs. TLS does not currently do this,
however, this property is included in the April 2017 TLS 1.3 draft.

The development of the code in this repository and the protocol 
itself has been financed by [ASSA ABLOY AB](http://www.assaabloy.com/) 
-- the global leader in door opening solutions. Thank you for 
supporting this work.

We also thank Daniel Bernstein for developing the underlying cryptographic
algorithms. Our work is completely based on his work.


Versions
========

Version v1 is the currently well-defined version of the protocol. 
We work with the v2 specifications and implementations for it. 
See [drafts](files/spec/).


Implementations
===============

Java (this repo)
----------------

This repository contains the Java implementation of Salt Channel. It is the reference 
implementation of the protocol. It is open source and released under the MIT License.


C implementation
----------------

See [github.com/assaabloy-ppi/salt-channel-c](https://github.com/assaabloy-ppi/salt-channel-c) for the C implementation. It is suitable for both
embedded devices and larger computers.


Python implementation
---------------------

See [github.com/assaabloy-ppi/salt-channel-python](https://github.com/assaabloy-ppi/salt-channel-python), 
Python 3 implementation, MIT License.


Others
------

* [github.com/assaabloy-ppi/salt-channel-js](https://github.com/assaabloy-ppi/salt-channel-js), 
  JavaScript implementation, work in progress, MIT license.



Authors
=======

Protocol authors:

* Frans Lundberg, ASSA ABLOY AB, Stockholm, Sweden.
* Simon Johansson, ASSA ABLOY AB, Stockholm, Sweden.


Optimized crypto
================

The library depends on the repo:
[github.com/assaabloy-ppi/salt-aa](https://github.com/assaabloy-ppi/salt-aa).
Note that this dependency is *copied* into this repo.

The salt-aa repo allows seemless use of optimized binaries (Libsodium) 
when available. If no native implementation is available, a pure Java 
implementation (github.com/InstantWebP2P/tweetnacl-java) is used. 
This is handled dynamically.


Build and develop
=================

Type "ant" to build using the build.xml script.

IDE tips: 

* Add the src, src-in, src-test directories as source 
directories.

* Set out/classes/ to the output for compiled class files.

* Include the libraries in lib, lib-dev.

Should work fine with any IDE. We have worked with Eclipse and IntelliJ.



Code conventions
================

The old Sun code conventions 
(http://www.oracle.com/technetwork/java/codeconvtoc-136057.html)
are followed loosly together with the following rules (which take precedence):

* Lines can be up to 100 characters long.
* Use four spaces as indent.
* Use '\n' for end-of-line.
* Use UTF-8 encoding.


Files
=====

* **files/** -- Non code files, specifications, docs.

* **src/** -- Primary source code.

* **src-in/** -- Source code copied from elsewhere. Do not edit, copy 
  from primary location instead.

* **src-test/** -- JUnit tests.

* **build.xml** -- ANT build script. Just type "ant" for the default build.

