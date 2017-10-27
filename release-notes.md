salt-channel/releases-notes.txt
===============================

Release history. The implementation started in 2016, but formal 
releases starts with release 2.0.


Release v2.4, 2017-xx-xx
========================

Next version here.

* Fixed Issue #13: LastFlag not set in M2 for NoSuchServer condition.



Release v2.3, 2017-10-26
========================

Implements v2-draft8.md. No known bugs.

* SaltServerSession.isDone() method added.

* SaltServerSession.getChannel() throws exception if session is done 
  (A1A2 session).
  
* Fixed issue #12, now A2 with NoSuchServer flag set is sent when
  it should.



Release v2.2, 2017-10-25
========================

Implements salt-channel-v2-draft8.md. No known bugs.

* ApplicationChannel now available to lib users. Includes
  information about last flag and multi app packet.



Release v2.1, 2017-10-17
========================

This release implements Salt Channel v2, draft8.



Release v2.0, 2017-10-17
========================

Implements Salt Channel v2, draft7 (spec-salt-channel-v2-draft7.md).
