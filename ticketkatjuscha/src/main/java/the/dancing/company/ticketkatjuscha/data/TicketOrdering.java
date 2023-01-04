package the.dancing.company.ticketkatjuscha.data;

import java.util.ArrayList;
import java.util.List;

public class TicketOrdering {
	
	private List<TicketOrder> _ticketOrders = new ArrayList<>();
	
	public TicketOrdering() {
	}

	public TicketOrdering addTicketOrder(int ticketAmount, double ticketPrice) {
		_ticketOrders.add(new TicketOrder(ticketAmount, ticketPrice));
		return this;
	}
	
	public int getTicketAmountSumUp() {
		return _ticketOrders.stream().map(TicketOrder::getTicketAmount).reduce(0, Integer::sum);
	}
	
	public double getTicketPriceSumUp() {
		return _ticketOrders.stream().map(t -> t.getTicketAmount() * t.getTicketPrice()).reduce(0.0, Double::sum);
	}

	public List<TicketOrder> getTicketOrders() {
		return _ticketOrders;
	}
}
