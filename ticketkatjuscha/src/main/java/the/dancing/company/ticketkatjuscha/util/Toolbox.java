package the.dancing.company.ticketkatjuscha.util;

import java.io.File;
import java.net.URL;
import java.util.jar.Manifest;

import org.apache.poi.ss.usermodel.Row;

public class Toolbox {
	
	private Toolbox(){
	}
	
	public static String getProgVersion(){
		String progVersion = "unknown";
		try {
			URL resource = Toolbox.class.getClassLoader().getResource("META-INF/MANIFEST.MF");
			if (resource != null){
				Manifest mani = new Manifest(resource.openConnection().getInputStream());
				progVersion = mani.getMainAttributes().getValue("Build-Number");
			}
		} catch (Throwable e) {
			//could not read prog version
			e.printStackTrace();
		}
		return progVersion;
	}
	
	public static void makeBackupIfExists(File file){
		if (file.exists()){
			File fileBackup = new File(file.getName() + ".bak");
			if (fileBackup.exists()){
				fileBackup.delete();
			}
			file.renameTo(fileBackup);
		}
	}
	
	public static boolean isEmpty(String val) {
		return val == null || val.trim().length() == 0;
	}
	
	public static int safeParseInt(String val) {
		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	public static String getSafeStringCellValue(Row row, int cellId) {
		return row.getCell(cellId) == null ? "" :  row.getCell(cellId).getStringCellValue();
	}
	
	public static double getSafeNumericCellValue(Row row, int cellId) {
		return row.getCell(cellId) == null ? 0 : row.getCell(cellId).getNumericCellValue();
	}
}
