spec-salt-channel.md
====================

About this document
-------------------

*Date*: 2017-01-04

*Author*: Frans Lundberg. ASSA ABLOY AB, Shared Technologies, Stockholm,
frans.lundberg@assaabloy.com, phone: +46707601861.


Changes
-------

Significant changes of this document.

* 2017-01-04. Clarifications based on audits by Assured AB and 
  Fabrizio De Santis. Language updates.

* 2016-11-11. Fixed spec error of field M4:g, "Signature2". Found by 
  Simon Johanssson. Thank you!



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

Salt Channel is "Powered by Curve25519".



Protocol overview
=================

The peer that initiates the communication is called Client. 
The other peer is called Server. Figure 1 shows an overview of 
the sequence of messages passed between Client 
and Server during a Salt Channel session.
    
    CLIENT                                                 SERVER
    
    ClientEncKey
    ProtocolVersion
    [ServerSigKey]                 ---M1--->
                
                                   <--M2----         ServerEncKey
                                   
                                                     ServerSigKey
                                   <--M3*---           Signature1
                                   
    ClientSigKey
    Signature2                     ---M4*-->
    
    ApplicationData                <--*---->      ApplicationData
    
            Figure 1: Salt Channel messages. An asterisk (*) is used to 
            indicate that a message is encrypted.
    
    
Each peer holds a long-term signing key pair. The peers are assumed to know
each other's public signing key before the Salt Channel session is started.
In addition to the signing key pair, each peer generates an 
ephemeral encryption key pair for use exclusively for one Salt Channel session.

The handshake begins with Client sending his ephemeral public 
encryption key (ClientEncKey) to the server and the server responding
with his ephemeral public encryption key (ServerEncKey).
The first message, M1, also contains ProtocolVersion
and optionally the Server's public signing key (ServerSigKey). 
ServerSigKey is needed in cases where multiple Salt Channel servers
are sharing the same communication endpoint. For example, it is possible for 
multiple Salt Channel servers to share a single TCP port on a computer.
We call this feature *virtual hosting* due to its similarity with 
virtual hosting [VIRTUAL] in the context of the World Wide Web.

So, in the first part of the handshake (M1, M2), Client and Server
exchange their ephemeral public keys in cleartext. After that,
the peers can create a common secret using Diffie-Hellman 
key agreement. This secret is used to encrypt all of the following
messages (M3, M4, ApplicationData). At this stage the peers can 
communicate confidentially. However, they have not authenticated to 
each other so they do not know for sure who they communicate with.

Messages M3, M4 achieve mutual authentication. In message M3, 
Server sends his public signing key together with the signature of 
ServerEncKey + ClientEncKey (Signature1). 
Client responds with his public signing key and the signature 
of ClientEncKey + ServerEncKey (Signature2).

Once Client has verified Signature1, he knows that he is communicating
with Server. More precisely, Client knows he communicates with someone
holding the private key corresponding to ServerSigKey.

Once Server has verified Signature2, he knows that he is communicating
with Client. More precisely, Server knows he communicates with someone
holding the private key corresponding to ClientSigKey.

Note that an attacker will not be able to determine the identity
of Client (ClientSigKey). A passive attacker (eavesdropping only), will
not be able to determine the identity of Server (ServerSigKey) either.

Note also that if the application protocol starts with a message from 
Client to Server (a common case) this first application message can be 
send together with M4 in one IO "write" operation without waiting for 
M4 to reach Server. This results in a handshake that only has a one 
round-trip overhead. Implementations should allow the application to 
send a first application message together with M4.

If M4 is invalid (in particular if the signature is invalid), any 
application message that follows must be discarded by Server.



Message details
===============

This section provides the details of the protocol messages.

Note that each message is of know size. How this size if specified depends on 
the underlying transport layer. When Salt Channel is used on top 
of a byte stream (TCP for example) a 4-byte size header is prepended.
See section "Salt Channel over a stream".

The messages are serialized to bytes using Binson. See [Binson]. 
Field names and types refer to Binson field names and their types.
Binson is an exceptionally light-weight serialization format with 
a small data overhead.

Message details are presented below. Each message is a Binson object.
Each field is a Binson field. The first column is the Binson field 
name, the second is the Binson type of the field. The third column 
contains the field name as used in this specification text and a 
description of the field. Messages that are encrypted and authenticated 
are marked with an asterisk (*). See section "Authenticated encryption"
for details about the message encryption.

