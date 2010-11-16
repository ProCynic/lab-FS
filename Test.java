import java.io.FileNotFoundException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Disk disk = new Disk(new TestWrite(), (float) 0);
			byte[] b = new byte[512];
			String str = "Hello World";
			for (int i = 0; i < str.length(); i++)
				b[i] = str.getBytes()[i];
			disk.startRequest(Disk.WRITE, 0, 0, b);
		} catch (Exception e) {
			System.out.print(e.toString());
			System.exit(-1);
		}
	}

}


class TestWrite implements DiskCallback{
	public void requestDone(DiskResult d) {
		System.out.println("Written");
		System.exit(0);
	}
}