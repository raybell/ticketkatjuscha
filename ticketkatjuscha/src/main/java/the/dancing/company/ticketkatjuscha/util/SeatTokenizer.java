package the.dancing.company.ticketkatjuscha.util;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.javatuples.Pair;

public class SeatTokenizer {
	public static String ROW_SEAT_SEPARATOR = ".";
	public static String SEAT_LIST_SEPARATOR = ",";
	
	public static List<Pair<String, String>> parseSeats(String text) throws NoSuchElementException{
		StringTokenizer seatsTokens = new StringTokenizer(text, SEAT_LIST_SEPARATOR);
		
		ArrayList<Pair<String, String>> list = new ArrayList<>();
		while(seatsTokens.hasMoreTokens()){
			StringTokenizer seatsCordinateTokens = new StringTokenizer(seatsTokens.nextToken(), ROW_SEAT_SEPARATOR);
			list.add(new Pair<String, String>(seatsCordinateTokens.nextToken().trim(), seatsCordinateTokens.nextToken().trim()));
		}
		
		return list;
	}
}
