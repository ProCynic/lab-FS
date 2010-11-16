import java.io.FileNotFoundException;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(new Thing().getb() == 0);
		
	}
	


}


class Thing {
	private int b;
	public int getb() {
		return this.b;
	}
}

class TestWrite implements DiskCallback{
	public void requestDone(DiskResult d) {
		System.out.println("Written");
		System.exit(0);
	}
}