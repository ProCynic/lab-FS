import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;


public class InternalNode extends Node{

	private static final long serialVersionUID = 7400442076200379954L;
	
	public short location;

	public InternalNode(int location) {
		this.pointers = new short[PTree.POINTERS_PER_INTERNAL_NODE];
		Arrays.fill(pointers, NULL_PTR);
	}
	
	public InternalNode(short location, byte[] buffer) throws ClassNotFoundException {
		this(location);
		this.fromBytes(buffer);
	}
	
	public InternalNode(short location, Node root) {
		this(location);
		for(int i = 0; i < root.pointers.length; i++)
			this.pointers[i] = root.pointers[i];
	}

	public void fromBytes(byte[] buffer) throws ClassNotFoundException {
		assert buffer.length == PTree.BLOCK_SIZE_BYTES;
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		try {
			ObjectInputStream ois = new ObjectInputStream(in);
			this.pointers = (short[]) ois.readObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public byte[] getBytes() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(this.pointers);
			assert out.toByteArray().length <= PTree.BLOCK_SIZE_BYTES;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		
		return Helper.fill(out.toByteArray(), new byte[PTree.BLOCK_SIZE_BYTES]);
	}
}