import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileWorker {
	private String siteName;
	
	public FileWorker(String siteName) {
		this.setSiteName(siteName);
		System.out.println(siteName);
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	List<String> bdSiteReader(){
	List<String> cookies = new ArrayList<String>();
		try{ 
			BufferedReader br = new BufferedReader(new FileReader("footlivehd"+siteName+".txt"));
			String sCurrentLine;
			
			while ((sCurrentLine = br.readLine()) != null) {
				cookies.add(sCurrentLine);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(cookies);
	return cookies;	
	}
	
	/*siteCookies(){
		List<String> cookies = new ArrayList<String>();
		cookies=bdSiteReader();
		
	}*/
	
}