Note that the order of the fields has no particular meaning. In fact, the 
order of the fields is the alphabetic order based on the field name as 
required by Binson [BINSON].

References to function names are the names of the NaCl (and TweetNaCl) 
C library functions by Bernstein. See [NACL].

    
    Message M1: Client --> Server
    =============================
    
    First message sent by Client. ServerSigKey is included only when Client
    wants to chose one server among many available at the same end point
    (virtual hosting).
    
    The tables below contain the following information of each field:
    Binson field name, type, long name (used in this document), and 
    a description.
    
    e  bytes  ClientEncKey
                Client's ephemeral public key for encryption.
                32 bytes. The key is created with crypto_box_keypair() [NACL].
                
    p  string ProtocolVersion
                Protocol identifier and version. Currently it always has
                the value "S1".
                  
    s  bytes  ServerSigKey
                OPTIONAL field. Server's public signing key. 
                32 bytes. The key is created with crypto_sign_keypair() [NACL].
    
    
    Message M2: Client <-- Server
    =============================
    
    e  bytes  ServerEncKey
                Server's ephemeral public key for encryption.
                32 bytes. The key is created with crypto_box_keypair() [NACL].
               
    
    Message M3*: Client <-- Server
    ==============================
    
    This message is encrypted.
    
    g  bytes  Signature1
                Signature of the concatenation of ServerEncKey + ClientEncKey.
                64 bytes. Only the signature itself is included.
                The signature is created with crypto_sign() [NACL].
    
    s  bytes  ServerSigKey
                Server's long-term public signing key.
                32 bytes. The key is created with crypto_sign_keypair() [NACL].
                
                
    Message M4*: Client --> Server
    ==============================
    
    This message is encrypted.
    
    c  bytes  ClientSigKey
                Client's long-term public signing key.
                32 bytes. The key is created with crypto_sign_keypair() [NACL].
                
    g  bytes  Signature2
                Signature of the concatenation of ClientEncKey + ServerEncKey.
                64 bytes. Only the signature itself is included.
                The signature is created with crypto_sign() [NACL].
    
    
    ApplicationData*
    ================
    
    After M4, arbitrary application encrypted messages are passed between the 
    peers. This is entirely up to the application protocol. Each application
    message (byte array) is encrypted that same way as M3 and M4.
    
            Figure 2: Messages details.
    
    
A peer should immediately terminate the communication session if it 
receives invalid data from the other peer. 
Data is considered invalid if it does not follow this specification.
In particular, a signature that does not verify is considered invalid data.

Implementations must ignore Binson fields in addition to the
fields defined here. That is, additional fields not befined here are allowed
and should not categorize the message as "invalid data".
The fields defined here must exist and must follow the specification.
Allowing additional fields like this allows backwards-compatible additions to 
the protocol and it allows the application layer to send additional 
data in the handshake messages. Such additional application-specific fields
do not, of course, have the security guarantees that the general 
application messages that follow after Message M4 have.



Authenticated encryption
========================

A secret shared between the peers is obtained using Diffie-Hellman key agreement 
based on the ephemeral key pairs. The crypto_box_beforenm() function [NACL] is
then used to create a shared symmetric key from that shared secret. 
The symmetric key is used in to encrypt and authenticate each message of 
the Salt Channel session except for messages M1 and M2.

Each encrypted message has the following format.

    Encrypted message
    =================
    
    b  bytes  EncryptedMessage
                Encrypted bytes, encrypted and authenticated with the  
                crypto_box() function [NACL]. EncryptedMessage can be of 
                arbitrary size. Note, for messages M3, and M4, EncryptedMessage
                is a Binson object. This is not a requirement of the application
                data messages. They are arbitrary byte arrays.
                
            Figure 3: Encrypted message format.
    
The authenticated encryption algorithm of the crypto_box() function 
of TweetNaCl [NACL, TWEET-1, TWEET-2] is used. The corresponding function to 
decrypt and verify is called crypto_box_open(). Note, in practice, 
implementations will use the functions crypto_box_beforenm(), 
crypto_box_afternm(), and crypto_box_open_afternm(). See [NACLBOX].

