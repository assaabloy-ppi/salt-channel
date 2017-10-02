Frans Lundberg, 2017-10-03


v2-notes.md
===========

Notes on Salt Channel v2, a new version of the protocol.

Document notes
==============

Note for v2 draft/final documents.

* Check TODO markers in text.

* TODO: consider pubkey in A1.

* TODO: how to use LastFlag from application layer.
  Do we need boolean LastFlag param to ByteChannel.write()?
  Perhaps we do.
  
* TODO: consider proxy info, domain + port in M1.

* TODO: generate new data for Appendix A.

* DONE! "SIG1" -> "SALTSIG1"  
* DONE! spec, v2 must have LastFlag!
* DONE! implement EosFlag in Java.
* DONE! Add role prefix to signatures.



Protocol development notes
==========================

* Independent message parsing. 
    Each packet should be possible to parse *independently*.
    Independently of the previous communication and any state.
    The pack/unpack can thus be completely independent.

* Single-byte alignment.
    There is not special reason to have 2, or 4-byte alignment of
    fields in this protocol. Compactness is preferred.

* Notation. Use style: "M1/Header".

* CloseFlag.
    Yes, needed! Needed now! See section "Use case: multi-link session"
    in v2 spec.
    


Java implementation
===================

* How to indicate LastFlag? 
    - Add method writeLast(bytes) to ByteChannel. 
    - Or, add method write(bytes, isLast)? And make the other write method deprecated.
    

