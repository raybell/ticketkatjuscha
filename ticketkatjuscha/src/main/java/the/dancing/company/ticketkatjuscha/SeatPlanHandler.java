package the.dancing.company.ticketkatjuscha;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.javatuples.Pair;

import the.dancing.company.ticketkatjuscha.util.Toolbox;

public class SeatPlanHandler {

	public static final String CELL_VAL_SEAT_NAME_SEP = ":";
	public static final String CELL_VAL_SEAT_ROW_SEP = ".";
	public static final short CELL_COLOR_UNTOUCHED = -1; 
	public static final short CELL_COLOR_FREE = HSSFColorPredefined.GREEN.getIndex();
	public static final short CELL_COLOR_TAKEN = HSSFColorPredefined.RED.getIndex();
	public static final short CELL_COLOR_INACTIVE = HSSFColorPredefined.GREY_25_PERCENT.getIndex();

	private PrintStream logWriter;
	
	public SeatPlanHandler(PrintStream logWriter){
		this.logWriter = logWriter;
	}
	
	public void makeNewPlan() throws IOException {
		File planConfig = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_SEATPLAN_CONFIG));
		File planFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_SEATPLAN_FILE));
		
		if (!planConfig.exists()){
			throw new IOException("Could not create seatplan because seatplan config '" + planConfig.getName() + "' isn't available");
		}
		
		//backup old plan
		Toolbox.makeBackupIfExists(planFile);
		
		//make new plan
		int lineNumber = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(planConfig)); HSSFWorkbook wb = new HSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(planFile)) {
			Sheet sheet = wb.createSheet();
		    String line;
		    int rowCounter = 1;
		    while ((line = br.readLine()) != null) {
		    	Row row = sheet.createRow(lineNumber);
		    	if (line.trim().length() > 0){
		    		addRowOfSeats(wb, row, rowCounter, line);
		    		rowCounter++;
		    	}
		    	lineNumber++;
		    }
            wb.write(fileOut);
		}
		
		File planForEmailFile = new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_SEATPLAN_EMAILTEMPLATE_FILE));
		Toolbox.makeBackupIfExists(planForEmailFile);
		Files.copy(planFile.toPath(), planForEmailFile.toPath());
	}
	
	public void freeSeats(List<Pair<String, String>> seats) throws IOException{
		setSeatsInPlan(new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_SEATPLAN_FILE)), 
				       seats, null, CELL_COLOR_FREE, CELL_COLOR_UNTOUCHED);
	}
	
	public void markSeatsAsSold(List<Pair<String, String>> seats, String buyerName, String bookingNumber) throws IOException{
		setSeatsInPlan(new File(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_SEATPLAN_FILE)),
				       seats, buyerName + "\n" + bookingNumber, CELL_COLOR_TAKEN, CELL_COLOR_UNTOUCHED);
	}
	
	public void markSeatsForEmailNotification(File tmpPlan, List<Pair<String, String>> seats) throws IOException{
		setSeatsInPlan(tmpPlan, seats, null, CELL_COLOR_TAKEN, CELL_COLOR_INACTIVE);
	}
	
	public void convertSeatPlanToPDF(File seatPlan, File pdfFile) throws IOException{
		//tbd
	}
	
	private void setSeatsInPlan(File seatplanFile, List<Pair<String, String>> seats, String seatIDAddon, short cellColor, short inactiveCellColor) throws IOException{
		List<Pair<String, String>> clonedSeats = new ArrayList<>(seats);
		FileInputStream fis = null;
		Workbook wb = null;
		
		if (!seatplanFile.exists()){
			makeNewPlan();
		}
		
		try{
			fis = new FileInputStream(seatplanFile);
			wb = WorkbookFactory.create(fis);
			
			Sheet sheet = wb.getSheetAt(0);
			Drawing<?> patr = sheet.createDrawingPatriarch();
			for (int rowIndex = 0; rowIndex < PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_SEATPLAN_SCANAREA_MAXROW); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null){
					continue;
				}
				for (int cellIndex = 0; cellIndex < PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_SEATPLAN_SCANAREA_MAXCOL); cellIndex++){
					Cell cell = row.getCell(cellIndex);
					if (cell == null){
						continue;
					}
					String seatId = identifySeatCell(cell);
					if (seatId != null){
						//found a seat
						Pair<String, String> seatPair = searchSeatInListAndRemove(seatId, clonedSeats);
						if (seatPair != null){
							//found seat in list
							//cell.setCellValue(seatId + ((seatIDAddon != null && seatIDAddon.trim().length() > 0)?CELL_VAL_SEAT_NAME_SEP + " " + seatIDAddon : ""));
							
							if (seatIDAddon != null){
								Comment comment = patr.createCellComment(new HSSFClientAnchor(0, 0, 0, 0, (short) 4, 2, (short) 6, 5)); 
								comment.setString(new HSSFRichTextString(((seatIDAddon != null && seatIDAddon.trim().length() > 0)?seatIDAddon : "")));
								cell.setCellComment(comment);
							}else{
								cell.removeCellComment();
							}
							
							CellStyle cs = wb.createCellStyle();
							cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
							cs.setFillForegroundColor(cellColor);
							cell.setCellStyle(cs);
						}else if (inactiveCellColor != CELL_COLOR_UNTOUCHED){
							//it seems we have to inactivate the seats not found in seatlist
							CellStyle cs = wb.createCellStyle();
							cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
							cs.setFillForegroundColor(inactiveCellColor);
							cell.setCellStyle(cs);
						}
					}else{
						//seems to be no seat, so let it bleed
					}
				}
			}
			if (clonedSeats.size() > 0){
				//seems i could not find some seats in plan
				log("Warning: could not find following seats in seatplan: " + clonedSeats);
			}
			
			//close fileinputstream before updating
			fis.close();
			
			//update the seatplan
			try(FileOutputStream fileOut = new FileOutputStream(seatplanFile)){
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
	
	private void addRowOfSeats(Workbook wb, Row row, int rowIndex, String rowSeats){
		StringTokenizer st = new StringTokenizer(rowSeats, ",");
		int seatCounter = 1;
		
		while(st.hasMoreTokens()){
			String token = st.nextToken();
			int beginIndex = 0;
			int endIndex = 0;
			int separatorIndex = token.indexOf("-");
			if (separatorIndex > -1){
				beginIndex = Integer.parseInt(token.substring(0, separatorIndex));
				endIndex = Integer.parseInt(token.substring(separatorIndex + 1));
			}else{
				beginIndex = Integer.parseInt(token);
				endIndex = beginIndex;
			}
			for(int i=beginIndex; i<=endIndex; i++){
				Cell cell = row.createCell(i-1, CellType.STRING);
				cell.setCellValue(rowIndex + CELL_VAL_SEAT_ROW_SEP + seatCounter);
				CellStyle cs = wb.createCellStyle();
				cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
				cs.setFillForegroundColor(CELL_COLOR_FREE);
				cell.setCellStyle(cs);
				seatCounter++;
			}
		}
	}
	
	private Pair<String, String> searchSeatInListAndRemove(String seatID, List<Pair<String, String>> seats) {
		Pair<String, String> seatIDPair = parseSeatId(seatID);
		if (seatIDPair != null){
			for (Pair<String, String> seat : seats) {
				if (seat.equals(seatIDPair)){
					seats.remove(seat);
					return seatIDPair;
				}
			}
		}
		return null;
	}
	
	private Pair<String, String> parseSeatId(String seatId){
		StringTokenizer st = new StringTokenizer(seatId, CELL_VAL_SEAT_ROW_SEP);
		if (st.countTokens() == 2){
			return new Pair<String, String>(st.nextToken(), st.nextToken());
		}else{
			log("invalid seatID found in seatplan: " + seatId);
		}
		return null;
	}
	
	private String identifySeatCell(Cell cell){
		if (cell.getCellStyle().getFillForegroundColor() == CELL_COLOR_FREE || cell.getCellStyle().getFillForegroundColor() == CELL_COLOR_TAKEN){
			String cellVal = cell.getStringCellValue();
			StringTokenizer st = new StringTokenizer(cellVal, CELL_VAL_SEAT_NAME_SEP);
			if (st.hasMoreTokens()){
				return st.nextToken();
			}
		}
		return null;
	}
	
	private void log(String message){ 
		if (this.logWriter != null){
			logWriter.println(message);
		}
	}
}
