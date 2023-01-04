package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.javatuples.Pair;

import the.dancing.company.ticketkatjuscha.EmailTemplate.TEMPLATES;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.exceptions.EmailTransmissionException;
import the.dancing.company.ticketkatjuscha.util.ProcessFeedback;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

/**
 * Handle ticket related notifications.
 *
 * @author marcel
 */
public class TicketNotifier {
	private ITicketProcessFailed failureHandler;
	private ProcessFeedback feedback;
	private String lastRecipient;

	public TicketNotifier(ITicketProcessFailed failureHandler, ProcessFeedback feedback){
		this.failureHandler = failureHandler;
		this.feedback = feedback;
	}
	
	public boolean sendReminderNotification(String bookingNumber){
		File paymentListFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PAYMENT_LIST_FILE));
		
		FileInputStream fis = null;
		Workbook wb = null;
		try{
			if (!paymentListFile.exists()){
				TicketPaymentHandler.makeInitialPaymentList();
			}
			fis = new FileInputStream(paymentListFile);
			wb = WorkbookFactory.create(fis);
			
			Sheet sheet = wb.getSheetAt(TicketPaymentHandler.SHEET_PAYMENTS);
			for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				Cell cell = row.getCell(TicketPaymentHandler.CELL_ID_BOOKING_NO);
				String sheetBookingNo = cell.getStringCellValue();
				if (row == null || Toolbox.isEmpty(sheetBookingNo)){
					return false;
				}
				if (sheetBookingNo.equalsIgnoreCase(bookingNumber)) {
					double oldValue = Toolbox.getSafeNumericCellValue(row, TicketPaymentHandler.CELL_ID_PAIDMENT_REMINDER);
					
					//prepare message data
					String subject = PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_EMAIL_NOTIFICATION_TEMPLATE_SUBJECT);
					String emailText = EmailTemplate.loadTemplate(TEMPLATES.NOTIFICATION_TEMPLATE)
												    .evaluateEmailText(row.getCell(TicketPaymentHandler.CELL_ID_CUSTOMERNAME).getStringCellValue(), 
												    				   null, 
												    				   new Double(row.getCell(TicketPaymentHandler.CELL_ID_ORDER_AMOUNT).getNumericCellValue()).doubleValue(), 
												    				   bookingNumber);
					String recipient = Toolbox.getSafeStringCellValue(row, TicketPaymentHandler.CELL_ID_CUSTOMEREMAIL);
					
					if (!Toolbox.isEmpty(recipient)) {
						//send notification message
						sendNotificationMessage(emailText, recipient, subject);
					}else {
						this.log("no email address found for booking number '" + bookingNumber + "' so i could not send reminder email notification");
					}
					this.lastRecipient = recipient;
					
					//update reminder counter
					row.createCell(TicketPaymentHandler.CELL_ID_PAIDMENT_REMINDER, CellType.NUMERIC).setCellValue(oldValue + 1);
					try(FileOutputStream fileOut = new FileOutputStream(paymentListFile)){
						wb.write(fileOut);
					}
					
					return true;
				}
			}
		}catch(EncryptedDocumentException | InvalidFormatException | EmailTransmissionException | IOException e){
			return terminateWithError("Beim Zugriff auf die Paymentliste bzw. beim Versenden der Zahlungserinnerung ist ein Fehler aufgetreten.", e);
		}finally{
			if (wb != null){
				try {
					wb.close();
				} catch (IOException e) {
					return terminateWithError("Beim Schliessen der Paymentliste ist ein unerwarteter Fehler aufgetreten. ", e);
				}
			}
		}
		return false;
	}
	
	public boolean sendRevocationNotification(List<Pair<String, String>> seats){
		return sendRevocationNotification(codeData -> {
			String seat = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT);
			return seats.containsAll(SeatTokenizer.parseSeats(seat));
		});
	}
	
	public boolean sendRevocationNotification(String bookingNumber){
		return sendRevocationNotification(codeData -> {
			String codeBookingNo = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_BOOKINGNUMBER);
			return bookingNumber.equals(codeBookingNo);
		});
	}
	
	public boolean sendRevocationNotification(Function<CodeData,Boolean> checkCodeRevocation){
		lastRecipient = null;
		List<Pair<String, String>> revokedSeats = new ArrayList<>();
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
			Boolean alreadyRevoked = null;
			for (CodeData codeData : codeList.values()) {
				if (checkCodeRevocation.apply(codeData)){
					boolean withdrawed = codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_WITHDRAWED);
					if (withdrawed) {
						alreadyRevoked = (alreadyRevoked == null) ? Boolean.TRUE : alreadyRevoked;
						continue;
					}else {
						alreadyRevoked = Boolean.FALSE;
					}
					
					String seat = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT);
					revokedSeats.addAll(SeatTokenizer.parseSeats(seat));
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

			if (alreadyRevoked == Boolean.TRUE) {
				return terminateWithError("tickets bereits geschreddert", null);
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

			String bookingNumber = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_BOOKINGNUMBER);

			try {
				String subject = PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_EMAIL_REVOCATION_TEMPLATE_SUBJECT);
				String emailtext = EmailTemplate.loadTemplate(TEMPLATES.REVOCATION_TEMPLATE).evaluateEmailText(codeData.getName(), makeSeatList(foundCodes), calcCompletePrice(foundCodes), bookingNumber);
				String recipient = foundEmails.keySet().iterator().next(); //codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_EMAIL);
				EmailTransmitter.transmitEmail(emailtext, null, recipient, subject);
				lastRecipient = recipient;

				//message specific post-processing
				for (CodeData code : foundCodes) {
					code.getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_WITHDRAWED, "true");
				}
				codeListHandler.saveCodeList(codeList);
				
				if (!PropertyHandler.getInstance().getPropertyBoolean(PropertyHandler.PROP_FREESEATSELECTION)) {
					//free seat in plan
					new SeatPlanHandler(this.feedback).freeSeats(revokedSeats);
				}
			} catch (IOException | EmailTransmissionException e) {
				return terminateWithError("Beim Versenden der eMail ist ein unerwarteter Fehler aufgetreten: " + e.getMessage(), e);
			}
			return true;
		} catch (NoSuchElementException e1) {
			return terminateWithError("NoNoNo, so nicht. Das sind ja ganz merkwürdige Sitze.", e1);
		}
	}
	
	private double calcCompletePrice(Set<CodeData> foundCodes) {
		Double completePrice = 0.0;
		
		for (CodeData codeData : foundCodes) {
			double price = codeData.getAdditionalCodeData().getDataAsDouble(ADDITIONAL_DATA.TICKET_PRICE);
			if (price <= 0){
				price = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_TICKET_PRICE);
			}
			completePrice = completePrice + price;
		}
		return completePrice;
	}

	void sendNotificationMessage(String emailText, String recipient, String subject) throws EmailTransmissionException{
		EmailTransmitter.transmitEmail(emailText, null, recipient, subject);
	}
	
	public String getLastRecipient(){
		return this.lastRecipient;
	}

	private boolean terminateWithError(String message, Exception e){
		log(message);
		if (e != null){
			log("Info: " + e.toString());
			this.feedback.printStackTrace(e);
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
		if (this.feedback != null){
			feedback.println(message);
		}
	}

}
