package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import the.dancing.company.ticketkatjuscha.data.CodeData;

public class TicketExpert {
	
	private int amountOfTickets;
	private String ownerName;
	
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
			if (!cmd.hasOption("a") || !cmd.hasOption("n")){
				//missing prameters
				System.err.println("Yo (wo)man, i think there is something missing....");
				printHelp();
				System.exit(1);
			}
			int ticketAmount = Integer.parseInt(cmd.getOptionValue("a"));
			String ownerName = cmd.getOptionValue("n");
			
			new TicketExpert(ticketAmount, ownerName).process();
			
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
	}
	
	public void process(){
		//generate codes
		CodeGenerator codeGenerator = new CodeGenerator(ownerName);
		HashMap<String, CodeData> newCodes = null;
		try {
			newCodes = codeGenerator.generateTicketCodes(amountOfTickets);
		} catch (GeneratorException e) {
			System.err.println("Problääääm. Could not generate new ticket codes: " + e.toString());
			System.exit(2);
		}
		
		//archive old tickets
		
		//generate new tickets
		
		//save codes
		try {
			codeGenerator.writeTicketCodes(newCodes);
		} catch (GeneratorException e) {
			System.err.println("Problääääm. Could not write new ticket codes: " + e.toString());
			System.exit(2);
		}
		
	}
}
