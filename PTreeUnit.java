
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
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testWriteAndReadSector(){
		
//		TransID TID = ptree.beginTrans();
//		int start = 0;
//		
//		byte[] buffer = new byte[Disk.SECTOR_SIZE];;
//		for(int i=0;i<buffer.length;i++){
//			buffer[i]=(byte)i;
//		}
//		ptree.writeSectors( TID, start, buffer);
//		
//		byte[] buffer2;
//		try{
//			buffer2 = ptree.readSectors(TID, start, buffer.length);
//		}catch(IOException e){
//			fail("Exception Fail");
//			buffer2=null;
//		}
//		assertTrue(buffer.length == buffer2.length);
//		for(int i=0;i<buffer.length;i++){
//			assertTrue(buffer[i]==buffer2[i]);
//		}
	}

}
