package the.dancing.company.ticketkatjuscha.data;

public class CodeData {
	private String checkCode;
	private String name;
	
	public CodeData(String checkCode, String name){
		this.checkCode = checkCode;
		this.name = name;
	}
	
	public String getCheckCode() {
		return checkCode;
	}
	
	public String getName() {
		return name;
	}
}
