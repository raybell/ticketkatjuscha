package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import the.dancing.company.ticketkatjuscha.data.PaymentData;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

public class TicketPaymentHandler {
	private static final int SHEET_PAYMENTS = 0;
	private static final int SHEET_CALCULATION = 1;
	private static final int CELL_ID_BOOKING_NO = 0;
	private static final int CELL_ID_CUSTOMERNAME = 1;
	private static final int CELL_ID_ORDER_AMOUNT = 4;
	private static final int CELL_ID_PAYMENT_METHOD = 7;
	private static final int CELL_ID_PAID = 5;
	private PrintStream logWriter;
	
	public TicketPaymentHandler(PrintStream logWriter){
		this.logWriter = logWriter;
	}
	
	public boolean setToPaid(String bookingNo, Number totalSumNum) throws IOException{
		File paymentListFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PAYMENT_LIST_FILE));
		
		if (!paymentListFile.exists()){
			makeInitialPaymentList();
		}
		
		FileInputStream fis = null;
		Workbook wb = null;
		try{
			fis = new FileInputStream(paymentListFile);
			wb = WorkbookFactory.create(fis);
			
			Sheet sheet = wb.getSheetAt(SHEET_PAYMENTS);
			for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				Cell cell = row.getCell(CELL_ID_BOOKING_NO);
				String sheetBookingNo = cell.getStringCellValue();
				if (row == null || Toolbox.isEmpty(sheetBookingNo)){
					return false;
				}
				if (sheetBookingNo.equalsIgnoreCase(bookingNo)) {
					String oldValue = row.getCell(CELL_ID_PAID).getStringCellValue();
					//set paid flag
					row.getCell(CELL_ID_PAID).setCellValue("yes");
					
					if (Toolbox.isEmpty(oldValue) || oldValue.equalsIgnoreCase("no")) {
						//add payment to calculation sheet
						addPaymentToCalcSheet(wb, row, totalSumNum);
					}
					
					try(FileOutputStream fileOut = new FileOutputStream(paymentListFile)){
						wb.write(fileOut);
					}
					return true;
				}
			}
		}catch(EncryptedDocumentException | InvalidFormatException e){
			throw new IOException(e);
		}finally{
			if (wb != null){
				wb.close();
			}
		}
		return false;
	}
	
	private void addPaymentToCalcSheet(Workbook wb, Row paymentRow, Number totalSumNum) throws IOException{
		Sheet sheet = getSheet(wb, SHEET_CALCULATION);
		
		if (sheet == null) {
			throw new IOException("could not find calculation sheet at index " + SHEET_CALCULATION);
		}
		 
		Row row = sheet.createRow(sheet.getLastRowNum()+1);
		SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.YY");
		int cellCounter = 0;
		Cell cell = row.createCell(cellCounter++, CellType.NUMERIC);
		cell.getCellStyle().setDataFormat(wb.createDataFormat().getFormat("dd.MM.YY"));
		cell.getCellStyle().setAlignment(HorizontalAlignment.CENTER);
		cell.setCellValue(sdfDate.format(new Date()));
		cell = row.createCell(cellCounter++, CellType.STRING);
		cell.setCellValue("Ticketverkauf");
		cell = row.createCell(cellCounter++, CellType.STRING);
		cell.setCellValue(getSafeStringValue(paymentRow, CELL_ID_BOOKING_NO) + " " + getSafeStringValue(paymentRow, CELL_ID_PAYMENT_METHOD) + " " + getSafeStringValue(paymentRow, CELL_ID_CUSTOMERNAME));
		cell = row.createCell(cellCounter++, CellType.NUMERIC);
		cell.getCellStyle().setDataFormat(wb.createDataFormat().getFormat("#,##0.00 €"));
		
		double safeNumericValue = getSafeNumericValue(paymentRow, CELL_ID_ORDER_AMOUNT);
		if (totalSumNum != null) {
			if (totalSumNum.doubleValue() < safeNumericValue) {
				throw new RuntimeException("provided total sum is smaller than overall ticket price, no no no, you can pay more, but not less.");
			}else {
				safeNumericValue = totalSumNum.doubleValue();
			}
		}
		cell.setCellValue(safeNumericValue);
	}
	
	private Sheet getSheet(Workbook wb, int sheetIndex) {
		try {
			return wb.getSheetAt(sheetIndex);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	private String getSafeStringValue(Row row, int cellId) {
		Cell cell = row.getCell(cellId);
		if (cell == null) {
			return "";
		}
		return cell.getStringCellValue();
	}
	
	private double getSafeNumericValue(Row row, int cellId) {
		Cell cell = row.getCell(cellId);
		if (cell == null) {
			return 0;
		}
		return cell.getNumericCellValue();
	}
	
	public void insertPayment(PaymentData payment) throws IOException{
		File paymentListFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PAYMENT_LIST_FILE));
		
		if (!paymentListFile.exists()){
			makeInitialPaymentList();
		}
		
		FileInputStream fis = null;
		Workbook wb = null;
		
		try{
			fis = new FileInputStream(paymentListFile);
			wb = WorkbookFactory.create(fis);
			
			Sheet sheet = wb.getSheetAt(SHEET_PAYMENTS);

			Row row = sheet.createRow(sheet.getLastRowNum()+1);
			SimpleDateFormat sdfDate = new SimpleDateFormat("dd.MM.YYYY");
			int cellCounter = 0;
			Cell cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getBookingNumber());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getCustomerName());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getCustomerEmail());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getNumberOfTickets());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getOrderPrice());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.isPaid()?"yes":"no");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(sdfDate.format(new Date(payment.getOrderDate())));
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getPaymentMethod());
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.isAddedToEmaiList()?"yes":"no");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue(payment.getCommunity());
			
			//close fileinputstream before updating
			fis.close();
			
			//update the seatplan
			try(FileOutputStream fileOut = new FileOutputStream(paymentListFile)){
				wb.write(fileOut);
			}
		}catch(EncryptedDocumentException | InvalidFormatException e){
			throw new IOException(e);
		}finally{
			if (wb != null){
				wb.close();
			}
		}
	}
	
	public void makeInitialPaymentList() throws IOException{
		File paymentListFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PAYMENT_LIST_FILE));
		
		//backup old plan
		Toolbox.makeBackupIfExists(paymentListFile);
		
		//make new plan
		try (HSSFWorkbook wb = new HSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(paymentListFile)) {
			Sheet sheet = wb.createSheet();
			Row row = sheet.createRow(0);
			
			int cellCounter = 0;
			Cell cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Booking number");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Customer name");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Customer email address");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Number of ordered tickets");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Order amount");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Paid");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Order date");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Payment method");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Added to email distribution list");
			cell = row.createCell(cellCounter++, CellType.STRING);
			cell.setCellValue("Community");
			
			wb.write(fileOut);
		}
	}
}
