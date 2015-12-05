package the.dancing.company.ticketkatjuscha.data;

public class Ticket {
	private String code;
	private CodeData codeData;
	
	public Ticket(String code, CodeData codeData) {
		super();
		this.code = code;
		this.codeData = codeData;
	}

	public String getCode() {
		return code;
	}

	public CodeData getCodeData() {
		return codeData;
	}
}
