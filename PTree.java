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
import java.nio.ByteBuffer;
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
	public static final int BLOCK_SIZE_SECTORS = numSectors(BLOCK_SIZE_BYTES);

	public static final int TNODES_PER_SECTOR = Disk.SECTOR_SIZE / TNode.TNODE_SIZE;
	public static final int ROOTS_LOCATION = ADisk.size() - Helper.paddedDiv(MAX_TREES, TNODES_PER_SECTOR) - 1;	
	public static final int FREE_LIST_LOCATION = ROOTS_LOCATION - 4;  //TODO: make dynamic
	public static final int AVAILABLE_SECTORS = FREE_LIST_LOCATION;




	private ADisk adisk;
	private SimpleLock lock;



	public PTree(boolean doFormat)
	{
		this.adisk = new ADisk(doFormat);
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
		TNode root = readRoot(xid, tnum);
		for(int n : root.pointers)
			if (n != Node.NULL_PTR)
				if (root.treeHeight == 1)
					freeSectors(xid, n, n+BLOCK_SIZE_SECTORS-1);
				else
					deleteNode(xid, readNode(xid, root.pointers[n]));
	}


	public int getMaxDataBlockId(TransID xid, int tnum)
	throws IOException, IllegalArgumentException
	{
		TNode root = readRoot(xid, tnum);
		if (root.treeHeight == 0)
			throw new IllegalArgumentException();
		for (int n = root.pointers.length; n > 0; --n) {
			if (root.pointers[n] != Node.NULL_PTR)
				if (root.treeHeight == 1)
					return n;
				else
					return getMaxDataBlockId(xid, readNode(xid, root.pointers[n]), root.treeHeight-1, POINTERS_PER_INTERNAL_NODE * n);
		}
		assert(false);
		return Integer.MIN_VALUE;
	}

	public int getMaxDataBlockId(TransID tid, InternalNode node, int height, int leavesLeft) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		for (int i = node.pointers.length; i > 0; --i)  {//Step through from right to left
			if (node.pointers[i] != Node.NULL_PTR)
				if (height == 1) //then the pointers are pointing to leaves
						return leavesLeft + i;
				else
					return getMaxDataBlockId(tid, readNode(tid, node.pointers[i]), height-1, leavesLeft + (POINTERS_PER_INTERNAL_NODE * i));
		}
		assert (false);  //This node has no children, so it shouldn't exist.
		return Integer.MIN_VALUE;
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

		if (root.treeHeight == 0) // if there are no written blocks, just expand the height.
			while(maxBlocks(root.treeHeight) < blockId) 
					root.treeHeight++;
		else //reroot the tree.
			while (maxBlocks(root.treeHeight) < blockId) {
				root.treeHeight++;
				InternalNode n = new InternalNode(getSectors(xid, BLOCK_SIZE_SECTORS), root);
				root.clear();
				root.pointers[0] = n.location;
				writeNode(xid, n);
			}
		writeRoot(xid, root.TNum, root);
		writeVisit(xid, root, blockId, buffer);

	}
	
	//private
	//Find the maximum block ID a tree of a given height supports
	public static int maxBlocks(int height) {
		return  TNODE_POINTERS * (int)Math.pow(POINTERS_PER_INTERNAL_NODE, height - 1) - 1;
	}

	public void readTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert (buffer.length <= METADATA_SIZE);
		TNode t = readRoot(xid, tnum);
		if (t == null) 
			throw new IllegalArgumentException();
		byte[] metadata = t.metadata;
		Helper.fill(metadata, buffer);

	}


	public void writeTreeMetadata(TransID xid, int tnum, byte buffer[])
	throws IOException, IllegalArgumentException
	{
		assert buffer.length <= METADATA_SIZE;
		TNode node = readRoot(xid, tnum);
		if (node == null)
			throw new IllegalArgumentException();
		Helper.fill(buffer,node.metadata);
		writeRoot(xid, tnum, node);
	}

	//Do we need a tid?  free list depends on transaction.
	//We'll just return the committed freelist.
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

	//find a tnum that is unallocated.
	//ResourceException if there are none.
	public int getTNum(TransID tid) throws ResourceException, IOException{
		for(int i = 0; i < MAX_TREES; i++)
			if (readRoot(tid, i) == null)
				return i;
		throw new ResourceException();
	}

	//private
	public int[] findRoot(int tnum) throws IndexOutOfBoundsException{
		if (tnum >= MAX_TREES)
			throw new IndexOutOfBoundsException();
		int[] position = new int[2];
		position[0] = ROOTS_LOCATION + tnum / TNODES_PER_SECTOR;
		position[1] = tnum % TNODES_PER_SECTOR;
		if (position[0] >= this.adisk.getNSectors())
			throw new IndexOutOfBoundsException();
		return position;
	}

	//private
	public TNode readRoot(TransID tid, int tnum) throws IOException, IndexOutOfBoundsException{
		int[] position = findRoot(tnum);
		int sectornum = position[0];
		int offset = position[1] * TNode.TNODE_SIZE;

		//		ByteBuffer buff = ByteBuffer.allocate(Disk.SECTOR_SIZE);
		byte[] buff = readSector(tid, sectornum);

		byte[] nodebytes = new byte[TNode.TNODE_SIZE];
		//		buff.get(nodebytes, offset, TNode.TNODE_SIZE);
		for(int i = 0; i < TNode.TNODE_SIZE; i++)
			nodebytes[i] = buff[i+offset];
		if (Arrays.equals(nodebytes, new byte[TNode.TNODE_SIZE]))
			return null;
		try {
			return new TNode(nodebytes);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	//private
	public void writeRoot(TransID tid, int tnum, TNode node) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {

		int[] position = findRoot(tnum);


		int sectornum = position[0];
		int offset = position[1] * TNode.TNODE_SIZE;

		byte[] buff = readSector(tid, sectornum);
		byte[] nodeBytes = node.getBytes();
		assert nodeBytes.length == TNode.TNODE_SIZE;
		assert offset + nodeBytes.length <= Disk.SECTOR_SIZE;
		for(int i = 0; i < nodeBytes.length; i++)
			buff[offset + i] = nodeBytes[i];

		writeSectors(tid, sectornum, buff);

	}

	//private
	public void readVisit(TransID tid, TNode root, int blockID, byte[] buffer) throws IllegalArgumentException, IOException {
		if (blockID > getMaxDataBlockId(tid, root.TNum)) {
			Helper.fill(new byte[BLOCK_SIZE_BYTES], buffer);
			return;
		}

		if (root.treeHeight == 1) {
			Helper.fill(readBlock(tid, root.pointers[blockID]), buffer);
			return;
		}

		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight-2) * TNODE_POINTERS;
		int index = blockID / leavesBelow;

		if(root.pointers[index] == Node.NULL_PTR) {
			Helper.fill(readBlock(tid, root.pointers[blockID]), buffer);
			return;
		}

		readVisit(tid, root.treeHeight-1, getChild(tid, root, index), blockID%leavesBelow, buffer);
	}

	//private
	public void readVisit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		assert (height >= 1);

		if (height == 1) {
			if (blockID >= POINTERS_PER_INTERNAL_NODE)
				Helper.fill(new byte[BLOCK_SIZE_BYTES], buffer);
			else if (node.pointers[blockID] == Node.NULL_PTR)
				Helper.fill(new byte[BLOCK_SIZE_BYTES], buffer);
			else
				Helper.fill(readBlock(tid, node.pointers[blockID]), buffer);
			return;
		}

		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height-1);
		int index = blockID / leavesBelow;

		if(node.pointers[index] == Node.NULL_PTR) {
			Helper.fill(new byte[BLOCK_SIZE_BYTES], buffer);
			return;
		}
		readVisit(tid, height-1, getChild(tid, node, index), blockID%leavesBelow, buffer);		
	}

	//private
	public void writeVisit(TransID tid, TNode root, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		assert(root.treeHeight >= 1);
		if (root.treeHeight == 1) {
			short block = getSectors(tid, BLOCK_SIZE_SECTORS);
			writeBlock(tid, block, buffer);
			root.pointers[blockID] = block;
			writeRoot(tid, root.TNum, root);			
			return;
		}
		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, root.treeHeight-1);
		int index =  (blockID / leavesBelow);
		writeVisit(tid, root.treeHeight-1, getChild(tid, root, index), blockID%leavesBelow, buffer);
	}

	//private
	public void writeVisit(TransID tid, int height, InternalNode node, int blockID, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		assert (height >= 1);
		if(height == 1) {
			assert (blockID < POINTERS_PER_INTERNAL_NODE);
			short block;
			if (node.pointers[blockID] != Node.NULL_PTR)
				block = node.pointers[blockID];
			else
				block = getSectors(tid, BLOCK_SIZE_SECTORS);
			
			writeBlock(tid, block, buffer);
			node.pointers[blockID] = block;
			writeNode(tid, node);	
			return;
		}

		int leavesBelow = (int) Math.pow(POINTERS_PER_INTERNAL_NODE, height-1);
		int index = blockID / leavesBelow;
		writeVisit(tid, height-1, getChild(tid, node, index), blockID%leavesBelow, buffer);
	}

	//private
	public void deleteNode(TransID tid, InternalNode node) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		if (node == null)
			return;
		for(int i : node.pointers)
			if (node.pointers[i] != Node.NULL_PTR) {
				deleteNode(tid, readNode(tid, node.pointers[i]));
				freeSectors(tid, node.pointers[i], node.pointers[i] + BLOCK_SIZE_SECTORS - 1);
			}
		freeSectors(tid, node.location, node.location + BLOCK_SIZE_SECTORS - 1);
	}

	//private
	//essentially c malloc, but with sectors, not bytes.
	public short getSectors(TransID tid, int numSectors) throws IllegalArgumentException, IndexOutOfBoundsException, IOException, ResourceException{
		BitMap freelist = readFreeList(tid);
		try {

			for(int i = freelist.nextClearBit(0); i < AVAILABLE_SECTORS;i=freelist.nextClearBit(i+1))   {
				if(freelist.nextSetBit(i) >= i+numSectors || freelist.nextSetBit(i) < 0) {
					freelist.set(i, i+numSectors);
					writeFreeList(tid, freelist);
					return (short) i;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			throw new ResourceException();
		}		
		
		throw new ResourceException();

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
		return new BitMap(readSectors(tid, FREE_LIST_LOCATION, ROOTS_LOCATION-1));
	}

	//private
	public void writeFreeList(TransID tid, BitMap freelist)  throws IllegalArgumentException, IndexOutOfBoundsException{
		writeSectors(tid, FREE_LIST_LOCATION, freelist.getBytes());
	}

	public InternalNode getChild(TransID tid, Node parent, int index) throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
		//If the requested node already exists, simpoly return it.
		if (parent.pointers[index] != Node.NULL_PTR)
			return readNode(tid, parent.pointers[index]);
		
		//otherwise allocate a new node and return that.
		short sector = getSectors(tid, BLOCK_SIZE_SECTORS);
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
	public InternalNode readNode(TransID tid, short sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		try {
			return new InternalNode(sectornum, readBlock(tid, sectornum));
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	//private
	public void writeNode(TransID tid, InternalNode node)  throws IllegalArgumentException, IndexOutOfBoundsException{
		writeBlock(tid, node.location, node.getBytes());
	}

	//private
	public byte[] readBlock(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
		return readSectors(tid, sectornum, sectornum + BLOCK_SIZE_SECTORS - 1);
	}

	//private
	public void writeBlock(TransID tid, int sectornum, byte[] buffer)  throws IllegalArgumentException, IndexOutOfBoundsException{
		assert (buffer.length == BLOCK_SIZE_BYTES);
		writeSectors(tid, sectornum, buffer);
	}

	//private
	public byte[] readSectors(TransID tid, int start, int finish) throws IllegalArgumentException, IndexOutOfBoundsException, IOException{
		if (finish < start)
			throw new IllegalArgumentException();
		byte[] sector = new byte[Disk.SECTOR_SIZE];
		byte[] buffer = new byte[(finish + 1 - start) * Disk.SECTOR_SIZE];
		for(int i = 0; i <= finish-start; i++) {
			this.adisk.readSector(tid, start+i, sector);
			for(int index = 0; index < Disk.SECTOR_SIZE; index++)
				buffer[i*Disk.SECTOR_SIZE + index] = sector[index];
		}
		return buffer;
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

	//private
	public byte[] readSector(TransID tid, int sectornum) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		byte[] buff = new byte[Disk.SECTOR_SIZE];
		this.adisk.readSector(tid, sectornum, buff);
		return buff;
	}

	//private
	public void writeSector(TransID tid, int sectornum, byte[] buffer) throws IllegalArgumentException, IndexOutOfBoundsException, IOException {
		this.adisk.writeSector(tid, sectornum, buffer);
	}

	public static int numSectors(byte[] buffer) {
		return numSectors(buffer.length);
	}

	public static int numSectors(int bytes) {
		return (bytes / Disk.SECTOR_SIZE) + (bytes % Disk.SECTOR_SIZE != 0 ? 1 : 0);
	}
}
