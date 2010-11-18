
public class Sector {
	public byte[] array;
	
	public Sector() {
		this.array = new byte[Disk.SECTOR_SIZE];
	}
	
	public Sector(byte[] array){
		this();
		this.update(array);
	}
	
	public Sector(Sector other) {
		this();
		this.update(other);
	}
	
	public void update(byte[] array) {
		if(array.length > this.size())
			throw new IllegalArgumentException();
		for(int i=0; i < array.length; i++)
			this.array[i] = array[i];
	}
	
	public void update (Sector other) {
		this.update(other.array);
	}
	
	public byte[] fill(byte[] buff) {
		if(buff.length < this.size())
			throw new IllegalArgumentException();
		for(int i = 0; i < this.size(); i++)
			buff[i] = this.array[i];
		return buff;
	}
	
	public int size(){
		return this.array.length;
	}
	
	public boolean equals(Sector other){
		//Now redundant
		if(this.size()!=other.size()){
			return false;
		}
		for(int i=0;i<this.size();i++){
			if(this.array[i]!=other.array[i])
				return false;			
		}
		return true;
	}
}
