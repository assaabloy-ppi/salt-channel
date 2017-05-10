Author: Frans Lundberg, ASSA ABLOY AB
Date: 2017-05-10


salt-channel-vs-tls-2017-05.md
==============================

Example comparing Salt Channel with TLS. Wireshark used to analys.
See code in saltchannel.dev.Tls/RunClient/RunServer.

Results:

    SALT CHANNEL VS TLS
    
    Application: client sends 6 bytes, server echos back the same bytes.
    
    Salt Channel: v2 used, no resume.
    
    TLS: protocol v1.2, suite TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256, 
    256-bit keys, curve P-256, client and server certs used (minimal, self-signed),
    no resume.
    
    RESULTS                    TLS           Salt Channel       Comment
    
    Total bytes:              2299                    404       TLS uses nearly 6 times more data.
    Round-trips:                 4                      2       3 round-trips was expected for TLS.
    Forward secrecy:           yes                    yes
    Client authenticated:      yes                    yes
    Client ID hidden:           no                    yes       Perhaps support in TLS 1.3 spec.
    EC curve:                P-256             Curve25519
    
