package webcrawler;

import com.chilkatsoft.CkSpider;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.Iterator;	

public class Main {
	
	//TODO: Should have relative max size based on max memory of host machine
	//See this for reference: http://stackoverflow.com/questions/12807797/java-get-available-memory
	public static ConcurrentLinkedQueue<String> GLOBAL_QUEUE = new ConcurrentLinkedQueue<String>(); //different domains queue
	
	private static int MAX_SIMULTANEOUS_CRAWLERS = 300; //600 crashed my computer (CAREFUL)
	
	private final static boolean DEBUG_BANDWIDTH = true;
	public final static boolean DEBUG_ADD_OUTBOUND_LINK = true;
	public final static boolean DEBUG_EMPTY_PAGE = false;
	private final static boolean DEBUG_GLOBAL_QUEUE_SIZE = true;
	
	static {
		try {

			System.loadLibrary("chilkat");

		} catch (UnsatisfiedLinkError e) {
			System.err.println("Native code library failed to load.\n" + e);
			System.exit(1);
		}
	}

	
	// Instantiate a Chilkat object and print it's version.
	public static void main(String argv[]){
		LinkedList<DomainCrawler> crawlers = new LinkedList<DomainCrawler>();
		
		DomainCrawler seed = new DomainCrawler("https://en.wikipedia.org/wiki/Main_Page");
		
		DomainCrawler newCrawler, crawler2Die;		
		
		crawlers.add(seed);
		seed.start();
		
		while(seed.getRuns() == 0){
			System.out.println("DEBUG: Waiting seed to complete");
		}; //Busy Waiting first batch of URLs
		
		double allBandwidth;		
		
		do{
			allBandwidth = 0;
			
			
			Iterator<DomainCrawler> crawlerIterator = crawlers.iterator();
			DomainCrawler auxCrawler;
			while(crawlerIterator.hasNext()){
				auxCrawler = crawlerIterator.next();
				if(auxCrawler.isAlive()) allBandwidth += auxCrawler.getBandwidth();
				else crawlerIterator.remove();				
			}			
			
			if(DEBUG_BANDWIDTH) System.out.println("DEBUG: Bandwidth is "+allBandwidth);
			if(GLOBAL_QUEUE.isEmpty() == false){
				
				if(allBandwidth > 1000000/8 || crawlers.size() > MAX_SIMULTANEOUS_CRAWLERS){
					crawler2Die = crawlers.removeFirst();
					crawler2Die.kill();
				}
				
				newCrawler = new DomainCrawler(GLOBAL_QUEUE.poll());
				crawlers.add(newCrawler);
				newCrawler.start();
				
				if(DEBUG_GLOBAL_QUEUE_SIZE) System.out.println("DEBUG: GLOBAL QUEUE size: "+GLOBAL_QUEUE.size());
				System.out.println("DEBUG: There are "+crawlers.size()+" threads");
			}
			
		
							
			
		}while(!crawlers.isEmpty() || !GLOBAL_QUEUE.isEmpty());
		System.out.println("Terminating "+crawlers.size()+" "+GLOBAL_QUEUE.size());
	}
}
