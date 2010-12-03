import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;


public class Test {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
//		InternalNode t = new InternalNode();
//		System.out.println(t.getBytes().length);
		
//		ByteBuffer b = ByteBuffer.allocate(1024);
//		byte[] array = new byte[576];
//		b.put(array);
//		System.out.println(b.array().length);
		
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(out);
		BitSet b = new BitSet(4096);
		oos.writeObject(b);
		byte[] buffer = out.toByteArray();
		System.out.println(buffer.length);

	}

}
