/*
 * PTree -- persistent tree
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */

import java.io.IOException;
import java.io.EOFException;

public class PTree{
	public static final int METADATA_SIZE = 64;
	public static final int MAX_TREES = 512;
	public static final int MAX_BLOCK_ID = Integer.MAX_VALUE; 

	//
	// Arguments to getParam
	//
	public static final int ASK_FREE_SPACE = 997;
	public static final int ASK_MAX_TREES = 13425;
	public static final int ASK_FREE_TREES = 23421;

	//
	// TNode structure
	//
	public static final int TNODE_POINTERS = 8;
	public static final int BLOCK_SIZE_BYTES = 1024;
	public static final int POINTERS_PER_INTERNAL_NODE = 256;
	
	private static final int ROOTS_SECTORS = 0; // TODO: set to TNode.TNODE_SIZE * MAX_TREES round up to nearest sector.


	private TNode[] roots;
	private ADisk adisk;


	private SimpleLock lock;
	
	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
		this.roots = new TNode[PTree.MAX_TREES];
		this.readRoots();
		this.lock = new SimpleLock();
	}


	public TransID beginTrans()
	{
		return this.adisk.beginTransaction();
	}

	public void commitTrans(TransID xid) 
	throws IOException, IllegalArgumentException
	{
		this.adisk.commitTransaction(xid);
	}

	public void abortTrans(TransID xid) 
	throws IOException, IllegalArgumentException
	{
		this.adisk.abortTransaction(xid);
	}

	/**helper method
	**/
	//does this need to mutex??
	public int findNextTNum(){
			for(int i=0; i<PTree.MAX_TREES; i++){
				if(this.roots[i]==null)
					return i;
			}
			return -1;//unable to find a TNum
	}

	public int createTree(TransID xid) 
	throws IOException, IllegalArgumentException, ResourceException
	{
		int newTNum = -1;
		try{
		lock.lock();		
		//get next available TNum
		newTNum = findNextTNum();
		
		//create tree
		this.roots[newTNum] = new TNode(newTNum);
		
//		public void writeSector(TransID tid, int sectorNum, byte buffer[])
		//allocate data structures into byte[]??
		byte[] buffer = null;
		
		//write to disk??
		this.adisk.writeSector(xid,2,buffer);
		}
		finally{
			lock.unlock();
		}
		return newTNum;		
	}

	public void deleteTree(TransID xid, int tnum) 
	throws IOException, IllegalArgumentException
	{
		//TODO: Delete tree from disk.
		this.updateRoots(tnum, null);
	}


	public void getMaxDataBlockId(TransID xid, int tnum)
	throws IOException, IllegalArgumentException
	{
	}

	public void readData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}


	public void writeData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}

	public void readTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}


	public void writeTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}

	public int getParam(int param)
	throws IOException, IllegalArgumentException
	{
		return -1;
	}
	
	private void readRoots() {
		// TODO Auto-generated method stub
	}

	private void updateRoots(int tnum, Object object) {
		;// TODO Auto-generated method stub
	}



}
