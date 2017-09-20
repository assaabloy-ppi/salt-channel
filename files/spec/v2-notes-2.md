Frans Lundberg, 2017-09-20


v2-notes.md
===========

Notes on Salt Channel v2, a new version of the protocol.

To consider
===========


Temporary notes
---------------

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
    Yes, needed! Needed now!

