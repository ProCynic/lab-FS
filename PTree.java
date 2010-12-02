/*
 * PTree -- persistent tree
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

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
	
	private static final int ROOTS_SECTOR = 0; // TODO: set to adisk max sectors - TNode.TNODE_SIZE * MAX_TREES round up to nearest sector.


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
		this.writeRoots(tnum, null);
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
	
	private byte[] readSectors(int start, int finish) {
		return null;  //TODO: Implement
	}
	
	private void readRoots() {
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(readSectors(ROOTS_SECTOR, this.adisk.getNSectors())));
			for(TNode t : this.roots)  //TODO: Can you write to t and have roots reflect it?
				t = (TNode)ois.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void writeRoots(int tnum, TNode obj) {
		this.roots[tnum] = obj;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			for(TNode t : this.roots)
				oos.writeObject(t);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[][] sectors = sectorize(out.toByteArray());
		TransID tid = this.adisk.beginTransaction();
		int sectornum = ROOTS_SECTOR;
		try {
			for (byte[] sector : sectors) {
				this.adisk.writeSector(tid, sectornum, sector);
				sectornum++;
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	private byte[][] sectorize(byte[] byteArray) {
		// TODO Auto-generated method stub
		return null;
	}



}
