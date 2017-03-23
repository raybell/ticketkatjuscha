package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;
import org.javatuples.Pair;

import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.exceptions.GeneratorException;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;
import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.data.Ticket;

public class CodeGenerator {
	private String owner;
	private Map<String, CodeData> codeList;
	private Map<String, CodeData> seatMap;
	private ICodeListHandler codeListHandler;

	public CodeGenerator(String owner) throws GeneratorException{
		this.owner = owner;
		this.codeListHandler = CodeListHandlerFactory.produceHandler();
		try {
			//load exiting codes
			this.codeList = this.codeListHandler.loadCodeList();
		} catch (EncryptedDocumentException | IOException e) {
			throw new GeneratorException(e);
		}
	}

	public HashMap<String, CodeData> generateNewTicketCodes(int amount, List<Pair<String, String>> seats, String emailRecipient, int price) throws GeneratorException{
		try {
			HashMap<String, CodeData> newCodeList = new HashMap<>();
			for (int i = 0; i < amount; i++) {
				Ticket newTicket = generateNewCode();
				newTicket.getCodeData().getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_SEAT, SeatTokenizer.makeSeatToken(seats.get(i)));
				newTicket.getCodeData().getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_EMAIL, emailRecipient);
				newTicket.getCodeData().getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_PRICE, "" + price);
				newCodeList.put(newTicket.getCode(), newTicket.getCodeData());
				codeList.put(newTicket.getCode(), newTicket.getCodeData());
			}
			return newCodeList;
		} catch (EncryptedDocumentException e) {
			throw new GeneratorException(e);
		}
	}

	public void writeTicketCodes() throws GeneratorException{
		if (codeList.size() == 0){
			//nothing to save
			return;
		}

		try {
			//backup old codelist
			backupCurrentCodelist(this.codeListHandler.getFileName());

			//create new codelist
			this.codeListHandler.saveCodeList(this.codeList);
		} catch (EncryptedDocumentException | IOException e) {
			throw new GeneratorException(e);
		}
	}

	public boolean checkIfSeatsAreFree(List<Pair<String, String>> seats){
		if (seatMap == null){
			//fill the map with seats
			seatMap = new HashMap<>();
			for (CodeData codeData : this.codeList.values()){
				String seat = codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_SEAT);
				boolean withdrawed = codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_WITHDRAWED);
				if (!withdrawed && seat != null){
					seatMap.put(seat, codeData);
				}
			}
		}
		for (Pair<String, String> pair : seats) {
			if (seatMap.containsKey(SeatTokenizer.makeSeatToken(pair))){
				return false;
			}
		}
		return true;
	}

	private Ticket generateNewCode(){
		int maxCodeDigits = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_MAX_CODE_DIGITS);
		String newCode = String.format("%0" +  maxCodeDigits + "d", Math.round(Math.random() * (Math.pow(10, maxCodeDigits))));
		if (codeList.containsKey(newCode)){
			//we already know this code, try to create a new one
			return generateNewCode();
		}else{
			return new Ticket(newCode, new CodeData(newCode, generateCheckCode(), owner, new AdditionalCodeData()));
		}
	}

	private String generateCheckCode(){
		int maxCheckcodeDigits = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_MAX_CHECKCODE_DIGITS);
		StringBuilder checkCode = new StringBuilder();
		for (int i = 0; i < maxCheckcodeDigits; i++) {
			checkCode.append((char)(Math.round(Math.random() * 100) % 26 + 65));	//big letters only
		}
		return checkCode.toString();
	}

	private void backupCurrentCodelist(String codeListName){
		File codeListFile = new File(codeListName);
		if (codeListFile.exists()){
			//make backup dir
			new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_CODELIST_BACKUP_DIR) + File.separator).mkdirs();
			//make backup
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			File codeListBackupFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_CODELIST_BACKUP_DIR) + File.separator + codeListName + "." + sdf.format(new Date()) + ".bak");
			//delete old backup
			if (codeListBackupFile.exists()){
				codeListBackupFile.delete();
			}
			codeListFile.renameTo(codeListBackupFile);
		}
	}
}
