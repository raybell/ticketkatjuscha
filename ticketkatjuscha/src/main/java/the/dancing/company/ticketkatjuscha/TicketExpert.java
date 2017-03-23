package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.javatuples.Pair;

import com.itextpdf.text.DocumentException;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;

import the.dancing.company.ticketkatjuscha.EmailTemplate.TEMPLATES;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.exceptions.EmailTransmissionException;
import the.dancing.company.ticketkatjuscha.exceptions.GeneratorException;
import the.dancing.company.ticketkatjuscha.ui.TicketOffice;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;

public class TicketExpert {
	private int amountOfTickets;
	private String ownerName;
	private List<Pair<String, String>> seats;
	private TicketGenerator ticketGenerator;
	private String emailRecipient;
	private int price;

	private static Options options = new Options();
	static{
		options.addOption("a", "amountOfTickets", true, "amount of tickets");
		options.addOption("n", "nameOfTicketOwner", true, "name of ticket owner");
		options.addOption("s", "server-mode", false, "start in server mode");
	}

	public static void main(String[] args) {
		//load cli arguments
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);

			if (cmd.getOptions() == null || cmd.getOptions().length == 0){
				//seems there is no freak using this app, so start the ui
				new TicketOffice();
				//finished
				return;
			}

			if (cmd.hasOption("s")){
				//start in server mode
				startServer();
				//finished
				return;
			}

			if (cmd.hasOption("a")){
				//cli mode
				int ticketAmount = Integer.parseInt(cmd.getOptionValue("a"));
				String ownerName = cmd.getOptionValue("n", "");

				new TicketExpert(ticketAmount, ownerName, null, null, -1).process(new ITicketProcessFailed() {
					@Override
					public boolean handleFailedState(String message, Exception cause) {
						System.exit(2);
						return false;
					}
				}, System.out);
				//finished
				return;
			}

