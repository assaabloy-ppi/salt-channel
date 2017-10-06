spec-salt-channel-v2-draft6.md
==============================

About
-----

About this document.

*Date*: 2017-10-06.

*Status*: Salt Channel v2 specification, DRAFT.

*Authors*:
 
* Frans Lundberg. ASSA ABLOY AB, Stockholm, frans.lundberg@assaabloy.com, 
  phone: +46707601861.
* Simon Johansson, ASSA ABLOY AB, Stockholm, simon.johansson@assaabloy.com.

*Thanks*:

* To Håkan Olsson for valuable comments and discussions.
* To Daniel Bernstein for creating TweetNaCl.


History
-------

* 2017-10-xx. DRAFT6. Resume feature removed from spec.

* 2017-10-02. DRAFT5. Prefixes "SC-SIG01", "SC-SIG02" added to signatures,
  LastFlag used. Text more complete. MultiAppPacket introduced.

* 2017-05-15. DRAFT4. 1-byte message types instead of 4 bits. Improved text.

* 2017-03-29. DRAFT3. Work in progress with adding resume feature.

* 2017-03-15. DRAFT2. 2-byte headers. Time fields added. A1A2 functionality.
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
WebSocket, RS485, NFC, and Bluetooth Low Energy (BLE) and also on 
combinations of such links, for example BLE + TCP.

This is the second version of the protocol, called *Salt Channel v2*. 
The major changes from v1 is the removal of the Binson dependency,
and the protection against delay attacks.

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
with v1 and replaces it.

The major changes are: 

1. The Binson dependency is removed.
2. Signature1, Signature2 are modified to include hash(M1) and hash(M2).
3. The *protocol info* (A1A2) feature is added.

The Binson dependency is removed to make the protocol independent 
of that specification. Also, it means more fixed sizes and offsets
which may be beneficiary for performance; especially on resource-constrained
processors.

The protocol info feature (messages A1, A2) allows the server to 
tell what protocols and protocol versions it supports before a the real session 
is initiated by the client. This allows easy future Salt Channel version 
upgrades since both the client and the server may support multiple
versions in parallel. Note, A1+A2 actually form an independent protocol 
that can be used for other protocols than Salt Channel.

The signatures changed to follow recommendations in Cryptography Engineering
[SCHNEIER]. It does make sense to add integrity checks to messages M1, M2. 
This way all messages have integrity protection and all but M1, M2 have 
confidentiality protection.




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

Low complexity is also, in itself, important to achieve high 
security.


Goals
-----

The following are the main goals and limitations of the protocol.

* **128-bit security**. 
    The best attack should be a 2^128 brute force attack. 
    No attack should be possible until there are (if there ever will be) 
    large-enough quantum computers.

* **Forward secrecy**.
    Recorded communication will not be possible to decrypt even 
    if one or both peer's private keys are compromised.

* **Secret client identity**.
    An active or passive attacker cannot retrieve the long-term 
    public key of the client. Tracking of the client is impossible.
    
* **Simple protocol**.
    It should be possible to implement in few lines of code. 
    Should be auditable just like TweetNaCl.

* **Compact protocol** (few bytes).
    Designed for Bluetooth Low Energy and other low-bandwidth channels.
    
* Limitation: No certificates.
    Simplicity and compactness are preferred.
    Using certificates together with the Salt Channel public keys
    is possible, but not included in the protocol.
    
* Limitation: the protocol is not intended to be secure for an 
    attacker with a large quantum computer. Such a computer does not exist.
    This is a limitation from the underlying TweetNaCl library.
    
* Limitation: no attempt is made to hide the length, sequence, or timing
    of the communicated messages.

* Limitation: no attempt is made to protect against denial-of-service 
    attacks. This is an important topic. Perhaps important enough to 
    warrant a dedicated protocol. Anyway, the possibility of 
    denial-of-service attacks varies profoundly with the situation, 
    such as the type of the underlying reliable channel. Solving this in 
    a generic way is too complex to include in this protocol.

A principle used is that each packet can be parsed *independently* of 
any session state. The pack/unpack code is thus simplified. There is
little to gain from not following this principle.




Layer below
===========

Salt Channel can be implemented on top of any underlying channel that provides
reliable, order-preserving, bidirectional communication. This section 
describes how Salt Channel is implemented on top of a stream-type of 
channel such as TCP, and how it is implemented on top of a Web Socket.

Excluding this section, this specification only deals with byte arrays 
*of known size*. The underlying layer provides an order-preserving 
exchange of byte arrays, each with a known size.


