package the.dancing.company.ticketkatjuscha;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import the.dancing.company.ticketkatjuscha.data.CodeData;

public interface ICodeListHandler {
	public Map<String, CodeData> loadCodeList() throws FileNotFoundException, IOException;
	
	public void saveCodeList(Map<String, CodeData> codeList) throws IOException;
	
	public String getFileName();
}
