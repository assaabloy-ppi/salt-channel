spec-salt-channel-v2-draft1.md
==============================

About
-----

About this document.

*Date*: 2017-02-22

*Status*: DRAFT1.

*Author*: Frans Lundberg. ASSA ABLOY AB, Shared Technologies, Stockholm,
frans.lundberg@assaabloy.com, phone: +46707601861.

*Thanks*: 
To Simon Johansson for valuable comments and discussions; especially for
the discussion that led to protection against delay attacks.


History
-------

Document history.

* 2017-02-22. DRAFT1 (spec-salt-channel-draft1.md).



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

Salt Channel v2 is a new version of Salt Channel. It is incompatible 
with Salt Channel v1.

The major changes are: 

1. Binson dependency removed.
2. Server protocol info added.

The Binson dependency is removed to make implementations more
independent. Also, it means more fixed sizes/offsets which is 
beneficiary for performance in some cases; especially on 
small embedded targets.

The server protocol info feature allows the server to tell what 
protocols and protocol versions it supports before a the real session 
is initiated by the client. This allows easier future Salt Channel version 
upgrades.


Temporary notes
===============

Not in final spec, of course.

* v2 does not contain delay attack protection.

* v2 does not include the resume feature.

* v2 uses CloseFlag.

* Independent message parsing. 
    Each packet should be possible to parse *independently*.
    Independently of the previous communication and any state.
    The pack/unpack code can thus be completely independent.

* Single-byte alignment.
    There is not special reason to have 2, or 4-byte alignment of
    fields in this protocol. Compactness is preferred.

* Notation. Use style: "M1/Header".



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
 
    Session = M1 M2 E(M3) E(M4) E(AppMessage)*

The M1, and M4 messages are sent by the client and M2, M3 by the server.
So, we have a three-way handshake (M1 from client, M2+M3 from server, and 
M4 from client). When the first application message is from the client, this
message can be sent together with M4 to achieve a two-way (one round-trip) 
Salt Channel overhead. Application layer messages (AppMessage*) are sent by 
either the client or the server in any order. The notation "E()" is used 
to indicate authenticated encryption; see EncryptedMessage.

A Salt Channel session can also exist of an A1-A2 session allowing the client
to ask the server about what protocols it supports:

    Session = A1 A2

After A2, the session is finished.


Message details
===============

This section describes how a message is represented as an array
of bytes. The size of the messages are known by the layer above.
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
public signing key.

Details:

    **** M1 ****

    1   Header.
        Message type and flags.

    3   ProtocolIndicator.
        Always ASCII 'SC2'. Protocol indicator for Salt Channel v2.

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
    

Messages A1 and A2
==================

Messages A1 and A2 are used by the client to query the server of which 
protocols it supports. These two messages are intended to stay stable
even if/when Salt Channel is upgraded to v3, v4, and so on.

No encryption is used. Any information sent by the server should be 
validated later once the secure channel has been established.
The A2 response by the server is assumed to be static for days or weeks or
longer. The client is allowed to cache this information.

    
    **** A1 ****
    
    Message sent by client to request server information.
    
    1   Header.
        Message type and flags.
    
    
    **** A1/Header ****
    
    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 8 for this message.
    
    1b  CloseFlag.
        Set to 1 for for this message.

    3b  Zero.
        Bits set to 0.
    

And Message A2:

    
    **** A2 ****
    
    The message sent by the server in response to an A1 message.
    
    1   Header.
        Message type and flags.
    
    1   Count
        Value between 1 and 127. The number of protocol entries
        (Prot) that follows.
        
    x   Prot+
        1 to 127 Prot packets.
    
    
    **** A2/Header ****
    
    4b  MessageType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 9 for this message.
    
    1b  CloseFlag.
        Set to 1 for for this message.

    3b  Zero.
        Bits set to 0.
    
    
    **** A2/Prot ****
    
    10  P1.
        Protocol ID of Salt Channel with version. 
        Exactly 10 ASCII bytes. Whitespace and control characters
        must be avoided.
        The value for this field in for this version of
        of Salt Channel MUST BE "SC2-------".
    
    10  P2.
        Protocol ID of the protocol on top of Salt Channel. 
        Exactly 10 ASCII bytes. Whitespace and control characters
        must be avoided.
        If the server does not wish to reveal any information about
        the layer above, the server MUST use value "----------" for 
        this field.
    

The server MUST use protocol ID "SC2-------" for this version (v2) of
Salt Channel. The plan is that future versions of Salt Channel should use
the same A1 and A2 messages. Salt Channel v2 should use "SC3-------" and 
v4 should use "SC4-------" and so on.

The server also has the possibility of specifiying a higher-level layer
ID in the A2 message. This way a client can determine whether there is any
use of connecting to the server. It may not support the protocol the client
wants to communicate with.

Note that messages A1, A2 together form a complete Salt Channel session.
An M1 message following A1, A2 should be considered a *new* Salt Channel 
session that must be completely independent of the previous A1-A2 session.


EncryptedMessage
================

Messages M3, M4, and the application messages (AppMessage) are encrypted.
They are included in the field EncryptedMessage/Body.

    
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

    This message is encrypted. The message is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    1   Header.
        Message type and flags.
        The header includes a close bit. It MUST be set in the last
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


List of message types
=====================

This section is informative.

    
    MessageType  Name
    
    0            Not used
    1            M1
    2            M2
    3            M3
    4            M4
    5            AppMessage
    6            EncryptedMessage
    7            Ticket (reserved for future version)
    8            A1
    9            A2
    