Salt Channel over a stream
--------------------------

When Salt Channel is implemented on top of a stream, such as TCP, the following
format is used:

    Stream = (Size Message)+
    
*Size* is a 32-bit integer with the byte size of 
the following Message. Its valid range is (0, 2^31-1), so either
an unsigned or signed 32-bit integer work for storing it in computer memory.
*Message* is the raw message bytes. The format of these messages is 
defined in this document.


Salt Channel over Web Socket
----------------------------

Binary Web Socket [WS] connections are already in the "chunked" format. 
Applications send byte arrays of know sizes rather than a stream of bytes.
The byte arrays arrive at the receiver application with know sizes. 
A binary web socket communicates using a stream of byte arrays rather
than a stream of individual bytes.

When Salt Channel is implemented over a binary Web Socket, the "Size" prefix 
used when Salt Channel is implementation over TCP, is unnecessary.
It is already provided by the Web Socket layer. So Salt Channel over Web Socket 
is very simple. Each web socket message is a message as specified in 
this document.




Session
=======

The message order of an ordinary successful Salt Channel session is:
 
    Session = M1 M2 E(M3) E(M4) E(AppPacket)*

The M1, and M4 messages are sent by the client and M2, M3 by the server.
So, we have the typical three-way handshake (M1 from client, 
M2+M3 from server, and M4 from client). In the common case when the 
first application message is from the client, this message SHOULD be 
sent together with M4 to achieve a one round-trip overhead (instead 
of two). Application layer messages (AppPacket:s) are sent by either 
the client or the server in any order. The notation "E()" is used to indicate
authenticated encryption; see the section on EncryptedMessage. This 
notation may be omitted for clarity; messages following M1, M2 are 
always encrypted.

A Salt Channel session can also exist of an A1-A2 session allowing the client
to ask the server about its public server protocol information.
    
    Session = A1 A2
    
After A2, the session is finished. Note, there is no confidentiality
or integrity protection for the A1-A2 exchange, the messages are
sent in cleartext.

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
    
    AppPacket                    <--E(AppPacket)-->     AppPacket
    
        Figure: Salt Channel messages. "E()" is used to indicate that a 
        message is encrypted and authenticated. Header and Time fields are not
        included in the figure. They are included in every message.
            

Later sections will describe these messages in detail.



Message details
===============

The message detail sections that follow describe how a message is 
represented as an array of bytes. The size of a message is always 
known from the layer below. When the layer below is a stream (like TCP
for example) each message is prefixed with the byte size of the 
message as described in the section "Salt Channel over a stream".

The term *message* is used for a whole byte array message of the 
protocol and *packet* is used to refer to any byte array -- 
either a full message or a part of a message.

Packets are presented below with fields of specified sizes.
If the size has a "b" suffix, the size is in bits, otherwise 
it is in bytes.

Regarding byte order and bit order; a *little-end-first* approach is 
used. *Little-endian byte order MUST be used*.
The first byte (Byte 0) is the least significant byte of an integer.
Bit order: the first bit (Bit 0) of a byte is the least-significant bit.

Unless otherwise stated explicitly, bits are set to 0.

Unless otherwise described, a range of an integer is expressed 
using an *inclusive* range, for example:
"0 to 127" means any integer between 0 and 127 including 0 and 127.

The word "OPT" is used to mark a field that MAY or MAY NOT exist
in the packet. It does not necessarily indicate an optional field 
in the sense that it independently may exist or not. Whether its existence
is optional, mandatory or forbidden may depend on other fields and/or
the state of the communication session so far.




M1
==
    
The first message of a Salt Channel session is always the M1 message.
It is sent from the client to the server. It includes a protocol indicator, 
the client's public ephemeral encryption key and optionally the server's
public signing key.

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
    
    
    **** M1/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 1 for this packet.
    
    1b  ServerSigKeyIncluded.
        Set to 1 when ServerSigKey is included in the message.
        
    7b  Zero.
        Bits set to 0.
    



M2 and M3
=========

The M2 message is the first message sent by the server when the 
handshake is performed. It is followed by Message M3, also sent 
by the server.

By using two messages, M2, M3, instead one, the M3 message can be encrypted
the same way as the application messages. Also, it allows the signature
computations (Signature1, Signature2) to be done in parallel. The server
MAY send the M2 message before Signature1 is computed.
In cases when computation time is long compared to communication time, 
this can decrease the overall handshake time.

