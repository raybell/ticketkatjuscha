package the.dancing.company.ticketkatjuscha;

public interface ITicketProcessFailed {
	public boolean handleFailedState(String message, Exception cause);
}
