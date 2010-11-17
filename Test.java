import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		ADisk adisk = new ADisk(false);
		byte[] b1 = new byte[512];
		byte[] b2 = new byte[512];
		ADisk.fill(b1, "Hello World".getBytes());
		try {
			adisk.aTrans(0, b2, Disk.READ);
			for (byte b : b2)
				System.out.print((char)b);
			System.out.println();
			adisk.aTrans(0, b1, Disk.WRITE);
			adisk.aTrans(0, b2, Disk.READ);
			for (byte b : b2)
				System.out.print((char)b);
			System.out.println();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			System.exit(0);
		}
		
	}
}