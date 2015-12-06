package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.io.OutputStream;

public class DummyTicketGenerator {
	public DummyTicketGenerator(){}
	
	public void generate(String code, String checkCode, String ticketOwnerName, OutputStream output) throws IOException{
		output.write((code + "/" + checkCode + "/" + ticketOwnerName).getBytes());
	}
}
