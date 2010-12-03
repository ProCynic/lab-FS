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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

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
	public static final int BLOCK_SIZE_SECTORS = numSectors(BLOCK_SIZE_BYTES);

	private static final int ROOTS_SECTOR = 0; // TODO: first sector of the tree root list.
	private static final int AVAILABLE_SECTORS = 0;  //TODO: highest sector available for data.

	private ADisk adisk;
	private HashSet<Integer> tnums;



	private SimpleLock lock;



	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
		this.tnums = new HashSet<Integer>(MAX_TREES);
		TransID tid = this.adisk.beginTransaction();
		try {
			for(int i = 0; i < MAX_TREES; i++) 
				if(readRoot(tid, i) != null)
					tnums.add(i);
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

	public int getTNum() {
		Iterator<Integer> iter = tnums.iterator();
		Integer newTNum = iter.next();
		iter.remove();
		return newTNum;

	}

	public int createTree(TransID xid) 
	throws IOException, IllegalArgumentException, ResourceException
	{
		int newTNum = -1;
		try{
			lock.lock();		

			//get next available TNum
			newTNum = getTNum();

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
		//TODO: deallocate tree sectors
		this.writeRoot(xid, tnum, null);  //TODO: Fix this.  writeRootSector uses TNode.getBytes.
		this.tnums.add(tnum);
	}


	public void getMaxDataBlockId(TransID xid, int tnum)
	throws IOException, IllegalArgumentException
	{
		//TODO: Depth first search from right, return first non null leaf.
	}

	public void readData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
	}


	public void writeData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		TNode root = readRoot(xid, tnum);
		while (TNODE_POINTERS * Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight) >= blockId) {
			root.treeHeight++;
		}
		//TODO: Move leaves down when you increase the height.  Reroot tree, I think.



	}

	public void readTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert (buffer.length <= METADATA_SIZE);
		byte[] metadata = readRoot(xid, tnum).metadata;
		fill(metadata, buffer);

	}


	public void writeTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert buffer.length <= METADATA_SIZE;
		fill(buffer,readRoot(xid, tnum).metadata);
	}

	public int getParam(int param)
	throws IOException, IllegalArgumentException
	{
		if(param == ASK_MAX_TREES)
			return MAX_TREES;
		else if (param == ASK_FREE_TREES)
			return tnums.size();
		else if (param == ASK_FREE_SPACE)
			return freeSpace();
		else
			throw new IllegalArgumentException();

	}

	private TNode readRoot(TransID tid, int tnum) throws IOException {
		int sectornum = findSector(tnum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		for (TNode n : nodes)
			if(n.TNum == tnum)
				return n;
		return null;
	}

	private void writeRoot(TransID tid, int tnum, TNode obj) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		int sectornum = findSector(tnum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		for (int n = 0; n < nodes.size(); n++)
			if(nodes.get(n).TNum == tnum)
				nodes.set(n,obj);
		writeRootSector(tid, sectornum, nodes);
	}

	private ArrayList<TNode> readRootSector(TransID tid, int sectornum) throws IOException {
		byte[] sector = readSectors(tid,sectornum, sectornum);
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
				oos.write(t.getBytes());
			assert out.toByteArray().length <= Disk.SECTOR_SIZE;
			writeSectors(tid, sectornum, out.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	private int findSector(int TNum){

		return 0; //TODO: Implement
	}

	//TODO: Test ASAP
	private void trans(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) {
		assert (buffer.length == BLOCK_SIZE_BYTES);

		if (height == 0) { //Write the leaf and return.
			int sectornum = getSectors(BLOCK_SIZE_SECTORS);
			node.pointers[blockID] = sectornum;
			writeBlock(tid, node.location, node.getBytes());
			return;
		}

		// Figure out which child is next on the path to the leaf.
		int maxbelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height);
		int ptr = blockID % maxbelow;
		InternalNode next = null;

		if (node.pointers[ptr] == Integer.MIN_VALUE) {  //If the internal node doesn't exist, create it.
			int sectornum = getSectors(BLOCK_SIZE_SECTORS);  
			node.pointers[ptr] = sectornum;
			writeNode(tid, node); //Update node on disk.
			next = new InternalNode(sectornum);
			writeNode(tid, next);  //Write the new node to disk.
		} else //Otherwise read it from disk.
			try {
				next = readNode(tid, node.pointers[ptr]);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			} catch (IndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}

			// drop a level and repeat.
			trans(tid, height-1, next, blockID/node.pointers.length, buffer);  //Do I need to divide blockID?
	}


	//essentially c malloc, but with sectors, not bytes.
	private int getSectors(int blockSizeSectors) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private void freeSectors(int start, int finish) {
		
	}
	
	private void writeNode(TransID tid, InternalNode node) {
		writeBlock(tid, node.location, node.getBytes());
	}
	
	private InternalNode readNode(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		return new InternalNode(sectornum, readBlock(tid, sectornum));
	}


	private byte[] readBlock(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
		return readSectors(tid, sectornum, sectornum + BLOCK_SIZE_SECTORS);
	}

	private void writeBlock(TransID tid, int sectornum, byte[] buffer) {
		assert (buffer.length == BLOCK_SIZE_BYTES);
		writeSectors(tid, sectornum, buffer);
	}

	private byte[] readSectors(TransID tid, int start, int finish) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
		assert finish >= start;
		assert start >= 0;
		assert finish < AVAILABLE_SECTORS;
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		ByteBuffer buffer = ByteBuffer.allocate((finish-start) * Disk.SECTOR_SIZE);
		for(int i = start; i < finish; i++) {
			this.adisk.readSector(tid, i, sector);
			buffer.put(sector);
		}
		return buffer.array();
	}

	private void writeSectors(TransID tid, int start, byte[] buffer) {
		ByteBuffer buff = ByteBuffer.allocate(numSectors(buffer));
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		int i = 0;
		try {
			while (buff.hasRemaining()) {
				buff.get(sector);
				this.adisk.writeSector(tid, start+i, sector);
				i++;
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int numSectors(byte[] buffer) {
		return numSectors(buffer.length);
	}

	public static int numSectors(int bytes) {
		return (bytes / Disk.SECTOR_SIZE) + (bytes % Disk.SECTOR_SIZE != 0 ? 1 : 0);
	}

	public static byte[] fill(byte[] src, byte[] dest) {
		assert (dest.length >= src.length);
		int i;
		for(i = 0; i < src.length; i++)
			dest[i] = src[i];
		for(; i < dest.length; i++)
			dest[i] = (byte) 0;
		return dest;
	}
}