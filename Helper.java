
public class Helper {
	
	public static byte[] fill(byte[] src, byte[] dest) {
		assert (dest.length >= src.length);
		int i;
		for(i = 0; i < src.length; i++)
			dest[i] = src[i];
		for(; i < dest.length; i++)
			dest[i] = (byte) 0;
		return dest;
	}
	
	public static int paddedDiv(int numerator, int divisor) {
		return numerator / divisor + (numerator % divisor != 0 ? 1 : 0);
	}

}
