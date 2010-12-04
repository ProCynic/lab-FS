/*
 * FlatFS -- flat file system
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */

import java.io.IOException;
import java.io.EOFException;
public class FlatFS{

  public static final int ASK_MAX_FILE = 2423;
  public static final int ASK_FREE_SPACE_BLOCKS = 29542;
  public static final int ASK_FREE_FILES = 29545;
  public static final int ASK_FILE_METADATA_SIZE = 3502;
  
  public static final byte EOF = (byte)28;
  
  private PTree ptree;

  public FlatFS(boolean doFormat)
    throws IOException
  {
	  this.ptree = new PTree(doFormat);
  }

  public TransID beginTrans()
  {
    return ptree.beginTrans();
  }

  public void commitTrans(TransID xid)
    throws IOException, IllegalArgumentException
  {
	  ptree.commitTrans(xid);
  }

  public void abortTrans(TransID xid)
    throws IOException, IllegalArgumentException
  {
	  ptree.abortTrans(xid);
  }

  public int createFile(TransID xid)
    throws IOException, IllegalArgumentException
  {
	  return ptree.createTree(xid); //tnum and inumber will be the same.
  }

  public void deleteFile(TransID xid, int inumber)
    throws IOException, IllegalArgumentException
  {
	  ptree.deleteTree(xid, inumber);
  }

  public int read(TransID xid, int inumber, int offset, int count, byte buffer[])
    throws IOException, IllegalArgumentException, EOFException
  {
	  int startBlock = Helper.paddedDiv(offset, PTree.BLOCK_SIZE_BYTES);
	  byte[][] blocks = new byte[Helper.paddedDiv(count, PTree.BLOCK_SIZE_BYTES)][PTree.BLOCK_SIZE_BYTES];
	  if (startBlock > ptree.getMaxDataBlockId(xid, inumber))
			  throw new EOFException();
	  for(int i = 0; i < blocks.length; i++)
		  ptree.readData(xid, inumber, startBlock+i, blocks[i]);
	  
	  for (int i = 0; i < count; i++) {
		  buffer[i] = blocks[i/PTree.BLOCK_SIZE_BYTES][i%PTree.BLOCK_SIZE_BYTES];
		  if (buffer[i] == EOF)
			  return i+1;
	  }
	  return count;
  }
    

  public void write(TransID xid, int inumber, int offset, int count, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  int startBlock = Helper.paddedDiv(offset, PTree.BLOCK_SIZE_BYTES);
	  byte[][] blocks = new byte[Helper.paddedDiv(count, PTree.BLOCK_SIZE_BYTES)][PTree.BLOCK_SIZE_BYTES];
	  for (int i =0; i < buffer.length; i++) {
		  blocks[i/PTree.BLOCK_SIZE_BYTES][i%PTree.BLOCK_SIZE_BYTES] = buffer[i];
	  }
	  for (int i = 0; i < blocks.length; i++)
		  ptree.writeData(xid, inumber, startBlock + i, blocks[i]);
  }

  public void readFileMetadata(TransID xid, int inumber, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  ptree.readTreeMetadata(xid, inumber, buffer);
  }


  public void writeFileMetadata(TransID xid, int inumber, byte buffer[])
    throws IOException, IllegalArgumentException
  {
	  ptree.writeTreeMetadata(xid, inumber, buffer);
  }

  public int getParam(int param)
    throws IOException, IllegalArgumentException
  {
    if (param == ASK_MAX_FILE)
    	return ptree.getParam(PTree.ASK_FREE_TREES);
    else if (param == ASK_FILE_METADATA_SIZE) 
    	return ptree.METADATA_SIZE;
    else if (param == ASK_FREE_FILES)
    	return ptree.getParam(PTree.ASK_FREE_TREES);
    else if (param == ASK_FREE_SPACE_BLOCKS)
    	return ptree.getParam(PTree.ASK_FREE_SPACE);
    else
    	throw new IllegalArgumentException();
  }
    

  
  

}
