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
		
//		PTree p = new PTree(true);
		
//		ByteArrayInputStream in = new ByteArrayInputStream(new byte[TNode.TNODE_SIZE]);
//		ObjectInputStream ois = new ObjectInputStream(in);
//		System.out.println((TNode)ois.readObject());
		
//		byte[] test = "Hello World".getBytes();
//		ByteBuffer buff = ByteBuffer.allocate(512);
//		
//		buff.put(new byte[512]);
//		
//		buff.put(test, 0, test.length);
//		System.out.println(buff.array());
				
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
		
		byte[] buff = new TNode(0).getBytes();
		
		System.out.println(buff.length);
		
		new TNode(buff);
//		
//		System.out.println(TNode.TNODE_SIZE);
//		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
//		ObjectInputStream ois = new ObjectInputStream(in);
//		TNode node = (TNode)ois.readObject();
//		System.out.println(node == null);

	}

}
