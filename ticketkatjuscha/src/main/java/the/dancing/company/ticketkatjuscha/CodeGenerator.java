package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.HashMap;

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

	private String owner;
	private static final String CODELIST_NAME = "codelist.xlsx";
	private static final String CODELIST_SHEET = "Ticketcodes";
	private static final int MAX_CODE_DIGITS = 4;
	private static final int MAX_CHECKCODE_DIGITS = 3;
	
	
	public CodeGenerator(String owner){
		this.owner = owner;
	}
	
	public HashMap<String, CodeData> generateTicketCodes(int amount) throws GeneratorException{
		try {
			HashMap<String, CodeData> loadCodelist = loadCodelist();
			HashMap<String, CodeData> newCodeList = new HashMap<>();
			for (int i = 0; i < amount; i++) {
				Ticket newTicket = generateNewCode(loadCodelist);
				newCodeList.put(newTicket.getCode(), newTicket.getCodeData());
				loadCodelist.put(newTicket.getCode(), newTicket.getCodeData());
			}
			return newCodeList;
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			throw new GeneratorException(e);
		}
	}
	
	public void writeTicketCodes(HashMap<String, CodeData> newCodeList) throws GeneratorException{
		try {
			Workbook wb = WorkbookFactory.create(new File(CODELIST_NAME));
			Sheet sheet = wb.getSheet(CODELIST_SHEET);
			for (String code : newCodeList.keySet()) {
				Row row = sheet.createRow(sheet.getLastRowNum() + 1);
				System.out.println(code + "/" + newCodeList.get(code).getCheckCode() + "/" + newCodeList.get(code).getName());
				row.createCell(0).setCellValue(code);
				row.createCell(1).setCellValue(newCodeList.get(code).getCheckCode());
				row.createCell(2).setCellValue(newCodeList.get(code).getName());
			}
			FileOutputStream fileOut = null;
			try {
				fileOut = new FileOutputStream(CODELIST_NAME, true);
				wb.write(fileOut);
			} finally {
				fileOut.close();
				wb.close();
			}
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			throw new GeneratorException(e);
		} 
	}
	
	private Ticket generateNewCode(HashMap<String, CodeData> codeList){
		long newCode = Math.round(Math.random() * (MAX_CODE_DIGITS * 1000));
		if (codeList.containsKey("" + newCode)){
			//we already know this code, try to create a new one
			return generateNewCode(codeList);
		}else{
			return new Ticket(String.format("%0" + MAX_CODE_DIGITS + "d", newCode), new CodeData(generateCheckCode(), owner));
		}
	}
	
	private String generateCheckCode(){
		StringBuilder checkCode = new StringBuilder();
		for (int i = 0; i < MAX_CHECKCODE_DIGITS; i++) {
			checkCode.append((char)(Math.round(Math.random() * 100) % 26 + 65));	//big letters only
		}
		return checkCode.toString();
	}
	
	private HashMap<String, CodeData> loadCodelist() throws IOException, EncryptedDocumentException, InvalidFormatException{
		HashMap<String,CodeData> codeList = new HashMap<>();
		File codeListFile = new File(CODELIST_NAME);
		if (!codeListFile.exists()){
			//create new/empty code list
			makeEmptyCodelist();
		}else{
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
	
	private void makeEmptyCodelist() throws IOException{
		Workbook wb = new XSSFWorkbook();
		
		Sheet sheet1 = wb.createSheet(CODELIST_SHEET);
		Row row = sheet1.createRow((short)0);
		row.createCell(0).setCellValue("Ticketcode");
		row.createCell(1).setCellValue("Checkcode");
		row.createCell(2).setCellValue("Name");
		
		FileOutputStream fileOut = null;
	    try {
			fileOut = new FileOutputStream(CODELIST_NAME);
			wb.write(fileOut);
		} finally {
			fileOut.close();
			wb.close();
		}
	}
}
