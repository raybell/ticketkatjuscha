package the.dancing.company.ticketkatjuscha.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.function.Function;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import the.dancing.company.ticketkatjuscha.ITicketProcessFailed;
import the.dancing.company.ticketkatjuscha.TicketExpert;
import the.dancing.company.ticketkatjuscha.TicketNotifier;
import the.dancing.company.ticketkatjuscha.TicketPaymentHandler;
import the.dancing.company.ticketkatjuscha.util.ProcessFeedback;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

@Path("/ticketservice")
public class TicketRestService {
	public static final int HTTP_STATUSCODES_OK = 200;
	public static final int HTTP_STATUSCODES_BADREQUEST = 400;
	public static final int HTTP_STATUSCODES_INTERNALERROR = 500;
	
	@GET
	@Path("/maketicket")
	@Produces("text/plain")
	public Response makeTicket(@QueryParam("amount") int amount, @QueryParam("name") String name, @QueryParam("seats") String seats, @QueryParam("recipient") String recipient, @QueryParam("price") String price) {
		StringBuilder response = new StringBuilder();
		int statusCode = HTTP_STATUSCODES_OK;
		if (amount > 0){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try{
				int iPrice = -1;
				try {
					iPrice = Integer.parseInt(price);
				} catch (Exception e) {
					//ignore, use default
				}

				boolean ticketsGenerated = new TicketExpert(amount, name, SeatTokenizer.parseSeats(seats), recipient, iPrice).process(new ITicketProcessFailed() {
					@Override
					public boolean handleFailedState(String message, Exception cause) {
						appendExceptionToResponse(response, "Bigga bigga problem.\nMessage: " + message, cause);
						return false;
					}
				}, new PrintStream(baos));
				if (ticketsGenerated){
					response.append("Successfully generated " + amount + " ticket(s)" + ((name != null && name.trim().length() > 0)?" for " + name : "") + ". Just have a look in your dropbox.");
				}
			}catch(Exception e){
				//something unexpected occured
				appendExceptionToResponse(response, "Unexpected exception occured: " + e.toString(), e);
				statusCode=HTTP_STATUSCODES_INTERNALERROR;
			}
			finally{
				try {
					baos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
				response.append("\n\n\nProcessing details:\n" + new String(baos.toByteArray()));
			}
		}else{
			response.append("How many tickets do you want (queryparameter=amount)? And optionally you can give me a name (queryparameter=name).");
			statusCode=HTTP_STATUSCODES_BADREQUEST;
		}
		return Response.status(statusCode).entity(response.toString()).build();
	}

	@GET
	@Path("/sendPaymentReminder")
	@Produces("text/plain")
	public Response sendPaymentReminder(@QueryParam("bookingnumber") String bookingnumber) {
		return sendTicketNotification("PaymentReminder", (TicketNotifier t) -> {return t.sendReminderNotification(bookingnumber);});
	}

	@GET
	@Path("/sendTicketRevocation")
	@Produces("text/plain")
	public Response sendTicketRevocationForSeats(@QueryParam("seats") String seats) {
		return sendTicketNotification("TicketRevocation", (TicketNotifier t) -> {return t.sendRevocationNotification(SeatTokenizer.parseSeats(seats));});
	}
	
	@GET
	@Path("/sendTicketRevocation")
	@Produces("text/plain")
	public Response sendTicketRevocationForBookingNo(@QueryParam("bookingNo") String bookingNo) {
		return sendTicketNotification("TicketRevocation", (TicketNotifier t) -> {return t.sendRevocationNotification(bookingNo);});
	}

	@GET
	@Path("/setTicketsToPaid")
	@Produces("text/plain")
	public Response setTicketsToPaid(@QueryParam("bookingNumber") String bookingNumber, @QueryParam("totalSum") String totalSum) {
		StringBuilder response = new StringBuilder();
		int statusCode = HTTP_STATUSCODES_OK;
		
		if (!Toolbox.isEmpty(bookingNumber)){
			Number totalSumNum = null;
			
			//optional parameter
			if (!Toolbox.isEmpty(totalSum)) {
				try {
					totalSumNum = DecimalFormat.getInstance(Locale.GERMAN).parse(totalSum);
				} catch (ParseException e) {
					appendExceptionToResponse(response, "problem reading totalsum value '" + totalSum + "'", e);
					statusCode=HTTP_STATUSCODES_BADREQUEST;
				}
			}
			
			if (statusCode == HTTP_STATUSCODES_OK) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try{
					boolean setToPaid = new TicketPaymentHandler(new PrintStream(baos)).setToPaid(bookingNumber, totalSumNum);
					if (setToPaid){
						response.append("Booking number '" + bookingNumber + "' set to paid.");
					}else {
						response.append("Booking number '" + bookingNumber + "' wasn't found");
					}
				}catch(Exception e){
					//something unexpected occured
					appendExceptionToResponse(response, "Unexpected exception occured: " + e.toString(), e);
					statusCode=HTTP_STATUSCODES_INTERNALERROR;
				}finally{
					try {
						baos.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
					response.append("\n\n\nProcessing details:\n" + new String(baos.toByteArray()));
				}
			}
		}else{
			response.append("Give me the booking number (requestparameter=bookingNumber) of the tickets you want to mark as paid.");
			statusCode=HTTP_STATUSCODES_BADREQUEST;
		}
		return Response.status(statusCode).entity(response.toString()).build();
	}
	
	@GET
	@Path("/restart")
	@Produces("text/plain")
	public Response restartService(){
		//terminate this VM instance and let the startscript invoke a restart
		new Thread(){
			@Override
			public void run() {
				try {
					sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.exit(3);
			}
		}.start();
		return Response.status(HTTP_STATUSCODES_OK).entity("restarting in 5s... stay tuned and wait for the magic...").build();
	}

	@GET
	@Path("/version")
	@Produces("text/plain")
	public Response getProgVersion(){
		return Response.status(HTTP_STATUSCODES_OK).entity(Toolbox.getProgVersion()).build();
	}

	private Response sendTicketNotification(String typeIdent, Function<TicketNotifier, Boolean> processNotification){
		StringBuilder response = new StringBuilder();

		ProcessFeedback feedback = new ProcessFeedback();
		TicketNotifier ticketNotifier = new TicketNotifier(new ITicketProcessFailed() {
			@Override
			public boolean handleFailedState(String message, Exception cause) {
				response.append("Bigga bigga problem.\n");
				response.append("Message: " + message + "\n");
				if (cause != null){
					StringWriter errorWriter = new StringWriter();
					cause.printStackTrace(new PrintWriter(errorWriter));
					response.append(errorWriter.getBuffer());
				}
				return false;
			}
		}, feedback);

		boolean sendNotification = processNotification.apply(ticketNotifier);

		if (sendNotification){
			response.append("Successfully sent " + typeIdent + " to " + ticketNotifier.getLastRecipient());
		}else {
			response.append("Something went wrong. Please check the details.");
		}
		
		
		if (!Toolbox.isEmpty(feedback.getMessages())){
			response.append("\n\n\nProcessing details:\n" + feedback.getMessages());
		}

		return Response.status(HTTP_STATUSCODES_OK).entity(response.toString()).build();
	}
	
	private static void appendExceptionToResponse(StringBuilder response, String message, Exception e) {
		if (!Toolbox.isEmpty(message)) {
			response.append(message + "\n");
		}
		if (e != null) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			response.append(sw.getBuffer().toString());
		}
	}
}