Note, the server MUST read M1 before sending M2 since M2 depends on the 
contents of M1. We could imagine a protocol where M2 could be sent before
the complete M1 has been read. However, this would not allow for the
virtual server functionality and the possibility of the server to support
multiple protocols on the same endpoint.
    
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
        public signature key does not exist at this end-point or could not be
        connected to. Note, when this happens, the client MUST ignore ServerEncKey.
        The server MUST send zero-valued bytes in ServerEncKey if this 
        condition happens.
        
    6b  Zero.
        Bits set to zero.
    
    1b  LastFlag.
        Set to 1 when this is the last message of the session.
        That is, when the NoSuchServer bit is set.
    

If the NoSuchServer condition occurs, the session is considered closed
once M2 has been sent and received. Furthermore, the client's behavior 
MUST NOT be affected be the value of M2/ServerEncKey when NoSuchServer
occurs. When the NoSuchServer bit is set the server MUST send 32 zero-valued
bytes in the field M2/ServerEncKey.
    
    **** M3 ****
    
    This message is encrypted. It is sent within the body of EncryptedMessage 
    (EncryptedMessage/Body).
    
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    32  ServerSigKey.
        The server's public signature key. Must be included even when
        it was specified in M1 (to keep things simple).
    
    64  Signature1
        The following: sig("SC-SIG01" + hash(M1) + hash(M2)).
        "SC-SIG01" are the bytes: 0x53, 0x43, 0x2d, 0x53, 0x49, 0x47, 0x30, 0x31.
        hash() is used to denote the SHA512 checksum.
        "+" is concatenation and "sig" is defined on the "Crypto details" section.
        Only the actual signature (64 bytes) is included in the field.
    
    
    **** M3/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 3 for this packet.
    
    8b  Zero.
        Bits set to 0.
    



M4
==

Message M4 is sent by the client. It finishes a three-way handshake.
It can (and typically should) be sent together with a first application 
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
        The following: sig("SC-SIG02" + hash(M1) + hash(M2)).
        hash() is used to denote the SHA512 checksum.
        "SC-SIG02" are the bytes: 0x53, 0x43, 0x2d, 0x53, 0x49, 0x47, 0x30, 0x32.
        "+" is concatenation and "sig" is defined in the "Crypto details" section.
        Only the actual signature (64 bytes) is included in the field.
    
    
    **** M4/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 4 for this packet.
    
    8b  Zero.
        Bits set to 0.
    



EncryptedMessage
================

Messages M3, M4, and the application messages (AppPacket, MultiAppPacket) 
are encrypted. They are included in the field EncryptedMessage/Body.
   
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
    
    7b  Zero.
        Bits set to 0.
        
    1b  LastFlag.
        Set to 1 for the very last message of the Salt Channel session
        (both directions) and 0 otherwise.
       

See the section "Crypto" for details of the authenticated encryption.




AppPacket and MultiAppPacket
============================

After the handshake, encrypted application packets are sent between 
the client and the server in any order. Either AppPacket:s are used or
MultiAppPacket:s. The MultiAppPacket is an optimization; it reduces the
overhead due to headers and crypto when sending multiple application 
message at once. 

    
    **** AppPacket ****

    This packet is encrypted. It is sent within the body of 
    EncryptedMessage (EncryptedMessage/Body).
    
    2   Header.
        Message type and flags.
        
    4   Time.
        See separate documentation.
    
    x   Data.
        The cleartext application data.
    
    
    **** AppPacket/Header ****
    
    1   PacketType.
        The packet type, an integer in the range 0 to 127.
        The value is 5 for this packet.
    
    8b  Zero.
        Bits set to 0.
    

The MultiAppPacket is specified below. It allows multiple application 
messages to be contained in one encrypted Salt Channel packet. This saves
IO and CPU, but provides no additional functionality over using only AppPacket
type of messages.


    **** MultiAppPacket ****

    This packet is encrypted.  It is sent within the body of
    EncryptedMessage (EncryptedMessage/Body). It may contain
    more than one application packet.

    2   Header.
        Message type and flags.

    4   Time.
        See separate documentation.

    2   Count.
        Number of following application messages.
        In the range 0 to 65535.

    x   Message+


    **** MultiAppPacket/Header ****

    1   PacketType.
        The packet the, an integer in the range 0 to 127.
        The value is 11 for this packet.

    8b  Zero.
        Bits set to 0.


    **** MultiAppPacket/Message ****

    2   Length.
        Length of application message (next field) in the rage 0 to 65535.

    x   Data.
        The cleartext application message.
    

