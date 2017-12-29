import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Date;

public class Do {

	private static ArrayList<String> postedHeaders = new ArrayList<String>();
	private static List<String> currentDbTranslationList = null;
	private static int dbPostCounter = 0;
	private static String dateTodayForTranslationPublish;
	private static String yesterdayDateForTranslationPublish;
	private static Logger logger = Logger.getLogger(Do.class.getName());
	private static List<String> todayHrefAndTitle = new ArrayList<String>();
	private static List<Integer> yesterdayMainPageNews = new ArrayList<Integer>();
	

	static void partThree(NewsService newsService) throws IOException, SQLException, ParseException, InterruptedException{
		
		System.out.println("Enter title");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String title = in.readLine();
		System.out.println("Enter site number");
		BufferedReader inp = new BufferedReader (new InputStreamReader(System.in));
		String siteNumber = inp.readLine();
		
		FileWorker file = new FileWorker(siteNumber);
		List<String> cookies = new ArrayList<String>();
		cookies=file.bdSiteReader();
		
		DbAccess dbWorker = new DbAccess(cookies.get(0),cookies.get(1),cookies.get(2));
		
		String metaValueThumbnail = newsService.getGuiMetaValue(title);
		int termTaxonomyId = newsService.getTerm(title);
		
		dbWorker.selectTranslationDetails(title,metaValueThumbnail,termTaxonomyId);
		
	}
	
	public static void main(String[] args) throws IOException, ParseException,ClassNotFoundException, InterruptedException, SQLException {
		/*System.out.println("publish on demand ?");
		BufferedReader input = new BufferedReader (new InputStreamReader(System.in));
		String action = input.readLine();
		if(action.equals("y")){
			NewsService newsService = new NewsService();
			partThree(newsService);
		}
		
		System.out.println("2.Enter site number");
		
		BufferedReader inp = new BufferedReader (new InputStreamReader(System.in));
		String siteNumber = inp.readLine();
		FileWorker file = new FileWorker(siteNumber);
		List<String> cookies = new ArrayList<String>();
		cookies=file.bdSiteReader();
		DbAccess dbWorker = new DbAccess(cookies.get(0),cookies.get(1),cookies.get(2));
		NewsService newsService = new NewsService();
			partOne(dbWorker, newsService);
		*/
		String site = "1";
		FileWorker file = new FileWorker(site);
		List<String> cookies = new ArrayList<String>();
		cookies=file.bdSiteReader();
		DbAccess dbWorker = new DbAccess(cookies.get(0),cookies.get(1),cookies.get(2));
		NewsService newsService = new NewsService();
			partOne(dbWorker, newsService);
		
			
	}

	static void partOne(DbAccess dbWorker, NewsService newsService)
			throws InterruptedException, IOException, ParseException,
			SQLException { 
		List<String> streamUrlsList = NewsService.getStreamsUrls();
		dbWorker.deleteTrash();
		for (int listHrefs = 0; listHrefs < streamUrlsList.size(); listHrefs++) {
			//Thread.sleep(100);
			dateTodayForTranslationPublish = DateFormator.formatDate(new Date(), DateFormator.ShortDateFormat);
			yesterdayDateForTranslationPublish = DateFormator.formatDate(DateFormator.substractDaysFromToday(-1),
					DateFormator.ShortDateFormat);
			
			logger.logp(Level.INFO, "Parser", "main", "Yesterday's date news "
					+ yesterdayDateForTranslationPublish);
			logger.logp(Level.INFO, "Parser", "main", "Today date news "
					+ dateTodayForTranslationPublish);
			
			Document doc = NewsService.loadPage(streamUrlsList.get(listHrefs));
			Elements newsHref = doc.select("table[class=main]");
			Elements hrefs = newsHref.select("a[class=live]");
			currentDbTranslationList = (dbWorker.selectTranslationQuery());
			

			// href
			String href = "http://livetv.sx";
			for (Element hrefsNews : hrefs) {
				href += hrefsNews.attr("href");
				Thread.sleep(400);
				String newsHeader = newsService.getNewsDateForHeader(href)
						+ newsService.getNewsHeader(href);
				// logger.logp(Level.INFO, "Parser", "main", "Посилання" +
				// href);
				// logger.logp(Level.INFO, "Parser", "main",
				// "Заголовок:"+newsHeader);
				todayHrefAndTitle = newsService.todayTranslations(newsHeader,
						href, dateTodayForTranslationPublish);
				System.out.println(todayHrefAndTitle);
				// перевірка БД на опублікованість і вставка
				for (int i = 0; i < currentDbTranslationList.size(); i++) {
					
					boolean isTopTeam = NewsService.hasTopTeamNameInString(newsHeader);
					if(isTopTeam){
						if (currentDbTranslationList.contains(newsHeader)) {
							break;
						} else if (i == currentDbTranslationList.size() - 1) {
							logger.logp(Level.INFO, "Parser", "main",
									"Заголовок новини не співпав з новиною в БД, Публікую:"
											+ newsHeader);
							String postName = newsService.newsNameGenerator(newsHeader);
							logger.logp(Level.INFO, "Parser", "main", "PostName:"
									+ postName);
						
							dbWorker.insertTranslationQuery(
									newsService.postTextGenerator(newsHeader,postName),
									newsHeader, postName);
							dbPostCounter++;
							postedHeaders.add(newsHeader);
							}
					}
				}
				// href
				href = "http://livetv.sx";
				logger.logp(Level.INFO, "Parser", "main", "------");
			}
			logger.logp(Level.INFO, "Parser", "main","Перевірка на опублікованість завершена ");
			long maxId = dbWorker.selectMaxID();
			
			long idCounter = maxId - dbPostCounter + 1;
			// seo thumb term

			if (dbPostCounter > 0) {
				for (int i = 0; i < postedHeaders.size(); i++, idCounter++) {
					String metaValueThumbnail = newsService
							.getGuiMetaValue(postedHeaders.get(i));
					int termTaxonomyId = newsService.getTerm(postedHeaders
							.get(i));

					dbWorker.insertThumbnail(idCounter, metaValueThumbnail);
					dbWorker.insertTerm(idCounter, termTaxonomyId);
					dbWorker.insertSeo(idCounter, postedHeaders.get(i));
				}
			}
		}

		dbWorker.deleteAllNewsFromTop();
		int todayTaxonomy=54;
		dbWorker.deleteTaxonomy(todayTaxonomy);
		// публікація на головну TOP
		for (int i = 0; i < currentDbTranslationList.size(); i++) {
			int postedId = dbWorker.postedId(currentDbTranslationList.get(i));
			
			dbWorker.mainPagePublishCurrentDate(currentDbTranslationList.get(i),
					dateTodayForTranslationPublish, postedId, dbWorker);

		}
		// write hrefs to a file
		//newsService.todayTranslationsWriter(todayHrefAndTitle);
		// delete yesterday's news
		yesterdayMainPageNews = dbWorker.getYesterdayNews(yesterdayDateForTranslationPublish);
		for (int i = 0; i < yesterdayMainPageNews.size(); i++) {
			dbWorker.deleteYesterdayNewsFromMainPage(yesterdayMainPageNews.get(i));

		}

		// remove all yesterday players
		dbWorker.deleteYesterdayPlayers(yesterdayDateForTranslationPublish);

		// remove the same news
		//newsService.deleteSameNewsMaker();
		System.out.println(postedHeaders);
		System.out.println(todayHrefAndTitle);
		System.out.println(dateTodayForTranslationPublish);
		
	}	
}