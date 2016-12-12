package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import the.dancing.company.ticketkatjuscha.exceptions.EmailTransmissionException;


public class EmailTransmitter {
	public static void transmitEmail(String emailtext, List<File> ticketFiles) throws EmailTransmissionException{
		Properties props = PropertyHandler.getInstance().getProperties();

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);
        
        try{
        	PropertyHandler propHandler = PropertyHandler.getInstance();
            message.setFrom(new InternetAddress(propHandler.getPropertyString(PropertyHandler.PROP_EMAIL_USER)));
            InternetAddress[] toAddress = new InternetAddress[]{new InternetAddress(propHandler.getPropertyString(PropertyHandler.PROP_DEFAULT_EMAIL_RECIPIENT))};

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(propHandler.getPropertyString(PropertyHandler.PROP_EMAIL_TEMPLATE_SUBJECT));
            
            //lets create a multipart message
            Multipart multipart = new MimeMultipart();
            
            //add body part
            MimeBodyPart body = new MimeBodyPart();
            body.setText(emailtext);
            multipart.addBodyPart(body);
            
            //add attachments
            for (File ticketFile : ticketFiles) {
            	MimeBodyPart attachMent = new MimeBodyPart();
                FileDataSource dataSource = new FileDataSource(ticketFile);
                attachMent.setDataHandler(new DataHandler(dataSource));
                attachMent.setFileName(ticketFile.getName());
                attachMent.setDisposition(MimeBodyPart.ATTACHMENT);
                multipart.addBodyPart(attachMent);
			}
            
            message.setContent(multipart);
            
            Transport transport = session.getTransport("smtp");
            transport.connect(propHandler.getPropertyString(PropertyHandler.PROP_EMAIL_USER), propHandler.getPropertyString(PropertyHandler.PROP_EMAIL_PASSWORD));
            transport.sendMessage(message, message.getAllRecipients());
        }
        catch (MessagingException e) {
            throw new EmailTransmissionException("jo jo jo, es geht nicht hin, wo es soll...", e);
        }
	}
}
