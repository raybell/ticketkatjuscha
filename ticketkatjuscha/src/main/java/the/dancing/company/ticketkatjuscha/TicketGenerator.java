package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.io.OutputStream;

import com.itextpdf.text.DocumentException;

public interface TicketGenerator {
    public void generate(String code, String checkCode, String ticketOwnerName, OutputStream output) throws IOException, DocumentException;
    
    public String getFileNameExtension();
}
