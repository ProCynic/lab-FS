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
	
	private static final int TNODES_PER_SECTOR = Disk.SECTOR_SIZE / TNode.TNODE_SIZE;
	private static final int ROOTS_LOCATION = ADisk.size() - MAX_TREES / TNODES_PER_SECTOR + (MAX_TREES % TNODES_PER_SECTOR != 0 ? 1 : 0); // TODO: verify	
	private static final int FREE_LIST_LOCATION = 0; //TODO: ROOTS_LOCATION - 1 bit for every sector
	private static final int AVAILABLE_SECTORS = 0;  //TODO: ADisk.size() - datastructures

	
	

	private ADisk adisk;
	private SimpleLock lock;



	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
		
		TransID tid = this.adisk.beginTransaction();
		try {
			if(doFormat) {
				BitMap freelist = new BitMap(AVAILABLE_SECTORS);
				freelist.set(0, freelist.size());  //TODO: verify.  maybe size - 1?
				writeFreeList(tid, freelist);  //Update freelist on disk
				for(int i = 0; i < MAX_TREES; i++) {
					writeRoot(tid, i, null);  //Fill out root array with null tnodes.
				}
			}
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

	public int getTNum(TransID tid) throws ResourceException, IOException{
		for(int i = 0; i < MAX_TREES; i++)
			if (readRoot(tid, i) == null)
				return i;
		throw new ResourceException();
	}
	
	public int createTree(TransID xid) 
	throws IOException, IllegalArgumentException, ResourceException
	{
		int newTNum = -1;
		try{
			lock.lock();		

			//get next available TNum
			newTNum = getTNum(xid);

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
//		visit(xid, )
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
			InternalNode n = new InternalNode(getSectors(xid, BLOCK_SIZE_SECTORS), root);
		}
		//TODO: Move leaves down when you increase the height.  Reroot tree, I think.



	}

	public void readTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert (buffer.length <= METADATA_SIZE);
		TNode t = readRoot(xid, tnum);
		if (t == null) 
			throw new IllegalArgumentException();
		byte[] metadata = t.metadata;
		fill(metadata, buffer);

	}


	public void writeTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert buffer.length <= METADATA_SIZE;
		TNode node = readRoot(xid, tnum);
		if (node == null)
			throw new IllegalArgumentException();
		fill(buffer,node.metadata);
		writeRoot(xid, tnum, node);
	}

	//Do we need a tid?
	public int getParam(int param)
	throws IOException, IllegalArgumentException
	{
		TransID tid = this.adisk.beginTransaction();
		if(param == ASK_MAX_TREES)
			return MAX_TREES;
		else if (param == ASK_FREE_TREES)
			return numFreeTrees(tid);
		else if (param == ASK_FREE_SPACE)
			return readFreeList(tid).cardinality();
		else
			throw new IllegalArgumentException();

	}

	//private
	public int numFreeTrees(TransID tid) throws IOException {
		int s = 0;
		for(int tnum = 0; tnum < MAX_TREES; tnum++)
			if(readRoot(tid, tnum) == null)
				s++;
		return s;
	}

	//private
	public TNode readRoot(TransID tid, int tnum) throws IOException {
		int[] position = findRoot(tnum);  //TODO: Fix to return offset as well
		int sectornum = position[0];
		int offset = position[1];
		
		ByteBuffer buff = ByteBuffer.allocate(Disk.SECTOR_SIZE);
		buff.put(readSectors(tid,sectornum, sectornum));
		
		byte[] nodebytes = new byte[TNode.TNODE_SIZE];
		buff.get(nodebytes, offset * TNode.TNODE_SIZE, TNode.TNODE_SIZE);
		
		ByteArrayInputStream in = new ByteArrayInputStream(nodebytes);
		ObjectInputStream ois = new ObjectInputStream(in);
		
		TNode ret = null;
		try {
			ret =  (TNode) ois.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		return ret;
	}
	
	//private
	public void writeRoot(TransID tid, int tnum, TNode node) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		int[] position = findRoot(node.TNum);  //TODO: Fix to return offset as well
		int sectornum = position[0];
		int offset = position[1];
		
		ByteBuffer buff = ByteBuffer.allocate(Disk.SECTOR_SIZE);
		buff.put(readSectors(tid,sectornum, sectornum));
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(node);
		
		buff.put(out.toByteArray(), offset * TNode.TNODE_SIZE, TNode.TNODE_SIZE);
		
		writeSectors(tid, sectornum, buff.array());
		
	}

	//private
	public int[] findRoot(int tnum){
		int[] position = new int[2];
		position[0] = ROOTS_LOCATION + tnum / TNODES_PER_SECTOR;  //TODO: verify this works.
		position[1] = tnum % TNODES_PER_SECTOR;
		return position;
	}

	//TODO: Test ASAP
	//private
	public void visit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer, Visitor visitor) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		assert (buffer.length == BLOCK_SIZE_BYTES);

		if (height == 0) { //Write the leaf and return.
			int sectornum = getSectors(tid, BLOCK_SIZE_SECTORS);
			node.pointers[blockID] = sectornum;
			writeBlock(tid, node.location, node.getBytes());
			return;
		}

		// Figure out which child is next on the path to the leaf.
		int maxbelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height);
		int ptr = blockID % maxbelow;
		InternalNode next = null;

		if (node.pointers[ptr] == Integer.MIN_VALUE) {  //If the internal node doesn't exist, create it.
			int sectornum = getSectors(tid, BLOCK_SIZE_SECTORS);  
			node.pointers[ptr] = sectornum;
			writeNode(tid, node); //Update node on disk.
			next = new InternalNode(sectornum);
			writeNode(tid, next);  //Write the new node to disk.
		} else //Otherwise read it from disk.
			next = readNode(tid, node.pointers[ptr]);
			// drop a level and repeat.
			visit(tid, height-1, next, blockID/node.pointers.length, buffer, visitor);  //Do I need to divide blockID?
	}


	//private
	//essentially c malloc, but with sectors, not bytes.
	public int getSectors(TransID tid, int blockSizeSectors) throws IllegalArgumentException, IndexOutOfBoundsException, IOException, ResourceException{
		BitMap freelist = readFreeList(tid);
		try {
			for(int i = 0; i < freelist.length();i=freelist.nextClearBit(i))   {//Could be more efficient, but I don't think it's a problem.
				if(freelist.nextClearBit(i) > i+blockSizeSectors)
					return i;
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ResourceException();
		}
		System.exit(-1);
		return 0;
		
	}
	
	//private
	public void freeSector(TransID tid, int sector) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		BitMap freelist = readFreeList(tid);
		freelist.set(sector);
		writeFreeList(tid, freelist);
	}
	
	//private
	public void freeSectors(TransID tid, int start, int finish) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		assert start <= finish;
		for(int i = start; i <= finish; i++) {
			freeSector(tid, i);
		}
	}
	
	//private
	public BitMap readFreeList(TransID tid) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		return new BitMap(readSectors(tid, FREE_LIST_LOCATION, this.adisk.getNSectors()));
	}
	
	//private
	public void writeFreeList(TransID tid, BitMap freelist)  throws IllegalArgumentException, IndexOutOfBoundsException{
		writeSectors(tid, FREE_LIST_LOCATION, freelist.getBytes());
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
	
//	class DeleteVisitor extends Visitor {
//		public DeleteVisitor(TransID tid){
//			super(tid);
//		}
//
//		@SuppressWarnings("unchecked")
//		@Override
//		public void visit(Class type, int location) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
//			if(type == TNode.class) {
//				writeRoot(this.tid, location, null); // In this case, location is tnum.  Otherwise sector number.
//			} else
//				freeSectors(this.tid, location, location + BLOCK_SIZE_SECTORS);
//		}
//	}
//	
//	class WriteVisitor extends Visitor {
//
//		public WriteVisitor(TransID tid) {
//			super(tid);
//			// TODO Auto-generated constructor stub
//		}
//
//		@Override
//		public void visit(Class type, int location)
//				throws IllegalArgumentException, IndexOutOfBoundsException,
//				IOException {
//			// TODO Auto-generated method stub
//			
//		}
}