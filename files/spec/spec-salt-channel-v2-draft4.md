spec-salt-channel-v2-draft4.md
==============================

About this document
-------------------

*Date*: 2017-05-10

*Status*: Work in progress, specification for Salt Channel v2.

*Authors*: Frans Lundberg. ASSA ABLOY AB, Stockholm, 
frans.lundberg@assaabloy.com, phone: +46707601861.
Simon Johansson, Tritech AB, Stockholm.

*Thanks*: 
To Håkan Olsson for valuable comments and discussions.


Document history
----------------

* 2017-05-xx. DRAFT4. 1-byte message types instead of 4 bits. Improved text.

* 2017-03-29. DRAFT3. Work in progress with adding resume feature.

* 2017-03-xx. DRAFT2. 2-byte headers. Time fields added. A1A2 functionality.
  added.

* 2017-02-22. DRAFT1.



Introduction
============

Salt Channel is a secure channel protocol based on the TweetNaCl 
("tweet salt") cryptography library by Daniel Bernstein et al 
[TWEET-1, TWEET-2]. Like TweetNaCl itself, Salt Channel is simple, small,
and auditable.

The protocol is essentially an implementation of the station-to-station [STS] 
protocol using the cryptography primitives of TweetNaCl.
Salt Channel relies on an underlying reliable bidirectional communication 
channel between the two peers communicating. TCP is an important example 
of such an underlying channel, but Salt Channel is in no way restricted to 
TCP. In fact, Salt Channel has been successfully implemented on top of 
WebSocket, RS485, Bluetooth, and Bluetooth Low Energy.

This is the second version of the protocol, called *Salt Channel v2*. 
The major changes from v1 is the removal of the Binson dependency,
the addition of the resume feature, and the protection against 
delay attacks.

Salt Channel is *Powered by Curve25519*. The cryptographic algorithms used
are those provided by TweetNaCl: ed25519 with sha512 for signatures, 
x25519+xsalsa20+poly1305 for authenticated public-key encryption, and
sha512 for secure hashing.

The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", 
"SHOULD", "SHOULD NOT", "RECOMMENDED",  "MAY", and "OPTIONAL" in this 
document are to be interpreted as described in [RFC2119];



Changes from v1
===============

Salt Channel v2 is a new version of Salt Channel. It is incompatible 
with v1.

The major changes are: 

1. The Binson dependency is removed.
2. The resume feature is added.
3. Signature1, Signature2 are modified to include sha512(M1) + sha512(M2).
4. The server protocol info is added.

The Binson dependency is removed to make the protocol independent 
of that specification. Also, it means more fixed sizes and offsets
which may be beneficiary for performance; especially on resource-constrained
processors.

The server protocol info feature allows the server to tell what 
protocols and protocol versions it supports before a the real session 
is initiated by the client. This allows easy future Salt Channel version 
upgrades since both the client and the server may support multiple
versions in parallel.

The signatures changed to follow recommendations in Cryptography Enginnering
[SCHNEIER]. It does make sense to add integrity checks to messages M1, M2. 
This way all messages have integrity protection and all but M1, M2 have 
confidentiality protection.


Author's notes
==============

Not in final spec.

* Check TODO markers in text.

* v2 uses CloseFlag.

* Independent message parsing. 
    Each packet should be possible to parse *independently*.
    Independently of the previous communication and any state.
    The pack/unpack code can thus be completely independent.

* Single-byte alignment.
    There is not special reason to have 2, or 4-byte alignment of
    fields in this protocol. Compactness is preferred.

* Notation: use style: "M1/Header".

* TODO: include example session in appendix. Generate from code.


Protocol design
===============

This section is informative.


Priorities
----------

The following priorities were used when designing the protocol.

1. The first priority is to achieve *high security*. 

2. The second priority is to achieve a *low network overhead*; 
   that is, few round-trips and a small data overhead.
   
3. The third priority is to allow for *low code complexity*, 
   and *low CPU requirements* of the communicating peers.

Low complexity is always important to achieve high security.


Goals
-----

The following are the main goals and limitations of the protocol.

* **128-bit security**. 
    The best attack should be a 2^128 brute force attack. 
    No attack should be possible until there are (if there ever will be) 
    large-enough quantum computers.

* **Forward secrecy**.
    Old recorded communication data will not be possible to 
    decrypt even if one or both peer's private keys are compromised.

* **Secret client identity**.
    An active or passive attacker cannot retrieve the long-term 
    public key of the client. Tracking of the client is impossible.
    
* **Simple protocol**.
    Should be possible to implement in few lines of code. Should be auditable 
    just like TweetNaCl.

