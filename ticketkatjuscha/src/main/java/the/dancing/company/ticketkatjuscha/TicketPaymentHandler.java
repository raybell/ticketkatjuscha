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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import the.dancing.company.ticketkatjuscha.data.PaymentData;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

public class TicketPaymentHandler {
	private PrintStream logWriter;
	
	public TicketPaymentHandler(PrintStream logWriter){
		this.logWriter = logWriter;
	}
	
	public void updatePaymentList(PaymentData payment) throws IOException{
		File paymentListFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PAYMENT_LIST_FILE));
		
		if (!paymentListFile.exists()){
			makeInitialPaymentList();
		}
		
		FileInputStream fis = null;
		Workbook wb = null;
		
		try{
			fis = new FileInputStream(paymentListFile);
			wb = WorkbookFactory.create(fis);
			
			Sheet sheet = wb.getSheetAt(0);

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
