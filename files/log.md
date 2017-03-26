log.md
======

Log and general notes.

2017-02-27
==========

While flying to Tel Aviv.


Frans and Simon about SCv2
----------------------------------------

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
* M1: protHeader ("SCv2", header, time, ...
* Simon: test host, test client (manual Telnet style) is useful.
  Built-in to salt-channel.jar perhaps.