* **Compact protocol** (few bytes).
    Designed for Bluetooth low energy and other low-bandwidth channels.
    
* Limitation: No certificates.
    Simplicity and compactness are preferred.
    The use of certificates together with the public keys of Salt Channel
    is a possibility, but not included in the protocol. It could be included
    in an upper layer.
    
* Limitation: the protocol is not intended to be secure for an 
    attacker with a large quantum computer. Such a computer does not exist.
    This is a limitation of the underlying TweetNaCl library.
    
* Limitation: no attempt is made to hide the length, sequence, or timing
  of the communicated messages.

* Limitation: no attempt is made to protect against denial-of-service 
  attacks. This is an important topic. Perhaps large enough to 
  warrant a special protocol for that. Anyway, the possibility of 
  denial-of-service attacks varies greatly with the situation, such as
  the type of the underlying reliable channel. Solving this in a generic way
  is too complex to be considered for Salt Channel.


Layer below
===========

Salt Channel can be implemented on top of any underlying channel that provides
reliable, order-preserving, bidirectional communication. This section 
describes how Salt Channel is implemented on top of a stream-type of 
channel such as TCP, and how it is implemented on top of a Web Socket.

Excluding this section, this specification only deals with byte arrays 
*of known size*. The underlying layer provides an order-preserving 
exchange of byte arrays with a known size.


Salt Channel over a stream
--------------------------

When Salt Channel is implemented on top of a stream, such as TCP, the following
format is used:

    Stream = (Size Message)+
    
*Size* is a 32-bit integer with the byte size of 
the following Message. Its valid range is (0, 2^31-1), so either
an unsigned or signed 32-bit integer work for storing it in computer memory.
*Message* is the raw message bytes. The format of these Message:s is 
defined in this document.


Salt Channel over Web Socket
----------------------------

Binary Web Socket [WS] connections are already in the "chunked" format. 
Applications send byte arrays of know sizes rather than a stream of bytes.
The byte arrays arrive at the receiver application as a byte arrays with
know sizes. A binary web socket communicates using a stream of byte arrays
rather than a stream of bytes.

When Salt Channel is implemented over a binary Web Socket, the "Size" prefix 
(32-bit), used when Salt Channel is implementation over TCP, is unnecessary.
It is already provided by the Web Socket layer. So Salt Channel over Web Socket 
is very simple. Each web socket message is a Binson object as specified in 
this document.



Session
=======

The message order of an ordinary successful Salt Channel session is:
 
    Session = M1 M2 E(M3) E(M4) E(AppMessage)*

The M1, and M4 messages are sent by the client and M2, M3 by the server.
So, we have a three-way handshake (M1 from client, M2+M3 from server, and 
M4 from client). In the common case when the first application message 
is from the client, this message should normally be sent together with M4
to achieve a one round-trip overhead (instead of two). Application layer 
messages (AppMessage*) are sent by either the client or the server in 
any order. The notation "E()" is used to indicate authenticated encryption; 
see the section on EncryptedMessage. This may be omitted for clarity; 
messages following M1, M2 are always encrypted.

A Salt Channel session can also exist of an A1-A2 session allowing the client
to ask the server about its public server information (not encrypted).

    Session = A1 A2

After A2, the session is finished.

An overview of a typical Salt Channel session is shown below.

    
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
            used to indicate that a message is encrypted and
            authenticated.
    
When the client holds a resume ticket, he can use it to achieve a
zero-round-trip overhead session. More about that later. See the
figure below for an overview.

    
    CLIENT                                                 SERVER
    
    Header
    ClientEncKey
    [ServerSigKey]
    Ticket
    AppMessage                   ---M1, App -->
                                                             Ticket
                                 <--TT, App ---          AppMessage
        
    AppMessage                   <--App------->          AppMessage

            Figure 2: Salt Channel messages for the resume
            case. First application message is sent with no
            round-trip overhead.
    
Later section will describe these messages in detail.


Message details
===============

The message detail sections that follow describe how a message is 
represented as an array of bytes. The size of a message is known 
from the layer below. When the layer below is a stream (like TCP
for example) each message is prefixed with the byte size of the 
message as described in the section "Salt Channel over a stream".

The term *message* is used for a whole byte array message of the 
protocol (encrypted or not) and *packet* is used to refer to any
byte array -- either a full message or a part of a message.

Packets are presented below with fields of specified sizes.
If the size number has a "b" suffix, the size is in bits, 
otherwise it is in bytes.

Regarding byte order and bit order; a *little-end-first* approach is 
used. *Little-endian byte order MUST be used*.
The first byte (Byte 0) is the least significant byte of an integer.
Bit order: the first bit (Bit 0) of a byte is the least-significant bit.

