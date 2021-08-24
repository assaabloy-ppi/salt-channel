Log 2021-08-24
==============

Frans: No news really. The Java-WebSocket-1.5.1.jar is the still the 
latest release of that library. Fixed LICENSE to include "2021".


Log 2020-05-05
==============

Frans: Fixed some issues. We now have zero known issues and are 
ready to make a new release.


Log 2020-04-24
==============

Merged work by Shawn Nock, https://github.com/nocko, into main branch.
Work on formal verification using Verifpal.


Log 2018-02-13
==============

Frans Lundberg. Status of Salt Channel:
Version 2 is stable, no breaking security issues has been found. 
Assured AB has audited the protocol and Java the Java implementation. 
Ready for production use! However, the specification could be improved
in some areas and the Java implementation would also benefit from some
addition love. An updated comparison with TLS would also be useful.



Log 2017-11-16
==============

Frans: Salt Channel v2 declared FINAL! 
Doc: salt-channel-v2-final1.md.



Log 2017-10-16
==============

Frans Lundberg: We are finishing work on v2 soon. Draft8 might be the 
last draft.



Log 2017-05-10
==============

Frans Lundberg: Example comparing with TLS. Using Wireshark. 
See saltchannel.dev.Tls/RunClient/RunServer.

Results:

    SALT CHANNEL VS TLS
    Application: client sends 6 bytes, server echos back the same bytes.
    Salt Channel: v2 used.
    TLS: protocol v1.2, suite TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, 
    256-bit keys, curve P-256, client and server certs used (minimal, self-signed).
    
    RESULTS         TLS           Salt Channel
    Total bytes:    2299          404
    Round-trips:    4             2
    
See separate report when available.

Frans: Work on Salt Channel v2. Draft 4.
Resume *will* be included in v2.


Log 2017-05-03
==============

Frans Lundberg:
The code of repo:salt-aa is currently duplicated in this project.
Repo:salt-aa is the primary source for the "saltaa" package.
To do: we should fix build process to either copy source from 
repo:salt-aa or to have a salt-aa.jar output from repo:salt-aa and use
this jar in repo:salt-channel (no jar dependence, but exploded/merged approach).


2017-03-29
==========

New approach: we will add the resume feature in v2
(not wait until v3).

End of day status: 

* first spike with a resume session that works!

* Case C, *invalid ticket* (in spec) is not implemented!
  We need an EncryptedChannel that we *test*. If ticket is invalid
  we must replace that EncryptedChannel.
  
* Spec needs to be updated.


Log 2017-03-13
==============

Alex Reshniuk:
Updated '.\lib' with new libsodiumjni Android lib builds.
Added 'lib-native' with libsodiumjni for Linux/Windows.

2017-02-27
==========

While flying to Tel Aviv.

Frans and Simon about SCv2
--------------------------

Decisions:
* 2-byte header (not really about timestamps).
* 4-byte time. LE, range [0, 2^31-1] (both signed and unsigned works).
* Time is in milliseconds since first message was sent by peer.
  Note, each peer uses a separate time scale. 
  Client uses time since M1 sent and Server uses time since M2 sent.
* Time == 0 is used to indicate: "no time available".
* Time == 1 is used in first messsage by peer (M1, M2) to indicate that
  timestamps are supported and will be provided in the following messages.
* Treat delay errors as I/O errors of the underlying transport channel.
  No special Salt Channel messages are used. Just terminate Salt Channel 
  session.
* Time between two consecutive messages must not exceed 10 days (24x2600x10 s).
* M1: protHeader ("SC2-------", header, time, ...
* Simon: test host, test client (manual Telnet style) is useful.
  Built-in to salt-channel.jar perhaps.

  
Log 2017-01-31
==============

Alex Reshniuk:
Quick and dirty integration of JNI-based libsodium library
inside the TweetNaCl.java (to simplify integration).
No Java encryption math is used in this version.

Log 2017-01-27
==============

Alex Reshniuk:
Ready to run Junit4 tests with Ant.
Usage: ant test


Log 2017-01-25
==============

Work on Salt Channel v2.


Log 2016-10-26
==============

Frans Lundberg:
First version of full implementation. Ready for protocol audit. 
Spec included in repo.


Log 2016-10-23
==============

Frans Lundberg: Created repo. Asked dependent tweet-nacl GPL library
to consider business-friendly license.

Committed some code, EncryptedChannel should work, no code "above" that yet.



