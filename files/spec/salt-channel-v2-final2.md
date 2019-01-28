# About
About this document.

*Date*: 2019-01-24

*Status*: FINAL.

*Title*: Specification of Slat Channel v2

* Authors*:
* Frans Lundberg. ASSA ABLOY AB, Stockholm, frans.lundberg@assaabloy.com, 
  phone: +46707601861.
* Simon Johansson, ASSA ABLOY AB, Stockholm, simon.johansson@assaabloy.com.

*Thanks*:

* To Håkan Olsson for comments and discussions.
* To Daniel Bernstein for creating TweetNaCl.
* To Kenneth Pernyer and Felix Grape for comments and implementations
  in Swift and JavaScript.
* To Felix Grape for carefully reviewing the protocol and improving
  the wording of the specification.

  ## History

* 2019-01-24. FINAL2. Clarification of specification. No changes to the protocol itself.

* 2017-11-16. FINAL1. This specification is declared final. No protocol changes are allowed from now on, only clarifications, examples and such.

* 2017-10-16. DRAFT8. Change Salt Channel protocol strings for A2 message. "SC2" -> "SCv2". Add '/' as valid character in P1 and P2

* 2017-10-09. DRAFT7. Address fields added to message A1.

* 2017-10-06. DRAFT6. Resume feature removed from spec.

* 2017-10-02. DRAFT5. Prefixes "SC-SIG01", "SC-SIG02" added to signatures, LastFlag used. Text more complete. MultiAppPacket introduced.

* 2017-05-15. DRAFT4. 1-byte message types instead of 4 bits. Improved text.

* 2017-03-29. DRAFT3. Work in progress with adding resume feature.

* 2017-03-15. DRAFT2. 2-byte headers. Time fields added. A1A2 functionality added.

* 2017-02-22. DRAFT1.