Unless otherwise stated explicitly, bits are set to 0.

Unless otherwise described, a range of an integer is expressed 
using an *inclusive* range, for example:
"0 to 127" means any integer between 0 and 127 including 0 and 127.

The word "OPT" is used to mark a field that may or may not exist
in the packet. It does not necessarily indicate a an optional field 
in the sense that it independently may exist or not. Whether its existence
is optional, mandatory or forbidden may depend on other fields and/or
the state of the communication so far.


Message M1
==========
    
The first message of a Salt Channel session is always the M1 message.
It is sent from the client to the server. It includes a protocol indicator, 
the client's public ephemeral encryption key and optionally the server's
public signing key. It may also include a resume ticket.

Details:

    
    **** M1 ****
    
    4   ProtocolIndicator.
        Always ASCII 'SCv2'. Protocol indicator for Salt Channel v2.
        
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    32  ClientEncKey.
        The public ephemeral encryption key of the client.
    
    32  ServerSigKey, OPT.
        The server's public signing key. Used to choose what virtual 
        server to connect to in cases when there are many to choose from.
    
    1   TicketSize, OPT.
        Size of the following ticket in bytes. 
        Allowed value range: 0-127.
        
    x   Ticket, OPT.
        A ticket received from the server in the previous
        session between this particular client and server.
        The ticket data MUST NOT be interpreted by the client,
        except for the ticket size.
        The exact interpretation of the bytes is up to
        the server. See separate documentation.
    
    
    **** M1/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 1 for this packet.
    
    1b  ServerSigKeyIncluded.
        Set to 1 when ServerSigKey is included in the message.
    
    1b  TicketIncluded.
        Set to 1 when Ticket is included in the message.
    
    1b  TicketRequested.
        Set to 1 to request a new resume ticket to use in the future to 
        connect quickly with the server.
        
    5b  Zero.
        Bits set to 0.
    

Messages M2 and M3
==================

The M2 message is the first message sent by the server when the 
handshake is performed. It is followed by Message M3, also sent 
by the server.

By using two messages, M2, M3, instead one, the M3 message can be encrypted
the same way as the application messages. Also, it allows the signature
computations (Signature1, Signature2) to be done in parallel. The server
MAY send the M2 message before Signature1 is computed.
In cases when computation time is long compared to communication time, 
this can decrease total handshake time.

Note, the server must read M1 before sending M2 since M2 depends on the 
contents of M1. We could imagine a protocol where M2 could be sent before
the complete M1 has been read. However, this would not allow for the
virtual server functionality and the possibility of the server to support
multiple protocols at the same endpoint.

            
    **** M2 ****
    
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    32  ServerEncKey.
        The public ephemeral encryption key of the server.
    
    
    **** M2/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 2 for this packet.
        
    1b  NoSuchServer.
        Set to 1 if ServerSigKey was included in M1 but a server with such a
        public signature key does not exist at this end-point.
        Note, when this happens, the client MUST ignore ServerEncKey.
        The server MUST send zero-valued bytes in ServerEncKey if this 
        condition happens.
        
    1b  ResumeSupported.
        Set to 1 if the server supports resume tickets.
    
    6b  Zero.
        Bits set to zero.
        

If the NoSuchServer condition occurs, the session is considered closed
once M2 has been sent and received.

    
    **** M3 ****
    
    This message is encrypted. It is sent within the body of EncryptedMessage 
    (EncryptedMessage/Body). If a ticket was requested in M1 and the server supports
    resume, a newly issued ticket is included in the message.
    
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    32  ServerSigKey.
        The server's public signature key. Must be included even when
        it was specified in M1 (to keep things simple).
    
    64  Signature1
        Signature of the following elements concatenated: hash(M1) + hash(M2).
        hash() is used to denote the SHA512 checksum.
        Only the actual signature (64 bytes) is included in the field.
    
    
    **** M3/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 3 for this packet.
    
    8b  Zero.
        Bits set to 0.
    

Message M4
==========

Message M4 is sent by the client. It finishes a three-way handshake.
If can (and typically should be) be sent together with a first application 
message (or messages) from the client to the server.

    
    **** M4 ****
    
    This packet is encrypted. The packet is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    2   Header.
        Packet type and flags.
        
    4   Time.
        See separate documentation.
    
    32  ClientSigKey.
        The client's public signature key.
        
    64  Signature2.
        Signature of the following elements concatenated: hash(M1) + hash(M2).
        hash() is used to denote the SHA512 checksum.
        Only the actual signature (64 bytes) is included in the field.
    
    
    **** M4/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 4 for this packet.
    
    8b  Zero.
        Bits set to 0.
    

