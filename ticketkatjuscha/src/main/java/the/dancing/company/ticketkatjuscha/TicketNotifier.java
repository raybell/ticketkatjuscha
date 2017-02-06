package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.javatuples.Pair;

import the.dancing.company.ticketkatjuscha.EmailTemplate.TEMPLATES;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.exceptions.EmailTransmissionException;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;

/**
 * Handle ticket related notifications.
 * 
 * @author marcel
 */
public class TicketNotifier {
	public static enum NOTIFICATION_TYPE{
		PAYMENT_NOTIFICATION("payment reminder"),
		TICKET_REVOCATION("ticket revocation");
		
		private String name;
		NOTIFICATION_TYPE(String name){
			this.name = name;
		}
		
		public String getName(){
			return name;
		}
	};
	
	private ITicketProcessFailed failureHandler;
	private String lastRecipient;
	
	public TicketNotifier(ITicketProcessFailed failureHandler){
		this.failureHandler = failureHandler;
	}

	public boolean sendNotification(List<Pair<String, String>> seats, NOTIFICATION_TYPE notificationType){
		lastRecipient = null;
		try {
			//load ticket codes
			ICodeListHandler codeListHandler = CodeListHandlerFactory.produceHandler();
			Map<String, CodeData> codeList = null;
			try {
				codeList = codeListHandler.loadCodeList();
			} catch (IOException e) {
				return terminateWithError(e.getMessage(), e);
			}
			
			//get assigned email addresses
			Map<String, CodeData> foundEmails = new HashMap<>();
			Set<CodeData> foundCodes = new HashSet<>();
			for (CodeData codeData : codeList.values()) {
				String seat = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT);
				boolean withdrawed = codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_WITHDRAWED);
				if (!withdrawed && seats.containsAll(SeatTokenizer.parseSeats(seat))){
					String email = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_EMAIL);
					if (email != null && email.trim().length() > 0){
						foundEmails.put(email, codeData);
						foundCodes.add(codeData);
					}else{
						return terminateWithError("seat " + seat + " does not have a valid email address", null);
					}
				}
			}
			
			if (foundEmails.size() == 0){
				//no emails found
				return terminateWithError("no email addresses found", null);
			}

			if (foundEmails.size() > 1){
				//ambigious email addresses found
				return terminateWithError("found more than one email address associated with the seats. please check the codelist.", null);
			}
			
			//generate email text and send email
			CodeData codeData = foundEmails.entrySet().iterator().next().getValue();
			String emailtext = "";
			
			try {
				switch(notificationType){
					case PAYMENT_NOTIFICATION:
						emailtext = EmailTemplate.loadTemplate(TEMPLATES.NOTIFICATION_TEMPLATE).evaluateEmailText(codeData.getName(), seats.size());
						break;
					case TICKET_REVOCATION:
						emailtext = EmailTemplate.loadTemplate(TEMPLATES.REVOCATION_TEMPLATE).evaluateEmailText(codeData.getName(), seats.size());
						break;
				}
				String recipient = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_EMAIL);
				EmailTransmitter.transmitEmail(emailtext, null, recipient);
				lastRecipient = recipient;
				
				//message specific post-processing
				switch(notificationType){
					case PAYMENT_NOTIFICATION:
						for (CodeData code : foundCodes) {
							code.getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_PAYMENT_REMINDER,
									"" + (code.getAdditionalCodeData().getDataAsInt(ADDITIONAL_DATA.TICKET_PAYMENT_REMINDER) + 1));
						}
						codeListHandler.saveCodeList(codeList);
						break;
					case TICKET_REVOCATION:
						for (CodeData code : foundCodes) {
							code.getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_WITHDRAWED, "true");
						}
						codeListHandler.saveCodeList(codeList);
						break;
				}
			} catch (IOException | EmailTransmissionException e) {
				return terminateWithError("Beim Versenden der eMail ist ein unerwarteter Fehler aufgetreten: " + e.getMessage(), e);
			} 
			return true;
		} catch (NoSuchElementException e1) {
			return terminateWithError("NoNoNo, so nicht. Das sind ja ganz merkwürdige Sitze.", e1);
		}
	}
	
	public String getLastRecipient(){
		return this.lastRecipient;
	}
	
	private boolean terminateWithError(String message, Exception e){
		System.err.println(message);
		if (e != null){
			System.err.println("Info: " + e.toString());
			e.printStackTrace(System.err);
		}
		
		return failureHandler.handleFailedState(message, e);
	}
}
