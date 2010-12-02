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


//	private TNode[] roots;
	private ADisk adisk;



	private SimpleLock lock;

	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
//		this.roots = new TNode[PTree.MAX_TREES];
		TransID tid = this.adisk.beginTransaction();
//		this.readRoots(tid);
		try {
			this.adisk.commitTransaction(tid);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.lock = new SimpleLock();
	}


	public TransID beginTrans()
	{
		return this.adisk.beginTransaction();
	}

	public void commitTrans(TransID xid) throws IOException, IllegalArgumentException {
		this.lock.lock();
		try {
			this.adisk.commitTransaction(xid);
		}finally {
			this.lock.unlock();
		}
		
	}

	public void abortTrans(TransID xid) 
	throws IOException, IllegalArgumentException
	{
		this.lock.lock();
		try {
			this.adisk.abortTransaction(xid);
		}finally {
			this.lock.unlock();
		}
		
	}
	
	public int findNextTNum(){
//			for(int i=0; i<PTree.MAX_TREES; i++){
//				if(this.roots[i]==null)
//					return i;
//			}
		//TODO: Implement
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
		writeRoot(xid, newTNum, new TNode(newTNum));
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
		this.writeRoot(xid, tnum, null);
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
		buffer = readRoot(xid, tnum).metadata;
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
	
//	private void readRoots(TransID tid) {
//		try {
//			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(readSectors(ROOTS_SECTOR, this.adisk.getNSectors())));
//			for(TNode t : this.roots)  //TODO: Can you write to t and have roots reflect it?
//				t = (TNode)ois.readObject();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}

//	private void writeRoots(TransID tid, int tnum, TNode obj) {
//		this.roots[tnum] = obj;
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		try {
//			ObjectOutputStream oos = new ObjectOutputStream(out);
//			for(TNode t : this.roots)
//				oos.writeObject(t);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		byte[][] sectors = sectorize(out.toByteArray());
//		int sectornum = ROOTS_SECTOR;
//		try {
//			for (byte[] sector : sectors) {
//				this.adisk.writeSector(tid, sectornum, sector);
//				sectornum++;
//			}
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IndexOutOfBoundsException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	private TNode readRoot(TransID tid, int tnum) throws IOException {
		int sectornum = findSector(tnum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		//TODO: figure out which node is the one you want.
		return null; //TODO: return nodes[something];
	}
	
	private void writeRoot(TransID tid, int tnum, TNode obj) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		int sectornum = findSector(tnum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		//TODO: update nodes with the new obj
		writeRootSector(tid, sectornum, nodes);
	}
	
	private ArrayList<TNode> readRootSector(TransID tid, int sectornum) throws IOException {
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		this.adisk.readSector(tid, sectornum, sector);
		ByteArrayInputStream in = new ByteArrayInputStream(sector);
		ObjectInputStream ois = new ObjectInputStream(in);
		ArrayList<TNode> nodes = new ArrayList<TNode>();
		try {
			while (ois.available() > 0) {
				 nodes.add((TNode)ois.readObject());
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return nodes;
	}
	
	private void writeRootSector(TransID tid, int sectornum, ArrayList<TNode> nodes) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			for(TNode t : nodes) 
				oos.writeObject(t);
			this.adisk.writeSector(tid, sectornum, out.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private int findSector(int TNum){
		
		return 0; //TODO: Implement
	}

}
