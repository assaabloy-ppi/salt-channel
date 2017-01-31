Frans Lundberg, 2017-01-31


v2-notes.md
===========

Some notes on Salt Channel v2 -- a proposed new version of the protocol.


Protocol
========


Message order
-------------

    Session = M1 [M2 M3 M4] AppMessage*


Message details
---------------



    **** M1 ****
    
    2   Protocol. 
        Always 0x8350 (ASCII "S2") for Salt Channel v2.

    2   Header1.

    32  ClientEncKey.
    
    32  ServerSigKey, OPTIONAL.
        Used to choose what virtual server to connect to.

    Total size is 36 bytes or 68 bytes when ServerSigKey is used.


   
    **** M2 ****
    
    2   Header2.
        Can contain indicator for no-such-server when the virtual server
        feature is used.
    
    32  ServerEncKey.

    16  AuthenticationTag.
        Bytes to authenticate the authenticated encryption of Encrypted1.

    96  Encrypted1.
        The bytes of the following message encrypted with crypto_box_afternm().

        32  ServerSigKey
        64  Signature 1

    Total size is 146 bytes.



    **** M3 ****

    16  AuthenticationTag.
        Bytes to authenticate the authenticated encryption of Encrypted2.
    
    96  Encrypted2.
        The bytes of the following message encrypted with crypto_box_afternm().

        32  ClientSigKey
        64  Signature2

    Total size is 112.



    **** AppMessage ****

    2   Header3.
        The header includes a close bit. If MUST be set for in the last
        AppMessage sent by Client and in the last AppMessage sent by Server.

    16  AthenticationTag.
        Bytes to authenticate the authenticated encryption of Encrypted3.

    x   Encrypted3.
        Application layer message encrypted with crypto_box_afternm().,



The grand total size for a handshake is 294 bytes (306 with size prefixes).
Same figure for Salt Channel v1 is 347 bytes.
An AppMessage of size N will result in a Salt Channel message of size N+18.
The overhead per app message decreases too. From ? to 18 bytes.


Resume feature
==============

How would "resume" idea be included in the protocol?


    **** M1 ****

    Same as before PLUS ResumeTicket:

    1   Ticket header. Currently = 0.
    
    1   Size.
        Size of encrypted ticket.

    (4   HostData).
        Unencrypted data, can be used by host implementation 
        for any purpose. It is not encrypted and can therefore be
        used by the host before the ticket is decrypted.
        For example as an ID for the symmetric key of the encrypted 
        data that follows.

    16  EncryptedTicketAuthenticator

    x   EncryptedTicket.
        Encrypted ticket data.
        First 4 bytes is always the HostData repeated. 
        For authentication-purposes.

    y   EncryptedAppMessage.
        First application layer message.

    The TicketIndex is used in the nonce for the encryption.


Ticket
------

Ticket format.

    2   Header.

    8   TicketIndex.

    16  SessionKey.
    
    32  ClientSigKey.

Total size: 58.


Only resume
-----------

    2   Protocol. 
        Always 0x8350 (ASCII 'S2') for Salt Channel v2.

    2   Header1.

    74  Authenticated and encrypted ticket.

    x   AppMessage.

For "unlock", we could likely do it in less than 100 bytes, one-way!


Comparison
----------

Comparing v1 with multiple cases of v2.

         Ways     Data

    v1   3        347    Salt Channel v1.

    v2A  3        294    Salt Channel v2, ordinary.

    v2B  1         96    Resume-only.

    v2C  1        128    Resume+ordinary, resumed used.

    v2D  3        322    Resume+ordinary, ordinary used.

    v2E  5        500    Resume-only. Resume-ticket not valid. Ordinary used.

Note also, the biggest saving could be the crypto, no assymetric crypto when 
resume is used.

Seems like there is too little value with the resume-only cases (B, E).
Instead, possible cases would be A, C, D.


