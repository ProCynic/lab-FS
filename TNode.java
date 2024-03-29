import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;


public class TNode extends Node{
	
	private static final long serialVersionUID = 5733757348481243149L;
	public byte[] metadata;
	public int treeHeight;
	int TNum;  //Helpful, even if not strictly necessary.
	public static final int TNODE_SIZE = 117;
	
	private TNode() {
		this.metadata = new byte[PTree.METADATA_SIZE];
		this.pointers = new short[PTree.TNODE_POINTERS];
		Arrays.fill(pointers, NULL_PTR);
		this.treeHeight = 0;
	}
	
	public TNode(int tnum) {
		this();
		this.TNum = tnum;
	}
	
	public void clear() {
		Arrays.fill(this.pointers, NULL_PTR);
	}
	
	public TNode(byte[] buffer) throws ClassNotFoundException {
		this();
		this.fromBytes(buffer);
	}
	
	@Override
	public void fromBytes(byte[] buffer) throws ClassNotFoundException {
		assert (buffer.length == TNODE_SIZE);
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		try {
			ObjectInputStream ois = new ObjectInputStream(in);
			this.TNum = ois.readInt();
			this.treeHeight = ois.readInt();
			int bytesRead = ois.read(metadata);
			assert (bytesRead == PTree.METADATA_SIZE);
			this.pointers = (short[]) ois.readObject();
			
		} catch (IOException e) {
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
