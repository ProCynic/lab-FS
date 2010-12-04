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

	public static final int TNODES_PER_SECTOR = Disk.SECTOR_SIZE / TNode.TNODE_SIZE;
	public static final int ROOTS_LOCATION = ADisk.size() - (MAX_TREES / TNODES_PER_SECTOR + (MAX_TREES % TNODES_PER_SECTOR != 0 ? 1 : 0)) - 1; // TODO: verify	
	public static final int FREE_LIST_LOCATION = ROOTS_LOCATION - 4;  //TODO: make dynamic
	public static final int AVAILABLE_SECTORS = FREE_LIST_LOCATION;




	private ADisk adisk;
	private SimpleLock lock;



	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
//		TransID tid = this.adisk.beginTransaction();
//		try {
//			if(doFormat) {
//				BitMap freelist = new BitMap(AVAILABLE_SECTORS);
//				writeFreeList(tid, freelist);
//				for(int i = 0; i < MAX_TREES; i++) {
//					writeRoot(tid, i, null);  //Fill out root array with null tnodes.
//					if(i%20 == 0) {
//						adisk.commitTransaction(tid); //Keep from hitting max writes per transaction
//						tid = this.adisk.beginTransaction();
//					}
//				}
//			}
//			adisk.commitTransaction(tid);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.exit(-1);
//		}		
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


	public int getMaxDataBlockId(TransID xid, int tnum)
	throws IOException, IllegalArgumentException
	{
		//TODO: Depth first search from right, return first non null leaf.
		return 0;
	}

	public void readData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		TNode root = readRoot(xid, tnum);
		readVisit(xid, root, blockId, buffer);
	}


	public void writeData(TransID xid, int tnum, int blockId, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		TNode root = readRoot(xid, tnum);
		while (TNODE_POINTERS * Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight) >= blockId) {
			root.treeHeight++;
			InternalNode n = new InternalNode(getSectors(xid, BLOCK_SIZE_SECTORS), root);
			root.clear();
			root.pointers[0] = n.location;
			writeNode(xid, n);
		}
		writeRoot(xid, root.TNum, root);
		writeVisit(xid, root, blockId, buffer);

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
	public int[] findRoot(int tnum) throws IndexOutOfBoundsException{
		if (tnum >= MAX_TREES)
			throw new IndexOutOfBoundsException();
		int[] position = new int[2];
		position[0] = ROOTS_LOCATION + tnum / TNODES_PER_SECTOR;  //TODO: verify this works.
		position[1] = tnum % TNODES_PER_SECTOR;
		if (position[0] >= this.adisk.getNSectors())
			throw new IndexOutOfBoundsException();
		return position;
	}

	//private
	public TNode readRoot(TransID tid, int tnum) throws IOException, IndexOutOfBoundsException{
		int[] position = findRoot(tnum);  //TODO: Fix to return offset as well
		int sectornum = position[0];
		int offset = position[1] * TNode.TNODE_SIZE;

//		ByteBuffer buff = ByteBuffer.allocate(Disk.SECTOR_SIZE);
		byte[] buff = readSectors(tid,sectornum, sectornum);

		byte[] nodebytes = new byte[TNode.TNODE_SIZE];
//		buff.get(nodebytes, offset, TNode.TNODE_SIZE);
		for(int i = 0; i < TNode.TNODE_SIZE; i++)
			nodebytes[i] = buff[i+offset];
		if (Arrays.equals(nodebytes, new byte[TNode.TNODE_SIZE]))
			return null;
		return new TNode(nodebytes);

//		ByteArrayInputStream in = new ByteArrayInputStream(nodebytes);
//		ObjectInputStream ois = new ObjectInputStream(in);
//
//		TNode ret = null;
//		try {
//			ret = (TNode) ois.readObject();
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			System.exit(-1);
//		}
//		return ret;
	}

	//private
	public void writeRoot(TransID tid, int tnum, TNode node) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		
		int[] position = findRoot(tnum);
		
		
		int sectornum = position[0];
		int offset = position[1] * TNode.TNODE_SIZE;
		

//		ByteBuffer buff = ByteBuffer.allocate(Disk.SECTOR_SIZE);
//		byte[] t = buff.array();   //TODO: Debug 
//		buff.put(readSectors(tid,sectornum, sectornum));
//		t = buff.array();   //TODO: Debug 
		
		byte[] buff = readSectors(tid, sectornum, sectornum);
		

//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		ObjectOutputStream oos = new ObjectOutputStream(out);
//		oos.writeObject(node);
//		t = out.toByteArray();   //TODO: Debug 
		
//		int off = offset * TNode.TNODE_SIZE;  //TODO: Debug 
//		int len = out.toByteArray().length;  //TODO: Debug

//		buff.put(out.toByteArray(), offset * TNode.TNODE_SIZE, out.toByteArray().length);
//		writeSectors(tid, sectornum, buff.array());
		
		byte[] nodeBytes = node.getBytes();
		assert nodeBytes.length == TNode.TNODE_SIZE;
		assert offset + nodeBytes.length <= Disk.SECTOR_SIZE;
		for(int i = 0; i < nodeBytes.length; i++)
			buff[offset + i] = nodeBytes[i];
		
		writeSectors(tid, sectornum, buff);

	}

