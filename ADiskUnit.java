import static org.junit.Assert.*;

import org.junit.Test;


public class ADiskUnit {
	
	ADisk adisk;
	
	public ADiskUnit() {
		this.adisk = new ADisk(true);
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
	}

	@Test
	public void testAbortTransaction() {
		TransID tid = adisk.beginTransaction();
		assertTrue(adisk.isActive(tid));
		adisk.abortTransaction(tid);
		assertFalse(adisk.isActive(tid));
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
	}
	
	@Test
	public void testReadLog() {
		//TODO: Implement
		fail("Not Implemented");
	}

	@Test
	public void testReadPtrs() {
		//TODO: Implement
		fail("Not Implemented");
	}
	
	@Test
	public void testWritePtrs() {
		//TODO: Implement
		fail("Not Implemented");
	}
}