The first 16 bytes of the ciphertext is used to authenticate the message when
it is decrypted. Encryption and authentication are done in one atomic function 
call and is described in the original NaCl paper [NACL].

The crypto_box_x() functions take a 24-byte nonce as a parameter.
Both Client and Server use the first 8 bytes of the nonce to store 
a signed 64-bit integer in little-endian byte order.
This integer is 1, 3, 5, ... for messages sent by Client; increasing by 2 for 
each message it sends. This integer is 2, 4, 6, ... for messages send by Server;
increasing by 2 for each message it sends.
The rest of the bytes of the nonce must be set to zero.
The nonce counters are reset for every Salt Channel session.
Note, no assumption is made on the order in which the peers send
application messages. For example, Server may send all application messages.
The nonce values used by Client and those used by Server are disjoint sets.
Note also that the nonce values used are *not* send over the communication 
channel. This is not necessary; they can easily be computed.



Salt Channel over a stream
==========================

When Salt Channel is implemented on top of a stream, such as TCP, the following
format is used:

    Stream = (Size Message)+
    
Size is a signed 32-bit integer (range: 0 to 2^31-1) with the byte size of 
the following Message. Little-endian-first byte order is used.
Message is the raw bytes. The format of those Message:s is defined in
this document.



Salt Channel over Web Socket
============================

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



Protocol design
===============

This section is informative.


Priorities
----------

The following priorities were used when designing the protocol.

1. The first priority is to achieve high security. 

2. The second priority is to achieve a low network overhead; 
   that is, few round-trips and a small data overhead.
   
3. The third priority is to allow low code complexity and low CPU and memory 
   requirements of the communicating peers. Low complexity is always 
   important to achieve high security.


Goals
-----

The following are the main goals and limitations of the protocol.

* 128-bit security. 
    Best attack should be 2^128 brute force.

* Forward secrecy.

* Client cannot be identified.
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


 
Possible changes
================

Possible specification changes and amendments for a future version.

* **A-field**. Field m1:p renamed to "A" to be inline with
  other Binson formats and to have the protocol type in the very 
  fixed offset from the beginning of the message.
  
* **Remove Binson**. Other serialization, independent of Binson?
  Just fixed offsets or something? Reduce code? Slightly less overhead.
  
* **Change encrypted messages**. Currently, the encrypted messages
  are a Binson message. This change is to remove the Binson use
  for encrypted messages (the b-field) and send encrypted bytes 
  directly. There seems to be little or no use of Binson for the
  symmetric encryption.
  
* **M3, M4 field names**. The order of signature and public key in 
M3, and M4 is different. Proposal: use "s" for signature and "k" for
the public signing key in both messages. There is little need to 
differentiate between Server and Client. Also, this is not done with
the "e" field. It is used for both ClientEncKey and ServerEncKey.

* **Remove the possibility of additional fields**. Remove the allowance
of "added fields". Fields not specified are allowed in this version 
of the protocol. This feature should be removed. The complexity is not
worth that possible benefits.

* **Improve spec 1**. Specify "virtual hosting" and "NoSuchHost" error
message. 

* **Improve spec 2**. Specify how a peer should handle a peer that 
does not follow the protocol.



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



Appendix A: Example session
===========================

This appendix contains a complete example of a Salt Channel handshake.

Salt Channel uses Binson [BINSON] to serialize messages. It is a light-weight 
data serialization format. Binson is a binary serialization format. However, 
the corresponding JSON representation of Binson objects are used when presenting 
the message data below.


Keys used
---------

Keys used in the example handshake session.

Note, in the API of TweetNaCl, a secret signing key *contains* the 
public key in the last 32 bytes of it. That is why the public key (32 bytes)
is repeated in the data above.

Server's signing key pair:

    sec: 7a772fa9014b423300076a2ff646463952f141e2aa8d98263c690c0d72eed52d07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b
    pub: 07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b

Server's ephemeral encryption key pair:

    sec: 5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb
    pub: de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f
  
Client's signing key pair:

    sec: 55f4d1d198093c84de9ee9a6299e0f6891c2e1d0b369efb592a9e3f169fb0f795529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b
    pub: 5529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b

Client's ephemeral encryption key pair:

    sec: 77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a
    pub: 8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a
  
Shared key for symmetric encryption:

    1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389