EncryptedMessage
================

Messages M3, M4, and the application messages (AppMessage) are encrypted.
They are included in the field EncryptedMessage/Body.

   
    **** EncryptedMessage ****
    
    2   Header.
        Message type and flags.
        
    x   Body.
        This is the ciphertext of the cleartext message.
        The message authentication prefix (16 bytes) is included.
        This field is 16 bytes longer than the corresponding cleartext.
    
    
    **** EncryptedMessage/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 6 for this packet.
    
    8b  Zero.
        Bits set to 0.
        

AppPacket
=========

After the handshake, encrypted application packets (E(AppPacket)*) 
are sent between the client and the server in any order.

    
    **** AppPacket ****

    This packet is encrypted. It is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    x   Data.
        The cleartext application data.
    
    
    **** AppMessage/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 5 for this packet.
    
    8b  Zero.
        Bits set to 0.


The time field
==============

Messages have a Time field. It either contains the time since the first
message of the peer was sent. That is, the time since M1 was sent for the
client and the time since M2 was sent for the server. The elapsed time is 
measured in milliseconds. The Time field MUST have the value 1 for the first
message sent (M1 for the client and M2 for the server) when the Time field
is supported by the peer.

The main reason to introduce these time-stamps is to protect against 
*delay attacks*; that is, a man-in-the-middle attacker that affects the 
application behavior by delaying a message sent between the two peers.
The blog post at [DELAY-ATTACK] describes this type of attack.

A peer that is capable of measuring relative time in milliseconds SHOULD
support the Time field in the messages. A peer that does not support
the Time field MUST set the value of the Time field to zero (four zero-valued
bytes) of *all* messages in a Salt Channel session.

Format: The Time field consists of an integer in the range 0 to 2^31-1.
This means that either a signed or an unsigned 32-bit integer can
be used to represent the time. Note: 2^31-1 milliseconds is more than 24 days.


Resume
======

The resume feature is implemented mostly on the server-side.
To the client, a resume ticket is just an arbitrary array of encrypted bytes
that can be used to improve Salt Channel performance.
The client MUST allow changes to the format of resume tickets.
However, the server SHOULD follow the specification here. The resume
ticket specification here is the one that is audited and it is conjectured
to be secure.

The resume feature is OPTIONAL. Servers MAY NOT implement it. In that
case a server MUST always set the M2/ResumeSupported bit to 0.
Also for a client, the resume feature is OPTIONAL. If a client does not
support resume, it MUST set never request a ticket from the server.
Technically, such a client MUST set the M1/Header/TicketRequested bit
to zero.


Idea
----

The idea with the resume tickets is to support session-resume to significantly
reduce the overhead of the Salt Channel handshake. A resumed session uses
less communication data and a zero round-trip overhead. Also, the handshake of
a resumed session does not require CPU intensive asymmetric cryptography.

Salt Channel resume allows a server to support the resume feature using only
one single bit of memory for each created ticket. This allows the server to 
have all data related to this feature in memory. The ticket
encryption key can be stored in memory. If it is lost due to power 
failure, the only affect is that outstanding tickets will become invalid
and a full handshake will required when a client connects.

A client stores one ticket per server. The client can choose whether to use
the resume feature or not. It can do this on a per-server basis.

A unique ticket ID (message field: Ticket/Encrypted/TicketId) is 
given to every ticket that is issued by the server. The first such 
index may, for example, be the number of microseconds since 
Unix Epoch (1 January 1970 UTC). After that, each issued ticket is 
given an ID that, for example, equals the previously issued 
ID plus one. It is entirely up to the server how to choose
ticket IDs.

A bitmap is used to protect against replay attacks. The bitmap stores one bit
per non-expired ticket that is issued. The bit is set to 1 when a
ticket is issued and to 0 when it is used. Thus, a ticket can only be used
once. Of course, the bitmap cannot be of infinite size. In fact, the server
implementation can use a fixed size circular bit buffer. Using one megabyte 
of memory, the server can keep track of which tickets, out of the last 
8 million issued tickets, that have been used.


Three cases
-----------

To introduce the resume feature from a communication standpoint, we consider
three cases: 

* Case A, *no ticket*,
* Case B, *valid ticket*, and
* Case C, *invalid ticket*.

Also, we consider a case where application message App1 is first sent
to the server and then the server replies with the App2 application message;
a simple request-response is the first messages exchanged at the application
layer.

