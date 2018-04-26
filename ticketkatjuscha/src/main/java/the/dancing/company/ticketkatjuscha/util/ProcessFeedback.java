package the.dancing.company.ticketkatjuscha.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

public class ProcessFeedback {

	private StringBuilder messages = new StringBuilder();
	private PrintStream outputstream;
	
	public ProcessFeedback() {}
	
	public ProcessFeedback(PrintStream outputstream) {
		this.outputstream = outputstream;
	}
	
	public void println(String message) {
		this.messages.append(message);
		if (this.outputstream != null) {
			this.outputstream.println(messages);
		}
	}
	
	public void printStackTrace(Throwable t) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(bout);
		t.printStackTrace(pw);
		
		try {
			bout.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.println(bout.toString());
	}
	
	public String getMessages() {
		return this.messages.toString();
	}
}
