package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.data.Ticket;

public class CodeGenerator {

	private static final String CODELIST_NAME = "codelist.xlsx";
	private static final String CODELIST_NAME_BACKUP = CODELIST_NAME + ".bak";
	private static final String CODELIST_SHEET = "Ticketcodes";
	private static final int MAX_CODE_DIGITS = 4;
	private static final int MAX_CHECKCODE_DIGITS = 3;
	
	private String owner;
	private Map<String, CodeData> codeList;
	
	public CodeGenerator(String owner) throws GeneratorException{
		this.owner = owner;
		try {
			//load exiting codes
			loadCodelist();
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
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
			backupCurrentCodelist();
			
			//create new codelist
			writeNewCodelist();
		} catch (EncryptedDocumentException | IOException e) {
			throw new GeneratorException(e);
		} 
	}
	
	private Ticket generateNewCode(){
		String newCode = String.format("%0" + MAX_CODE_DIGITS + "d", Math.round(Math.random() * (MAX_CODE_DIGITS * 1000)));
		if (codeList.containsKey(newCode)){
			//we already know this code, try to create a new one
			return generateNewCode();
		}else{
			return new Ticket(newCode, new CodeData(generateCheckCode(), owner));
		}
	}
	
	private String generateCheckCode(){
		StringBuilder checkCode = new StringBuilder();
		for (int i = 0; i < MAX_CHECKCODE_DIGITS; i++) {
			checkCode.append((char)(Math.round(Math.random() * 100) % 26 + 65));	//big letters only
		}
		return checkCode.toString();
	}
	
	private Map<String, CodeData> loadCodelist() throws IOException, EncryptedDocumentException, InvalidFormatException{
		codeList = new TreeMap<>();
		File codeListFile = new File(CODELIST_NAME);
		if (codeListFile.exists()){
			Workbook wb = null;
			try {
				//load existing codes
				wb = WorkbookFactory.create(new File(CODELIST_NAME));
				Sheet sheet = wb.getSheet(CODELIST_SHEET);
				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row row = sheet.getRow(i);
					codeList.put("" + row.getCell(0), new CodeData("" + row.getCell(1), "" + row.getCell(2)));
				} 
			} finally {
				wb.close();
			}
		}
		return codeList;
	}
	
	private void writeNewCodelist() throws IOException{
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = wb.createSheet(CODELIST_SHEET);
		Row row0 = sheet.createRow((short)0);
		row0.createCell(0).setCellValue("Ticketcode");
		row0.createCell(1).setCellValue("Checkcode");
		row0.createCell(2).setCellValue("Name");
		for (String code : codeList.keySet()) {
			Row row = sheet.createRow(sheet.getLastRowNum() + 1);
			row.createCell(0).setCellValue(code);
			row.createCell(1).setCellValue(codeList.get(code).getCheckCode());
			row.createCell(2).setCellValue(codeList.get(code).getName());
		}
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(CODELIST_NAME, true);
			wb.write(fileOut);
		} finally {
			fileOut.close();
			wb.close();
		}
	}
	
	private void backupCurrentCodelist(){
		File codeListFile = new File(CODELIST_NAME);
		if (codeListFile.exists()){
			File codeListBackupFile = new File(CODELIST_NAME_BACKUP);
			//delete old backup
			if (codeListBackupFile.exists()){
				codeListBackupFile.delete();
			}
			codeListFile.renameTo(codeListBackupFile);
		}
	}
	
}
