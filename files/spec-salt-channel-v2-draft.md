spec-salt-channel-v1.md
=======================

About this document
-------------------

*Date*: 2017-02-09

*Status*: WORK IN PROGRESS. Not a complete draft yet.

*Author*: Frans Lundberg. ASSA ABLOY AB, Shared Technologies, Stockholm,
frans.lundberg@assaabloy.com, phone: +46707601861.

*Thanks*: 
To Simon Johansson for valuable comments and discussions.


Changes
-------

Significant changes of this document since first non-draft version.

* None.



Introduction
============

Salt Channel is a secure channel protocol based on the TweetNaCl 
("tweet salt") cryptography library by Daniel Bernstein et al 
[TWEET-1, TWEET-2]. Like TweetNaCl itself, Salt Channel is simple and 
light-weight.

The protocol is essentially an implementation of the station-to-station [STS] 
protocol using the crypto primitives of TweetNaCl.
It relies on an underlying reliable communication channel between two 
peers. TCP is an important example of such a channel, but Salt Channel is in
no way restricted to TCP. In fact, Salt Channel has been successfully 
implemented on top of WebSocket, RS232, Bluetooth, and Bluetooth Low Energy.

This is the second version of the protocol, *Salt Channel v2*. The major changes
is the removal of the Binson dependency and the addition of the resume feature
which allows repeated sessions between a particular client and server to be 
started more efficiently.

Salt Channel is "Powered by Curve25519".


Changes from v1
===============

Salt Channel v2 is new version of Salt Channel. It is incompatible 
with Salt Channel v1. However a Salt Channel server MAY support both 
v1 and v2 at the same time. The first message from the client has
a protocol indicator that can be used to differential between the 
two version of the protocol.

The major changes are: 

1. Binson dependency removed,
2. the resume feature.

The Binson dependency is removed to make implementation more
independent. Also, it means more fixed sizes/offsets which can 
be beneficiary for performance; especially on small embedded targets.

The resume feature allows zero-way overhead for repeated connections 
between a specific client-host pair. In addition to zero-way communication 
overhead a resumed session handshake uses less data (down to around 100 bytes) 
and *no asymmetric crypto*.


Temporary notes
===============

Not in final spec, of course.

* NOTE. We need to consider attacker-delayed messages.
    Discussed, phone call, Simon-Frans, 2017-02-09.
    For example, the protocol is used to communicate between a digital key
    and a door lock with an attacker that is man-in-the-middle.
    The attacker could delay an "unlock" message, wait until the real
    user walks away, then send the valid message to the door lock. It will then
    unlock for the attacker!

* Independent message parsing. 
    Each packet should be possible to parse *independently*.
    Independently of the previous communication and any state.
    The pack/unpack can thus be completely independent.

* Single-byte alignment.
    There is not special reason to have 2, or 4-byte alignment of
    fields in this protocol. Compactness is preferred.

* Notation. Use style: "M1/Header".

* Symmetric protocol, not.
    We could design a protocol that is symmetric. Both peers could
    send their EncKey. Possibly in parallel and immediately when 
    connection is established. 
    This is beautiful and efficient, but it complicates things when 
    we add the resume ticket feature.
    So, decision now is to *not* do this.

* CloseFlag.
    Is it needed? Should it be used?



Protocol design
===============

This section is informative.


Priorities
----------

The following priorities were used when designing the protocol.

1. The first priority is to achieve high security. 

2. The second priority is to achieve a low network overhead; 
   that is, few round-trips and a small data overhead.
   
3. The third priority is to allow for low code complexity, low CPU requirements, 
   and low memory requirements of the communicating peers.

 Low complexity is always important to achieve high security.


Goals
-----

The following are the main goals and limitations of the protocol.

* 128-bit security. 
    The best attack should be a 2^128 brute force attack. 
    No attack should be possible until there are (if there ever will be) 
    large-enough quantum computers.

* Forward secrecy.

* Client ID hidden.
    An attacker cannot tell whether the same client key pair (long-term signing
    key pair) is used in two sessions.
    
* Simple protocol.
    Should be possible to implement in few lines of code. Should be auditable 
    just like TweetNaCl.

* Compact protocol (few bytes).
    Designed for Bluetooth low energy, for example. Low bandwith, in the order
    of 1 kB/s.
    
* It is a goal of Salt Channel to work well together with TCP Fast Open.

* Limitation: No certificates.
    Simplicity and compactness are preferred.
    
* Limitation: the protocol is not intended to be secure for an 
    attacker with a large quantum computer. This is a limitation of 
    the underlying TweetNaCl library.
    
* Limitation: no attempt is made to hide the length, sequence, or timing
  of the communicated messages.


Session
=======

The message order of an ordinary successful Salt Channel session is:
 
    Session = M1 M2 M3 M4 AppMessage*