//	private enum VisitAction {
//		write,
//		read;
//	}
//	
//		//private
//	public void visit(TransID tid, int height, Node node, int blockID, byte[] buffer, VisitAction action) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
////		visitor.visit(tid, InternalNode.class, node.location, buffer);
//		
//		int numpointers = node.getClass() == TNode.class ? TNODE_POINTERS : POINTERS_PER_INTERNAL_NODE;
//
//		if(height == 1) {
//			if (action == VisitAction.write)
//				writeBlock(tid, node.pointers[blockID], buffer);
//			else if (action == VisitAction.read) {
//				if (blockID >= numpointers)
//					throw new IllegalArgumentException();
//				if (node.pointers[blockID] == Node.NULL_PTR)
//					Arrays.fill(buffer, (byte)0);
//				else
//					fill(readBlock(tid, node.pointers[blockID]), buffer);
//			}
//		}
//		else {
//			int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height-1);
//			int ptr = blockID / leavesBelow;
//			int next = node.pointers[ptr];
//			InternalNode n;
//			if (next == Node.NULL_PTR) {
//				if (action == VisitAction.read) {
//					Arrays.fill(buffer, (byte)0);
//					return;
//				}
//				int sector = getSectors(tid, BLOCK_SIZE_SECTORS);
//				n = new InternalNode(sector);
//				writeNode(tid, n);
//				node.pointers[ptr] = sector;
//				if (node.getClass() == TNode.class) {
//					TNode t = (TNode) node;
//					writeRoot(tid, t.TNum, t);
//				}else
//					writeNode(tid, (InternalNode)node);
//					
//			} else
//				n = readNode(tid, next);
//			visit(tid, height-1, n, blockID%leavesBelow, buffer, action);
//		}
//	}
	
	//private
	public void readVisit(TransID tid, TNode root, int blockID, byte[] buffer) throws IllegalArgumentException, IOException {
		if (blockID > getMaxDataBlockId(tid, root.TNum)) {
			fill(new byte[BLOCK_SIZE_BYTES], buffer);
			return;
		}
		
		if (root.treeHeight == 1) {
			fill(readBlock(tid, root.pointers[blockID]), buffer);
			return;
		}
		
		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight-2) * TNODE_POINTERS;
		int index = blockID / leavesBelow;
		
		if(root.pointers[index] == Node.NULL_PTR) {
			fill(readBlock(tid, root.pointers[blockID]), buffer);
			return;
		}
		
		readVisit(tid, root.treeHeight-1, getChild(tid, root, index), blockID%leavesBelow, buffer);
	}
	
	//private
	public void readVisit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		assert (height >= 1);
		
		if (height == 1) {
			if (blockID >= POINTERS_PER_INTERNAL_NODE)
				fill(new byte[BLOCK_SIZE_BYTES], buffer);
			else if (node.pointers[blockID] == Node.NULL_PTR)
				fill(new byte[BLOCK_SIZE_BYTES], buffer);
			else
				fill(readBlock(tid, node.pointers[blockID]), buffer);
			return;
		}
		
		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height-1);
		int index = blockID / leavesBelow;
		
		if(node.pointers[index] == Node.NULL_PTR) {
			fill(new byte[BLOCK_SIZE_BYTES], buffer);
			return;
		}
		readVisit(tid, height-1, getChild(tid, node, index), blockID%leavesBelow, buffer);		
	}
	
	//private
	public void writeVisit(TransID tid, TNode root, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		assert(root.treeHeight >= 1);
		if (root.treeHeight == 1) {
			writeBlock(tid, root.pointers[blockID], buffer);
			return;
		}
		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight-2) * TNODE_POINTERS;
		int index = blockID / leavesBelow;
		writeVisit(tid, root.treeHeight-1, getChild(tid, root, index), blockID%leavesBelow, buffer);
	}
	
	//private
	public void writeVisit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		assert (height >= 1);
		assert (height >=1);
		if(height == 1) {
			assert (blockID < POINTERS_PER_INTERNAL_NODE);
			writeBlock(tid, node.pointers[blockID], buffer);
			return;
		}
		
		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height-1);
		int index = blockID / leavesBelow;
		writeVisit(tid, height-1, getChild(tid, node, index), blockID%leavesBelow, buffer);
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
	
	public InternalNode getChild(TransID tid, Node parent, int index) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		if (parent.pointers[index] != Node.NULL_PTR)
			return readNode(tid, parent.pointers[index]);
		int sector = getSectors(tid, BLOCK_SIZE_SECTORS);
		InternalNode node = new InternalNode(sector);
		parent.pointers[index] = sector;
		writeNode(tid, node);
		if(parent.getClass() == TNode.class) {
			TNode t = (TNode) parent;
			writeRoot(tid, t.TNum, t);
		}else
			writeNode(tid, (InternalNode)parent);
		return node;
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
		if (start > finish)
			throw new IllegalArgumentException();
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		ByteBuffer buffer = ByteBuffer.allocate((finish + 1 -start) * Disk.SECTOR_SIZE);
		for(int i = start; i <= finish; i++) {
			this.adisk.readSector(tid, i, sector);
			buffer.put(sector);
		}
		return buffer.array();
	}

	//private
	public void writeSectors(TransID tid, int start, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException{
		ByteBuffer buff = ByteBuffer.allocate(numSectors(buffer)*Disk.SECTOR_SIZE);
		buff.put(buffer);
		buff.rewind();
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
