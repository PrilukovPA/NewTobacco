package newtobacco;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * �������������� ���������, ������������ ������� ������� ���� � ������ Hills Staff Feeding
 * ������ ���������� ��������� ������ � ������ ���������
 * � ������������ � ������������ ����� ��������.
 *
 */
@Command(name = "New Tobacco", mixinStandardHelpOptions = true, version = "1.0")
public class Main implements Runnable
{
	@Option(names = { "-S", "--settings" }, required = true, paramLabel = "<xml file>", description = "Load settings xml-file")
	private File settings;
	
    private static final Logger log = LoggerFactory.getLogger("ru.evenx.logback");
	
	public static void main( String[] args )
    {
		CommandLine.run(new Main(), args);
    }

	public void run() {

		if (!settings.exists()) {
			log.error("Settings file not found");
			return;
		}
		
		String subjSuffix = ": OK";
		
		NewTobacco newTobacco = new NewTobacco();
		try {
			newTobacco.readSettings(settings);
			newTobacco.doWork();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			subjSuffix = ": ERROR";
			
		} finally {
			newTobacco.sendLog(subjSuffix);
		}
	}
}