import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException {
//		InternalNode t = new InternalNode();
//		System.out.println(t.getBytes().length);
		
//		ByteBuffer b = ByteBuffer.allocate(1024);
//		byte[] array = new byte[576];
//		b.put(array);
//		System.out.println(b.array().length);
		
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		oos.writeObject(new TNode(0));
		System.out.println(out.toByteArray().length);
		System.out.println(TNode.TNODE_SIZE);
//		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//		ObjectInputStream ois = new ObjectInputStream(in);
//		TNode node = (TNode)ois.readObject();
//		System.out.println(node == null);

	}

}
