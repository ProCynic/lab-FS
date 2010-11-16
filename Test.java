import java.io.FileNotFoundException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Disk disk = new Disk(new TestWrite(), (float) 0);
			byte[] b = "Hello World".getBytes();
			disk.startRequest(Disk.WRITE, 0, 0, b);
		} catch (Exception e) {
			System.exit(-1);
		}
	}

}


class TestWrite implements DiskCallback{
	public void requestDone(DiskResult d) {
		System.out.println("Written");
	}
}