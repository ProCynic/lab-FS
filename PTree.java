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

	private static final int ROOTS_LOCATION = ADisk.size() - numSectors(TNode.TNODE_SIZE * MAX_TREES); // TODO: verify	
	private static final int FREE_LIST_LOCATION = 0; //TODO: location of first sector of free list.
	private static final int AVAILABLE_SECTORS = 0;  //TODO: ADisk.size() - datastructures

	private ADisk adisk;
	private HashSet<Integer> tnums;
	private BitMap freelist;  // 1 is free, 0 is not free.



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
			if(doFormat) {
				this.freelist = new BitMap(AVAILABLE_SECTORS);
				this.freelist.set(0, this.freelist.size());  //TODO: verify.  maybe size - 1?
				writeFreeList(tid);  //Update freelist on disk
			}
			else
				readFreeList(tid);
			adisk.commitTransaction(tid);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
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

	public int getTNum() throws ResourceException{
		if (tnums.isEmpty())
			throw new ResourceException();
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
			writeRoot(xid, new TNode(newTNum));
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
		this.writeRoot(xid, null);  //TODO: Fix this.  writeRootSector uses TNode.getBytes.
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
		TNode node = readRoot(xid, tnum);
		fill(buffer,node.metadata);
		writeRoot(xid, node);
	}

	public int getParam(int param)
	throws IOException, IllegalArgumentException
	{
		if(param == ASK_MAX_TREES)
			return MAX_TREES;
		else if (param == ASK_FREE_TREES)
			return tnums.size();
		else if (param == ASK_FREE_SPACE)
			return freelist.cardinality();
		else
			throw new IllegalArgumentException();

	}

	//private
	public TNode readRoot(TransID tid, int tnum) throws IOException {
		int sectornum = findRoot(tnum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		for (TNode n : nodes)
			if(n.TNum == tnum)
				return n;
		return null;
	}

	//private
	public void writeRoot(TransID tid, TNode obj) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		int sectornum = findRoot(obj.TNum);
		ArrayList<TNode> nodes = readRootSector(tid, sectornum);
		for (int n = 0; n < nodes.size(); n++)
			if(nodes.get(n).TNum == obj.TNum)
				nodes.set(n,obj);
		writeRootSector(tid, sectornum, nodes);
	}

	//private
	public ArrayList<TNode> readRootSector(TransID tid, int sectornum) throws IOException {
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

	//private
	public void writeRootSector(TransID tid, int sectornum, ArrayList<TNode> nodes) {
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
			System.exit(-1);
		}
	}


	//private
	public int findRoot(int tnum){
		return ROOTS_LOCATION + (TNode.TNODE_SIZE * tnum / Disk.SECTOR_SIZE);  //TODO: verify this works.
	}

	//TODO: Test ASAP
	//private
	public void visit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
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
			next = readNode(tid, node.pointers[ptr]);
			// drop a level and repeat.
			visit(tid, height-1, next, blockID/node.pointers.length, buffer);  //Do I need to divide blockID?
	}


	//private
	//essentially c malloc, but with sectors, not bytes.
	public int getSectors(int blockSizeSectors) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	//private
	public void freeSectors(int start, int finish) {
		assert start <= finish;
		for(int i = start; i <= finish; i++) {
			freelist.set(i);
		}
	}
	
	//private
	public void readFreeList(TransID tid) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		this.freelist.fromBytes(readSectors(tid, FREE_LIST_LOCATION, this.adisk.getNSectors()));
	}
	
	//private
	public void writeFreeList(TransID tid)  throws IllegalArgumentException, IndexOutOfBoundsException{
		writeSectors(tid, FREE_LIST_LOCATION, this.freelist.getBytes());
	}
	
	//private
	public void writeNode(TransID tid, InternalNode node)  throws IllegalArgumentException, IndexOutOfBoundsException{
		writeBlock(tid, node.location, node.getBytes());
	}

	//private
	public InternalNode readNode(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		return new InternalNode(sectornum, readBlock(tid, sectornum));
	}


	//private
	public byte[] readBlock(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
		return readSectors(tid, sectornum, sectornum + BLOCK_SIZE_SECTORS);
	}

	//private
	public void writeBlock(TransID tid, int sectornum, byte[] buffer)  throws IllegalArgumentException, IndexOutOfBoundsException{
		assert (buffer.length == BLOCK_SIZE_BYTES);
		writeSectors(tid, sectornum, buffer);
	}

	//private
	public byte[] readSectors(TransID tid, int start, int finish) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
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

	//private
	public void writeSectors(TransID tid, int start, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException{
		ByteBuffer buff = ByteBuffer.allocate(numSectors(buffer));
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		int i = 0;
		while (buff.hasRemaining()) {
			buff.get(sector);
			this.adisk.writeSector(tid, start+i, sector);
			i++;
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