spec-salt-channel-v2-draft1.md
==============================

About this document
-------------------

*Date*: 2017-02-22

*Status*: DRAFT1.

*Author*: Frans Lundberg. ASSA ABLOY AB, Shared Technologies, Stockholm,
frans.lundberg@assaabloy.com, phone: +46707601861.

*Thanks*: 
To Simon Johansson and Håkan Olsson for valuable comments and discussions.


Document history
----------------

* 2017-02-22. DRAFT1.



Introduction
============

Salt Channel is a secure channel protocol based on the TweetNaCl 
("tweet salt") cryptography library by Daniel Bernstein et al 
[TWEET-1, TWEET-2]. Like TweetNaCl itself, Salt Channel is small och
simple.

The protocol is essentially an implementation of the station-to-station [STS] 
protocol using the crypto primitives of TweetNaCl.
It relies on an underlying reliable communication channel between two 
peers. TCP is an important example of such a channel, but Salt Channel is in
no way restricted to TCP. In fact, Salt Channel can be successfully 
implemented on top of WebSocket, RS485, Bluetooth, and NFC.

This is the second version of the protocol, *Salt Channel v2*. The major changes
is the removal of the Binson dependency and the addition of the server
protocol support messages that allows the client to query the server about 
what protocols it supports.

Salt Channel is "Powered by Curve25519".


Changes from v1
===============

Salt Channel v2 is a new version of Salt Channel. It is incompatible 
with v1.

The major changes are: 

1. Binson dependency removed.
2. Server protocol info added.
3. Signature1, Signature2 modified to include sha512(M1) + sha512(M2).

The Binson dependency is removed to make the protocol independent 
of the specification. Also, it means more fixed sizes and offsets
which can be beneficiary for performance; especially on 
small embedded processors.

The server protocol info feature allows the server to tell what 
protocols and protocol versions it supports before a the real session 
is initiated by the client. This allows easy future Salt Channel version 
upgrades since both the client and the server may support multiple
versions in parallel.

The Signatures changed to follow recommendations in Cryptography Enginnering
[SCHNEIER]. It does make sense to add integrity checks to M1, M2. This way
all messages have integrity protection and all but M1, M2 have confidentiality
protection.


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
 
    Session = M1 M2 M3 M4 AppMessage*

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

Overview of a typical Salt Channel session:

    
    CLIENT                                                 SERVER
    
    ProtocolIndicator
    ClientEncKey
    [ServerSigKey]               ---M1----->
                
                                 <--M2------         ServerEncKey
                                   
                                                     ServerSigKey
                                 <--E(M3)---           Signature1
    
    ClientSigKey
    Signature2                   ---E(M4)--->
    
    AppMessage                   <--E(App)-->          AppMessage
    
            Figure 1: Salt Channel messages. "E()" is 
            used to indicate that a message is authenticated
            and encrypted.
    


Message details
===============

This section describes how a message is represented as an array
of bytes. The size of a message is known by the layer above.
The term *message* is used for a whole byte array message and
*packet* is used to refer to a byte array -- either a full message
or a part of a message.

Packets are presented below with fields of specified sizes.
If the size number has a "b" suffix, the size is in bits, otherwise
it is the byte size.

Byte order: little-endian byte order is used. The first byte is the 
least significant one. Bit order: the "first" bit (Bit 0) of a 
byte is the least-significant bit.

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
    
    4   ProtocolIndicator.
        Always ASCII 'SCv2'. Protocol indicator for Salt Channel v2.
        
    1   Header.
        Message type and flags.
    
    32  ClientEncKey.
        The public ephemeral encryption key of the client.
    
    32  ServerSigKey, OPT.
        The server's public signing key. Used to choose what virtual 
        server to connect to in cases when there are many to choose from.
    
    
    **** M1/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 1 for this packet type.
    
    1b  ServerSigKeyIncluded.
        Set to 1 when ServerSigKey is included in the message.
    
    3b  Zero.
        Bits set to 0.
    

Messages M2 and M3
==================

The M2 message is the first message sent by the server when the 
handshake is performed. It is followed by Message M3, also sent 
by the server.

By using two messages, M2, M3, instead one, the M3 message can be encrypted
the same way as the application messages. Also, it allows the signature
computations (Signature1, Signature2) to be done in parallel. The server
MAY send the M2 message before Signature1 is computed and M3 sent. 
In cases when computation time is long compared to communication time, 
this may decrease total handshake time significantly.

Note, the server must read M1 before sending M2 since M2 depends on the 
contents of M1. We could imagine a protocol where M2 could be sent before
the complete M1 has been read. However, this would not allow for the
virtual server functionality and the possibility of the server to support
multiple protocols at the same endpoint.

            
    **** M2 ****
    
    1   Header.
        Message type and flags.
    
    32  ServerEncKey.
        The public ephemeral encryption key of the server.
    
    
    **** M2/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 2 for this type of packet.
    
    1b  NoSuchServer.
        Set to 1 if ServerSigKey was included in M1 but a server with such a
        public signature key does not exist at this end-point.
        Note, when this happens, the client MUST ignore ServerEncKey.
        The server SHOULD send zero-valued bytes in ServerEncKey if this 
        condition happens.

    3b  Zero.
        Bits set to zero.
        