The M1, and M4 messages are sent by the client and M2, M3 by the server.
So, we have a three-way handshake (M1 from client, M2+M3 from server, and 
M4 from client). When the first application message is from the client, this
message can be sent together with M4 to achieve a two-way (one round-trip) 
Salt Channel overhead. Application layer message (AppMessage*) are sent by 
either the client or the server in any order.

The message order of a successful resumed Salt Channel session is:

    From client: M1 AppMessage*
    From server: M2 AppMessage*

When the first application message is from the client to the server
(a common case), a resumed Salt Channel will have a zero-way overhead.
The first application message can be sent together with M1 before the M2
response from the server.


Message details
===============

This section describes how a message is represented as an array
of bytes. The size of the messages are know by the layer above.
The term *message* is used for a whole byte array message and
*packet* is used to refer to a byte array -- either a full message
or a part of a message.

Messages are presented below with fields of specified byte size.
If the size number has a "b" suffix, the size is in bits, otherwise
it the byte size. 

Bit order: the "first" bit (Bit 0) of a byte is the least-significant bit.
Byte order: little-endian byte order is used. The first byte is the 
least significant one.

Unless otherwise stated explicitely, bits are set to 0.

The word "OPT" is used to mark a field that may or may not exist
in the packet. It does not necesserily indicate a an optional field 
in the sense that it independently can exist or not. Whether its existance
is optional, mandatory or forbidden may dependend on other fields and/or
the state of the communication so far.


Message M1
==========
    
The first message of a Salt Channel session is always the M1 message.
It is sent from the client to the server. It includes a protocol indicator, 
the client's public ephemeral encryption key and optionally the server's
public signing key. For a fast handshake, a resume ticket may be included
in the message.

Details:

    **** M1 ****

    1   Header.
        Message type and flags.

    2   ProtocolIndicator.
        Always 0x5332 (ASCII 'S2') for Salt Channel v2.

    32  ClientEncKey.
        The public ephemeral encryption key of the client.

    32  ServerSigKey, OPT.
        The server's public signing key. Used to choose what virtual 
        server to connect to in cases when there are many to choose from.

    1   TicketSize, OPT.
        The size of the following ticket encoded in one byte.
        Allowed range: 1-127.

    x   Ticket, OPT.
        A resume ticket received from the server in a previous
        session between this particular client and server.
        The bytes of the ticket MUST NOT be interprented by the client.
        The exact interprentation of the bytes is entirely up to
        the server. See separate section.


    **** M1/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 1 for this message.

    1b  ServerSigKeyIncluded.
        Set to 1 when ServerSigKey is included in the message.

    1b  TicketIncluded.
        Set to 1 when TicketSize and Ticket are included in the message.

    1b  TicketRequested.
        Set to 1 to request a new resume ticket to use in the future to 
        connect quickly with the server.

    1b  Zero.
        Bit set to 0.
    

Messages M2 and M3
==================

The M2 message is the first message sent by the server when an 
ordinary three-way handshake is performed. It is followed by 
Message M3, also sent by the server.

By two message, M2, M3, instead one, the M3 message can be encrypted
the same way as the application messages. Also, it allows the signature
computations (Signature1, Signature2) to be done in parallel. The server
MAY send the M2 message before Signature1 is computed and M3 sent. 
In cases when computation time is long compared to communication time, 
this can decrease total handshake time significantly.

Note, the server must read M1 before sending M2 since M2 depends on the 
contents of M1.

    **** M2 ****
    
    1   Header.
        Message type and flags.
    
    32  ServerEncKey, OPT.
        The public ephemeral encryption key of the server.
    
    
    **** M2/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 2 for this message.

    1b  ServerEncKeyIncluded.
        Set to 1 when ServerEncKey is included in the message.

    1b  ResumeSupported.
        Set to 1 if the server implementation supports resume tickets.

    1b  NoSuchServer.
        Set to 1 if ServerSigKey was included in M1 but a server with such a
        public signature key does not exist at the end-point.

    1b  BadTicket.
        Set to 1 if Ticket was included in M1 but the ticket is not valid
        for some reason (bad format, expired, already used).
    

If the NoSuchServer condition occurs, ServerEncKey MUST NOT be included in 
the message. When it happens the client and the server should consider the
session closed once M2 has been sent.


    **** M3 ****

    This message is encrypted. It is sent within the body of EncryptedMessage 
    (EncryptedMessage/Body).

    1   Header.
        Message type and flags.

    32  ServerSigKey, OPT.
        The server's public signature key. MUST NOT be included if client 
        sent it in Message M1.

    64  Signature1
        The signature of ServerEncKey+ClientEncKey concatenated.


    **** M3/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 3 for this message.

    1b  ServerSigKeyIncluded.
        Set to 1 if ServerSigKey is included in the message.

    3b  Zero.
        Bits set to 0.
    

Message M4
==========