*Case A, no ticket*, is shown in the figure below.
    
    ---> M1
    <--- M2, M3
    ---> M4, App1
    <--- TT, App2
    
    Figure: The message flow for Case A. The client does not have a ticket, but requests one.
    
The client does not have a ticket, but requests one. 
The normal three-way handshake is used with an additional message
TT that is sent together with the first application message from the
server to the client.
The client requests a ticket (M1/Header/TicketRequested is set to 1)
and the server supports resume, so the server will include a 
newly created ticket in TT.
The messages M3, M4, App1, TT, App2 are encrypted (EncryptedMessage).
The new ticket cannot be issued until after M4.
Before that, the server does not know the identity (ClientSigKey)
of the client.

If the client does not request a new ticket, the server sends
no TT message.

*Case B, valid ticket*, is shown in the figure below.
    
    ---> M1, App1
    <--- TT, App2
    
    Figure: The message flow for Case C. The client has a valid ticket.
    
The client sends the ticket in M1. The first application message,
App1, is sent together with M1. The server determines that the 
ticket is valid and replies with a TT message. If requested in M1,
TT contains a new ticket to be used next time the client connects
to this server. The messages App1, TT, App2 are encrypted
(EncryptedMessage) with the ticket encryption key.

*Case C, invalid ticket*, is shown in the figure below.
    
    ---> M1, App1A
    <--- M2, M3
    ---> M4, App1B
    <--- App2
    
    Figure: The message flow for Case B. The client has an invalid ticket.
    
The client sends the ticket in M1. The first application message, 
App1A, is sent together with M1. However, the server determines that
the ticket is not valid and replies with M2, M3. The M2 has the bit
M2/Header/BadTicket set to 1. The client notices that the ticket
was not accepted and then sends M4, App1B. Note that App1 
must be sent again to the server with the new encryption key
computed from a key agreement using the public keys exchanged 
in M1 and M2.

If a new ticket was requested in M1 (M1/Header/TicketRequested set to 1),
a ticket will be included in M3.
Message App1A is encrypted with the (invalid) ticket encryption key.
Messages M3, M4, App1B, App2 are encrypted with the session encryption key.


Message TT
----------

This message is sent in response to a message M1 that includes a 
valid ticket. The TT message includes a new ticket if the client
requested one.

    **** TT ****

    This packet is encrypted. The packet is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    2   Header. 
        Packet type and flags.
        
    4   Time.
        See separate documentation.
    
    2   Ticket, OPT.
        A Ticket (encrypted) from the server.
    
    
    **** TT/Header ****

    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 10 for this packet.

    1b  TicketIncluded.
        Set to 1 when Ticket is included in the message.
        
    11b Zero.
        Bits set to 0.
    

Ticket details
--------------

The details of the RECOMMENDED ticket format.
TODO: update this.

    **** Ticket ****

    2   Header. 
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

    12b Zero.
        Bits set to 0.
    
    
    **** Ticket/Encrypted ****

    This is an encrypted packet.
    TODO update this.

    2   Header.
        The Ticket/Header repeated. For authentication purposes.
        The server MUST consider the ticket invalid if Ticket/Encrypted/Header
        differs from Ticket/Header.

    2   KeyId.
        Repeated for data authentication purposes.
        The server MUST consider the ticket invalid if Ticket/Encrypted/KeyId
        differs from Ticket/KeyId.

    8   TicketId
        The ID of the ticket.
        A 8-byte integer in the range: 0 to 2^63-1.

    32  ClientSigKey.
        The client's public signature key. Used to identify the client.

    32  SessionKey.
        The symmetric encryption key to use to encrypt and decrypt messages
        of this session.
    

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
    
    2   Header.
        Message type and flags.
    
    
    **** A1/Header ****
    
    4b  PacketType.
        Four bits that encodes an integer in range 0-15.
        The integer value is 8 for this message.
    
    1b  CloseFlag.
        Set to 1 for for this message.

    11b Zero.
        Bits set to 0.
    

And Message A2:

    
    **** A2 ****
    
    The message sent by the server in response to an A1 message.
    
    2   Header.
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
    
    11b Zero.
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

TODO: write about the how messages are encrypted in detail. With reference to TweetNaCl/NaCl.

TODO: write about how signatures are computed in detail. With reference to TweetNaCl/NaCl.


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
    7            Ticket
    8            A1
    9            A2
    10           TT
    11-127      Not used
    


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

* **DELAY-ATTACK**, http://blog.franslundberg.com/2017/02/delay-attacks-forgotten-attack.html.

* **RFC2119**, RFC 2119 by S. Bradner, https://www.ietf.org/rfc/rfc2119.txt
