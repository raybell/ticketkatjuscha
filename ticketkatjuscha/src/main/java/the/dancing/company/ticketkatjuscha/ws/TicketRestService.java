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

@Path("/ticketservice")
public class TicketRestService {
	@GET
	@Path("/maketicket")
	@Produces("text/plain")
	public Response responseMsg(@QueryParam("amount") int amount, @QueryParam("name") String name) {
		StringBuilder response = new StringBuilder();
		if (amount > 0){
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try{
				boolean ticketsGenerated = new TicketExpert(amount, name).process(new ITicketProcessFailed() {
					@Override
					public boolean handleFailedState(Exception cause) {
						response.append("Bigga bigga problem.\n");
						StringWriter errorWriter = new StringWriter();
						cause.printStackTrace(new PrintWriter(errorWriter));
						response.append(errorWriter.getBuffer());
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
}
