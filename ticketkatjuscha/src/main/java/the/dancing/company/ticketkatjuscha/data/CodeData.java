package the.dancing.company.ticketkatjuscha.data;

public class CodeData {
	private String checkCode;
	private String name;
	private AdditionalCodeData addCodeData;
	
	public CodeData(String checkCode, String name, AdditionalCodeData additionalData){
		this.checkCode = checkCode;
		this.name = name;
		this.addCodeData = additionalData;
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