An implementation MUST support receiving both types of messages and 
MUST treat them as logically equivalent. The application layer above
should not need to know about the difference of these two types of messages.
A sending peer MAY chose to use MultiAppPacket when possible or use 
AppPacket:s.

This specification focuses on AppPacket in definitions of message flow
to keep the specification simple. Anywhere where AppPacket:s are used, 
MultiAppPacket:s can be used instead (if message lengths allow it). This
is specified *here in this section* and not necessarily specified elsewhere
in the document.

Message M4 MUST NOT be contained in a MultiAppPacket. Use a plain AppPacket.



The time field
==============

Most messages have a Time field. It contains the time since the first
message of the peer was sent, or it is set to 0, or it is set to 1.
The elapsed time is measured in milliseconds. The Time field MUST have 
the value 1 for the first message sent by the peer (M1 for the client 
and M2 for the server) when time-stamping is supported by the peer. 
If time-stamping is *not supported* by the peer, the peer MUST set 
the time field value to 0.

The reason to introduce these time-stamps is to protect against 
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



A1 and A2
=========

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
    
    1   PacketType.
        One byte in range 0-127 with the packet type.
        The value is 8 for this packet.
        
    8b  Zero.
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
    
    1   PacketType.
        One byte in range 0-127 with the packet type.
        The integer value is 9 for this message.
        
    7b  Zero.
        Bits set to 0.
        
    1b  LastFlag.
        Always set to 1 for this message to indicate that this is
        the last message of the the session.
    
    
    **** A2/Prot ****
    
    10  P1.
        Protocol ID of Salt Channel with version. 
        Exactly 10 ASCII bytes. Whitespace and control characters
        MUST be avoided.
        The value for this field in for this version of
        of Salt Channel MUST BE "SC2-------".
    
    10  P2.
        Protocol ID of the protocol on top of Salt Channel. 
        Exactly 10 ASCII bytes. Whitespace and control characters
        MUST be avoided.
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

Note that messages A1, A2 together form a complete session.
An M1 message following A1, A2 should be considered a *new* 
session that is completely independent of the previous A1-A2 session.




Session close
=============

This protocol is designed so that both Salt Channel peers will 
be able to agree on when a Salt Channel ends.

The underlying reliable channel may be reused for multiple sequential
Salt Channel sessions. Multiple concurrent sessions over
a single underlying channel is *not* within scope of this protocol.

A Salt Channel session ends after a message with the LastFlag set
is sent by either peer. This includes the following cases:

1. After message A2 is sent by Server.

2. After message M2 is sent by Server with the M2/NoSuchServer bit 
   set to 1.

3. After an EncryptedMessage is sent by either peer and its
   LastFlag is set to 1.




Crypto details
==============

This section describes the crypto primitives in detail.

The field EncryptedMessage/Body uses authenticated encryption
as defined by the function crypto_box() of TweetNaCl 
[NACL, TWEET-1, TWEET-2]. The corresponding function to 
decrypt is crypto_box_open(). In practice, implementations will use 
the functions crypto_box_beforenm(), crypto_box_afternm(), 
and crypto_box_open_afternm() as an optimization. See [NACLBOX].

The first 16 bytes of the ciphertext is used to authenticate the message when
it is decrypted. Encryption and authentication are done in one atomic function 
call and is described in the original NaCl paper [NACL].

The crypto_box_x() functions take a 24-byte nonce as a parameter.
Both the client and the server use the first 8 bytes of the nonce to store 
a signed 64-bit integer in little-endian byte order.
This integer is 1, 3, 5, ... for messages sent by the client; 
increasing by 2 for each message it sends.
This integer is 2, 4, 6, ... for messages sent by Server;
increasing by 2 for each message it sends.
The rest of the bytes of the nonce must be set to zero.
The nonce counters are reset for every Salt Channel session.
Note, no assumption is made on the order in which the peers send
application messages. For example, the server may send all 
application messages. The nonce values used by the client and those 
used by the server are disjoint sets. Note also that the nonce 
values used are *not* sent over the communication channel. This is 
not necessary; they can easily be computed.

Signatures (fields M3/Signature1, M4/Signature2) are generated
and verified using the signature scheme defined by the functions
crypto_sign() and crypto_sign_open() of TweetNaCl 
[NACL, TWEET-1, TWEET-2]. This scheme uses Ed25519 and sha512
to produce signatures that are 64 bytes long.

