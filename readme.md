salt-channel
============

This repo contains [the specification](files/spec/salt-channel-v2-final2.md) and the 
Java reference implementation of 
*Salt Channel* - a simple, light-weight secure channel protocol based on the 
[TweetNaCl](http://tweetnacl.cr.yp.to/) API / library by Bernstein et al.
Salt Channel is "Powered by Curve25519".

Salt Channel is simpler than TLS and works well on small embedded processors.
It has a lower handshake overhead. See [this comparison](files/salt-channel-vs-tls-2017-05.md).
Salt Channel *always* uses mutual authentication and forward secrecy.
The protocol hides the identity of the client.

The development of the code in this repository and the protocol 
itself has been financed by [ASSA ABLOY AB](http://www.assaabloy.com/) -- the 
global leader in door opening solutions. Thank you for 
supporting this work. We also thank Daniel Bernstein for developing the 
underlying cryptographic algorithms. Our work is completely based on his work.

The protocol has been audited. [This statement](files/v2-review-statement-2018-02.pdf)
is a summary of the result of the latest security audit. It was performed 
by Assured AB, thank you!

Also, the protocol has been analysed with formal methods using Proverif. 
It proves, within a realistic mathematical model, that the main security goals
have been met. See the results in [the report](files/Proverif-SaltChannelReport1.1-2018.pdf).


Versions
========

Version v2 as defined in [salt-channel-v2-final2.md](files/spec/salt-channel-v2-final2.md) 
is the latest stable version of the protocol. New applications should use this version.
We recommend that existing applications using v1 should eventually be migrated
to v2. As of today (February 2019) we have found no critical security concern 
with v1, so from a security point of view, there is no rush to upgrade.



Contributors
============

## Protocol authors

* Frans Lundberg, ASSA ABLOY AB, Stockholm, Sweden.
* Simon Johansson, ASSA ABLOY AB, Stockholm, Sweden.

Feel free to contact us.

## Thanks!

Thank you, Shawn Nock (https://github.com/nocko), for your work on formal verification 
using Verifpal.



Implementations
===============


Java (this repo)
----------------

This repository contains the Java implementation of Salt Channel. It is the reference 
implementation of the protocol. It is open source and released under the MIT License.


C
---

See [github.com/assaabloy-ppi/salt-channel-c](https://github.com/assaabloy-ppi/salt-channel-c) for the C implementation. It is suitable for both
embedded devices and larger computers.


JavaScript
----------

Available here: [github.com/assaabloy-ppi/salt-channel-js](https://github.com/assaabloy-ppi/salt-channel-js), 
JavaScript implementation, MIT license.


Swift
-----

Available here: [github.com/assaabloy-ppi/salt-channel-swift](https://github.com/assaabloy-ppi/salt-channel-swift),
Swift for iPhone and more, MIT license.


Python
------

See [github.com/assaabloy-ppi/salt-channel-python](https://github.com/assaabloy-ppi/salt-channel-python), 
Python 3 implementation, MIT License.



Optimized crypto
================

The library depends on the repo:
[github.com/assaabloy-ppi/salt-aa](https://github.com/assaabloy-ppi/salt-aa).
Note that this dependency is *copied* into this repo.

The salt-aa repo allows seamless use of optimized binaries (Libsodium) 
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

Should work fine with any IDE. We have worked with Eclipse, IntelliJ, simple 
text editors.



Code conventions (Java)
=======================

The following code conventions apply to the Java code in this repository.

The old Sun code conventions 
(http://www.oracle.com/technetwork/java/codeconvtoc-136057.html)
are followed loosely together with the following rules (which take precedence):

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

