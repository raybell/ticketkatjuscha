package the.dancing.company.ticketkatjuscha.data;

public class PaymentData {
	private String bookingNumber = "";
	private String customerName = "";
	private String customerEmail = "";
	private int numberOfTickets = 0;
	private long orderPrice = 0;
	private long orderDate = 0;
	private boolean paid = false;
	private String paymentMethod = "";
	private boolean addedToEmaiList = false;
	private String community;
	
	public String getBookingNumber() {
		return bookingNumber;
	}
	public void setBookingNumber(String bookingNumber) {
		this.bookingNumber = bookingNumber;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public String getCustomerEmail() {
		return customerEmail;
	}
	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}
	public int getNumberOfTickets() {
		return numberOfTickets;
	}
	public void setNumberOfTickets(int numberOfTickets) {
		this.numberOfTickets = numberOfTickets;
	}
	public long getOrderPrice() {
		return orderPrice;
	}
	public void setOrderPrice(long orderPrice) {
		this.orderPrice = orderPrice;
	}
	public long getOrderDate() {
		return orderDate;
	}
	public void setOrderDate(long orderDate) {
		this.orderDate = orderDate;
	}
	public boolean isPaid() {
		return paid;
	}
	public void setPaid(boolean paid) {
		this.paid = paid;
	}
	public String getPaymentMethod() {
		return paymentMethod;
	}
	public void setPaymentMethod(String paymentMethod) {
		this.paymentMethod = paymentMethod;
	}
	public boolean isAddedToEmaiList() {
		return addedToEmaiList;
	}
	public void setAddedToEmaiList(boolean addedToEmaiList) {
		this.addedToEmaiList = addedToEmaiList;
	}
	public String getCommunity() {
		return community;
	}
	public void setCommunity(String community) {
		this.community = community;
	}
}
