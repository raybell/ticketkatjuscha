package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;

public class EmailTemplate {
	private static String TEMPLATE_PLACEHOLDER_NAME = "%%name%%";
	private static String TEMPLATE_PLACEHOLDER_PRICE = "%%price%%";
	private static String TEMPLATE_PLACEHOLDER_SEATS = "%%seats%%";
	private static String TEMPLATE_PLACEHOLDER_BOOKING_NUMBER = "%%bookingNumber%%";

	public static enum TEMPLATES {
		TICKET_TEMPLATE(PropertyHandler.PROP_EMAIL_TEMPLATE_FILE),
		NOTIFICATION_TEMPLATE(PropertyHandler.PROP_EMAIL_NOTIFICATION_TEMPLATE_FILE),
		REVOCATION_TEMPLATE(PropertyHandler.PROP_EMAIL_REVOCATION_TEMPLATE_FILE);

		private String prop;

		TEMPLATES(String prop){
			this.prop = prop;
		}

		String getProp(){
			return prop;
		}
	}

	private String templateText;

	public static EmailTemplate loadTemplate(TEMPLATES template) throws IOException{
		String emailTemplateFile = PropertyHandler.getInstance().getPropertyString(template.getProp());
		if (emailTemplateFile == null){
			throw new IllegalArgumentException("missing template configuration");
		}
		File f = new File(emailTemplateFile);
		if (f.exists()){
			return new EmailTemplate(new String(Files.readAllBytes(f.toPath()), "ISO8859-1"));
		} else{
			throw new IOException("template file '" + f.getAbsolutePath() + "' not found");
		}
	}

	public EmailTemplate(String templateText){
		this.templateText = templateText;
	}

	public String evaluateEmailText(String recipientName, int ticketAmount, String seats, int price, String bookingNumber){
		return this.templateText.replaceAll(TEMPLATE_PLACEHOLDER_NAME, StringUtils.splitByWholeSeparator(recipientName, StringUtils.SPACE)[0].trim())
		                        .replaceAll(TEMPLATE_PLACEHOLDER_PRICE, "" + (price * ticketAmount))
		                        .replaceAll(TEMPLATE_PLACEHOLDER_SEATS, seats == null ? "" : seats)
		                        .replaceAll(TEMPLATE_PLACEHOLDER_BOOKING_NUMBER, bookingNumber);
	}

}
