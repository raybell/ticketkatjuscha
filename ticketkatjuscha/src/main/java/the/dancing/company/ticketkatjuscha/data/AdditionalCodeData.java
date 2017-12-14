package the.dancing.company.ticketkatjuscha.data;

public class AdditionalCodeData {

	//the order of the enums correlates with the position in data export file, so never change it (it's uniqueness by position)
	public enum ADDITIONAL_DATA{
		TICKET_WITHDRAWED("Withdrawed"),
		TICKET_INVALIDATED("Invalidated"),
		TICKET_INVALIDATION_TIMESTAMP("Invalidation time"),
		TICKET_SEAT("Seat"),
		TICKET_EMAIL("eMail"),
		TICKET_PAYMENT_REMINDER("Payment Reminder Counter"),
		TICKET_PRICE("Ticket price"),
		TICKET_UNPAID("Unpaid"),
		TICKET_BOOKINGNUMBER("BookingNumber");

		private String columnTitle;
		ADDITIONAL_DATA(String columnTitle){
			this.columnTitle = columnTitle;
		}
		public String getColumnTitle(){
			return this.columnTitle;
		}
	};
	private String[] data = new String[ADDITIONAL_DATA.values().length];

	public void setAdditionalData(ADDITIONAL_DATA dataType, String value){
		data[dataType.ordinal()] = value;
	}

	public String getData(ADDITIONAL_DATA dataType){
		return data[dataType.ordinal()];
	}

	public boolean getDataAsBoolean(ADDITIONAL_DATA dataType){
		String data = getData(dataType);
		return Boolean.parseBoolean(data) || parseIntAsBool(data);
	}

	public int getDataAsInt(ADDITIONAL_DATA dataType){
		String data = getData(dataType);
		try {
			return Integer.parseInt(data);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	public String[] getDataList(){
		return data;
	}

	private static boolean parseIntAsBool(String val){
		try {
			return Integer.parseInt(val) == 1;
		} catch (NumberFormatException e) {
			return false;
		}
	}
}
