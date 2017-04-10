package webcrawler;

import java.util.LinkedList;

import com.chilkatsoft.CkSpider;

public class DomainCrawler extends Thread {

	private boolean kill = false;
	private CkSpider spider;
	private int runs = 0;
	private double bytes_downloaded = 0;
	public final long creation_time;
	public final String domain;
	
	public static String getDomainFromURL(String url){
		try{
			int beginIndex = url.indexOf('.')+1;
			int endIndex = url.indexOf('/', url.indexOf('.')+1);
			if(endIndex > 0) return url.substring(beginIndex, endIndex);
			else return url.substring(beginIndex);
		}	
		catch(StringIndexOutOfBoundsException stringIndexOutOfBoundsException){
			System.out.println("URL ERROR DOMAIN: "+url);
			stringIndexOutOfBoundsException.printStackTrace();
			return null;
		}
	}
	
	public DomainCrawler(String url){
		this.spider = new CkSpider();
		this.spider.Initialize(url);	
		this.creation_time = System.currentTimeMillis();
		this.domain = getDomainFromURL(url);
	}
	
	@Override
	public void run() {
		String html, url;
		int size, char_count;
		
		while(kill == false){
			
			long beforeCrawl = System.currentTimeMillis();
			if(spider.lastHtml().length() != 0 || this.runs == 0) spider.CrawlNext(); // bool return
			else spider.RecrawlLast();
			
			//System.out.println("Time Elapsed to crawl: "+((double)(System.currentTimeMillis()- beforeCrawl))/1000);
			
			Main.PAGES_REQUEST++;
			this.runs++;
			
			this.bytes_downloaded += spider.lastHtml().length()*8;
			
			if(Main.DEBUG_EMPTY_PAGE && spider.lastHtml().length() == 0) System.out.println("DEBUG: Empty page ("+spider.lastUrl()+")"); 
			
		    html = spider.lastHtml().replaceAll("\n", " ").replaceAll("|", " "); // Saves HTML		    		    
		    
		    int num_outbound_links = spider.get_NumOutboundLinks();
			String domain;
			for (int i = 0; i < num_outbound_links; i++) {
				url = spider.getOutboundLink(i);
				
				if(url != null){
					Main.GLOBAL_QUEUE.add(url);
					if(Main.DEBUG_ADD_OUTBOUND_LINK) System.out.println("DEBUG: Adding URL to queue "+url);
				}				
			}
			
			if(spider.lastHtml().length() > 0) {
				
				Main.PAGES_CRAWLED++;
			}
			if(spider.get_NumUnspidered() == 0) this.kill = true;
			else{
				try {
					Main.CRAWLERS_SLEEPING++;
					int random_extra_time = (int) Math.ceil(Math.random()*10000);
					sleep(30000+random_extra_time);
					Main.CRAWLERS_SLEEPING--;
				} catch (InterruptedException e) {
					
					//e.printStackTrace();
					return;
				}
			}
			
		}
		
	}
	
	public void kill(){
		this.kill = true;
	}	
	
	public double getBandwidth(){
		if(this.bytes_downloaded == 0) return 0; //Need this code because some times System.currentTimeMillis() == this.creation_time and we get division per 0
		return this.bytes_downloaded/((System.currentTimeMillis() - this.creation_time) * 1000);
	}
	
	public int getRuns(){
		return this.runs;
	}
	

}
