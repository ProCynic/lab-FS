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
	public void testCommitTrans() {
		TransID tid = flatFS.beginTrans();
		try {
			flatFS.commitTrans(tid);			
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}

	@Test
	public void testAbortTrans() {
		TransID tid = flatFS.beginTrans();
		try {
			flatFS.abortTrans(tid);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception Fail");
		}
	}

	@Test
	public void testCreateAndDeleteFile() {
		try{
			TransID tid = flatFS.beginTrans();
			int inode = flatFS.createFile(tid);
			flatFS.deleteFile(tid,inode);
			
		}catch(IOException e){
			e.printStackTrace();
			fail("Excpetion fail");
		}
		
	}	

	@Test
	public void testRead() {
		fail("Just didn't write tests yet");
	}

	@Test
	public void testWrite() {
		fail("Just write tests yet");
	}

	@Test
	public void testReadFileMetadata() {
		try{
			flatFS.getParam(FlatFS.ASK_MAX_FILE);
			flatFS.getParam(FlatFS.ASK_FREE_FILES);
			flatFS.getParam(FlatFS.ASK_FREE_SPACE_BLOCKS);
			flatFS.getParam(FlatFS.ASK_FILE_METADATA_SIZE);
		}catch(IOException e){
			e.printStackTrace();
		
		}
	}

	

	@Test
	public void testGetParam() {
		try{
			TransID tid = flatFS.beginTrans();
			int tnum = flatFS.createFile(tid);
			byte buffer[] = new byte[FlatFS.ASK_FILE_METADATA_SIZE];
			for (int i=0;i<buffer.length;i++){
				buffer[i]=(byte)i;}		
			flatFS.writeFileMetadata( tid, tnum, buffer);
			byte buffer2[] =new byte[PTree.METADATA_SIZE];
			flatFS.readFileMetadata(tid,tnum,buffer2);			
			assertTrue(Arrays.equals(buffer,buffer2));				
		}catch(IOException e){
			e.printStackTrace();
					
		}
	}

}
