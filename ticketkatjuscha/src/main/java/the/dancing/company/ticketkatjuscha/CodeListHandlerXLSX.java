package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import the.dancing.company.ticketkatjuscha.data.CodeData;

public class CodeListHandlerXLSX implements ICodeListHandler{
	private static final String CODELIST_SHEET = "Ticketcodes";
	
	private String fileName;
	public CodeListHandlerXLSX(String fileName){
		this.fileName = fileName;
	}
	
	@Override
	public Map<String, CodeData> loadCodeList() throws FileNotFoundException, IOException {
		Map<String, CodeData> codeList = new TreeMap<>();
		File codeListFile = new File(fileName);
		if (codeListFile.exists()){
			try ( Workbook wb = WorkbookFactory.create(new File(fileName))){
				//load existing codes
				Sheet sheet = wb.getSheet(CODELIST_SHEET);
				
				Row firstRow = sheet.getRow(0);
				int filledCellCount = 0;
				while(isCellFilled(firstRow.getCell(filledCellCount++))){};
				
				for (int i = 1; i <= sheet.getLastRowNum(); i++) {
					Row row = sheet.getRow(i);
					codeList.put("" + row.getCell(0), new CodeData("" + row.getCell(0), "" + row.getCell(1), "" + row.getCell(2), null));
				} 
			}catch(InvalidFormatException e){
				throw new IOException(e);
			}
		}
		return codeList;
	}

	@Override
	public void saveCodeList(Map<String, CodeData> codeList) throws IOException {
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
			fileOut = new FileOutputStream(this.fileName, true);
			wb.write(fileOut);
		} finally {
			fileOut.close();
			wb.close();
		}
	}
	
	public static boolean isCellFilled(Cell cell){
		return cell != null && cell.toString().trim().length() > 0;
	}

	@Override
	public String getFileName() {
		return this.fileName;
	}

}
