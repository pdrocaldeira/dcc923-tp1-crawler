package webcrawler;

import com.chilkatsoft.CkSpider;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Iterator;	
import java.util.Comparator;
import java.util.HashSet;

public class Main {
	
	public static final int MAX_SIMULTANEOUS_CRAWLERS = 32;//600 crashed my computer (CAREFUL)
	private static final int MAX_SIZE_RECENT_DOMAINS = 100000;
	private static final long START_TIME = System.currentTimeMillis();
	public static AtomicLong PAGES_CRAWLED = new AtomicLong();
	public static int CRAWLERS_SLEEPING = 0;
	public static int PAGES_REQUEST = 0;
	public static int CRAWLERS_KILLED = 0;
	
	private final static boolean DEBUG_BANDWIDTH = false;
	public final static boolean DEBUG_ADD_OUTBOUND_LINK = false;
	public final static boolean DEBUG_EMPTY_PAGE = false;
	private final static boolean DEBUG_GLOBAL_QUEUE_SIZE = true;
	private final static boolean DEBUG_NUMBER_OF_CRAWLERS = false;
	private final static boolean DEBUG_PAGES_CRAWLED_PER_SECOND = true;
	private final static boolean DEBUG_PAGES_CRAWLED = false;
	private final static boolean DEBUG_CRAWLERS_SLEEPING = false;
	private final static boolean DEBUG_PAGES_REQUEST_PER_SECOND = false;
	private final static boolean DEBUG_CRAWLERS_KILLED = false;
	
		
	public static RecentDomainList recentDomainList = new RecentDomainList(MAX_SIZE_RECENT_DOMAINS);
	
	//TODO: Should have relative max size based on max memory of host machine
	//See this for reference: http://stackoverflow.com/questions/12807797/java-get-available-memory
	public static PriorityBlockingQueue<String> GLOBAL_QUEUE = new PriorityBlockingQueue<String>(100000000, new Comparator<String>(){
		public int compare(String url1, String url2){						
			
			int count_bad_characters1 = url1.length() - url1.replace("/", "").replace("=","").replace("&","").replace(":","").replace(";","").replace("%","").length();
			int count_bad_characters2 = url2.length() - url2.replace("/", "").replace("=","").replace("&","").replace(":","").replace(";","").replace("%","").length();
			
			return count_bad_characters1 - count_bad_characters2; //they both aren't recent domains
			
/*			if(recentDomainList.contains(DomainCrawler.getDomainFromURL(url1))){
				if(recentDomainList.contains(DomainCrawler.getDomainFromURL(url2))) return 0; //they both are recent domains
				else return 1; //URL2 have priority because it isn't in recent domain
			}
			else if(recentDomainList.contains(DomainCrawler.getDomainFromURL(url2))) return -1; //URL1 have priority because it isn't in recent domain*/
			
			
		}
	}); //different domains queue		
	
	
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
		long last_debug_time = System.currentTimeMillis();
		
		LinkedList<WorkerCrawler> crawlers = new LinkedList<WorkerCrawler>();
		WorkerCrawler crawler;
		
		GLOBAL_QUEUE.add("http://allrecipes.com.br/");												
				
		recentDomainList.addLast(WorkerCrawler.getDomainFromURL("http://allrecipes.com.br/"));
		
		for(int i = 0; i < MAX_SIMULTANEOUS_CRAWLERS; i++){
			crawler = new WorkerCrawler();
			crawlers.add(crawler);
			crawler.start();
		}
						
		double allBandwidth;		
		
		try{
			
		
			do{				
				
				Thread.sleep(200);
					if(DEBUG_GLOBAL_QUEUE_SIZE) System.out.println("DEBUG: GLOBAL QUEUE size: "+GLOBAL_QUEUE.size());
					if(DEBUG_NUMBER_OF_CRAWLERS) System.out.println("DEBUG: There are "+crawlers.size()+" threads");
					if(DEBUG_PAGES_CRAWLED_PER_SECOND) System.out.println("DEBUG: Pages Crawled per second: "+((double)PAGES_CRAWLED.get()*1000)/(System.currentTimeMillis() - START_TIME));
					if(DEBUG_PAGES_CRAWLED) System.out.println("DEBUG: Pages Crawled: "+PAGES_CRAWLED.get());
					if(DEBUG_CRAWLERS_SLEEPING) System.out.println("DEBUG: Sleeping Crawlers "+CRAWLERS_SLEEPING);
					if(DEBUG_PAGES_REQUEST_PER_SECOND) System.out.println("DEBUG: Pages Requested per second: "+((double)PAGES_REQUEST*1000)/(System.currentTimeMillis() - START_TIME));
					if(DEBUG_CRAWLERS_KILLED) System.out.println("DEBUG: Crawlers Killed: "+CRAWLERS_KILLED);
				
			
								
				
			}while(!crawlers.isEmpty() || !GLOBAL_QUEUE.isEmpty() || CRAWLERS_SLEEPING > 0);
		}catch(Exception e){
			e.printStackTrace();
		}
		catch(OutOfMemoryError oome){
			System.out.println("OOME");
			
		}
		System.out.println("Terminating "+crawlers.size()+" "+GLOBAL_QUEUE.size());
	}
}