If the NoSuchServer condition occurs, the session is considered closed
once M2 has been sent and received.

    
    **** M3 ****
    
    This message is encrypted. It is sent within the body of EncryptedMessage 
    (EncryptedMessage/Body).
    
    1   Header.
        Message type and flags.
    
    32  ServerSigKey.
        The server's public signature key. Must be included even when
        it was specified in M1 (to keep things simple).
    
    64  Signature1
        Signature of the following elements concatenated:
        ServerEncKey + ClientEncKey + hash(M1) + hash(M2).
        hash() is used to denote the SHA512 checksum.
        Only the actual signature (64 bytes) is included in the field.
    
       
    **** M3/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 3 for this packet.
    
    4b  Zero.
        Bits set to 0.
    

Message M4
==========

Message M4 is sent by the client. It finishes a three-way handshake.
If can (and often should be) be sent together with a first application 
message from the client to the server.

    
    **** M4 ****
    
    This packet is encrypted. The packet is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    1   Header.
        Packet type and flags.
    
    32  ClientSigKey.
        The client's public signature key.
        
    64  Signature2.
        Signature of the following elements concatenated:
        ClientEncKey + ServerEncKey + hash(M1) + hash(M2).
        hash() is used to denote the SHA512 checksum.
        Only the actual signature (64 bytes) is included in the field.
    
    
    **** M4/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 4 for this packet.
    
    4b  Zero.
        Bits set to 0.
    

EncryptedMessage
================

Messages M3, M4, and the application messages (AppMessage) are encrypted.
They are included in the field EncryptedMessage/Body.

    
    **** EncryptedMessage ****
    
    1   Header.
        Message type and flags.
        
    x   Body.
        This is the ciphertext of the cleartext message encrypted with 
        [TODO insert here]. 
        The message authentication prefix (16 bytes) is included.
        This field is 16 bytes longer than the corresponding cleartext.
    
    
    **** EncryptedMessage/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 6 for this packet.
    
    4b  Zero.
        Bits set to 0.
        

AppPacket
=========

After the handshake, encrypted application packets (E(AppPacket)*) 
are sent between the client and the server in any order.

    
    **** AppPacket ****

    This packet is encrypted. It is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    1   Header.
        Message type and flags.
    
    x   Data.
        The cleartext application data.
    
    
    **** AppMessage/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 5 for this packet type.
    
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
    
    4b  PacketType.
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
    
    4b  PacketType.
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

The server also has the possibility of specifying a higher-level layer
protocol in the A2 message. This way a client can determine whether there 
is any use of connecting to the server.

Note that messages A1, A2 together form a complete Salt Channel session.
An M1 message following A1, A2 should be considered a *new* Salt Channel 
session that is completely independent of the previous A1-A2 session.


Session close
=============

This protocol is designed so that both Salt Channel peers will 
be able to agree on when a Salt Channel ends in case the 
session does not start an application layer session.
If the application layer starts successfully (handshake completed), 
it is up to the application layer to determine when the session ends.

The underlying reliable channel may be reused for multiple sequential
Salt Channel sessions. Multiple concurrent sessions over
a single underlying channel is *not* within scope of this protocol.

A Salt Channel session ends when one the the following conditions occur:

1. After message A2 is sent by Server.

2. After message M2 is sent by Server with the M2/NoSuchServer bit set to 1.

3. After the session of the layer on top (AppMessage*) ends. This is
   entrirely up to that layer to determine when the session ends.
   The Salt Channel implementation will be able to determine this.


Encryption
==========

TODO: write about the how messages are encrypted.
How Signatures are computed.


List of message types
=====================

This section is informative.
    
    PacketType   Name
    
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
    10-15        Not used
    


References
==========

* **TWEET-1**, *TweetNaCl: a crypto library in 100 tweets*. 
    Progress in Cryptology - LATINCRYPT 2014,
    Volume 8895 of the series Lecture Notes in Computer Science pp 64-83.

* **TWEET-2**, web: https://tweetnacl.cr.yp.to/.

* **NACL**, web: http://nacl.cr.yp.to/.

* **BINSON**, web: http://binson.org/.

* **STS**, *Authentication and authenticated key exchanges*, 
  Diffie, W., Van Oorschot, P.C. & Wiener, M.J. Des Codes Crypt (1992) 2: 107. 
  doi:10.1007/BF00124891.

* **VIRTUAL**, *Virtual hosting* at Wikipedia, 2017-01-04, 
  https://en.wikipedia.org/wiki/Virtual_hosting.
  
* **WS**, RFC 7936, *The WebSocket Protocol*. December 2011.

