package the.dancing.company.ticketkatjuscha.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import the.dancing.company.ticketkatjuscha.ITicketProcessFailed;
import the.dancing.company.ticketkatjuscha.TicketExpert;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

@Path("/ticketservice")
public class TicketRestService {
	@GET
	@Path("/maketicket")
	@Produces("text/plain")
	public Response makeTicket(@QueryParam("amount") int amount, @QueryParam("name") String name, @QueryParam("seats") String seats, @QueryParam("recipient") String recipient) {
		StringBuilder response = new StringBuilder();
		if (amount > 0){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try{
				boolean ticketsGenerated = new TicketExpert(amount, name, SeatTokenizer.parseSeats(seats), recipient).process(new ITicketProcessFailed() {
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
				}, new PrintStream(baos));
				if (ticketsGenerated){
					response.append("Successfully generated " + amount + " ticket(s)" + ((name != null && name.trim().length() > 0)?" for " + name : "") + ". Just have a look in your dropbox.");
				}
			}catch(Exception e){
				//something unexpected occured
				response.append("Unexpected exception occured: " + e.toString() + "\n");
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				response.append(sw.getBuffer().toString());
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
		}
		return Response.status(200).entity(response.toString()).build();
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
		return Response.status(200).entity("restarting in 5s... stay tuned and wait for the magic...").build();
	}
	
	@GET
	@Path("/version")
	@Produces("text/plain")
	public Response getProgVersion(){
		return Response.status(200).entity(Toolbox.getProgVersion()).build();
	}
}
