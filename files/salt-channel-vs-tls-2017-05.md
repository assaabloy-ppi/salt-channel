Frans Lundberg, ASSA ABLOY AB, 2017-05-10


salt-channel-vs-tls-2017-05.md
==============================

Example comparing Salt Channel with TLS. Wireshark is used 
to analyse the network traffic. 
See code in saltchannel.dev.Tls/RunClient/RunServer for 
implementation details.

The application protocol: 

    Client sends 6 bytes (0x010505050505), the server echoes the
    same six bytes back to the client. 12 bytes sent, one round-trip.

Results:

    SALT CHANNEL VS TLS
    
    Salt Channel: v2, no resume.
    
    TLS: v1.2, suite TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, 
    256-bit keys, curve P-256, client and server certificates used (minimal, self-signed),
    no resume.
    
    RESULTS                    TLS    Salt Channel    Comment
    
    Total sent bytes:         2299             404    TLS: 6 times more data.
    Round-trips:                 4               2    
    Forward secrecy:           yes             yes    TLS: for this cipher suite.
    Client authenticated:      yes             yes    TLS: for this scenario.
    Client ID hidden:           no             yes    Supported in TLS1.3 draft.
    EC curve:                P-256      Curve25519
    ECC key size (bits)        256             256
    Symmetric key size (bits)  128             128
    

TLS has a much larger handshake overhead, about 6 times more data
is sent and 4 round-trips are used while Salt Channel only needs 
2 round-trips.

Note that [TLS1.3] will likely reduce the differences in overhead.
According to the current draft [TLS1.3], TLS 1.3 will reduce 
round-trip overhead and will support raw public keys. TLS 1.3 will
also have support for hidden client IDs. See comments below.

Comments:

* TLS uses about 6 times more data.

* TLS uses 4 round-trips while Salt Channel needs only 2.

* 3 round-trips were expected for the TLS case. However, 
    no investigation has been made to explain this 
    unexpected behavior.

* The [TLS1.3] draft has protection of client ID from an active attacker.
  Ref: personal email to frans.lundberg@assaabloy.com from 
  Eric Rescorla, the author of [TLS1.3] (2017-05-11).
  
* The [TLS1.3] draft has support for using "raw public keys" as
  defined in [RFC7250]. This would likely reduce the data overhead of
  the handshake significantly.
  
* ed25519 signature is included in the current TLS 1.3 draft.

* The current TLS 1.3 draft [TLS1.3] says that
  a TLS-compliant application SHOULD support key exchange 
  with X25519.

* Using RSA 4096-bit keys would increase the handshake overhead
    significantly.
    
* Using real certificate chains would increase the
    handshake data significantly. In this example, both the client 
    certificate and the server certificate are self-signed. The certificate
    chains on both sides contain only one certificate.


References
==========

[TLS1.3] TLS version 1.3, draft-ietf-tls-tls13-20, April 2017. 
See https://tools.ietf.org/html/draft-ietf-tls-tls13-20.

[RFC7250] RFC 7250, "Using Raw Public Keys in Transport Layer Security (TLS)
and Datagram Transport Layer Security (DTLS)", June 2014.

