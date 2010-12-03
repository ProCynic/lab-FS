import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;


public class TNode extends Node{

	private static final long serialVersionUID = 5733757348481243149L;
	int[] pointers;
	public byte[] metadata;
	public int treeHeight;
	int TNum;  //Helpful, even if not strictly necessary.
	public static final int TNODE_SIZE = 133; //TODO: Make sure this is correct before finalizing the TNode class.
	
	public TNode(int location) {
		super(location);
		this.metadata = new byte[PTree.METADATA_SIZE];
		this.pointers = new int[PTree.TNODE_POINTERS];
		Arrays.fill(pointers, Integer.MIN_VALUE);
		this.treeHeight = 0;
	}
	
	public TNode(int location, int tnum) {
		this(location);
		this.TNum = tnum;
	}
	
	public TNode(int location, byte[] buffer) {
		super(location, buffer);
	}
	
	@Override
	public void fromBytes(byte[] buffer) {
		assert (buffer.length == TNODE_SIZE);
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		try {
			ObjectInputStream ois = new ObjectInputStream(in);
			this.TNum = ois.readInt();
			this.treeHeight = ois.readInt();
			int bytesRead = ois.read(metadata);
			assert (bytesRead == PTree.METADATA_SIZE);
			this.pointers = (int[]) ois.readObject();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream(TNODE_SIZE);
		try {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeInt(TNum);
			oos.writeInt(treeHeight);
			oos.write(metadata);
			oos.writeObject(pointers);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		
		return out.toByteArray();
	}


}