
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class NewsService {
	private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML,"
        + "like Gecko) Chrome/47.0.2526.106 Safari/537.36";
	private static String dateRegexp = "(<meta.+=" + "\"" + ")"
			+ "([0-9]+?)-([0-9]+?)-([0-9]+?)T([0-9]+?:[0-9]+?):";
	static Logger logger = Logger.getLogger(NewsService.class.getName());
	static LinkedHashMap <String, String> hashmapHrefAndTitle = new LinkedHashMap <String, String>();
	
	//DbAccess db=new DbAccess();
	
	static Document loadPage(String http) {
		Document doc = null;
			while (doc == null) {
				try {
					doc = Jsoup.connect(http).timeout(10000).userAgent(USER_AGENT).get();
				} catch (java.net.UnknownHostException he) {
					logger.logp(Level.WARNING, "NewsService", "mainPageLoader", "Перепідключення");
					he.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
			}
			logger.logp(Level.INFO, "NewsService", "mainPageLoader", "Підключення до головної");
		}
	return doc;
	}

	static String dataFinderNewsDate(String postBody) {
		Pattern patt = Pattern.compile(dateRegexp);
		Matcher match = patt.matcher(postBody);
		String date = "";
	
		while (match.find()) {
			date = match.group(2) + "-" + match.group(3) + "-" + match.group(4)+ " " + match.group(5) + ":00";
		}
	return date;
	}

	static String dataFinderNewsDateWithoutTime(String postBody) {
		Pattern patt = Pattern.compile(dateRegexp);
		Matcher match = patt.matcher(postBody);
		String date = "";
		
		while (match.find()) {
			date = match.group(4) + "." + match.group(3) + "." + match.group(2);
		}
	return date;
	}

	static String getNewsDate(String http) throws IOException {
		Document doc = null;
			while (doc == null) {
				try {
					doc = Jsoup.connect(http).timeout(10000).userAgent(USER_AGENT).get();
				} catch (java.net.SocketTimeoutException e) {
					logger.logp(Level.INFO, "NewsService", "getNewsDate", " Час очікування підключення вийшов");
					e.printStackTrace();
					getNewsDate(http);
				}
		}
		String data = dataFinderNewsDate(doc.select("meta[itemprop=startDate]").toString());
			return data;
	}

	String getNewsDateForHeader(String http) throws IOException {
		Document doc = null;
			while (doc == null) {
				try {
					doc = Jsoup.connect(http).timeout(10000).userAgent(USER_AGENT).get();
				} catch (java.net.SocketTimeoutException e) {
					logger.logp(Level.INFO, "NewsService", "getNewsDateForHeader", " Час очікування підключення вийшов");
					e.printStackTrace();
					getNewsDate(http);
				} catch (org.jsoup.HttpStatusException e){
					logger.logp(Level.INFO, "NewsService", "getNewsDateForHeader", " Час очікування підключення вийшов");
					e.printStackTrace();
					try {
						Thread.sleep(700);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					getNewsDate(http);
				}
		}
			
		String data = dataFinderNewsDate(doc.select("meta[itemprop=startDate]").toString());
		String dataDay = data.substring(8, 10);
		String dataMonth = data.substring(5, 7);
		String dataTime = data.substring(11, data.length() - 3);
		dataTime = dataTime.replace(":", "-");
		String generalData = dataDay + "." + dataMonth + "," + dataTime + " ";
		
		return generalData;
	}

	static String getNewsDateWithoutTime(String http) throws IOException {
		
		Document doc = null;
		String data = null;
		
		while (doc == null) {
			try {
				doc = Jsoup.connect(http).timeout(10000).userAgent(USER_AGENT).get();
				data = dataFinderNewsDateWithoutTime(doc.select("meta[itemprop=startDate]").toString());
			} catch (java.net.SocketTimeoutException e) {
				logger.logp(Level.INFO, "NewsService", "getNewsDateWithoutTime", " Час очікування підключення вийшов");
				getNewsDateWithoutTime(http);
			}
		}
			return data;
	}

	String getNewsHeader(String http) throws IOException {
		Document doc = null;
		String title = "";
		
		while (doc == null) {
			try{
				doc = Jsoup.connect(http).timeout(350).followRedirects(true).userAgent(USER_AGENT).get();
			}catch(java.net.SocketTimeoutException ex){
				ex.printStackTrace();
			}catch(org.jsoup.HttpStatusException e) {
				e.printStackTrace();
			}
		}
		// return doc.select("h1[itemprop=name]").toString();
		title = doc.title();
		title = title.replace("LiveTV", "");
		title = title.replaceAll("/", "");
		title = title.replace("Прямая трансляция  Футбол.", "");
		title = title.replace("  ", " ");
		title = title.substring(0, title.length() - 1);
		title = "Трансляция матча " + title + ". Смотреть онлайн";
		title = title.replaceAll(" [0-9]+ [а-я]+", "");
		//tested part if error don't touch
		/*
		 * title = title+" Смотреть онлайн трансляцию "; title =
		 * title.replaceAll("Прямая", "Онлайн");
		 */
	return title;
	}

	String getGuiMetaValue(String header) throws IOException {
		XmlWorker xml=new XmlWorker();
		String gui=xml.readXml(header,"guids.xml","post","postTitle","id");

		return gui;
	}

	int getTerm(String header) throws IOException {
		Integer termId;
		XmlWorker xml=new XmlWorker();
		String gui=xml.readXml(header,"terms.xml","term","termTitle","id");
		try{
			termId = Integer.valueOf(gui);
		}catch(java.lang.NumberFormatException ex){
			termId=57;
		}

		return termId;

	}

	String newsNameGenerator(String header) {
		header = header.toLowerCase();
		header = header.replace(" ", "-");
		header = header.replace(".", "-");
		header = header.replace("–", "");
		header = header.replace(",", "-");

		char[] english = { 'a', 'b', 'v', 'g', 'd', 'e', 'e', 'j', 'z', 'i',
				'k', 'l', 'm', 'n', 'o', 'p', 'r', 's', 't', 'h', 'f', 'u',
				's', 's', 'q', 'u', 'a', 'i', 'c', 'j', 'c' };

		char[] russian = { 'а', 'б', 'в', 'г', 'д', 'е', 'э', 'ж', 'з', 'и',
				'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'х', 'ф', 'у',
				'ш', 'щ', 'ь', 'ю', 'я', 'ы', 'ч', 'й', 'ц' };
		for (int i = 0; i < header.length(); i++) {
			for (int j = 0; j < russian.length; j++) {
				if (header.charAt(i) == russian[j]) {
					header = header.replace(russian[j], english[j]);

				}
			}
		}
		header = header.replaceAll("translacia", "online");
		header = header.replaceAll("matca", "");
		header = header.replaceAll("cempionat", "tournament");
		header = header.replace("---", "-");
		header = header.replace("--", "-");
		header = header.replace("-smotretq-onlajn", "");
		// header=header.replaceAll("[0-9].+", "");
			return header;
	}	

	
	String postTextGenerator(String header,String postName) throws IOException {
		
		String[] time = header.split("[А-Я].+");
		List<String> list = Arrays.asList(time);
		String playerButton =
				
				"<meta http-equiv=\"refresh\" content=\"2;URL=http://www.matchttv.ru/"+postName+"\"/>"+"\n" +
				"<a href=\"http://www.matchttv.ru/"+postName+"\"><img class=\"alignnone size-full wp-image-7714\" src=\"/wp-content/uploads/watch.jpg\" alt=\"Смотреть\" width=\"634\" height=\"355\" /></a>";
		
		String ads=
				playerButton+
				"<div id=\"MIXADV_1531\" class=\"MIXADVERT_NET\"></div>"+
				"<script type=\"text/javascript\" src=\"https://s.mixadvert.com/show/?id=1531\" async></script>";

		String mainText=ads+
				"<em><span style=\"color: #ff0000;\"><strong>Начало матча:"+ list.get(0)+"</strong></span></em>"+
		"Ссылки на плеера с трансляциями к матчу, а также ссылки на Sopcast трансляции будут доступны за 15-20 минут до начала матча."+
		"[button color=\"red\" size=\"small\" link=\"http://www.matchttv.ru/"+postName+"\" target=\"blank\" ]Смотреть трансляцию матча![/button]"+"\n";
		
	return mainText;
	}
	
	 static List<String> getStreamsUrls(){
		 	List<String> list = new ArrayList<String>();
		 	list.add("http://livetv.sx/competitions/276/");//
		 	list.add("http://livetv.sx/competitions/775/");//
			list.add("http://livetv.sx/competitions/778/");//
			list.add("http://livetv.sx/competitions/777/");//
			list.add("http://livetv.sx/competitions/196/");
		 	list.add("http://livetv.sx/competitions/408/");//biathlon
			list.add("http://livetv.sx/competitions/657/");//тов турнир
			list.add("http://livetv.sx/competitions/84/");//ч.м хокей
			list.add("http://livetv.sx/competitions/65/");//fnl
			list.add("http://livetv.sx/competitions/768/");//hz
			list.add("http://livetv.sx/competitions/930/"); //hz
			list.add("http://livetv.sx/competitions/273/"); //hz
			list.add("http://livetv.sx/competitions/418/");//hockey
			list.add("http://livetv.sx/competitions/167/");//hockey
			list.add("http://livetv.sx/competitions/43/"); // чемп укр
			list.add("http://livetv.sx/competitions/82/"); // куб нім
			list.add("http://livetv.sx/competitions/129/"); //суперкуб нім
			list.add("http://livetv.sx/competitions/36/"); //чемп нім
			list.add("http://livetv.sx/competitions/75/"); // куб італ
			list.add("http://livetv.sx/competitions/11/"); //чемп італ
			list.add("http://livetv.sx/competitions/5/"); //товарняк
			list.add("http://livetv.sx/competitions/55/"); //куб ісп
			list.add("http://livetv.sx/competitions/141/"); //суперкуб ісп
			list.add("http://livetv.sx/competitions/15/"); //чемп ісп
			list.add("http://livetv.sx/competitions/1/"); //чемп анг
			list.add("http://livetv.sx/competitions/143/"); //куб анг л
			list.add("http://livetv.sx/competitions/8/"); //куб анг
			list.add("http://livetv.sx/competitions/242/");//supercups
			list.add("http://livetv.sx/competitions/90/"); //куб рос*/
			list.add("http://livetv.sx/competitions/42/"); //чемп рос
			list.add("http://livetv.sx/competitions/900/");//q le
			list.add("http://livetv.sx/competitions/901/");//qualify
			list.add("http://livetv.sx/competitions/265/"); //ЛЄ
			list.add("http://livetv.sx/competitions/7/"); //ЛЧ
			list.add("http://livetv.sx/competitions/1250/");//EUR
			list.add("http://livetv.sx/competitions/201/");//club world
			list.add("http://livetv.sx/competitions/149/");//uefa
			
		return list;
		}
	 
	 static boolean hasTopTeamNameInString(String header){
		 System.out.println(header);
		 String [] topTeams = new String [] {
				 "Арсенал",
				 "Челси",
				 "Манчестер",
				 "Лестер",
				 "Ливерпуль",
				 "Эвертон",
				 "Бавария",
				 "Боруссия",
				 "Милан",
				 "Ювентус",
				 "Интер",
				 "Наполи",
				 "Мадрид",
				 "Барселона",
				 "Севилья",
				 "ЦСКА",
				 "Ростов",
				 "Зенит",
				 "Краснодар",
				 "Локомотив",
				 "Динамо",
				 "Рубин",
				 "Спартак",
				 "Суперкубок",
				 "Чемпионат России",
				 "Шахтер",
				 "ПСЖ",
				 "Тоттенхэм",
				 "Лига Европы",
				 "Лига Чемпионов",
				 "Чемпионат Мира",
				 "Чемпионат Европы",
				 "ЧМ-2018",
				 "КХЛ",
				 "Хоккей",
				 "Салават",
				 "Россия",
				 "Биатлон"
		 };
		 
	     for(int i=0;i<topTeams.length;i++){
    		 if(header.contains(topTeams[i])){
    			 return true;
    		 }
    	 }
	  return false;	
	 }
	 
	List<String> todayTranslations(String title,String href,String currentDateForPublishHeader){
		hashmapHrefAndTitle.put(title, href);
		
		ArrayList<String> todayHrefs = new ArrayList<String>();
		Set<Map.Entry<String, String>> set = hashmapHrefAndTitle.entrySet();
		
		for (Map.Entry<String, String> me : set) { 
			 if (me.getKey().contains(currentDateForPublishHeader)) {
				logger.logp(Level.INFO, "NewsService", "todayTranslations", "Сьогоднішні дати співавли, додано в поточний список трансляцій");
				todayHrefs.add(me.getValue());
			}else{
				logger.logp(Level.INFO, "NewsService", "todayTranslations", "Сьогоднішні дати не співавли,не додано в поточний список трансляцій");
			}
		}
	return todayHrefs; 
	}
	
	void todayTranslationsWriter(List<String> todayHrefAndTitle){
		try {
			String  content = null;
			File file = new File("todayTranslationsDesk.txt");
				if (!file.exists()) {
					file.createNewFile();
				}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
				for(int i=0;i<todayHrefAndTitle.size();i++){
					content=(String) todayHrefAndTitle.get(i);
					bw.write(content+"\n");
				}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	static String  timeCutter(String title){
		title=title.substring(11,title.length());
	return title;
	}
	
	void deleteSameNewsMaker() throws SQLException{
		DbAccess db = new DbAccess();
		ArrayList<String> cuttedTitles = new ArrayList<String>();
		LinkedHashMap<String, Integer> newsPost = new  LinkedHashMap<String,Integer>();
		List<String> allTranslationTitles = new ArrayList<String>();
		allTranslationTitles=db.sameTitlesFinder();
		for (int i = 0; i < allTranslationTitles.size(); i++) {
			String newTitle=timeCutter(allTranslationTitles.get(i));
			cuttedTitles.add(newTitle);
		}
		
		for(int i=0;i<cuttedTitles.size();i++){
			for(int j=i+1;j<cuttedTitles.size();j++){
				if(cuttedTitles.get(i).equals(cuttedTitles.get(j))){
					
					db.sameTitlesContent(cuttedTitles.get(i),newsPost);
				}
			}
		}
		
		Set<Entry<String, Integer>> set = newsPost.entrySet();
		String header;
		Integer id;
		ArrayList<String> headers= new ArrayList<String>();
		ArrayList<Integer> ids= new ArrayList<Integer>();
		for (Entry<String, Integer> me : set) {
			header=me.getKey(); 
			headers.add(header);
			id=me.getValue();
			ids.add(id);
		}
		for(int i=0;i<headers.size()-1;i++){
			logger.logp(Level.INFO, "NewsService", "deleteSameNews","The same news"+"Head "+timeCutter(headers.get(i)) +"id "+ ids.get(i));
			if(timeCutter(headers.get(i)).equals(timeCutter(headers.get(i+1)))){
				if(ids.get(i)>ids.get(i+1)){
					db.deleteSameNews(ids.get(i));
				}else if(ids.get(i+1)>ids.get(i)){
					db.deleteSameNews(ids.get(i+1));
				}
			}
		}
	}
}