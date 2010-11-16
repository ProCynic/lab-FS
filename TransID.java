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
public class TransID implements Iterable<Action>{

  //
  // Implement this class
  //
	
	public long tid;
	
	private ArrayList<Action> actions;
	
	public TransID() {
	}

	@Override
	public Iterator<Action> iterator() {
		return actions.iterator();
	}
  
}

abstract class Action {
	
}

class Write extends Action{
	
}

class Read extends Action{
	

}
