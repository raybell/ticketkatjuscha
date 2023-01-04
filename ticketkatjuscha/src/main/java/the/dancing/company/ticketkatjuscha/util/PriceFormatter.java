package the.dancing.company.ticketkatjuscha.util;

import java.text.DecimalFormat;

public class PriceFormatter {

	public static String formatTicketPrice(double price) {
		if (((int)price) != price) {
			return new DecimalFormat("0.00").format(price);
		}else {
			return new DecimalFormat("0").format(price);
		}
	}
}
