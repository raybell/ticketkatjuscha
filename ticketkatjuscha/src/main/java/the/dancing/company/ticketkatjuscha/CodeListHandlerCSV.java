package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;

public class CodeListHandlerCSV implements ICodeListHandler{
	
	private static final char CODELIST_CSV_SEP = ';';
	private static final char CODELIST_CSV_QUOTE = '\"';
	
	private String fileName;
	
	public CodeListHandlerCSV(String fileName){
		this.fileName = fileName;
	}
	
	@Override
	public Map<String, CodeData> loadCodeList() throws FileNotFoundException, IOException {
		Map<String, CodeData> codeList = new TreeMap<>();
		File f = new File(fileName);
		if (f.exists()){
			try (CSVReader reader = new CSVReader(new FileReader(fileName), CODELIST_CSV_SEP, CODELIST_CSV_QUOTE)) {
				String[] nextLine;
				//ignore the first line (headers)
				reader.readNext();
				while ((nextLine = reader.readNext()) != null) {
					codeList.put("" + nextLine[0], new CodeData(nextLine[1], nextLine[2], loadAdditionalCodeData(nextLine, 3)));
				}
			}
		}
		return codeList;
	}

	private AdditionalCodeData loadAdditionalCodeData(String [] lineData, int fromIndex){
		AdditionalCodeData addData = new AdditionalCodeData();
		int addDataCounter = 0;
		while(fromIndex + addDataCounter < lineData.length && ADDITIONAL_DATA.values().length > addDataCounter){
			addData.setAdditionalData(ADDITIONAL_DATA.values()[addDataCounter], lineData[fromIndex + addDataCounter]);
			addDataCounter++;
		}
		return addData;
	}
	
	@Override
	public void saveCodeList(Map<String, CodeData> codeList) throws IOException {
		ArrayList<String[]> csvData = new ArrayList<>();
		
		//write header
		ArrayList<String> headerData = new ArrayList<>();
		headerData.add("Ticketcode");
		headerData.add("Checkcode");
		headerData.add("Name");
		for (ADDITIONAL_DATA addData : ADDITIONAL_DATA.values()) {
			headerData.add(addData.getColumnTitle());
		}
		csvData.add(headerData.toArray(new String[headerData.size()]));
		
		//write data
		for (String code : codeList.keySet()) {
			ArrayList<String> listData = new ArrayList<>();
			listData.add(code);
			listData.add(codeList.get(code).getCheckCode());
			listData.add(codeList.get(code).getName());
			
			AdditionalCodeData additionalCodeData = codeList.get(code).getAdditionalCodeData();
			if (additionalCodeData != null && additionalCodeData.getDataList() != null){
				addArrayToList(listData, additionalCodeData.getDataList());
			}
			
			csvData.add(listData.toArray(new String[listData.size()]));
		}
		
		try(CSVWriter writer = new CSVWriter(new FileWriter(fileName), CODELIST_CSV_SEP, CODELIST_CSV_QUOTE)){
			writer.writeAll(csvData);
		}
	}

	private <T> ArrayList<T> addArrayToList(ArrayList<T> list, T[] data){
		for (T t : data) {
			list.add(t);
		}
		return list;
	}

	@Override
	public String getFileName() {
		return this.fileName;
	}
	
}
