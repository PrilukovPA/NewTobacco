package newtobacco;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.mail.EmailException;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.FileAppender;
import oracle.sql.BLOB;


class NewTobacco {
	
	private static final String MASTERKEY = "masterkey";
	private static final String OPERATION_DOWLOAD = "download";
	private static final String OPERATION_UPLOAD = "upload";
	
	private static final Logger log = LoggerFactory.getLogger("ru.evenx.logback");
	
	private Settings settings = null;
	private Mailer logMailer = new Mailer();
	private String period = getOrdersPeriod();
	
	private String getOrdersPeriod() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, -1);
		Date yesteray = c.getTime();		
		//return dateFormat.format(yesteray) + "-" + dateFormat.format(new Date());
		return "20190101-20190130";
	}
	
	public void readSettings(File settingsFile) throws NewTobaccoException {	      
		try {
        	JAXBContext jc = JAXBContext.newInstance(Settings.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
			settings = (Settings) unmarshaller.unmarshal(settingsFile);
		} catch (JAXBException e) {
			throw new NewTobaccoException("Read settings exception", e);
		}
	}	
	
	public void doWork() throws Exception {		
		logMailer.setSettings(settings.logmail);		
		switch (settings.operation.value.toLowerCase()) {
			case OPERATION_DOWLOAD: 
				upload();
				//download();
				break;	
			case OPERATION_UPLOAD: 
				upload();
				break;
			default:
				throw new NewTobaccoException("Unknown operation in settings file");
		}
	}
	
	private void download() throws Exception {		
		org.apache.camel.main.Main main = new org.apache.camel.main.Main();		
		main.bind("newTobaccoDataSource", setupDataSource());
		main.addRouteBuilder(new GetOrdersRoute());
        main.run();
	}	
	
	private class GetOrdersRoute extends RouteBuilder {
		
        public void configure() throws NewTobaccoException {        	
            from("timer://runOnce?repeatCount=1&delay=1000")
            	.setHeader(Exchange.HTTP_METHOD, simple("GET"))
            	.setHeader(Exchange.CONTENT_TYPE, simple("text/xml; charset=utf-8"))
            	.to("http4://"+ settings.gateway.url + "?token=" + decryptPass(settings.gateway.token) + "&period=" + period)
                .convertBodyTo(String.class)
                .setHeader("xml", body())
                .to("sql-stored:NewTobacco.AcceptOrders(CLOB ${headers.xml})");
        }
	}
	
	private void upload() throws Exception {
		org.apache.camel.main.Main main = new org.apache.camel.main.Main();		
		main.bind("newTobaccoDataSource", setupDataSource());
		main.addRouteBuilder(new SendReportRoute());
        main.run();
	}
	
	private class SendReportRoute extends RouteBuilder {
		
        public void configure() throws NewTobaccoException {        	
            from("timer://runOnce?repeatCount=1&delay=1000")
            	.to("sql-stored:NewTobacco.camel_test()")
            	.setBody(constant("SELECT C2 ORDERID, BL1 FILEBODY FROM UNIVERSALTABLE WHERE C1 = 'NewTobacco'"))
            	.to("jdbc:newTobaccoDataSource?useGetBytesForBlob=true")
            	.split(body()) // потому что сюда приходит список мэпов и нужно его разбить на отдельные мэпы.
            	.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						@SuppressWarnings("unchecked")
						Map<String, Object> record = exchange.getIn().getBody(Map.class);
						String fileName = (String) record.get("ORDERID") + ".xlsx";
						byte[] file = (byte[]) record.get("FILEBODY");
						exchange.getIn().setBody(file);
						exchange.getIn().setHeader("fileName", fileName);
					}
            	
            	})
            	.to("file:data?fileName=${headers.fileName}");
        }
	}
	
	public static String decryptPass(String pass) throws NewTobaccoException {		
		BasicTextEncryptor encryptor = new BasicTextEncryptor();
		encryptor.setPassword(MASTERKEY);
		return encryptor.decrypt(pass);
	}
	
	public void sendLog(String subjSuffix) {		
		FileAppender<?> fa = (FileAppender<?>) ((ch.qos.logback.classic.Logger) log).getAppender("file");
		String fileName = fa.getFile();
		try {
			logMailer.addAttachment(new File(fileName));
			logMailer.send(subjSuffix);
		} catch (EmailException e) {
			log.error("Send mail exception", e);
		}
	}
	
	public static DataSource setupDataSource() throws NewTobaccoException { 
		try {
			Properties props = new Properties(); 
			try (InputStream in = NewTobacco.class.getClassLoader().getResourceAsStream("database.properties"))			
			{ 
				props.load(in); 
			}	 
			String drivers = props.getProperty("jdbc.drivers"); 
			if (drivers != null) { 
				System.setProperty("jdbc.drivers", drivers);
			}
			BasicDataSource ds = new BasicDataSource();
		    ds.setDriverClassName(drivers);
		    ds.setUsername(props.getProperty("jdbc.username"));
		    ds.setPassword(decryptPass(props.getProperty("jdbc.password")));
		    ds.setUrl(props.getProperty("jdbc.url"));
		    
		    return ds;
		    
		} catch (IOException e) {
			throw new NewTobaccoException("DB connection exception", e);
		}
	 } 
}























