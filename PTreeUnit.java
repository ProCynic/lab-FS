
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class PTreeUnit {

	PTree ptree;
	
	@Before
	public void setUp() throws Exception {
		 ptree = new PTree(true);
		 System.out.println("Beginning Test");
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
	
	

}