The shared key is computed by the client using his private 
key and the server's public key. The server computes the shared key
using his private key and the public key of the client. This is
Diffie-Hellman key agreement.


Message data
------------

Below is the data sent in the example handshake.
m1, m2, m3, m4 are clear-text, while m3enc and m4enc are encrypted with 
symmetric encryption.

    m1 raw: (size 46) 4014016518208520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a1401701402533141
    m1 Binson: {
      "e": "0x8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a", 
      "p": "S1"
    }

    m2 raw: (size 39) 401401651820de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f41
    m2 Binson: {
      "e": "0xde9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f"
    }

    m3 raw: (size 108) 401401671840fad9747882a3d6e9bf4d6bf709f20da72694f839962038fa1b9fc02342733bc01d27847bd131b09355aa055a2c7f554ef1cd5bf7e12c62f77f1d18ace5ca0300140173182007e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b41
    m3 Binson: {
      "g": "0xfad9747882a3d6e9bf4d6bf709f20da72694f839962038fa1b9fc02342733bc01d27847bd131b09355aa055a2c7f554ef1cd5bf7e12c62f77f1d18ace5ca0300", 
      "s": "0x07e28d4ee32bfdc4b07d41c92193c0c25ee6b3094c6296f373413b373d36168b"
    }

    m3enc raw: (size 131) 40140162187cc11a5740752f9ef562f7552123819a0085d9da0ea02ed4a1be9fd079eaab69d5e5b528668fdfae7bf4e9656c5c70ef9d151d5442c67932720146f779bf2089e7313840b9f153a83541ed446626de2d185b7aeffeefa70520ede8b68f96cb30b1566684efdcd28c962d1bfee1ee2a8367db31eab1d313dcb9d65853cb41
    m3enc Binson: {
      "b": "0xc11a5740752f9ef562f7552123819a0085d9da0ea02ed4a1be9fd079eaab69d5e5b528668fdfae7bf4e9656c5c70ef9d151d5442c67932720146f779bf2089e7313840b9f153a83541ed446626de2d185b7aeffeefa70520ede8b68f96cb30b1566684efdcd28c962d1bfee1ee2a8367db31eab1d313dcb9d65853cb"
    }

    m4 raw: (size 108) 4014016318205529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b1401671840d2383c7eb5e49eac2056feed24b54525507d91594190493b7d4389f27c0ee11152db278248bfa4a3d7b4b15e1b8fb56192f1364f32af658eadf7bd799c814f0741
    m4 Binson: {
      "c": "0x5529ce8ccf68c0b8ac19d437ab0f5b32723782608e93c6264f184ba152c2357b", 
      "g": "0xd2383c7eb5e49eac2056feed24b54525507d91594190493b7d4389f27c0ee11152db278248bfa4a3d7b4b15e1b8fb56192f1364f32af658eadf7bd799c814f07"
    }

    m4enc raw: (size 131) 40140162187cdae551bde10f0b543bbc591125c6e646f73bfc662578a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb7ed71646480906c138b023aac5262616246da2481b0944ab80f41c3db20568bc40b100d72c90f75b7ec411f1d23ad620d89da9a35e3a01685041280219cd05c40e4e60ffb26541
    m4enc Binson: {
      "b": "0xdae551bde10f0b543bbc591125c6e646f73bfc662578a54bdcc8eef60a47d0bf53057418b6054eb260cca4d827c068edff9efb48f0eb7ed71646480906c138b023aac5262616246da2481b0944ab80f41c3db20568bc40b100d72c90f75b7ec411f1d23ad620d89da9a35e3a01685041280219cd05c40e4e60ffb265"
    }

The code in HandshakeExampleData.java was used to create the above output.


Total
-----

In total, the handshake example requires a three-way communication exchange
before the application protocol can start. In many cases, the client is the 
first peer to send data at the application protocol layer. In these cases, 
the first application message can be sent together with m4 and the handshake
will have a one round-trip overhead.

The total amount of data sent is 46 + 39 + 131 + 131 + 4*4 = 363 bytes.

        CLIENT     SERVER
       
        50 -------------> 
        <------------ 178
        135 ------------>
    
        Figure: Amount of data exchanged in a Salt Channel handshake, the
        total is 363 bytes.

The figure above shows the amount of data exchanged in the handshake example.
The four-byte size header of each message is included.
