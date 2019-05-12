package newtobacco;

import java.io.File;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailAttachment;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;

public class Mailer {
	
	private MultiPartEmail email = new MultiPartEmail();
	
	public void setSettings(MailSettings settings) throws EmailException, NewTobaccoException {

		email.setHostName(settings.host);
        email.setSmtpPort(Integer.parseInt(settings.port));
        email.setSSLOnConnect(Boolean.parseBoolean(settings.ssl));
        
        if (Boolean.parseBoolean(settings.auth)) {
        	email.setAuthenticator(new DefaultAuthenticator(settings.user, NewTobacco.decryptPass(settings.pass)));
        }

        email.setFrom(settings.from);
        for (Recipient recipient: settings.recipients) {
        	email.addTo(recipient.value);
        }

        email.setSubject(settings.subject);
	}
	
	public void addAttachment(File file) throws EmailException {
		EmailAttachment attachment = new EmailAttachment();
        attachment.setPath(file.getAbsolutePath());
        attachment.setDisposition(EmailAttachment.ATTACHMENT);
        email.attach(attachment);
	}
	
	public void send(String subjSuffix) throws EmailException {
		email.setSubject(email.getSubject() + subjSuffix);
		email.send();
	}
	
	public void send() throws EmailException {
		email.send();
	}
}
