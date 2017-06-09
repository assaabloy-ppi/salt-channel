salt-channel
============

This repo contains [the specification](files/spec/spec-salt-channel-v1.md) and the 
Java reference implementation of 
*Salt Channel* - a simple, light-weight secure channel protocol based on 
[TweetNaCl](http://tweetnacl.cr.yp.to/) by Bernstein et al.
Salt Channel is "Powered by Curve25519".

Salt Channel is simpler than TLS and works well on small embedded processors. It has a lower handshake overhead.
See [this comparison](files/salt-channel-vs-tls-2017-05.md).
Salt Channel *always* implements mutual authentication and forward secrecy.
The protocol supports secret client IDs. TLS does not currently. However, this property is included in the April 2017 TLS 1.3 draft.

The development of the code in this repository and the protocol 
itself has been financed by [ASSA ABLOY AB](http://www.assaabloy.com/) 
-- the global leader in door opening solutions. Thank you for 
supporting this.

We thank Daniel Bernstein for developing the underlying cryptography 
algorithms. Our work here is completely based on his work.

Versions
========

Version v1 is currently well-defined version of the protocol. 
We work with the v2 specifications and implementations for it. 
See [drafts](files/spec/).


Java implementation
===================

This repository contains the Java implementation of Salt Channel. It is the reference implementation of the protocol. It is open source and
released under the MIT License.


C Implementation
================

See [github.com/assaabloy-ppi/salt-channel-c](https://github.com/assaabloy-ppi/salt-channel-c) for the C implementation. It is suitable for both
embedded devices and larger computers.


Authors
=======

* Frans Lundberg, ASSA ABLOY AB, Stockholm, Sweden.
* Simon Johansson, ASSA ABLOY AB, Stockholm, Sweden.