			//hmm, there must be something wrong
			System.err.println("Yo (wo)man, i think there is something missing....");
			printHelp();
			System.exit(1);

		} catch (ParseException | NumberFormatException e) {
			System.err.println( "Uiuiui huiuiui puhhhhhh, damn, what is going on here...  Lock, perhaps you are able to find it out: " + e.toString());
			System.err.println( "No? OK, i have some really really nasty expert information for you, perhaps you can read this crap..." );
			e.printStackTrace(System.err);
			System.err.println( "Still no idea? Ok, just check this.");
			printHelp();
			System.exit(1);
		}
	}

	private static void printHelp(){
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar ticketkatjuscha.jar", options);
	}

	private static void startServer() {
        ResourceConfig rc = new PackagesResourceConfig("the.dancing.company.ticketkatjuscha.ws");
        System.out.println("Starting the grizzly...");
        HttpServer server = null;
        try {
			server = GrizzlyServerFactory.createHttpServer(getServerBaseURI(), rc);
			while(true){
				try {
					System.in.read();
				} catch (IOException e) {
					//in case we are running in background (nohup) we may get exceptions reading from the system.in
					e.printStackTrace();
				}
				try {
					Thread.sleep(600000);
				} catch (InterruptedException e) {
					//hui seems we got interupted, try to proceed and look what happens
					e.printStackTrace();
				}
				System.out.println("i will never stop running...");
			}
		} catch (IllegalArgumentException | NullPointerException | IOException e) {
			e.printStackTrace();
		}finally{
			if (server != null){
				//ok, now i stop
				server.stop();
			}
		}
    }

	private static URI getServerBaseURI() throws IllegalArgumentException, UriBuilderException, UnknownHostException{
		return UriBuilder.fromUri("http://" + NetworkListener.DEFAULT_NETWORK_HOST)
				         .port(PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_SERVER_PORT))
				         .build();
	}

	public TicketExpert(int amountOfTickets, String ownerName, List<Pair<String, String>> seats, String recipient, int price){
		this.amountOfTickets = amountOfTickets;
		this.ownerName = ownerName;
		this.seats = seats;
		this.emailRecipient = recipient;
		this.price = price > 0 ? price : PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_TICKET_PRICE);
		//check the seats
		if (seats == null || seats.size() != amountOfTickets){
			//seats does not fit amount of tickets
			throw new IllegalArgumentException("Problääääm. Seats does not fit the ticket amount.");
		}
		this.ticketGenerator = new PDFTicketGenerator();
	}

	public boolean process(ITicketProcessFailed failHandler, PrintStream logWriter){
		//load current properties
		logWriter.println("Loading current properties...");
		PropertyHandler.load();

		//generate codes
		logWriter.println("Generating new ticket codes...");
		CodeGenerator codeGenerator = null;
		HashMap<String, CodeData> newCodes = null;
		try {
			codeGenerator = new CodeGenerator(ownerName);

			//check if the seats are still free
			if (!codeGenerator.checkIfSeatsAreFree(this.seats)){
				return terminateWithError("Problääääm. One or more of the seats haben already einen sitzen.", null, false, failHandler);
			}

			//make new code
			newCodes = codeGenerator.generateNewTicketCodes(amountOfTickets, this.seats, this.emailRecipient, this.price);
		} catch (GeneratorException e) {
			return terminateWithError("Problääääm. Could not generate new ticket codes.", e, false, failHandler);
		}

		//archive old tickets
		logWriter.println("Archiving old tickets...");
		archiveOldTickets(logWriter);

		//make precheck for template file
		File templateFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_TEMPLATE_FILE));
		if (!templateFile.exists() || !templateFile.isFile()){
			return terminateWithError("Problääääm. Could not find the ticket template file in :" + templateFile.getAbsolutePath(), new FileNotFoundException("Missing or invalid template file " + templateFile.getAbsolutePath()), false, failHandler);
		}

		//generate new tickets
		logWriter.println("Generating tickets (" + newCodes.size() + ") ...");
		List<File> ticketFiles = new ArrayList<>();

		for (String newCode : newCodes.keySet()){
			CodeData codeData = newCodes.get(newCode);
			File tmpFile = null;
			FileOutputStream outputStream = null;
			try {
				tmpFile = File.createTempFile(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_NAME_PREFIX), this.ticketGenerator.getFileNameExtension());
//				tmpFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) +
//						File.separator + PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_NAME_PREFIX) + System.currentTimeMillis());
				outputStream = new FileOutputStream(tmpFile);
				CodeData newCodeData = newCodes.get(newCode);
				this.ticketGenerator.generate(newCode, newCodeData.getCheckCode(), newCodeData.getName(),
						                     SeatTokenizer.parseSeats(newCodeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT)).get(0),
						                     newCodeData.getAdditionalCodeData().getDataAsInt(ADDITIONAL_DATA.TICKET_PRICE),
						                     outputStream);
			} catch (IOException e) {
				return terminateWithError("Problääääm. Seems the ticket generator did not find the right byte code sequence.", e, true, failHandler);
			} catch (DocumentException e) {
				return terminateWithError("Problääääm. Cannot create PDF.", e, true, failHandler);
            } finally{
				if (outputStream != null){
					try {
						outputStream.flush();
						outputStream.close();
					} catch (IOException e) {
						return terminateWithError("Problääääm. Could not close the ticket file.", e, true, failHandler);
					}
				}
			}

			//move tmp file to expected output directory
			File ticketFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator +
					makeCompatibleFileName(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_NAME_PREFIX) + "_" + newCode + "_" + codeData.getName()) + "." + this.ticketGenerator.getFileNameExtension());
			tmpFile.renameTo(ticketFile);
			ticketFiles.add(ticketFile);
		}

		//save codes
		logWriter.println("Updating code list...");
		try {
			codeGenerator.writeTicketCodes();
		} catch (GeneratorException e) {
			return terminateWithError("Problääääm. Could not write new ticket codes.", e, true, failHandler);
		}

		//generate email
		logWriter.println("Sending email notification...");
		try {
			String emailText = EmailTemplate.loadTemplate(TEMPLATES.TICKET_TEMPLATE).evaluateEmailText(ownerName, amountOfTickets, null, this.price);
			EmailTransmitter.transmitEmail(emailText, ticketFiles, emailRecipient);
		} catch (IOException | EmailTransmissionException e) {
			logWriter.print("Problääääm. Could not send the email notification: " + e.getMessage() + ". But your tickets were generated, so check the output-directory.");
			e.printStackTrace(logWriter);
		}

		//save props [disabled because it scrambles the props...]
//		logWriter.println("Storing properties...");
//		PropertyHandler.persist();

		//finished
		logWriter.println("Yeah, seems we got it without any errors, puh. Just check path '"
					       + new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator).getAbsolutePath()
					       + "' to find your tickets.");
		return true;
	}

	private boolean terminateWithError(String message, Exception e, boolean clearOutputDirectory, ITicketProcessFailed failHandler){
		System.err.println(message);
		if (e != null){
			System.err.println("Info: " + e.toString());
			e.printStackTrace(System.err);
		}

		if (clearOutputDirectory){
			File outputDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator);
			for (File f : outputDir.listFiles()){
				f.delete();
			}
		}
		return failHandler.handleFailedState(message, e);
	}

	private String makeCompatibleFileName(String filePartName){
		return filePartName.replaceAll("\\W+", "_");
	}

	private void archiveOldTickets(PrintStream logWriter){
		File currentTicketsDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator);
		File archiveTicketsDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_ARCHIVE_DIR) + File.separator);
		if (!currentTicketsDir.exists()){
			currentTicketsDir.mkdirs();
		}
		if (!archiveTicketsDir.exists()){
			archiveTicketsDir.mkdirs();
		}
		for (File f : currentTicketsDir.listFiles()){
			//check if the ticket already exists in archive (maybe a zombie?)
			File archiveFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_ARCHIVE_DIR) + File.separator + f.getName());
			if (archiveFile.exists()){
				logWriter.println("Warning: the ticket '" + f.getName() + "' already exists in archive. Maybe a zombie from an old event? I will delete it to archive the current ticket. Hope you do not miss it.");
				archiveFile.delete();
			}
			f.renameTo(archiveFile);
		}
	}
}
