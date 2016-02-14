package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.EncryptedDocumentException;

import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.data.Ticket;

public class CodeGenerator {
	
	
	private String owner;
	private Map<String, CodeData> codeList;
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

	public HashMap<String, CodeData> generateNewTicketCodes(int amount) throws GeneratorException{
		try {
			HashMap<String, CodeData> newCodeList = new HashMap<>();
			for (int i = 0; i < amount; i++) {
				Ticket newTicket = generateNewCode();
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
	
	private Ticket generateNewCode(){
		int maxCodeDigits = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_MAX_CODE_DIGITS);
		String newCode = String.format("%0" +  maxCodeDigits + "d", Math.round(Math.random() * (Math.pow(10, maxCodeDigits))));
		if (codeList.containsKey(newCode)){
			//we already know this code, try to create a new one
			return generateNewCode();
		}else{
			return new Ticket(newCode, new CodeData(generateCheckCode(), owner, null));
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
