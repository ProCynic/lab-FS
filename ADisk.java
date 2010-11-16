/*
 * ADiskI.java
 *
 * Interface to ADisk
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007, 2010 Mike Dahlin
 *
 */
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class ADisk implements DiskCallback{

	//-------------------------------------------------------
	// The size of the redo log in sectors
	//-------------------------------------------------------
	public static final int REDO_LOG_SECTORS = 1024;
	private int logHead = 0;
	private int logTail = 0;
	private Disk disk;
	private SimpleLock lock;

	//This holds the only references to Transactions, allowing controlled gc
	private HashMap<TransID, Transaction> transactions;

	//-------------------------------------------------------
	//
	// Allocate an ADisk that stores its data using
	// a Disk.
	//
	// If format is true, wipe the current disk
	// and initialize data structures for an empty 
	// disk.
	//
	// Otherwise, initialize internal state, read the log, 
	// redo any committed transactions, and reset the log.
	//
	//-------------------------------------------------------
	public ADisk(boolean format) {
		this.lock = new SimpleLock();
		try {
			this.disk = new Disk(this, 0);
		} catch (FileNotFoundException e){
			// TODO: Real error handling
			System.out.println(e.toString());
			System.exit(-1);
		}
		if (format) {
			;//TODO: Delete DISK.dat
			//TODO: Init Data Structures
		}
		else {
			;//TODO: Init state
			//TODO: Read log, redo any commited transactions, update log
		}
	}



	//-------------------------------------------------------
	//
	// Return the total number of data sectors that
	// can be used *not including space reseved for
	// the log or other data sructures*. This
	// number will be smaller than Disk.NUM_OF_SECTORS.
	//
	//-------------------------------------------------------
	public int getNSectors()
	{
		return Disk.NUM_OF_SECTORS - ADisk.REDO_LOG_SECTORS;  // Can change if we add other data structures
	} 

	//-------------------------------------------------------
	//
	// Begin a new transaction and return a transaction ID
	//
	//-------------------------------------------------------
	public TransID beginTransaction()
	{
		TransID tid = new TransID();
		Transaction trans = new Transaction();
		assert (this.transactions.put(tid, trans) == null);
		return tid;
	}

	//-------------------------------------------------------
	//
	// First issue writes to put all of the transaction's
	// writes in the log.
	//
	// Then wait until all of those writes writes are 
	// safely on disk (in the log.)
	//
	// Then, mark the log to indicate that the specified
	// transaction has been committed. 
	//
	// Then wait until the "commit" is safely on disk
	// (in the log).
	//
	// Then take some action to make sure that eventually
	// the updates in the log make it to their final
	// location on disk. Do not wait for these writes
	// to occur. These writes should be asynchronous.
	//
	// Note: You must ensure that (a) all writes in
	// the transaction are in the log *before* the
	// commit record is in the log and (b) the commit
	// record is in the log before this method returns.
	//
	// Throws 
	// IOException if the disk fails to complete
	// the commit or the log is full.
	//
	// IllegalArgumentException if tid does not refer
	// to an active transaction.
	// 
	//-------------------------------------------------------
	public void commitTransaction(TransID tid) throws IOException, IllegalArgumentException {

		Transaction t = this.transactions.get(tid);
		if (t == null)
			throw new IllegalArgumentException();
		for (byte[] b : t.getSectors())
			this.disk.startRequest(Disk.WRITE, writeTag(), logHead, b);//TODO: Write b to log
		return;
	}



	//-------------------------------------------------------
	//
	// Free up the resources for this transaction without
	// committing any of the writes.
	//
	// Throws 
	// IllegalArgumentException if tid does not refer
	// to an active transaction.
	// 
	//-------------------------------------------------------
	public void abortTransaction(TransID tid) 
	throws IllegalArgumentException
	{
		this.transactions.put(tid, null);
	}


	//-------------------------------------------------------
	//
	// Read the disk sector numbered sectorNum and place
	// the result in buffer. Note: the result of a read of a
	// sector must reflect the results of all previously
	// committed writes as well as any uncommitted writes
	// from the transaction tid. The read must not
	// reflect any writes from other active transactions
	// or writes from aborted transactions.
	//
	// Throws 
	// IOException if the disk fails to complete
	// the read.
	//
	// IllegalArgumentException if tid does not refer
	// to an active transaction or buffer is too small
	// to hold a sector.
	// 
	// IndexOutOfBoundsException if sectorNum is not
	// a valid sector number
	//
	//-------------------------------------------------------
	public void readSector(TransID tid, int sectorNum, byte buffer[])
	throws IOException, IllegalArgumentException, 
	IndexOutOfBoundsException
	{
		if (buffer.length < Disk.SECTOR_SIZE)
			throw new IllegalArgumentException();
		if (sectorNum < ADisk.REDO_LOG_SECTORS || sectorNum >= Disk.NUM_OF_SECTORS)
			throw new IndexOutOfBoundsException();
		//TODO: Check unwritten transactions
		disk.startRequest(Disk.READ, readTag(), sectorNum, buffer);
	}

	//-------------------------------------------------------
	//
	// Buffer the specified update as part of the in-memory
	// state of the specified transaction. Don't write
	// anything to disk yet.
	//  
	// Concurrency: The final value of a sector
	// must be the value written by the transaction that
	// commits the latest.
	//
	// Throws 
	// IllegalArgumentException if tid does not refer
	// to an active transaction or buffer is too small
	// to hold a sector.
	// 
	// IndexOutOfBoundsException if sectorNum is not
	// a valid sector number
	//
	//-------------------------------------------------------
	public void writeSector(TransID tid, int sectorNum, byte buffer[])
	throws IllegalArgumentException, 
	IndexOutOfBoundsException 
	{
		this.transactions.get(tid).add(sectorNum, buffer);

	}

	public void requestDone(DiskResult r) {
		//TODO: Do this when a request is completed
	}

	private int readTag() {
		return 0;  //TODO: Fix
	}

	private int writeTag() {
		return 0; //TODO: Fix
	}
}
