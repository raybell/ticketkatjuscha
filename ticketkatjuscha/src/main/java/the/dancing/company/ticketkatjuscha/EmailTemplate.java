package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;

public class EmailTemplate {
	private static String TEMPLATE_PLACEHOLDER_NAME = "%%name%%";
	private static String TEMPLATE_PLACEHOLDER_PRICE = "%%price%%";
	
	private String templateText;
	
	public static EmailTemplate loadTemplate() throws IOException{
		String emailTemplateFile = PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_EMAIL_TEMPLATE_FILE);
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
	
	public String evaluateEmailText(String recipientName, int ticketAmount){
		return this.templateText.replaceAll(TEMPLATE_PLACEHOLDER_NAME, StringUtils.splitByWholeSeparator(recipientName, StringUtils.SPACE)[0].trim())
		                        .replaceAll(TEMPLATE_PLACEHOLDER_PRICE, "" + PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_TICKET_PRICE) * ticketAmount);
	}
	
}
