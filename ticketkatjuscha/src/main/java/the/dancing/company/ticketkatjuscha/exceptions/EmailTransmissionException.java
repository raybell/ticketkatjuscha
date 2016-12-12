package the.dancing.company.ticketkatjuscha.exceptions;

public class EmailTransmissionException extends Exception{
	private static final long serialVersionUID = -4692218503719692436L;

	public EmailTransmissionException(String message, Throwable cause){
		super(message, cause);
	}
}
