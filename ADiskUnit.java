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
		int sectorNum = 1025;
		byte[] buffer = new byte[Disk.SECTOR_SIZE];
		ADisk.fill(buffer, "Hello World".getBytes());
		int type = Disk.WRITE;
		try {
			adisk.aTrans(sectorNum, buffer, type);
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
		fail("Not yet implemented"); // TODO
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
