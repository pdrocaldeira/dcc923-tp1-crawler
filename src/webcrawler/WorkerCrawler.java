package webcrawler;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicInteger;

import com.chilkatsoft.CkSpider;
import com.chilkatsoft.CkString;

public class WorkerCrawler extends Thread {
	private boolean kill = false;	
	private int runs = 0;
	private double bytes_downloaded = 0; //GET OUT OF THERE IS GONNA EXPLODE <..Terrorist Wins>
	public final long creation_time;	
	private double bandwidth = 0;
	
	private static AtomicInteger num_files = new AtomicInteger();
	private static AtomicInteger html_per_file = new AtomicInteger();
	private static StringBuffer buffer = new StringBuffer("||| ");
	
	private static void addHTML(String url, String html){
		if(html_per_file.get() < 100){
			buffer.append(url+" | "+html+" ||| ");
			html_per_file.incrementAndGet();
		}
		else{
			PrintWriter writer;
			try {
				writer = new PrintWriter("html/html"+num_files, "UTF-8");
				writer.print(buffer.toString());
				writer.close();
				html_per_file = new AtomicInteger();
				num_files.incrementAndGet();
				buffer = new StringBuffer("||| ");
			} catch (FileNotFoundException | UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}						
		}
	}
	
	public static String getDomainFromURL(String url){
		if(url == null) return null;
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
	
	public WorkerCrawler(){			
		this.creation_time = System.currentTimeMillis();		
	}
	
	@Override
	public void run() {
		CkSpider spider = null;
		String current_domain = null;
		String html, url, next_domain, next_url;
		int domain_try = 0;
		int runs_in_this_domain = 0;
		CkString error = new CkString();
		
		do{
			if(spider != null && spider.lastHtml().length() == 0 && domain_try < 4) {
				try {
					domain_try++;
					System.out.println("DEBUG: Recrawling "+spider.lastUrl());
					sleep(1000);
					spider.RecrawlLast();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
			}
			else if(Main.GLOBAL_QUEUE.isEmpty() || (runs_in_this_domain < 30 && runs_in_this_domain > 1) && domain_try < 4){
				try {
					Main.CRAWLERS_SLEEPING++;
					sleep(1000); //it should wait these seconds before make another request to this domain
					Main.CRAWLERS_SLEEPING--;
					if(spider != null){
						spider.CrawlNext();
						runs_in_this_domain++;
					} 
				} catch (InterruptedException e) {					
					return; //KILLING THIS THREAD
				}				
			}				
			else{
				
				do{
					spider = new CkSpider();					

					spider.put_ConnectTimeout(1);
					next_url = Main.GLOBAL_QUEUE.poll();
					spider.Initialize(next_url);
					current_domain = getDomainFromURL(next_url);
					//System.out.println("DEBUG: Crawling "+next_url+" DOMAIN: "+current_domain);
					
					domain_try = 0;
					spider.CrawlNext();
					runs_in_this_domain=1;
					
					
				}while(spider.lastHtml().length() == 0 && Main.GLOBAL_QUEUE.size() > Main.MAX_SIMULTANEOUS_CRAWLERS);									

			}
						
			if(spider != null){
				if(Main.DEBUG_EMPTY_PAGE && spider.lastHtml().length() == 0) System.out.println("DEBUG: Empty page ("+spider.lastUrl()+")"); 						
				
				this.bytes_downloaded += spider.lastHtml().length()*8;					
				
			    html = spider.lastHtml().replace("|", " "); // Saves HTML	
			    
			    if(spider.lastHtml().length() > 0) {
					
					Main.PAGES_CRAWLED.incrementAndGet();
					addHTML(spider.lastUrl(), html);
					
				}
			    if(Main.GLOBAL_QUEUE.size() < 900000){
				    int num_outbound_links = spider.get_NumOutboundLinks();
					
					for (int i = 0; i < num_outbound_links; i++) {
						url = spider.getOutboundLink(i);
						
						if(url != null){
							String add_domain = getDomainFromURL(url);
							if(!Main.recentDomainList.contains(add_domain) || Main.GLOBAL_QUEUE.size() < Main.MAX_SIMULTANEOUS_CRAWLERS ){
								Main.GLOBAL_QUEUE.add(url);
								Main.recentDomainList.add(getDomainFromURL(url));						
								if(Main.DEBUG_ADD_OUTBOUND_LINK) System.out.println("DEBUG: Adding URL to queue "+url);
							}					
						}				
					}
			    }
			}
			
		}while(!kill);
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
