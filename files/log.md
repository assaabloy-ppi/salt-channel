log.md
======

Log and general notes.

2017-10-04
==========

Frans: Implementation should now support v2-draft6, MultiAppPacket:s and 
lastFlag. Made release salt-channel-1.9.20171004.jar. 

Versioning
----------

Note the new versioning scheme. For example:
salt-channel-1.9.YYYYMMDD.jar is used for development builds and 
salt-channel-2.0.jar for release build.

The major version follows the version of the Salt Channel specification
while the minor version does not.


2017-10-02
==========

Frans: Finalized draft5, starting work on draft6.

* DECISION: don't use symmetric protocol.
    We could design a protocol that is symmetric. Both peers could
    send their EncKey. Possibly in parallel and immediately when 
    connection is established. 
    This is beautiful and efficient, but it complicates things when 
    we add the resume ticket feature.
    So, decision now is to *not* do this.
    
* DECISION: LastFlag is needed! And should be included in v2.
    We need it now for v2 final. We want to support the browser-to-thing
    communication via WebSocket/TCP/BLE (for example).

* DECISION: Resume feature is removed from v2 spec.
    Should likely be added in 2.1 or v3. Needs more time, more implementations
    than Java and perhaps a security audit. The feature will be kept in the
    Java implementation however. That feature is however currently experimental.

* DECISION: No address info (domain+port).
    Adding address of server in M1 is tempting, but it is not added
    in v2. Needs more thought and implementations. The pubkey in M1 is enough
    for now. Should WebSocket be supported? Use URL or domain+port?
    



2017-05-10
==========

Frans: Work on Salt Channel v2. Draft 4.
Resume *will* be included in v2.


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
