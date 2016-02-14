package the.dancing.company.ticketkatjuscha;

public final class CodeListHandlerFactory {
	private static final String CODELIST_NAME_XLSX = "codelist.xlsx";
	private static final String CODELIST_NAME_CSV = "codelist.csv";
	
	private CodeListHandlerFactory(){}
	
	public static ICodeListHandler produceHandler(){
		//use a prop, perhaps...
		return produceCSVHandler();
	}
	
	private static ICodeListHandler produceCSVHandler(){
		return new CodeListHandlerCSV(CODELIST_NAME_CSV);
	}
	
	private static ICodeListHandler produceXLSXHandler(){
		return new CodeListHandlerXLSX(CODELIST_NAME_XLSX);
	}
	
}
