import java.io.IOException;

import junit.framework.TestCase;


public class ADiskUnit extends TestCase {

	ADisk adisk;

	public ADiskUnit(String arg0) {
		super(arg0);
		this.adisk = new ADisk(true);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testCommitTransaction(){
		TransID tid = adisk.beginTransaction();
		int sectorNum = 1025;  //First available sector.
		byte[] buffer = new byte[Disk.SECTOR_SIZE];
		ADisk.fill(buffer, "Hello World".getBytes());
		try {
			adisk.writeSector(tid, sectorNum, buffer);
			adisk.commitTransaction(tid);
			byte[] buff2 = ADisk.blankSector();
			adisk.readSector(tid, sectorNum, buff2);
			for (int i = 0; i < Disk.SECTOR_SIZE; i++) 
				assertEquals(buffer[i], buff2[i]);
		} catch (Exception e) {
			fail();
		}
	}

	public void testAbortTransaction() {
		TransID tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
		adisk.abortTransaction(tid);
		assertFalse(adisk.isActive(tid));
	}

	public void testReadSector() {
		TransID tid2 = adisk.beginTransaction();
		TransID tid1 = adisk.beginTransaction();
		byte[] wbuffer = ADisk.blankSector();
		byte[] rbuffer = ADisk.blankSector();
		
		int sectorNum = 1025;
		
		ADisk.fill(wbuffer, "Hello World".getBytes());
		
		adisk.writeSector(tid1,sectorNum, wbuffer);
		try {
			adisk.readSector(tid1, sectorNum, rbuffer);
			adisk.commitTransaction(tid1);
			adisk.readSector(tid2, sectorNum, rbuffer);
			Thread.sleep(2000);
			adisk.readSector(tid2, sectorNum, rbuffer);
		}catch (Exception e) {
			fail("Exception!");
		}
		
	}

	public void testBlankSector() {
		byte[] b = ADisk.blankSector();
		assertEquals(b.length, 512);
		for(int i = 0; i < b.length; i++)
			assertEquals(b[i], null);
	}

	public void testIsActive() {
		TransID tid = null;
		assertFalse(adisk.isActive(tid));
		tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
	}
	//	public TestResult run(){
	//		setUp();
	//		testCommitTransaction();
	//		tearDown();
	//		return null;
	//	}
}
