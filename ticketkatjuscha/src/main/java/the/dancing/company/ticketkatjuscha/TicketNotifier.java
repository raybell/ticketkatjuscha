package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
	private PrintStream logWriter;
	private String lastRecipient;

	public TicketNotifier(ITicketProcessFailed failureHandler, PrintStream logWriter){
		this.failureHandler = failureHandler;
		this.logWriter = logWriter;
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
					}else{
						//no email found, use default
						foundEmails.put(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_DEFAULT_EMAIL_RECIPIENT), codeData);
//						return terminateWithError("seat " + seat + " does not have a valid email address", null);
					}
					foundCodes.add(codeData);
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
			String subject = "";

			int price = codeData.getAdditionalCodeData().getDataAsInt(ADDITIONAL_DATA.TICKET_PRICE);
			if (price <= 0){
				price = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_TICKET_PRICE);
			}
			String bookingNumber = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_BOOKINGNUMBER);

			try {
				switch(notificationType){
					case PAYMENT_NOTIFICATION:
						subject = PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_EMAIL_NOTIFICATION_TEMPLATE_SUBJECT);
						emailtext = EmailTemplate.loadTemplate(TEMPLATES.NOTIFICATION_TEMPLATE).evaluateEmailText(codeData.getName(), foundCodes.size(), null, price, bookingNumber);
						break;
					case TICKET_REVOCATION:
						subject = PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_EMAIL_REVOCATION_TEMPLATE_SUBJECT);
						emailtext = EmailTemplate.loadTemplate(TEMPLATES.REVOCATION_TEMPLATE).evaluateEmailText(codeData.getName(), foundCodes.size(), makeSeatList(foundCodes), price, bookingNumber);
						break;
				}
				String recipient = foundEmails.keySet().iterator().next(); //codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_EMAIL);
				EmailTransmitter.transmitEmail(emailtext, null, recipient, subject);
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
						//free seat in plan
						new SeatPlanHandler(logWriter).freeSeats(seats);
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
		log(message);
		if (e != null){
			log("Info: " + e.toString());
			e.printStackTrace(logWriter);
		}

		return failureHandler.handleFailedState(message, e);
	}

	private String makeSeatList(Set<CodeData> ticketCodes){
		StringBuffer sb = new StringBuffer();

		//sort by seat
		Iterator<CodeData> ticketCodesIt = ticketCodes.stream()
		                                              .sorted((t1, t2) -> t1.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT)
		                                                      .compareTo(t2.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT))).iterator();
		while (ticketCodesIt.hasNext()){
			CodeData codeData = ticketCodesIt.next();
			if (sb.length() > 0){
				sb.append(", ");
			}
			sb.append(codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT));
			sb.append(" (Code: \"");
			sb.append(codeData.getCode());
			sb.append("\"; Checkcode: \"");
			sb.append(codeData.getCheckCode());
			sb.append("\")");
		}

		return sb.toString();
	}
	
	private void log(String message){
		if (this.logWriter != null){
			logWriter.println(message);
		}
	}

}