# Table of Content
* [Table of Content](#table-of-content)
* [Introduction](#introduction)
* [Protocol design](#protocol-design)
  * [Priorities](#priorities)
  * [Goals](#goals)
  * [Limitations](#limitations)
* [Layer below](#layer-below)
  * [Salt Channel over TCP](#salt-channel-over-tcp)
  * [Salt Channel over WebSocket](#salt-channel-over-websocket)
  * [Salt Channel over a byte stream](#salt-channel-over-a-byte-stream)
* [Salt Channel sessions](#salt-channel-sessions)
  * [Handshaked session](#handshaked-session)
  * [A1A2 session](#a1a2-session)
  * [Session close](#session-close)
* [Message details](#message-details)
  * [A1](#a1)
  * [A2](#a2)
  * [M1](#m1)
  * [M2](#m2)
  * [M3](#m3)
  * [M4](#m4)
  * [AppPacket](#apppacket)
  * [MultiAppPacket](#multiapppacket)
  * [EncryptedMessage](#encryptedmessage)
  * [Time  field](#time--field)
  * [List of packet types](#list-of-packet-types)
* [Crypto details](#crypto-details)
  * [Identity](#identity)
  * [Salt Channel session key, key agreement](#salt-channel-session-key,-key-agreement)
  * [Message encryption](#message-encryption)
  * [Salt Channel handshake authentication](#salt-channel-handshake-authentication)
* [Multi-link session](#multi-link-session)
* [References](#references)
* [Appendix A - Example session data](#appendix-a---example-session-data)
* [Appendix B - Byte order, bit order and bit numbering](#appendix-b---byte-order,-bit-order-and-bit-numbering)
  * [Notation, byte and bit order](#notation,-byte-and-bit-order)
  * [Examples](#examples)

# Introduction
The protocol is essentially an implementation of the station-to-station [STS] protocol. Salt Channel relies on an underlying reliable bidirectional communication channel between the two peers communicating. TCP is an example of such an underlying channel, but Salt Channel is in no way restricted to TCP. In fact, Salt Channel has been successfully implemented on top of WebSocket, RS485, NFC, and Bluetooth Low Energy (BLE) and also on combinations of such links, for example BLE + TCP.

This is the second version of the protocol, called Salt Channel v2. The major changes from v1 is the removal of the Binson dependency, and the protection against delay attacks.
Salt Channel is "Powered by Curve25519". The cryptographic algorithms required can be found in [ED25519], [X25519], [XSALSA20], [SHA512] and [POLY1305].
* Signatures use ed25519 with sha512.
* Authenticated public-key encryption use x25519+xsalsa20+poly1305.
* Secure hashing uses sha512.
The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and "OPTIONAL" in this document are to be interpreted as described in [RFC2119].

# Protocol design
This section describes the design goals for Salt Channel and the limitations.

## Priorities
The following priorities were used when designing the protocol.
1. The first priority is to achieve high security.
2. The second priority is to achieve a low network overhead; that is, few round-trips and a small data overhead.
3. The third priority is to allow for low code complexity, and low CPU requirements of the communicating peers.

Low complexity is also, in itself, important to achieve high security.

## Goals
The following are the main goals of the protocol.
* **128-bit security**. The best attack should be a 2128 brute force attack. No attack should be feasible until there are (if there ever will be) large-enough quantum computers.
* **Internet-capable**. The protocol should protect against all attacks that can occur on public communication channels. The attacker can read, modify, redirect all packages sent between any pair of peers. The attacker has access to every Salt Channel package ever sent and packages from all currently active Salt Channel sessions world-wide.
* **Forward secrecy**. Recorded communication will not be possible to decrypt even if one or both peer's private keys are compromised.
* **Delay attack protection**. The protocol should protect against delay attacks. If the attacker delays a package, the receiving peer should detect this if the delay is abnormal.
* **Secret client identity**. An active or passive attacker cannot retrieve the long-term public key of the client. Tracking of the client is impossible.
* **Simple protocol**. It should be possible to implement in few lines of code and should be practically auditable.
* **Compact protocol (few bytes)**. Designed for Bluetooth Low Energy and other low-bandwidth channels.

## Limitations
Salt Channel is limited in scope in the following ways:
* **No certificates**. Simplicity and compactness are preferred. Using certificates together with the Salt Channel public keys is possible, but not included in the protocol.
* **No quantum security**. The protocol is not intended to be secure for an attacker with a large quantum computer. Such a computer does not exist.
* **Public length, sequence and timing**. No attempt is made to hide the length, sequence, or timing of the communicated messages.
* **No DoS protection**. No attempt is made to protect against denial-of-service attacks. This is an important topic. Perhaps important enough to warrant a dedicated protocol. Anyway, the possibility of denial-of-service attacks varies profoundly with the situation, such as the type of the underlying reliable channel. Solving this in a generic way is too complex to include in this protocol.

# Layer below
Salt Channel can be implemented on top of any underlying channel that provides reliable, order-preserving, bidirectional communication. This section describes how Salt Channel is implemented on top of a TCP, WebSocket and a general stream similar to TCP.
Note, except for this section, this specification only deals with byte arrays of known size. The underlying layer provides an order-preserving exchange of byte arrays, each with a known size.

## Salt Channel over TCP
When Salt Channel is implemented on top of TCP, the following "chunking format" is used:
```
Stream = StreamMessage+

StreamMessage
              0               1               2               3
       7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 0    | Size                                                          |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 4    | Message                                                       |
 .    \                                                               \
 X    | Message                                                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
*Size* is a 32-bit integer with the byte size of the following Message. Its valid range is [0, 2<sup>31</sup>-1], so either an unsigned or signed 32-bit integer work for storing it in computer memory. The size bytes sent must be represented as little endian. Message is the raw message bytes. The following is an example of a StreamMessage with a message length of 18 bytes.
```
Example stream message
            0               1               2               3
     7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 0  | Size = 18                                                     |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 4  | Message                                                       |
 8  \                                                               \
12  | Message                                                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
16  | Message                       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
The different message and packet types are defined in the [Message details](#message-details) section.
It is RECOMMENDED that the TCP connection is closed when the Salt Channel session is closed. This behavior MUST be the default behavior of compliant implementations.

## Salt Channel over WebSocket
WebSocket [WS] connections are already in a "chunked" format and transmit binary data either as ArrayBuffer (byte array-like object) or Blob (file-like object). Because WebSockets using the binary type ArrayBuffer delivers a stream of byte arrays of known size, as opposed to individual bytes, Salt Channel over WebSocket is very simple. There is no need for the size prefix that is needed when implementing Salt Channel over TCP. Each WebSocket message is a message as specified in this document.

It is RECOMMENDED that the WebSocket connection is closed when the Salt Channel session is closed. This behavior MUST be the default behavior of compliant implementations.

## Salt Channel over a byte stream
The chunking format as defined in the section "Salt Channel over TCP" is RECOMMENDED when Salt Channel is implemented over any type of byte stream similar to TCP; for example for Salt Channel over RS232 ("serial port").

# Salt Channel sessions
This section describes the two different types of Salt Channel sessions, the ordinary handshaked session for exchanging application messages and the A1A2 session for querying about a servers available protocols.

## Handshaked session
The message order of an ordinary successful Salt Channel session is:
```
Session = M1 M2 E(M3) E(M4) [E(AppPacket)|E(MultiAppPacket)]+
```
Where E() denotes the packet type EncryptedMessage that carries encrypted payload. The encrypted payload has been encrypted using an authenticated encryption scheme. Note that only M1 and M2 are sent in cleartext, M3 and all subsequent packets are always encrypted.

The Salt Channel handshake is a three-way handshake that results in a Diffie-Hellman key agreement and mutual authentication. A client initiates the handshake by sending M1, the server responds with M2 and M3, and the client finishes the handshake by sending M4. When the first application message is sent by the client, this message SHOULD be sent in the same IO operation as M4 by the underlying layer to achieve a one round-trip overhead (instead of two). Application layer messages (AppPackets and MultiAppPackets) are sent by both client and server in any order.

An overview of a typical Salt Channel session is shown below, header and time fields are left out of this sequence diagram for readability.
```
    CLIENT                                                    SERVER
    
    ProtocolIndicator
    ClientEncKey
    [ServerSigKey]             ---M1----->
                
                               <--M2------            ServerEncKey

                                                        ServerSigKey
                               <--E(M3)---              Signature1
    
    ClientSigKey
    Signature2                 ---E(M4)--->
    
    AppPacket                  <--E(AppPacket)-->        AppPacket

    AppPacket                  <--E(AppPacket)-->        AppPacket
                              With LastFlag = 1
    
        Figure: Salt Channel messages. "E()" is used to indicate that a 
        message is encrypted and authenticated. Header and Time fields are not
        included in the figure. They are included in every message.
```
Later sections describes these messages in detail.

## A1A2 session
The A1 and A2 messages also form a Salt Channel session, the A1A2 session, which allows the client to ask the server about its public server protocol information. This message exchange is intended to stay stable even if/when Salt Channel is upgraded to v3, v4 and so on. No confidentiality or integrity protection is used during an A1A2 session. Information sent by the server SHOULD therefore be validated once a secure channel has been established.

The session is initiated by the client by sending the A1 message and the server responds with the A2 message. The A2 response sent by the server is assumed to be static for days, weeks, or longer, and contains informations about the protocol stacks available on the server. The client SHOULD cache the A2 response.
```
A1A2Session = A1 A2
```
Note that the session is closed when the client has received the A2 response from the server. See the [Message details](#message-details) section for more information about the specifics of the A1 and A2 messages.

## Session close
The Salt Channel protocol is designed so that both peers will be able to agree on when a Salt Channel ends.
The underlying reliable channel MAY be reused for multiple sequential Salt Channel sessions. Multiple concurrent sessions over a single underlying channel is not within scope of this protocol.

A Salt Channel session ends after a message with the LastFlag set is sent by either peer. This includes the following cases:
1. After message A2 is sent by Server.
2. After message M2 is sent by Server with the M2/NoSuchServer bit set to 1.
3. After an EncryptedMessage is sent by either peer and its LastFlag bit is set to 1.

A Salt Channel session also ends if a peer receives a packet that does not follow the protocol. This can happen, but is not limited to, if either one of the signatures in M3 or M4 cannot be verified, if a peer receives an EncryptedMessage with a Body field that cannot be decrypted, if the PacketType field value in the header does not match the expected value. If such an error occurs the peer that received the bad packet MUST immediately close the Salt Channel without notifying the peer that sent the packet. It is up to the implementation and/or the code that uses the Salt Channel to decide if the underlying layer is closed or not.

# Message details
This section describes how the different message and packet types are defined as an array of bytes. The size of a packet is always known from the underlying layer. When the layer below is a stream (like TCP for example) each message is prefixed with the byte size of the message as described in the section "Salt Channel over a stream". The following list defines some notational conventions for this document and implementation requirements:
Message is used for a byte array that is a complete protocol message.

* Packet is used to for any byte array -- either a complete message or a part of a message.
* Packets are presented below with fields of specified sizes.
* Bit values that has not been explicitly stated MUST be set to 0.

See [Appendix B](#appendix-b---byte-order,-bit-order-and-bit-numbering) for format specification of messages.

## A1
A1 is the first message in the A1A2 session and is sent by the client to query the server for which protocols it supports.
```
A1
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | PacketType    |   Zero        | AddressType   | AddressSize   |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | AddressSize   | Address                                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
.  | Address                                                       |
.  \                                                               \
X  | Address                                                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### A1/PacketType
A byte used as packet type identifier. Always 0x08 for A1. See section [List of packet types](#list-of-packet-types) for details.

### A1/Zero
Zero indicates that all 8 bits in the Zero field MUST be set to 0.

### A1/AddressType
Two address types are currently supported.
```
AddressType       Description
value
 
0x00              The any type. The client does not specify a particular
                  server-side key holder to connect to, it connects to the
                  server default. When AddressType is 0, AddressSize MUST be 0,
                  and Address MUST be zero bytes in length.
 
0x01              An Ed25519 public key 'address'. A 32-byte public signing key
                  as defined in Salt Channel.
                  Thus, AddressSize MUST be 32 for this address type.
 
0x02-0x7F         Reserved for future use.
```

### A1/AddressSize
The byte size of the Address field. An unsigned 16 bit integer indicating the size of the A1/Address field. Valid range is [0, 65535].

### A1/Address
The raw representation of the address on the format as defined by its address type and address size.

## A2
The A2 message is the response from the server which holds what Salt Channel protocol versions are used, and possibly what application layer protocols that are used on top of each respective version.
```
A2
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | PacketType    |L| Zero      |N| Count         | Prot*         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  |                            Prot*                              |
8  \                                                               \
12 \                                                               \
16 |                            Prot*                              |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
20 | Prot*                                         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### A2/PacketType
A byte used as packet type identifier. Always 0x09 for A2. See section [List of packet](#list-of-packets) types for details.

### A2/L
A2/L (LastFlag) is a flag indicating that this is the last message in the session.
```
A2/L
 
Value       Description
1           This is the last message in the A1A2 session.
            This bit is always set to 1 in A2.
            Indicating that the Salt Channel session is
            to be considered closed.
```

### A2/Zero
Zero indicates that all 6 bits in the Zero field MUST be set to 0.

### A2/N
A2/N (NoSuchServer) is a flag indicating that the server specified in A1/Address could not be connected to.
```
A2/N
 
Value       Description
0           The server specified in A1/Address is the one that A2/Prot is valid for.
 
1           No server with the address specified in A1/Address could be connected to.
            When A2/N is set to 1, A2/Count MUST be set to 0
            and there MUST be zero occurences of A2/Prot.
```

### A2/Count
Number of following Prot fields. An 8 bit signed integer with valid range [0,127]. For a given count, the message size of A2 will be length(A2) = 3 + Count * 20.

### A2/Prot
The Prot field consists of two fields making up a pair, P1 and P2. They contain the raw byte representation of two protocol identifier strings. P1 is a protocol identifier for a Salt Channel version that is available on the server. P2 is a protocol identifier for an application layer protocol available on top of the protocol specified by the P1 string.
```
A2/Prot
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | P1                                                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | P1                                                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
8  | P1                            | P2                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
12 | P2                                                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
16 | P2                                                            |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
For the P1 string the server MUST use the protocol identifier "SCv2------" for Salt Channel v2. The plan is that future versions of Salt Channel will use the same A1 and A2 messages. Salt Channel v3 SHOULD use "SCv3------" and v4 SHOULD use "SCv4------" and so on. I.e. If the identifier is shorter than 10 characters: it is padded with hyphens.

For the P2 string the server can choose not to reveal any information about the application layer protocol, in such cases the P2 string MUST be "----------" (ten hyphens).

The strings on P1 and P2 MUST only contain ASCII characters from the following set:
```
------------------------------------------------
|   ASCII   |     Description      |   ASCII   |
| character |                      | Hex value |
|-----------|----------------------|-----------|
|  '-'      | hyphen (a.k.a. minus)| 0x2D      |
|  '.'      | period (a.k.a. dot)  | 0x2E      |
|  '/'      | slash  (forward)     | 0x2F      |
|  '0'-'9'  | digits zero to nine  | 0x30-0x39 |
|  'A'-'Z'  | A to Z upper case    | 0x41-0x5A |
|  '_'      | underscore           | 0x5F      |
|  'a'-'z'  | a to z lower case    | 0x61-0x7A |
------------------------------------------------
I.e. the closed intervals:
[0x2D, 0x39], [0x41, 0x5A], [0x5F, 0x5F], and [0x61, 0x7A]
```

## M1
The first message of a Salt Channel handshake MUST be the M1 message. It is sent from the client to the server. It includes a protocol indicator, the client's public ephemeral encryption key and optionally the server's public signing key.
```
M1
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 0 | ProtocolIndicator                                             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 4 | PacketType    | Zero        |S| TimeSupported                 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 8 | TimeSupported                 | ClientEncPub                  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
12 | ClientEncPub                                                  |
16 \                                                               \
20 \                                                               \
24 \                                                               \
28 \                                                               \
32 \                                                               \
36 | ClientEncPub                                                  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
40 | ClientEncPub                  | ServerSigPub?                 \
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
44 | ServerSigPub?                                                 |
48 \                                                               \
52 \                                                               \
56 \                                                               \
60 \                                                               \
64 \                                                               \
68 | ServerSigPub?                                                 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
72 | ServerSigPub?                 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### M1/ProtocolIndicator
The protocol indicator field. Indicates which Salt Channel version the client expects that it is connecting to. For Salt Channel v2 the field MUST be the ASCII bytes for "SCv2". The field MUST be on the format defined below.
```
M1/ProtocolIndicator
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | 0x53          | 0x43          | 0x76          | 0x32          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### M1/PacketType
A byte used as packet type identifier. Always 0x01 for M1. See section [List of packet types](#list-of-packet-types) for details.

### M1/Zero
Zero indicates that all 7 bits in the Zero field MUST be set to 0.

### M1/S
One bit indicating wheter or not a desired identity for the server's public signature key (ServerSigPub) is included or not.
```
M1/S
 
Value       Description
0           The server's public signature key is not and MUST NOT be included in M1.
            Total M1 message size: 42 bytes
 
1           The server's public signature key is included in M1.
            Total M1 message size: 74 bytes
```

### M1/TimeSupported
This is a 32 bit integer with valid range [0, 1]. See section [Time  field](#time--field) for details.
```
M1/TimeSupported
 
Value       Description
0           The client does not support time, the following fields in
            subsequent packets form the client MUST be set to zero (0):
            M4/Time, AppPacket/Time and MultiAppPacket/Time.
 
1           The client supports time,
            The following fields in subsequent packets from the client
            MUST contain a relative timestamp: M4/Time, AppPacket/Time
            and MultiAppPacket/Time.
```

### M1/ClientEncPub
The 32 byte public part of the ephemeral x25519 key pair used by the client for the upcoming Salt Channel session. See section [Crypto details](#crypto-details) for details.

### M1/ServerSigPub
The 32 byte public part of a Ed25519 key pair. This field MUST only be included if the M1/S bit is set to 1. This public key is the identity that the client wishes to communicate with. See section [Crypto details](#crypto-details) for details.

## M2
M2 is the first message sent by the server during a Salt Channel handshake. It is followed directly by M3, also sent by the server.
```M2
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 0 | PacketType    |L| Zero      |N| TimeSupported                 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 4 | TimeSupported                 | ServerEncPub                  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 8 | ServerEncPub                                                  |
12 \                                                               \
16 \                                                               \
20 \                                                               \
24 \                                                               \
28 \                                                               \
32 | ServerEncPub                                                  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
36 | ServerEncPub                  |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### M2/PacketType
A byte used as packet type identifier. Always 0x02 for M2. See section [List of packet types](#list-of-packet-types) for details.

### M2/L and M2/N
M2/L (LastFlag) is a flag indicating if this is the last message in the session and that the session is to be considered closed.

M2/N (NoSuchServer) is a flag indicating that the server with the identity specified by the client in M1/ServerSigPub could not be connected to.
```
+--------+--------+---------------------------------------------------------+
|  M2/L  |  M2/N  |  Description                                            |
+--------+--------+---------------------------------------------------------+
|   0    |   0    | Everything is OK.                                       |
|        |        | This message is not the last message in the session and |
|        |        | the requested server was found.                         |
+--------+--------+---------------------------------------------------------+
|   0    |   1    | Illegal combination. See note below for details         |
+--------+--------+---------------------------------------------------------+
|   1    |   0    | Illegal combination. See note below for details         |
+--------+--------+---------------------------------------------------------+
|   1    |   1    | The NoSuchServer condition has occurred, the M2/N bit   |
|        |        | is set to 1, the M2/ServerEncPub field MUST be set to   |
|        |        | zero valued bytes.                                      |
|        |        | Note that this condition can only occur if the client   |
|        |        | sets M1/S to 1 and includes the M1/ServerSigPub field.  |
+--------+--------+---------------------------------------------------------+
```
Note, these flags MUST have the same bit value, i.e. M2/L MUST only be set to 1 if and only if M2/N is set to 1.

This is because the only reason for closing the session at this stage is if the requested server in M1/Address is not possible to connect to. All other errors (e.g. not supported M1/ProtocolIndicator, wrong PacketType or wrong packet lengths) MUST cause an immediate close of the underlaying layer, thus M2 is not sent at all.

### M2/Zero
Zero indicates that all 6 bits in the Zero field MUST be set to 0.

### M2/TimeSupported
This is a 32 bit integer with valid range [0, 1]. See section [Time  field](#time--field) for details.

### M2/Time
```
Value       Description
0           The server does not support time, the following fields in
            subsequent packets from the server MUST be set to zero (0):
            M3/Time, AppPacket/Time and MultiAppPacket/Time.
 
1           The server supports time,
            The following fields in subsequent packets from the server
            MUST contain a relative timestamp: M3/Time, AppPacket/Time
            and MultiAppPacket/Time.
```

### M2/ServerEncPub
The 32 byte public part of the ephemeral x25519 key pair used by the server for the upcoming Salt Channel session. See section [Crypto details](#crypto-details) for details.

## M3
M3 is encrypted and the ciphertext is sent within the body of an EncryptedMessage. This section defines the clear text for M3. M3 is sent by the server. When the client has received M3 it has enough information to verify the identity of the server, and decide if wheter to proceed with the handshake and sent M4 or not. See the section [EncryptedMessage](#encryptedmessage) for details.
```
M3
            0               1               2               3
     7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  0 | PacketType    | Zero          | Time                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  4 | Time                          | ServerSigPub                  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  8 | ServerSigPub                                                  |
 12 \                                                               \
 16 \                                                               \
 20 \                                                               \
 24 \                                                               \
 28 \                                                               \
 32 | ServerSigPub                                                  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 36 | ServerSigPub                  | Sig01                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 40 | Sig01                                                         |
 44 \                                                               \
 48 \                                                               \
 52 \                                                               \
 56 \                                                               \
 60 \                                                               \
 64 \                                                               \
 68 \                                                               \
 72 \                                                               \
 76 \                                                               \
 80 \                                                               \
 84 \                                                               \
 88 \                                                               \
 92 \                                                               \
 96 | Sig01                                                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
100 | Sig01                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### M3/PacketType
A byte used as packet type identifier. Always 0x03 for M3. See section [List of packet types](#list-of-packet-types) for details.

### M3/Zero
Zero indicates that all 8 bits in the Zero field MUST be set to 0.

### M3/Time
This is a 32 bit integer with valid range is [0, 2<sup>31</sup>-1]. As such it can be stored as both a signed and unsigned 32 bit integer. See section [Time  field](#time--field) for details.

### M3/ServerSigPub
The public part of the server's Ed25519 key pair. See section [Crypto details](#crypto-details) for details.

### M3/Sig01
A 64 byte signature. M3/Sig01 together with the M3/ServerSigPub is used by the client to verify the identity of the server. See section [Crypto details](#crypto-details) for details.

## M4
M4 is encrypted and the ciphertext is sent within the body of an EncryptedMessage. This section defines the clear text for M4. M4 is sent by the client. When M4 has been sent and received both peers have authenticated themselves to each other, and they have agreed upon a symmetric encryption key, i.e. the Salt Channel handshake is complete. See the section [EncryptedMessage](#encryptedmessage) for details.
```
M4
            0               1               2               3
     7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  0 | PacketType    | Zero          | Time                          |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  4 | Time                          | ClientSigPub                  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
  8 | ClientSigPub                                                  |
 12 \                                                               \
 16 \                                                               \
 20 \                                                               \
 24 \                                                               \
 28 \                                                               \
 32 | ClientSigPub                                                  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 36 | ClientSigPub                  | Sig02                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 40 | Sig02                                                         |
 44 \                                                               \
 48 \                                                               \
 52 \                                                               \
 56 \                                                               \
 60 \                                                               \
 64 \                                                               \
 68 \                                                               \
 72 \                                                               \
 76 \                                                               \
 80 \                                                               \
 84 \                                                               \
 88 \                                                               \
 92 \                                                               \
 96 | Sig02                                                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
100 | Sig02                         |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### M4/PacketType
A byte used as packet type identifier. Always 0x04 for M4. See section [List of packet types](#list-of-packet-types) for details.

### M4/Zero
Zero indicates that all 8 bits in the Zero field MUST be set to 0.

### M4/Time
This is a 32 bit integer with valid range is [0, 2<sup>31</sup>-1]. As such it can be stored as both a signed and unsigned 32 bit integer. See section [Time  field](#time--field) for details.

### M4/ClientSigPub
The public part of the client's Ed25519 key pair. See section [Crypto details](#crypto-details) for details.

### M4/Sig02
A 64 byte signature. M4/Sig02 together with the M4/ClientSigPub is used by the server to verify the identity of the client. See section [Crypto details](#crypto-details) for details.

## AppPacket
AppPackets are encrypted and the ciphertext is sent within the body of an EncryptedMessage. This section defines the clear text for AppPacket. When the Salt Channel handshake is complete the peers can start exchanging application packets as either AppPacket or MultiAppPacket. AppPacket is the simplest packet type that can be used to send application layer data.
```
AppPacket
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | PacketType    | Zero          | Time                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | Time                          | Data                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
.  | Data                                                          |
.  \                                                               \
X  | Data                                                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### AppPacket/PacketType
A byte used as packet type identifier. Always 0x05 for AppPacket. See section [List of packet types](#list-of-packet-types) for details.

### AppPacket/Zero
Zero indicates that all 8 bits in the Zero field MUST be set to 0.

### AppPacket/Time
This is a 32 bit integer with valid range is [0, 2<sup>31</sup>-1]. As such it can be stored as both a signed and unsigned 32 bit integer. See section [Time  field](#time--field) for details. 

### AppPacket/Data
The application layer data.

## MultiAppPacket
MultiAppPackets are encrypted and the ciphertext is sent within the body of an EncryptedMessage. This section defines the clear text for MultiAppPacket.

MultiAppPacket is an optimization that reduces overhead when sending multiple application messages at once by reducing the number of headers and by encryption operations. All implementations MUST support receiving both types of messages and MUST treat them as logically equivalent. The application layer above MUST NOT need to know about the difference of these two types of packets. A sending peer MAY choose to use MultiAppPackets when possible or use multiple AppPackets. The MultiAppPacket is specified below.
```
MultiAppPacket
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | PacketType    | Zero          | Time                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | Time                          | Count                         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
8  | Message+                                                      |
.  \                                                               \
X  | Message+                                                      |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
MultiAppPacket/Message
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | Length                        | Data                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | Data                                                          |
.  \                                                               \
X  | Data                                                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### MultiAppPacket/PacketType
A byte used as packet type identifier. Always 0x0B for MultiAppPacket. See section [List of packet types](#list-of-packet-types) for details.

### MultiAppPacket/Zero
Zero indicates that all 8 bits in the Zero field MUST be set to 0.

### MultiAppPacket/Time
This is a 32 bit integer with valid range is [0, 2<sup>31</sup>-1]. As such it can be stored as both a signed and unsigned 32 bit integer. See section [Time  field](#time--field) for details.

### MultiAppPacket/Count
Number of following Message fields. A 16 bit unsigned integer with valid range [1, 65535].

### MultiAppPacket/Message

#### MultiAppPacket/Message/Length
Size of the following MultiAppPacket/Message/Data field. A 16 bit unsigned integer with valid range [0, 65535].

#### MultiAppPacket/Message/Data
The application layer data. If MultiAppPacket/Message/Length is zero, this field is zero bytes in length.

## EncryptedMessage
Packets of type M3, M4, AppPacket and MultiAppPacket are sent encrypted. The ciphertext and MAC of those packets are included in the field EncryptedMessage/Body. See section [Crypto details](#crypto-details) for details.
```
EncryptedMessage
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | PacketType    |L|  Zero       | Body                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4  | Body                                                          |
.  \                                                               \
X  | Body                                                          |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### EncryptedMessage/PacketType
A byte used as packet type identifier. Always 0x06 for EncryptedMessage. See section [List of packet types](#list-of-packet-types) for details.

### EncryptedMessage/L
EncryptedMessage/L (LastFlag) is a flag indicating if this is the last message in the session. When this bit is set to 1 this indicates that the session is to be considered closed and that no more messages are to be sent in by either peer.

### EncryptedMessage/Zero
Zero indicates that all 7 bits in the Zero field MUST be set to 0.

### EncryptedMessage/Body
See section Crypto Details for details on the authenticated encryption.

## Time  field
Some messages have a Time field, a little endian 32 bit integer in the range [0, 2<sup>31</sup>-1]. The reason to introduce these timestamps is to protect against delay attacks, i.e. a man-in-the-middle attacker that affects the application behavior by delaying a message sent between the two peers. The blog post at [DELAY-ATTACK] describes this type of attack. All peers that are capable of measuring relative time in milliseconds SHOULD support the Time field.

Peers that support timestamping MUST set the TimeSupported field value to 1 in the first message, M1 for clients and M2 for servers. For all subsequent messages the Time field value is the number of milliseconds since the first message was sent. When the Time field value reaches 2<sup>31</sup> it can no longer protect against delay attacks because of integer wrap around. Therefore it is RECOMMENDED that Salt Channel sessions between peers that support delay attack protection lasts for a duration shorter than 2<sup>31</sup> milliseconds by some margin to avoid integer wrap around in the delay attack calculation. Note that 2<sup>31</sup> milliseconds is just short of 25 days.

If timestamping is not supported by a peer it MUST always set the TimeSupported and Time field value to 0 in all messages and packets.
Any peer that supports timestamping have to store both ClientEpoch and ServerEpoch, recording the time at which the first message was sent and received respectively, in order to send a correct value in the Time field and verify the value in the Time field of incoming packets.
A server that supports timestamping and uses the Time field to protect against delay attacks does so by recording the time at which M1 arrived as ClientEpoch and the time at which M2 is sent as ServerEpoch. For incoming messages the server can compute an expected value of the Time field in the message by taking the current time in milliseconds and subtracting ClientEpoch. If the difference between the expected value and the actual Time field value from the client is too large, the server can reject the message. For outgoing messages the server takes the current time in milliseconds and subtracts ServerEpoch and uses that as the value for the Time field.

A client that supports timestamping and uses the Time field to protect against delay attacks performs the analogous steps to protect against delay attacks. The time at which M1 is sent is recorded as ClientEpoch and the time at which M2 is received as ServerEpoch.

When the delay attack protection computation generates a result indicating a delayed message it is RECOMMENDED that the peer immediately closes the underlying layer without notifying the other peer. The threshold for when a message is regarded as delayed will depend on the use case. A possible use case can be that a user (client) connects via BLE to a door lock (server). The threshold would then depends on how long it takes for the user to give up on waiting for the lock to react and walk away, this might be somewhere around 10 seconds. The precision of the clock in client and server must also be considered. Too large clock drift can cause the delay attack protection mechanism to falsely detect a message as delayed.

When only one of the peers sets the TimeSupported field to 1 to indicate that they wish to use the delay attack protection it is RECOMMENDED that both peers ignore the Time field for this session. However a peer MAY require delay protection and MAY simply close the underlying layer if the other peer does not support delay attack protection. If a client closes the underlying layer due to lack of support for delay attack protection it is RECOMMENDED that the user is notified that the connection was closed due to this fact.

## List of packet types
```
PacketType       Name
    
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
    10           Not used
    11           MultiAppPacket
    12-127       Not used
```

# Crypto details

## Identity
Salt Channel uses Ed25519 key pairs for identity.
EdDSA Edwards-Curve Digital Signature Algorithm [RFC8032]

## Salt Channel session key, key agreement
To meet the goal forward secrecy every salt-channel session uses a unique key to encrypt the messages sent during the session. Salt-channel uses x25519 which is an elliptic curve Diffie-Hellman key exchange using Curve25519 to agree on a symmetric key for the session. This means that, prior to the handshake, both peers MUST generate a new x25519 key pair from a good source of entropy. Based on those ephemeral key pairs, a shared key is calculated. This key is later used for encrypting and decrypting the payload sent during the session.
Details about x25519 can be found in: [RFC7748]
Examples of such key pairs is:
```
The client generates 32 random bytes as the secret key:
ClientEncSec: 77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a
Then calculates the corresponding public key using x25510:
ClientEncPub = X25519(ClientEncSec, 9)
ClientEncPub: 8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
 
Analogously, the server perform the same operations server side:
ServerEncSec: 5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb
ServerEncPub: de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
 
 
SharedKey = X25519(ClientEncPub, ServerEncSec) = X25519(ServerEncPub, ClientEncSec)
CommonEncSec: 1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389
```

## Message encryption
The symmetric session key is used to encrypt and decrypt the messages sent during the salt-channel session. Salt-channel uses xsalsa20 + poly1305 for authenticated encryption and the specific details can be found in [XSALSA20] and [POLY1305]. The functions to encrypt and decrypt take a 24-byte nonce as a parameter. Both the client and the server use the first 8 bytes of the nonce to store a signed 64-bit integer in little-endian byte order. This integer is 1, 3, 5, ... for messages sent by the client; increasing by 2 for each message it sends. This integer is 2, 4, 6, ... for messages sent by Server; increasing by 2 for each message it sends. The rest of the bytes of the nonce MUST be set to zero. The nonce counters are reset for every Salt Channel session. Note, no assumption is made on the order in which the peers send application messages. For example, the server MAY send all application messages. The nonce values used by the client and those used by the server are disjoint sets. Note also that the nonce values used are not sent over the communication channel to reduce network overhead. This is not necessary as they can easily be computed.
```
When applying xsalsa20 + poly1305 to a clear text, the output will be:
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0  | MAC                                                           |
4  \                                                               \
8  \                                                               \
12 | MAC                                                           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
16 | Ciphertext                                                    |
.  \                                                               \
X  | Ciphertext                                                    |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
Examples of using xsalsa20 + Poly1305 to encrypt and authenticate a payload using the key 1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389:
--------------------------------------------------------------------------------------------------------------------------------------
| Clear text           | Nonce                                            | Mac + Cipher                                             |
--------------------------------------------------------------------------------------------------------------------------------------
| 05000000010505050505 | 030000000000000000000000000000000000000000000000 | 5089769da0def9f37289f9e5ff6e78710b9747d8a0971591abf2e4fb |
--------------------------------------------------------------------------------------------------------------------------------------
| 05000000010505050505 | 040000000000000000000000000000000000000000000000 | 82eb9d3660b82984f3c1c1051f8751ab5585b7d0ad354d9b5c56f755 |
--------------------------------------------------------------------------------------------------------------------------------------
```

## Salt Channel handshake authentication
After the M1 and M2 message, the two communicating peers has agreed on a symmetric key to encrypt the messages sent during the session. However, at this point there is no authentication. I.e., the peers doesn't know each others identities. The Ed25519 key pairs are used as identity, and the M3 and M4 messages are used to authenticate and prove this identity. For authentication, the Ed25519 signature scheme is used to sign a challenge based on the M1 and M2 messages. Ed25519 uses the elliptic curve Curve25519 and SHA-512 for hashing, see [RFC8032]

The first part to prove the identity is the server. A challenge, Challenge01, is created with a fixed prefix and the SHA512 hashes of M1 and M2. The server signs this challenge with the private part (ServerSigSec) of the server's ed25519 keypair. The output is a 64 byte signature, Sig01, which is sent in M3 along with the public part (ServerSigPub) of the key pair. Note that Challenge01 is not sent to the client since the client has al information to recreate it.

When the client receives the M3 message from the server, the signature is in M3/Sig01 is verified with M3/ServerSigPub. After the verification, the server has successfully authenticated it self to the client.
```
Challenge01:
---------------------------------------------------
| ASCII for "SC-SIG01"  | SHA512(M1) | SHA512(M2) |
---------------------------------------------------
| 8 bytes               | 64 bytes   | 64 bytes   |
---------------------------------------------------
| byte 0                                 byte 103 |
---------------------------------------------------
 
 
Server side:
    Challenge01 = { "SC-SIG01" || SHA512(M1) || SHA512(M2) }
    Sig01 = sign(Challenge01, ServerSigSec)
 
Client side:
    Challenge01 = { "SC-SIG01" || SHA512(M1) || SHA512(M2) }
    verify(Sig01, Challenge01, ServerSigPub)
 
 
---------------------------------------------------------
| ASCII for "SC-SIG01"                                  |
---------------------------------------------------------
| 0x53 | 0x43 | 0x2D | 0x53 | 0x49 | 0x47 | 0x30 | 0x31 |
---------------------------------------------------------
| byte 0                                         byte 7 |
---------------------------------------------------------
```
Note that "||" is used as a binary operator to denote concatenation of two byte arrays.

Analogously, the client sign a similar challenge, Challenge02, and uses its private sign key to sign it. The difference between Challenge01 and Challenge02 is the prefix. When the server receives the M4 message, the signature in M4/Sig02 is verified with M4/ClientSigPub. After the verification, the client has successfully authenticated it self to the server.
```
Challenge02:
---------------------------------------------------
| ASCII for "SC-SIG02"  | SHA512(M1) | SHA512(M2) |
---------------------------------------------------
| 8 bytes               | 64 bytes   | 64 bytes   |
---------------------------------------------------
| byte 0                                 byte 103 |
---------------------------------------------------
 
 
Client side:
    Challenge01 = { "SC-SIG02" || SHA512(M1) || SHA512(M2) }
    Sig02 = sign(Challenge02, ServerSigSec)
 
Server side:
    Challenge01 = { "SC-SIG02" || SHA512(M1) || SHA512(M2) }
    verify(Sig02, Challenge02, ServerSigPub)
 
 
---------------------------------------------------------
| ASCII for "SC-SIG02"                                  |
---------------------------------------------------------
| 0x53 | 0x43 | 0x2D | 0x53 | 0x49 | 0x47 | 0x30 | 0x32 |
---------------------------------------------------------
| byte 0                                         byte 7 |
---------------------------------------------------------
```

# Multi-link session
This section is not normative.
Consider the use case shown in the figure below.
```
C ---(WebSocket)--> R1 ---(TCP)--> R2 ---(BLE)--> S
     
        Figure: A client, C, connects to a server, S, via two relay servers:
        R1, R2. An end-to-end Salt Channel is established over three different
        types of unencrypted channels.
```
The client, C, wants to establish a Salt Channel with the server, S. C sends M1 to R1 over a WebSocket connection. The M1/ServerSigKey field is included. R1 does not handle this host directly, but knows that R2 might. R1 therefore establishes a TCP connection to R2 and sends M1 to R2. R2, in turn, does not directly handle this host, but R2 knows that S does. R2 therefore establishes a BLE connection with S and sends M1 to S. From there the Salt Channel session can be established over multiple unencrypted links.

However, in this case, R1 will not know when the Salt Channel session terminates. Same for relay R2. They only see encrypted application data in EncryptedMessage packets once the session has been established. This results in the situation where R2 MUST keep the BLE connection open even after the session is closed. This could waste valuable resources and possibly hinder new connections from being established.
These type of situations motivates the principle:
```
Anyone on the transport path of a Salt Channel (a relay server for example)
    MUST be able to determine whether a Salt Channel session has been closed
    without having access to the encrypted data.
```
So, to conclude, we have to have a last message flag that is not encrypted. It MUST be set by the application layer for the last message of the Salt Channel session and have to be readable to any relay node on the transportation path between the client and the server.

# References
* STS, Authentication and authenticated key exchanges, Diffie, W., Van Oorschot, P.C. & Wiener, M.J. Des Codes Crypt (1992) 2: 107. doi:10.1007/BF00124891.
* WS, RFC 7936 The WebSocket Protocol, December 2011, https://tools.ietf.org/html/rfc7936
* DELAY-ATTACK, A blog post by Frans Lundberg explaining the delay attack, http://blog.franslundberg.com/2017/02/delay-attacks-forgotten-attack.html
* RFC2119, RFC 2119 - Key words for use in RFCs to Indicate Requirement Levels, by S. Bradner, https://tools.ietf.org/html/rfc2119
* ED25519, RFC 8032 - Edwards-Curve Digital Signature Algorithm (EdDSA), https://tools.ietf.org/html/rfc8032
* X25519, RFC 7748 - Elliptic Curves for Security, https://tools.ietf.org/html/rfc7748
* XSALSA20, Extending the Salsa20 nonce, https://cr.yp.to/snuffle/xsalsa-20110204.pdf
* SHA512, RFC 6234 - US Secure Hash Algorithms (SHA and SHA-based HMAC and HKDF), https://tools.ietf.org/html/rfc6234
* POLY1305, RFC 7539 - ChaCha20 and Poly1305 for IETF Protocols, https://tools.ietf.org/html/rfc7539
* RFC8032, RFC 8032 Edwards-Curve Digital Signature Algorithm (EdDSA), https://tools.ietf.org/html/rfc8032
* RFC7748, RFC 7748 Elliptic Curves for Security, https://tools.ietf.org/html/rfc7748

# Appendix A - Example session data
Example session data for a simple echo server scenario. Fixed key pairs are used for a deterministic result. Obviously, such an approach MUST NOT be used in production. The encryption key pair MUST be generated for each session to achieve the security goals.

On the application layer, a simple request-response exchange occurs. The client sends the application data: 0x010505050505 and the same bytes are echoed back by the server.
```
No timestamps are used, neither by the server nor the client. The Time fields of the messages are all set to zero.
======== ExampleSession1 ========
 
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
    0600e47d66e90702aa81a7b45710278d02a8c6cddb69b86e299a47a9b1f1c18666e5cf8b000742bad609bfd9bf2ef2798743ee092b07eb32a45f27cd
    a22cbbd0f0bb7ad264be1c8f6e080d053be016d5b04a4aebffc19b6f816f9a02e71b496f4628ae471c8e40f9afc0de42c9023cfcd1b07807f43b4e25
120 -->   WRITE
    0600b4c3e5c6e4a405e91e69a113b396b941b32ffd053d58a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb8454ee0b
    1215dfa08b3ebb3ecd2977d9b6bde03d4726411082c9b735e4ba74e4a22578faf6cf3697364efe2be6635c4c617ad12e6d18f77a23eb069f8cb38173
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
Note to authors: the above output was generated with the Java class saltchannel.dev.ExampleSession1, date: 2018-12-21.
```

# Appendix B - Byte order, bit order and bit numbering

## Notation, byte and bit order
This appendix defines the byte order, bit order and bit numbering used in this document.

### Byte and bit order
* Bytes and bits are zero-indexed, i.e. the leftmost byte and bit is numbered 0.
* Little endian byte order is used for integers, i.e. Least Significant Byte is the leftmost byte.
* Most significant bit first bit order is used,  i.e. the Most Significant bit in a byte is the leftmost bit.
* LSB 0 (least significant bit 0) bit numbering is used, i.e. bit 0 is the least significant bit. 
Considered an unsigned byte, then the value of bit X is 2<sup>X</sup>, i.e.
the value of bit 0 is 2<sup>0</sup> = 0x01 = 1, 
the value of bit 5 is 2<sup>5</sup> = 0x10 = 16
* Bits are numbered in octal, i.e. 0 to 7. This makes it easy to distinguish between bytes.

### Notation for optional fields, and quantification of fields
* "?" (the question mark character) is used to mark a field that MAY or MAY NOT exist in the packet, i.e. the field is prestent zero or one time. 
It does not necessarily indicate an optional field in the sense that it MAY independently exist or not. Wheter a field's existance is optional, mandatory or forbidden could depend on other fields and/or the state of the communication session so far.
* "+" (the plus sign character) is used to mark that a field is present one or more times.
* "*" (the star sign character) is used to mark that a field is present zero or more times.

### Notation for value ranges
The valid range for an integer is expressed using the standard range notation for closed intervals. [0, 127] denotes the closed interval from 0 to 127, including both 0 and 127.

## Examples
The following diagram shows the indices of the individual bytes in messages if the message is stored as a zero indexed array of 8 bit bytes.
```
Byte numbers of bytes
 in first row:--------------------------> 0               1               2               3
Bit numbers:---------------------> 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
                                  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                                0 |       0       |       1       |       2       |       3       |
Byte number of first              +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 byte in second row:----------> 4 |       4       |       5       |       6       |       7       |
                                  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
Messages with fields that span multiple lines are written as a continuous block using backslashes. Consider the following message with two fields, ShortField (length 1 byte) and LongField (length 21 bytes). 
```
           0               1               2               3
    7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 0 | ShortField  | LongField                                       |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 4 | LongField                                                     |
 8 \                                                               \
12 \                                                               \
16 | LongField                                                     |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
20 | LongField   |
   +-+-+-+-+-+-+-+
```
The following diagrams shows different integers as they would be written in this document.
```
8 bit integer value 1
 MSbit         LSbit
   |             |
          0
   7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+
0 |0 0 0 0 0 0 0 1|
  +-+-+-+-+-+-+-+-+
 
 
8 bit unsigned integer value 171
          0
   7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+
0 |1 0 1 0 1 0 1 1|
  +-+-+-+-+-+-+-+-+
 
 
32 bit integer value 513
          0               1               2               3
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |0 0 0 0 0 0 0 1|0 0 0 0 0 0 1 0|0 0 0 0 0 0 0 0|0 0 0 0 0 0 0 0|
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
64 bit integer value 513
          0               1               2               3
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |0 0 0 0 0 0 0 1|0 0 0 0 0 0 1 0|0 0 0 0 0 1 0 0|0 0 0 1 0 0 0 0|
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4 |0 0 0 0 0 0 0 0|0 0 0 0 0 0 1 0|0 0 0 0 0 0 0 0|0 0 0 0 0 0 0 0|
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ 
```
Two examples of a message with a "?" field.
```
Message definition:
          0               1               2
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |       C?      |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
With the optional field C included:
          0               1               2
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |       C       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
Without the optional field C:
          0               1
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```
Two examples of a message with a "+" field.
```
Message definition:
          0               1               2
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |       C+      |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
With minimum number (one) of C field occurrences:
          0               1               2
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |       C       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 
 
 
With three C field occurrences:
          0               1               2               3
   7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0 7 6 5 4 3 2 1 0
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
0 |       A       |       B       |       C       |       C       |
  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
4 |       C       |
  +-+-+-+-+-+-+-+-+
  ```