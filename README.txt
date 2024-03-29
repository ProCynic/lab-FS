Administrative: 
  Geoffrey Parker - grp352 - gparker
  Alex Chan - ayc87- aychan
  Slip Days this project: 1
  Slip Days total: 5

Source:
  Everything in the tarball we downloaded.
  BitMap.java
  FlatFS.java
  FlatFSUnit.java
  Helper.java
  InternalNode.java
  Node.java
  Ptree.java
  PTreeUnit.java
  TNode.java
   
Description:

  Our project mainly adheres to the specs in the project description.  We implemented an atomic disk that utilizes a write-back log to insure a state of consistency while mainting thread safety.  For the log,
  it is stored in the first 1024 sectors of the disk.  The format of the log consists of chunks of write meta data, then the actual write bytes, and then a commit sector.  We used a transaction hash map to
  store the transID to transaction objects.  This also allowed for efficient garbage collection as the the only places where a transaction are referenced is in the write back queue and in the hash map.
  In using a transaction object, it allowed us to manage transactions easier by using a function called aTrans to atomically execute read or writes.  The write-back queue is a queue of committed transactions
  that is consumed by a separate write-back thread.  This is also used for recovery.  We keep track of the log itself with a head and tail pointer which are also stored to the disk.
  
  We have one lock in ADisk.  It controlls access to the shared data, namely the disk and the write back queue.  There is a condition on this lock for threads waiting for disk transactions to 
  complete to wait on.  There is another condition to ensure that only one commit happens at a time.  We create a new thread that runs a write back object, consuming the wait back queue.
  
  Recovery first reads the head and tail pointers from the disk.  They are stored in sector 1025.  Then it reads all the transactions from the tail to the head and, if they have a commit, adds them to
  the write back queue, where they will be written to the disk as ususal.

Testing:
  We are using JUnit 4 for our unit tests.
  We have 2 program, PTreeUnit and FlatFSUnit, that runs all our unit tests.
  We tested to make sure that most of the pubilc methods of PTree work, and a few of FlatFS work while having exceptions specified in the spec throw those exceptions when appropriate.
  We tested the public api of PTree to make sure it functions correctly for the most part.  FlatFS was not tested as extensively.
