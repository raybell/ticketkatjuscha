package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.itextpdf.text.DocumentException;

import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.ui.TicketOffice;

public class TicketExpert {
	private static final String TICKET_TEMPLATE_DIR = "template";
	private static final String TICKET_NAME_SUFFIX = ".pdf";
	
	private int amountOfTickets;
	private String ownerName;
	private TicketGenerator ticketGenerator;
	
	private static Options options = new Options();
	static{
		options.addOption("a", "amountOfTickets", true, "amount of tickets");
		options.addOption("n", "nameOfTicketOwner", true, "name of ticket owner");
	}

	public static void main(String[] args) {
		//load cli arguments
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			
			if (cmd.getOptions() == null || cmd.getOptions().length == 0){
				//seems there is no freak using this app, so start the ui
				new TicketOffice();
			}else{
				if (!cmd.hasOption("a") || !cmd.hasOption("n")){
					//missing prameters
					System.err.println("Yo (wo)man, i think there is something missing....");
					printHelp();
					System.exit(1);
				}
				int ticketAmount = Integer.parseInt(cmd.getOptionValue("a"));
				String ownerName = cmd.getOptionValue("n");
				
				new TicketExpert(ticketAmount, ownerName).process(new ITicketProcessFailed() {
					@Override
					public void handleFailedState(Exception cause) {
						System.exit(2);
					}
				});
			}
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
	
	public TicketExpert(int amountOfTickets, String ownerName){
		this.amountOfTickets = amountOfTickets;
		this.ownerName = ownerName;
		this.ticketGenerator = new PDFTicketGenerator(TICKET_TEMPLATE_DIR);
	}
	
	public void process(ITicketProcessFailed failHandler){
		//generate codes
		System.out.println("Generating new ticket codes...");
		CodeGenerator codeGenerator = null;
		HashMap<String, CodeData> newCodes = null;
		try {
			codeGenerator = new CodeGenerator(ownerName);
			newCodes = codeGenerator.generateNewTicketCodes(amountOfTickets);
		} catch (GeneratorException e) {
			terminateWithError("Problääääm. Could not generate new ticket codes.", e, false, failHandler);
		}
		
		//archive old tickets
		System.out.println("Archiving old tickets...");
		archiveOldTickets();
		
		//generate new tickets
		System.out.println("Generating tickets (" + newCodes.size() + ") ...");
		for (String newCode : newCodes.keySet()){
			CodeData codeData = newCodes.get(newCode);
			File tmpFile = null;
			FileOutputStream outputStream = null;
			try {
				tmpFile = File.createTempFile(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_NAME_PREFIX), TICKET_NAME_SUFFIX);
				outputStream = new FileOutputStream(tmpFile);
				this.ticketGenerator.generate(newCode, newCodes.get(newCode).getCheckCode(), newCodes.get(newCode).getName(), outputStream);
			} catch (IOException e) {
				terminateWithError("Problääääm. Seems the ticket generator did not find the right byte code sequence.", e, true, failHandler);
			} catch (DocumentException e) {
                terminateWithError("Problääääm. Cannot create PDF.", e, true, failHandler);
            } finally{
				if (outputStream != null){
					try {
						outputStream.flush();
						outputStream.close();
					} catch (IOException e) {
						terminateWithError("Problääääm. Could not close the ticket file.", e, true, failHandler);
					}
				}
			}
			
			//move tmp file to expected output directory
			File ticketFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator + 
					makeCompatibleFileName(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_NAME_PREFIX) + "_" + newCode + "_" + codeData.getName()) + TICKET_NAME_SUFFIX);
			tmpFile.renameTo(ticketFile);
		}
		
		//save codes
		System.out.println("Updating code list...");
		try {
			codeGenerator.writeTicketCodes();
		} catch (GeneratorException e) {
			terminateWithError("Problääääm. Could not write new ticket codes.", e, true, failHandler);
		}
		
		//save props
		System.out.println("Storing properties...");
		PropertyHandler.persist();
		
		//finished
		System.out.println("Yeah, seems we got it without any errors, puh. Just check path '" 
					       + new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator).getAbsolutePath() 
					       + "' to find your tickets.");
	}
	
	private void terminateWithError(String message, Exception e, boolean clearOutputDirectory, ITicketProcessFailed failHandler){
		System.err.println(message + " Info: " + e.toString());
		e.printStackTrace(System.err);
		if (clearOutputDirectory){
			File outputDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator);
			for (File f : outputDir.listFiles()){
				f.delete();
			}
		}
		failHandler.handleFailedState(e);
	}
	
	private String makeCompatibleFileName(String filePartName){
		return filePartName.replaceAll("\\W+", "_");
	}
	
	private void archiveOldTickets(){
		File currentTicketsDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_GEN_DIR) + File.separator);
		File archiveTicketsDir = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_ARCHIVE_DIR) + File.separator);
		if (!currentTicketsDir.exists()){
			currentTicketsDir.mkdirs();
		}
		if (!archiveTicketsDir.exists()){
			archiveTicketsDir.mkdirs();
		}
		for (File f : currentTicketsDir.listFiles()){
			f.renameTo(new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_ARCHIVE_DIR) + File.separator + f.getName()));
		}
	}
}