Message M4 is sent by the client. It finishes a three-way handshake.
If can (and often should be) be sent together with a first application 
message from the client to the server.


    **** M4 ****

    This message is encrypted. The message is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).

    1   Header.
        Message type and flags.

    32  ClientSigKey.
        The client's public signature key.
        
    64  Signature2.
        Signature of ClientEncKey+ServerEncKey concatenated.


    **** M4/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 4 for this message.

    4b  Zero.
        Bits set to 0.


EncryptedMessage
================

Messages M3, M4, and the application messages are encrypted. They are included
in the field EncryptedMessage/Body.

    **** EncryptedMessage ****
    
    1   Header.
        Message type and flags.
        
    x   Body.
        This is the ciphertext of the cleartext message encrypted with 
        [TODO insert here]. The message authentication prefix (16 bytes) is included.
        So, this field is at least 16 bytes long.


    **** EncryptedMessage/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 6 for this message.

    1b  CloseFlag.
        Set to 1 for the last message sent by the peer.

    3b  Zero.
        Bits set to 0.
        

Application messages
====================

After the handshake, application messages are sent between the client
and the server in any order.


    **** AppMessage ****

    1   Header.
        Message type and flags.
        The header includes a close bit. If MUST be set for in the last
        AppMessage sent by Client and in the last AppMessage sent by Server.

    x   EncryptedMessage.
        Encrypted application message.


    **** AppMessage/Header ****

    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 5 for this message.

    1b  CloseFlag.
        Set to 1 for the last message sent by the peer.

    3b  Zero.
        Bits set to 0.



Encryption
==========

TODO: write about the how messages are encrypted.
How Signatures are computed.



Resume feature
==============

The resume feature is implemented mostly on the server-side.
To the client, a resume ticket is just an arbitrary array of bytes
that can be used to improve handshake performance.
The client MUST allow changes to the format of resume tickets.
However, the server SHOULD follow the specification here. The resume
ticket specification here is the one that will be audited and should
have the highest possible security.

The resume feature is OPTIONAL. Servers may not implemement it. In that
case a server MUST always set the M2/ResumeSupported bit to 0.


Idea
----

The idea with the resume tickets is to support session-resume to signicantly
reduce the overhead of the Salt Channel handshake. A resumed session uses
less communication data and a zero-way overhead. Also, the handshake of
a resumed session does not require computationally heavy asymmetric 
cryptography operations.

Salt Channel resume allows a server to support the resume feature using only
one single bit of memory for each created ticket. This allows the server to 
have all sensitive data related to this feature in memory. Also the ticket
encryption key can be stored in memory only. If it is lost due to power 
failure the only affect is that outstanding tickets will become invalid
and a full handshake will required when client connect.

The clients store one ticket per server. A client can choose whether to use
the resume feature or not. It can do this per-server if it choses to.

A unique ticket index (Ticket/Encrypted/TicketIndex) is given to every 
ticket that is issued by the server. The first such index may, for example,
be the number of microseconds since Unix Epoch (1 January 1970 UTC).
After that, each issued ticket is given an index that equals the previously
issued index plus one.

A bitmap is used to protect agains replay attacks. The bitmap stores one bit
per non-experied ticket that is issued. The bit is set to 1 when a
ticket is issued and to 0 when it is used. Thus, a ticket can only be used
once. Of course, the bitmap cannot be of infinite size. In fact, the server
implementation can use a fixed size circular bit buffer. Using one megabyte 
of memory, the server can keep track of which tickets, out of the last 
8 million issued tickets, that have been used.


Ticket details
--------------


    **** Ticket ****

    1   Header. 
        Packet type and flags.
    
    2   KeyId.
        The server can used KeyId to choose one among multiple 
        encryption keys to decrypt the encrypted part of the ticket.
        Note, server-side implementation may choose to use only one 
        ticket encryption key for all outstanding tickets.

    16  Nonce.
        Nonce to use when decrypting Ticket/Encrypted.
        The nonce MUST be unique among all tickets encrypted with
        a particular key.

    x   Encrypted.
        Encrypted ticket data.
    
    
    **** Ticket/Header ****

    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 6 for this packet.

    4b  Zero.
        Bits set to 0.
    
    
    **** Ticket/Encrypted ****

    This is an encrypted packet.

    1   Header.
        The Ticket/Header repeated. For authentication purposes.
        The server MUST consider the ticket invalid if Ticket/Encrypted/Header
        differs from Ticket/Header.

    2   KeyIndex.
        The KeyIndex bytes repeated. For authentication purposes.
        The server MUST consider the ticket invalid if Ticket/Encrypted/KeyIndex
        differs from Ticket/KeyIndex.

    8   TicketIndex
        The ticket index of the ticket.
        A 8-byte integer in the range: 0 to 2^63-1 (inclusive).

    32  ClientSigKey.
        The client's public signature key. Used to identify the client.

    32  SessionKey.
        The symmetric encryption key to use to encrypt and decrypt messages
        of this session.
    

