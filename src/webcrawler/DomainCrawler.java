package webcrawler;

import java.util.LinkedList;

import com.chilkatsoft.CkSpider;

public class DomainCrawler extends Thread {

	private boolean kill = false;
	private CkSpider spider;
	private int runs = 0;
	private double bytes_downloaded = 0;
	private long creation_time;
	
	public DomainCrawler(String domain){
		this.spider = new CkSpider();
		this.spider.Initialize(domain);	
		this.creation_time = System.currentTimeMillis();
	}
	
	@Override
	public void run() {
		String html, url;
		int size, char_count;
		
		while(kill == false){
			if(spider.lastHtml().length() != 0 || this.runs == 0) spider.CrawlNext(); // bool return
			else spider.RecrawlLast();
			
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
			
			this.runs++;
			if(spider.get_NumUnspidered() == 0) this.kill = true;
			else{
				try {
					sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
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
