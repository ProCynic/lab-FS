import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/*
 * TransId.java
 *
 * Interface to ADisk
 *
 * You must follow the coding standards distributed
 * on the class web page.
 *
 * (C) 2007 Mike Dahlin
 *
 */
public class TransID{

	private static long newtid = 0;
	private static long newTid() {
		return newtid++;
	}
	
	private long tid;
	
	TransID () {
		this.tid = TransID.newTid();
	}
	
	public long getTid() {
		return this.tid;
	}
	
	public boolean equals (TransID other) {
		if (other.getTid() == this.getTid())
			return true;
		else
			return false;
	}
	
  
}

