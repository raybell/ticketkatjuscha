package the.dancing.company.ticketkatjuscha;

import java.io.IOException;
import java.io.OutputStream;

import org.javatuples.Pair;

import com.itextpdf.text.DocumentException;

public interface TicketGenerator {
    public void generate(String code, String checkCode, String ticketOwnerName, Pair<String, String> seat, int price, OutputStream output) throws IOException, DocumentException;

    public String getFileNameExtension();
}
