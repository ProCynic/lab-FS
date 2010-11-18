import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ADiskUnit {
	
	ADisk adisk;
	
	public ADiskUnit() {
		this.adisk = new ADisk(true);
	}
	
	@Before
	public void setUp() throws Exception {
		this.adisk = new ADisk(true);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testCommitTransaction() {
		TransID tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
		int sectorNum = 1025;  //First available sector.
		Sector buffer = new Sector("Hello World".getBytes());
		try {
			TransID tid2 = adisk.beginTransaction();
			adisk.writeSector(tid, sectorNum, buffer.array);
			adisk.commitTransaction(tid);
			Sector buff2 = new Sector();
			adisk.readSector(tid2, sectorNum, buff2.array);
			assertTrue(buff2.equals(buffer));
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception fail");
		}
		System.out.println("Test Passed");
	}

	@Test
	public void testAbortTransaction() {
		TransID tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
		adisk.abortTransaction(tid);
		assertFalse(adisk.isActive(tid));
		System.out.println("Test Passed");
	}

	@Test
	public void testReadSector() {
		
		TransID tid2 = adisk.beginTransaction();
		TransID tid1 = adisk.beginTransaction();
		Sector wbuffer = new Sector("Hello World".getBytes());
		Sector rbuffer = new Sector();
		
		int sectorNum = 1025;
		
		adisk.writeSector(tid1,sectorNum, wbuffer.array);
		try {
			adisk.readSector(tid1, sectorNum, rbuffer.array);
			assertTrue(rbuffer.equals(wbuffer));
			adisk.commitTransaction(tid1);
			adisk.readSector(tid2, sectorNum, rbuffer.array);
			//We did this test a few times, and found these reads in these locations every time
			assertTrue(rbuffer.equals(wbuffer));  //Found in writeback queue
			Thread.sleep(2000);
			adisk.readSector(tid2, sectorNum, rbuffer.array);  //found on disk
			assertTrue(rbuffer.equals(wbuffer));
		}catch (Exception e) {
			fail("exception fail");
		}
		System.out.println("Test Passed");
	}
	
	//tests pointers as well
	@Test
	public void testReadLog() {
		
		this.adisk = new ADisk(true,false);  //Create an ADisk with writeback turned off.
				
		TransID tid1 = adisk.beginTransaction();
		TransID tid2 = adisk.beginTransaction();
		Sector wbuffer = new Sector("Hello World".getBytes());
		Sector rbuffer = new Sector();
		
		int sectorNum = 1025;
		
		adisk.writeSector(tid1,sectorNum, wbuffer.array);
		try {
			adisk.readSector(tid1, sectorNum, rbuffer.array);
			assertTrue(rbuffer.equals(wbuffer));
			adisk.commitTransaction(tid1);
			adisk.readSector(tid2, sectorNum, rbuffer.array);
			assertTrue(rbuffer.equals(wbuffer));
			this.adisk=null;
			this.adisk = new ADisk(false,true);
			Thread.sleep(2000);
			tid2 = adisk.beginTransaction();
			adisk.readSector(tid2, sectorNum, rbuffer.array);  //found on disk
			assertTrue(rbuffer.equals(wbuffer));
		}catch (Exception e) {
			e.printStackTrace();
			fail("exception fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testComit2() {
		TransID tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
		int sectorNum = 1025;  //First available sector.
		Sector buffer = new Sector("Hello World".getBytes());
		
		adisk.writeSector(tid, sectorNum, buffer.array);
		try {
			adisk.abortTransaction(tid);
			adisk.commitTransaction(tid);
		}catch(IOException e) {
			fail("Disk Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test
	public void testCommit3() {
		this.adisk = new ADisk(true,false); //No write back
		TransID tid;
		Sector write = new Sector("Some stuff".getBytes());
		int counter = 0;
		try {
			while(true) {
				tid = adisk.beginTransaction();
				adisk.writeSector(tid, 5748, write.array);
				adisk.commitTransaction(tid);
				counter += 3;
				assertTrue(counter <= adisk.getNSectors());
			}
		}catch (IllegalArgumentException e) {
			fail("Exception fail");
		} catch (IOException e) {
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testAbort2() {
		TransID tid = adisk.beginTransaction();
		adisk.abortTransaction(tid);
		adisk.abortTransaction(tid);
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testReadSector2() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[7];
		try {
			adisk.readSector(tid, 5894, buff);
		} catch (IndexOutOfBoundsException e) {
			fail("Exception fail");
		} catch (IOException e) {
			fail("Exception fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testReadSector3() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.abortTransaction(tid);
			adisk.readSector(tid, 5894, buff);
		} catch (IndexOutOfBoundsException e) {
			fail("Exception Fail");
		} catch (IOException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testReadSector4() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.readSector(tid, Integer.MAX_VALUE, buff);
		} catch (IllegalArgumentException e) {
			fail("Exception Fail");
		} catch (IOException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testReadSector5() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.readSector(tid, 50, buff);
		} catch (IllegalArgumentException e) {
			fail("Exception Fail");
		} catch (IOException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testWriteSector2() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[7];
		try {
			adisk.writeSector(tid, 5894, buff);
		} catch (IndexOutOfBoundsException e) {
			fail("Exception fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testWriteSector3() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.abortTransaction(tid);
			adisk.writeSector(tid, 5894, buff);
		} catch (IndexOutOfBoundsException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testWriteSector4() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.writeSector(tid, Integer.MAX_VALUE, buff);
		} catch (IllegalArgumentException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
	public void testWriteSector5() {
		TransID tid = adisk.beginTransaction();
		byte[] buff = new byte[512];
		try {
			adisk.writeSector(tid, 50, buff);
		} catch (IllegalArgumentException e) {
			fail("Exception Fail");
		}
		System.out.println("Test Passed");
	}
}
