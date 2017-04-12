package webcrawler;

import java.util.HashSet;
import java.util.LinkedList;

public class RecentDomainList extends LinkedList<String>{
		
		private static final long serialVersionUID = -2316584929155058695L;
		private final int MAX_SIZE;
		private HashSet<String> hash ;
		
		public RecentDomainList(int max_size){
			this.MAX_SIZE = max_size;
			this.hash = new HashSet<String>(max_size, 2); //it should never be bigger than max_size
		}
		
		public void addLast(String e){
			boolean contains = !this.hash.add(e);
			if(!contains){
				if(this.size() == MAX_SIZE) {
					this.removeFirst();
					this.hash.remove(e);
				}		
				super.addLast(e);
			}						
		}
		
		public boolean add(String e){
			this.addLast(e);
			return true;
		}
		
		public boolean contains(final String e){
			return this.hash.contains(e);
		}
	}