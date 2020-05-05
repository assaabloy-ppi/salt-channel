salt-channel/releases-notes.txt
===============================

Release history. The implementation started in 2016, but formal 
releases started November 2017.


v2.6, 2020-05-05
================

Minor fixes. We now have zero known issues!

* Fixed Issue #24, improved exception handling, now protects against missing
  checks in TimeChecker implementations.
* Fixed Issue #21, nonce overflow.
* Fixed Issue #19, improved test tool (TcpTestServer).


v2.5, 2017-11-17
================

Implements salt-channel-v2-final1. 

* Minor refactorings.
* Fixed issues #16, #17, #18. 



v2.4, 2017-11-06
================

Implements v2-draft8.md. No known bugs.

* Fixed Issue #13: LastFlag not set in M2 for NoSuchServer condition.
* Fixed Issue #14: ExampleSession3, now correct TimeKeeper in test data.



v2.3, 2017-10-26
================

Implements v2-draft8.md. No known bugs.

* SaltServerSession.isDone() method added.

* SaltServerSession.getChannel() throws exception if session is done 
  (A1A2 session).
  
* Fixed issue #12, now A2 with NoSuchServer flag set is sent when
  it should.



v2.2, 2017-10-25
================

Implements salt-channel-v2-draft8.md. No known bugs.

* ApplicationChannel now available to lib users. Includes
  information about last flag and multi app packet.



v2.1, 2017-10-17
================

This release implements Salt Channel v2, draft8.



v2.0, 2017-10-17
================

Implements Salt Channel v2, draft7 (spec-salt-channel-v2-draft7.md).

