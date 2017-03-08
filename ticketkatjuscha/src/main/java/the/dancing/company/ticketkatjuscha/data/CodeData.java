package the.dancing.company.ticketkatjuscha.data;

public class CodeData {
	private String code;
	private String checkCode;
	private String name;
	private AdditionalCodeData addCodeData;
	
	public CodeData(String code, String checkCode, String name, AdditionalCodeData additionalData){
		this.code = code;
		this.checkCode = checkCode;
		this.name = name;
		this.addCodeData = additionalData;
	}
	
	public String getCode() {
		return code;
	}
	
	public String getCheckCode() {
		return checkCode;
	}
	
	public String getName() {
		return name;
	}
	
	public AdditionalCodeData getAdditionalCodeData(){
		return this.addCodeData;
	}
}
