package the.dancing.company.ticketkatjuscha.data;

public class TicketOrder {
	
	private int _ticketAmount;
	private double _ticketPrice;

	public TicketOrder(int ticketAmount, double ticketPrice) {
		_ticketAmount = ticketAmount;
		_ticketPrice = ticketPrice;
	}

	public int getTicketAmount() {
		return _ticketAmount;
	}

	public double getTicketPrice() {
		return _ticketPrice;
	}
	
}
