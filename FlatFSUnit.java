import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class FlatFSUnit {
	FlatFS flatFS;
	
	@Before
	public void setUp() throws Exception {
		this.flatFS = new FlatFS(true);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testFlatFS() {
		fail("Not yet implemented");
	}

	@Test
	public void testBeginTrans() {
		fail("Not yet implemented");
	}

	@Test
	public void testCommitTrans() {
		fail("Not yet implemented");
	}

	@Test
	public void testAbortTrans() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateAndDeleteFile() {
		try{
			TransID tid = flatFS.beginTrans();
			int inode = flatFS.createFile(tid);
		}catch(IOException e){
			e.printStackTrace();
			fail("Excpetion fail");
		}
		
	}

	@Test
	public void testDeleteFile() {
		fail("Not yet implemented");
	}

	@Test
	public void testRead() {
		fail("Not yet implemented");
	}

	@Test
	public void testWrite() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadFileMetadata() {
		fail("Not yet implemented");
	}

	@Test
	public void testWriteFileMetadata() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetParam() {
		fail("Not yet implemented");
	}

}
