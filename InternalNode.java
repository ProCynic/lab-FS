import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class InternalNode extends Node{

	private static final long serialVersionUID = 7400442076200379954L;
	
	public int location;

	public InternalNode(int location) {
		this.pointers = new int[PTree.POINTERS_PER_INTERNAL_NODE];
		Arrays.fill(pointers, NULL_PTR);
	}
	
	public InternalNode(int location, byte[] buffer) {
		this(location);
		this.fromBytes(buffer);
	}
	
	public InternalNode(int location, Node root) {
		this(location);
		for(int i = 0; i < root.pointers.length; i++)
			this.pointers[i] = root.pointers[i];
	}

	@Override
	public void fromBytes(byte[] buffer) {
		assert buffer.length == PTree.BLOCK_SIZE_BYTES;
		ByteArrayInputStream in = new ByteArrayInputStream(buffer);
		try {
			ObjectInputStream ois = new ObjectInputStream(in);
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
		ByteBuffer buff = ByteBuffer.allocate(PTree.BLOCK_SIZE_BYTES);
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
		buff.put(out.toByteArray());  //Resize to BLOCK_SIZE_BYTES
		return buff.array();
	}
}