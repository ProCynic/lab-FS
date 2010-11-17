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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;

public class ADisk implements DiskCallback{

	//-------------------------------------------------------
	// The size of the redo log in sectors
	//-------------------------------------------------------
	public static final int REDO_LOG_SECTORS = 1024;
	private Integer logHead = 0;
	private Integer logTail = 0;
	private Disk disk;
	private SimpleLock lock;
	
	private Condition diskDone;
	private Condition commit;
	private Condition writeWait;
	private boolean commitInProgress = false;
	//This holds the only references to Transactions, allowing controlled garbage collection
	private HashMap<TransID, Transaction> transactions;
	
	private int lastCompletedAction;


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
		this.diskDone = lock.newCondition();
		this.commit = lock.newCondition();
		this.writeWait = lock.newCondition();
		try {
			this.disk = new Disk(this, 0);
			if (format) {
				File hdd = new File("DISK.dat");
				hdd.delete();
				this.disk = new Disk(this, 0);
			}
			else {
				;//TODO: Recover
			}
			byte[] b = ADisk.blankSector();
			this.aTrans(0, b, Disk.WRITE);
		}catch (FileNotFoundException e) {
			System.out.println(e.toString());
			System.exit(-1);
		}catch (IOException e) {
			assert(false);
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
		return Disk.NUM_OF_SECTORS - ADisk.REDO_LOG_SECTORS - 1;  // Can change if we add other data structures
	} 

	//-------------------------------------------------------
	//
	// Begin a new transaction and return a transaction ID
	//
	//-------------------------------------------------------
	public TransID beginTransaction()
	{
		lock.lock();
		TransID tid = new TransID();
		Transaction trans = new Transaction();
		assert (this.transactions.put(tid, trans) == null);
		lock.unlock();
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
		lock.lock();
		while(this.commitInProgress)
			this.commit.awaitUninterruptibly();
		this.commitInProgress = true;
		Transaction t = this.transactions.get(tid);
		if (t == null)
			throw new IllegalArgumentException();
		
		if (t.size() >= ADisk.REDO_LOG_SECTORS)
			throw new IOException();
		int tmp = logHead + t.size();
		if (logHead < logTail && tmp >= logTail)
			throw new IOException();
		if (logHead > logTail && tmp >= ADisk.REDO_LOG_SECTORS && tmp % ADisk.REDO_LOG_SECTORS >= logTail)
			throw new IOException();
				
		for (byte[] b : t.getSectors()) {
			this.aTrans(logHead, b, Disk.WRITE);
			logHead = logHead + 1 % Disk.ADISK_REDO_LOG_SECTORS;
		}
		//TODO: Start process to write log to disk.
		this.commitInProgress = false;
		this.commit.signal();
		this.writeWait.signal();
		lock.unlock();
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
	public void abortTransaction(TransID tid) throws IllegalArgumentException {
		lock.lock();
		try {
			if (this.transactions.put(tid, null) != null)
				throw new IllegalArgumentException();
		}catch (IllegalArgumentException e) {
			throw e;
		}finally {
			lock.unlock();
		}
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
		lock.lock();
		try {
			if (buffer.length < Disk.SECTOR_SIZE)
				throw new IllegalArgumentException();
			if (sectorNum < ADisk.REDO_LOG_SECTORS || sectorNum >= Disk.NUM_OF_SECTORS)
				throw new IndexOutOfBoundsException();
			if (!this.isActive(tid))
				throw new IllegalArgumentException();
			Transaction t = this.transactions.get(tid);
			boolean found = false;
			for (Write w : t)
				if (w.sectorNum == sectorNum) {
					ADisk.fill(buffer, w.buffer);
					found = true;
				}
			if(found)
				return;
			//TODO: Check committed but not written writes.
			this.aTrans(sectorNum, buffer, Disk.READ);
			return;
		} catch (IllegalArgumentException e) {
			throw e;
		}catch (IndexOutOfBoundsException e){
			throw e;
		}finally {
			lock.unlock();
		}
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
		lock.lock();
		this.lastCompletedAction = r.getTag();
		this.diskDone.signalAll();
		lock.unlock();
	}
	
	public static byte[] blankSector() {
		return new byte[Disk.SECTOR_SIZE];
	}
	
	private static int actiontag = Integer.MIN_VALUE;
	private static int actionTag() {
		if (actiontag == -1) // prevent 0 from being a tag.
			actiontag++;
		if (actiontag == Integer.MAX_VALUE-1) //reserve max int.
			actiontag++;
		return actiontag++;  //Assuming that we won't cycle through all ints and still have active requests
	}
	
	public boolean isActive(TransID tid) {
		if (this.transactions.get(tid) == null)
			return false;
		return true;
	}
	
	
	public void aTrans(int sectorNum, byte[] buffer, int type) throws IllegalArgumentException, IOException {
		lock.lock();
		assert (type == Disk.READ || type == Disk.WRITE);
		int tag = ADisk.actionTag();
		this.disk.startRequest(type, tag, sectorNum, buffer);
		while(this.lastCompletedAction != tag)
			this.diskDone.awaitUninterruptibly();
		lock.unlock();
		return;
	}
	

	public static void fill(byte[] buff1, byte[] buff2) {
		assert (buff2.length <= buff1.length);
		for (int i = 0; i < buff2.length; i++)
			buff1[i] = buff2[i];
		return;
	}
	
	private void writeBack(){
		lock.lock();
		while(true) {
			while (logHead == logTail)
				this.writeWait.awaitUninterruptibly();
		}
	}
}
