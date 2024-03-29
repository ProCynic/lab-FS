
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class PTreeUnit {

	PTree ptree;
	
	@Before
	public void setUp() throws Exception {
		 ptree = new PTree(true);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testCommit() {
		TransID tid = ptree.beginTrans();
		try {
			ptree.commitTrans(tid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail");
		}

	}
	
	@Test()
	public void testWriteAndReadSector(){
		TransID tid = ptree.beginTrans();
		int start = 0;
		
		byte[] buffer = new byte[Disk.SECTOR_SIZE];;
		for(int i=0;i<buffer.length;i++){
			buffer[i]=(byte)i;
		}
		ptree.writeSectors(tid, start, buffer);
		
		byte[] buffer2;
		try{
			buffer2 = ptree.readSector(tid, start);
		}catch(IOException e){
			fail("Exception Fail");
			buffer2=null;
		}
		assertTrue(buffer.length == buffer2.length);
		for(int i=0;i<buffer.length;i++){
			assertTrue(buffer[i]==buffer2[i]);
		}
	}
	
	@Test
	public void testReadNullRoots() {
		TransID tid = ptree.beginTrans();
		for(int tnum = 0; tnum < PTree.MAX_TREES; tnum++)
			try {
				assertTrue(ptree.readRoot(tid, tnum) == null);
			} catch (IOException e) {
				e.printStackTrace();
				fail("Exception Fail");

			}
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testNegTnum() {
		TransID tid = ptree.beginTrans();
		try {
			ptree.readRoot(tid, -1);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testHighTnum() {
		TransID tid = ptree.beginTrans();
		try {
			ptree.readRoot(tid, PTree.MAX_TREES);
		} catch (IOException e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}
	
	@Test
	public void testFreeList() {
		TransID tid = ptree.beginTrans();
		BitMap freelist = null;
		try {
			freelist = ptree.readFreeList(tid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail.");
		}
		assertTrue(freelist.cardinality() == 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testGetMaxBlockID0(){
		TransID tid = ptree.beginTrans();
		try{
			int tnum = ptree.createTree(tid);
			ptree.getMaxDataBlockId(tid, tnum);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
			
		}
	}
	
	@Test
	public void testGetMaxBlockId() throws IllegalArgumentException, ResourceException, IOException {
		TransID tid = ptree.beginTrans();
		int tnum = ptree.createTree(tid);
		ptree.writeData(tid, tnum, 5, new byte[PTree.BLOCK_SIZE_BYTES]);
		ptree.commitTrans(tid);
		tid = ptree.beginTrans();
		ptree.writeData(tid, tnum, 5417, new byte[PTree.BLOCK_SIZE_BYTES]);
		assertTrue(ptree.getMaxDataBlockId(tid, tnum) == 5417);
	}

	@Test
	public void testWriteData(){
		TransID tid = ptree.beginTrans();
		int tnum = -1;
		try{
			tnum = ptree.createTree(tid);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");	
		}		
		
		int blockId = 0;
		byte[] buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		for(int i=0;i<buffer.length;i++){
			buffer[i]=(byte)i;
		}
		
		try{
			ptree.writeData(tid, tnum, blockId, buffer);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
		}				
		
	}
	
	@Test
	public void testWriteData2(){
		TransID tid = ptree.beginTrans();
		int tnum = -1;
		try{
			tnum = ptree.createTree(tid);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");	
		}		
		
		
		byte[] buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		for(int i=0;i<buffer.length;i++){
			buffer[i]=(byte)i;
		}
		
		try{
			int blockId = 0;
			ptree.writeData(tid, tnum, blockId, buffer);
			blockId = 1000;
			ptree.writeData(tid,tnum,blockId,buffer);
			ptree.commitTrans(tid);
			tid = ptree.beginTrans();
			blockId=500;
			ptree.writeData(tid,tnum,blockId,buffer);			
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
		}	
	}

	@Test
	public void testWriteReadData() throws IllegalArgumentException, IOException {
		TransID tid = ptree.beginTrans();
		int tnum = -1;
		try{
			tnum = ptree.createTree(tid);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");	
		}
		
		int blockId = 573;
		byte[] buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		for(int i=0;i<buffer.length;i++){
			buffer[i]=(byte)i;
		}
		byte[] buffer2 = new byte[PTree.BLOCK_SIZE_BYTES];
		
		ptree.writeData(tid, tnum, blockId, buffer);
		ptree.readData(tid, tnum, blockId, buffer2);
		assertTrue(Arrays.equals(buffer, buffer2));
		
	}
	
	
	public void testGetParamException(){
		try{
			ptree.getParam(PTree.ASK_MAX_TREES);
			ptree.getParam(PTree.ASK_FREE_SPACE);
			ptree.getParam(PTree.ASK_FREE_TREES);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
		}
	}
	
	@Test
		public void testGetSectors() throws IllegalArgumentException, IndexOutOfBoundsException, ResourceException, IOException {
			TransID tid = ptree.beginTrans();
			for (int i = 0; i < 100; i+=2) {
				assertTrue(ptree.getSectors(tid, 2) == i);
	//			if((i/2) % 10 == 0) {
				ptree.commitTrans(tid);
				tid = ptree.beginTrans();
	//			}
			}
		}

	@Test
	public void testAllocateRoots() {
		TransID tid = ptree.beginTrans();
		int tnum = -1;
		for (int i = 0; i < PTree.MAX_TREES; i++) {
			try {
				tnum = ptree.getTNum(tid);
				ptree.writeRoot(tid, tnum, new TNode(tnum));
				if(i % 30 == 0) {
					ptree.commitTrans(tid);
					tid = ptree.beginTrans();
				}
					
			} catch (IOException e) {
				e.printStackTrace();
				fail("Exception Fail");
			}
		}
		try {
			ptree.commitTrans(tid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}
	
	@Test(expected=ResourceException.class)
	public void testAllocateRoots2() {
		TransID tid = ptree.beginTrans();
		int tnum = -1;
		for (int i = 0; i <= PTree.MAX_TREES; i++) {
			try {
				tnum = ptree.getTNum(tid);
				ptree.writeRoot(tid, tnum, new TNode(tnum));
//				System.out.println("Wrote tnum: " + tnum);
				if(i % 30 == 0) {
					ptree.commitTrans(tid);
					tid = ptree.beginTrans();
				}
					
			} catch (IOException e) {
				e.printStackTrace();
				fail("Exception Fail");
			}
		}
		try {
			ptree.commitTrans(tid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}
	
	@Test
	public void testWriteAndReadBlock(){
		TransID tid = ptree.beginTrans();
		int sectornum=0;		
		byte[] buffer = new byte[PTree.BLOCK_SIZE_BYTES];
		
		for(int i=0;i<buffer.length;i++){
			buffer[i]=(byte)i;
		}		
		ptree.writeBlock(tid,sectornum, buffer);
		byte[] buffer2 = null;
		try{
			buffer2 = ptree.readBlock(tid, sectornum);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
		}
		assertTrue(buffer.length == buffer2.length);
		for(int i=0;i<buffer.length;i++){
			assertTrue(buffer[i]==buffer2[i]);
		}
	}
	
	@Test
	public void testTree(){
		TransID tid = ptree.beginTrans();
		try{
			ptree.createTree(tid);
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");
		}				
	}
	
	@Test
	public void testWriteandReadTreeMetadata(){
		try{
			TransID tid = ptree.beginTrans();
			int tnum = ptree.createTree(tid);
			byte buffer[] = new byte[PTree.METADATA_SIZE];
			for (int i=0;i<buffer.length;i++){
				buffer[i]=(byte)i;}		
			ptree.writeTreeMetadata( tid, tnum, buffer);
			byte buffer2[] =new byte[PTree.METADATA_SIZE];
			ptree.readTreeMetadata(tid,tnum,buffer2);			
			assertTrue(Arrays.equals(buffer,buffer2));				
		}catch(IOException e){
			e.printStackTrace();
			fail("Exception Fail");			
		}
		
	}

}