The term "encryption key" in this document refers to an x25519 
key following the terminology of [NACL]. An encryption key pair
is really used for Diffie-Hellman key agreement, but indirectly
it is used for encryption.
 



List of message types
=====================

This section is informative.
    
    PacketType   Name
    
    0            Not used
    1            M1
    2            M2
    3            M3
    4            M4
    5            AppPacket
    6            EncryptedMessage
    7            reserved (has been used for Ticket in v2 drafts)
    8            A1
    9            A2
    10           TT (not used in v2 spec)
    11           MultiAppPacket
    12-127       Not used




Use case: multi-link session
============================

This section is not normative.

Consider the use case shown in the figure below.
    
    C ---(WebSocket)--> R1 ---(TCP)--> R2 ---(BLE)--> S
    
        Figure: A client, C, connects to a server, S, via two relay servers:
        R1, R2. An end-to-end Salt Channel is established over three different
        types of unencrypted channels.
        
The client, C, wants to establish a Salt Channel with the server, S.
C sends M1 to R1 over a WebSocket connection. The M1/ServerSigKey field
is included. R1 does not handle this host directly, but knows that R2 
might. R1 therefore establishes a TCP connection to R2 and sends M1 to 
R2. R2, in turn, does not directly handle this host, but R2 knows that
S does. R2 therefore establishes a BLE connection with S and sends M1
to S. From there the Salt Channel session can be established over
multiple unencrypted links.

However, in this case, R1 will not know when the Salt Channel session 
terminates. Same for relay R2. They only see encrypted application
data in AppPacket packets once the session has been established.
This results in the situation where R2 must keep the BLE connection
open even after the session is closed. This may waste valuable resources 
and possibly hinders new connections from being established.

These type of situations motivates the principle:

    Anyone on the transport path of a Salt Channel (a relay server for example) 
    should be able to determine whether a Salt Channel session has been closed
    without having access to the encrypted data.

So, to conclude, we must have a last message flag that is not encrypted.
It must be set by the application layer for the last message
of the Salt Channel session and must be readable to any relay node on the 
transportation path between the client and the server.




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




Appendix A: Example session data
================================

Example session data for a simple echo server scenario.
Fixed key pairs are used for a deterministic result. Obviously, such
an approach *must not* be used in production. The encryption key 
pair *must* be generated for each session to achieve the security goals.

On the application layer, a simple request-response exchange occurs.
The client sends the application data: 0x010505050505 and the same
bytes are echoed back by the server.

No timestamps are used, neither by the server nor the client.
The Time fields of the messages are all set to zero.

    
    ======== ExampleSessionData ========
    
    Example session data for Salt Channel v2.
    
    ---- key pairs, secret key first ----
    
    client signature key pair:
        55f4d1d198093c84de9ee9a6299e0f6891c2e1d0b369efb592a9e3f169fb0f795529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b
        5529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b
    client encryption key pair:
        77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a
        8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
    server signature key pair:
        7a772fa9014b423300076a2ff646463952f141e2aa8d98263c690c0d72eed52d07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b
        07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b
    server encryption key pair:
        5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb
        de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
    
    --- Log entries ----
    
     42 -->   WRITE
        534376320100000000008520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
    <--  38   READ
        020000000000de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
    <-- 120   READ
        0600e47d66e90702aa81a7b45710278d02a8c6cddb69b86e299a47a9b1f1c18666e5cf8b000742bad609bfd9bf2ef2798743ee092b07eb32a45f27cda22cbbd0f0bb7ad264be1c8f6e080d053be016d5b04a4aebffc19b6f816f9a02e71b496f4628ae471c8e40f9afc0de42c9023cfcd1b07807f43b4e25
    120 -->   WRITE
        0600b4c3e5c6e4a405e91e69a113b396b941b32ffd053d58a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb8454ee0b1215dfa08b3ebb3ecd2977d9b6bde03d4726411082c9b735e4ba74e4a22578faf6cf3697364efe2be6635c4c617ad12e6d18f77a23eb069f8cb38173
     30 -->   WRITE_WITH_PREVIOUS
        06005089769da0def9f37289f9e5ff6e78710b9747d8a0971591abf2e4fb
    <--  30   READ
        068082eb9d3660b82984f3c1c1051f8751ab5585b7d0ad354d9b5c56f755
    
    ---- Other ----
    
    session key: 1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389
    app request:  010505050505
    app response: 010505050505
    total bytes: 380
    total bytes, handshake only: 320
    

Note to authors: the above output was generated with the Java class 
saltchannel.dev.ExampleSessionData, date: 2017-10-06.


