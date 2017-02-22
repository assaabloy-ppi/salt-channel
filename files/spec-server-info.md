spec-server-info.md
===================

About this document
-------------------

*Date*: 2017-02-22

*Status*: WORK IN PROGRESS.

*Author*: Frans Lundberg. ASSA ABLOY AB, Shared Technologies, Stockholm,
frans.lundberg@assaabloy.com, phone: +46707601861.

*Thanks*: 
To Håkan Olsson valuable discussions.



Introduction
============

The Server Info protocol is an extremely simple client-server protocol
over a reliable communication layer. It consists of one request by the client
following by a response from the server. Typically, it is used 
to choose a real communication protocol for further communication.
It allows the server to tell what protocols it supports so the client can
chose a suitable one for futher communication.

The server information (Response message) can be assumed to change seldom and 
can be cached by a client.


Security
========

Typically this protocol is over a cleartext channel. Care must be taken 
to validate any server information later once a secure channel has been
established.

Server information in the Response message should be considered public 
information. Secret information MUST NOT be revealed.


Details
=======

The whole client-server session is a Request from the client followed
by a Response from the server.

    Session = Request Response

    Request = "SINFO" NL
        ; ASCII characters, NL = new line = ASCII 10.
        ; The Request message is always that same. Always 6 bytes long.

    Response = Header Line*

    Header = "SINFO1-" LineCount NL

    Line = Prot "|" Prot "|" Prot NL
        ; Up to three protocol indicators. The first one is the lowest-layer
        ; protocol, the third one is the highest-layer protocol
        ; closest to the application.

    Prot = 
        ; Eleven ASCII characters. Must start with a character in a-z, A-Z
        ; followed by characters that are not whitespace of ASCII control 
        ; characters. Ending dashes '-' should be 
        ; used to fill the 12 bytes required. 
        ; The Prot string is used to identify a protocol and the 
        ; version of it. Possibly feature support can be revealed here.

    LineCount =
        ; Two ASCII characters representing the number of Line elements that
        ; will follow. Must be between "00" and "99". Always two bytes long.

    NL = "\n"
        ; One ASCII character, value 10, "new line".

That is the complete syntax.

Note, that fixed offsets are used for easy reading by machines (embedded etc).
And ASCII text is used for easy reading by humans (developers).

The size of Request is 6 bytes. The size of Response is 10 + LineCount x 36.


Examples
========

Example session (new lines are implicit):
    
    Client:
    
        SINFO
    
    Server:
    
        SINFO1-02
        sc2--------/MyProt-----/AppX-------
        sc3--------/MyProt-----/AppX-------
    
The Response says that the server can handle the AppX protocol over MyProt
over Salt Channel (sc) of either version 2 or version 3.

Another example:
    
    Client:
    
        SINFO
    
    Server:
    
        SINFO1-02
        sc2--------/-----------/-----------
        sc3--------/-----------/-----------
    
This example is the same as the previous, except that the type of the 
higher-level layers are not revealed. A Prot value of "-----------"
SHOULD be used to indicate a layer type that is not revealed to the client.


References
==========

[SC] Salt Channel, https://github.com/assaabloy-ppi/salt-channel/.


