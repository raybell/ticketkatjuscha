/**
 * 
 */
package the.dancing.company.ticketkatjuscha;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.javatuples.Pair;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.Barcode39;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 *
 */
public class PDFTicketGenerator implements TicketGenerator {

    public PDFTicketGenerator() {
    }

    @Override
    public void generate(String code, String checkCode, String ticketOwnerName, Pair<String, String> seat, OutputStream output) throws IOException, DocumentException {
        InputStream is = new FileInputStream(PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_TEMPLATE_FILE));
        generate(code, checkCode, ticketOwnerName, seat, output, is);        
    }

    public void generate(String code, String checkCode, String ticketOwnerName, Pair<String, String> seat, OutputStream output, InputStream templateStream) throws IOException, DocumentException {
        
        PdfReader reader = new PdfReader(templateStream);
        
        int n = reader.getNumberOfPages();
        PdfStamper stamper = new PdfStamper(reader, output);
        for (int i = 1; i <= n; i++) {
            
            PdfContentByte cb = stamper.getOverContent(i);
            Rectangle pageSize = reader.getPageSize(i);
            //float width = pageSize.getWidth();
            float height = pageSize.getHeight();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, "winansi", true);
            
            // code1 -> (88mm / 102mm)
            drawText(code, mm2point(88.0f), height - mm2point(105.0f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            // code 2 -> (132mm / 102mm)
            drawText(checkCode, mm2point(132.0f), height - mm2point(105.0f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            // name -> (115mm / 63.6mm)
            drawText(ticketOwnerName, mm2point(115.0f), height - mm2point(63.6f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);

            // row
            drawText(seat.getValue0(), mm2point(52.0f), height - mm2point(73.5f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            //seat
            drawText(seat.getValue1(), mm2point(98.0f), height - mm2point(73.5f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            // horizontal barcode (13mm / 105mm)
            drawBarcode(code + " " + checkCode, mm2point(13.0f), height - mm2point(105.0f), 0, PdfContentByte.ALIGN_LEFT, cb);
            
            // vertical boarcode (29mm / 13mm)
            drawBarcode(code + " " + checkCode, mm2point(29.0f), height - mm2point(13.0f), 90, PdfContentByte.ALIGN_RIGHT, cb);
            
        }
        stamper.close();
        
        reader.close();
    }
    
    @Override
	public String getFileNameExtension() {
		return "pdf";
	}

    /**
     * 
     * @param code
     * @param xPt
     * @param yPt
     * @param rotation
     * @param align
     * @param cb
     * @throws DocumentException
     */
    private void drawBarcode(String code, float xPt, float yPt, int rotation, int align, PdfContentByte cb) throws DocumentException {

        if (rotation % 90 != 0 || rotation > 360 || rotation < 0) {
            throw new IllegalArgumentException("Invalid rotation value: " + rotation);
        }
        
        float angle = rotation * (float)Math.PI / 180;
        float cosinus = (float)Math.cos(angle);
        float sinus = (float)Math.sin(angle);
        
        Barcode39 bc = new Barcode39();
        bc.setBarHeight(mm2point(12));
        bc.setCode(code);
        bc.setX(1.2f);
        bc.setN(2.0f);
        
        Image img = bc.createImageWithBarcode(cb, BaseColor.BLACK, BaseColor.BLACK);
        img.setAlignment(align);
        float w = img.getPlainWidth();
        float h = img.getPlainHeight();
        
        if (rotation % 180 == 0) {
            cb.addImage(img, 
                        cosinus * w, 
                        sinus   * h, 
                        -sinus  * w, 
                        cosinus * h, 
                        xPt - (align == Image.RIGHT ? w : (align == Image.MIDDLE ? w / 2 : 0)), 
                        yPt);
        }
        else {
            cb.addImage(img, 
                        cosinus * h, 
                        sinus   * w, 
                        -sinus  * h, 
                        cosinus * w, 
                        xPt, 
                        yPt - (align == Image.RIGHT ? w : (align == Image.MIDDLE ? w / 2 : 0)) );
        }
        
        /*
               | a,   b,   0 |    
               | c,   d,   0 |
               | e,   f,   1 |
         */
    }

    /**
     * 
     * @param code
     * @param xPt
     * @param yPt
     * @param i
     * @param align
     * @param baseFont
     * @param fontSize
     * @param cb
     */
    private void drawText(String code, float xPt, float yPt, int i, int align, BaseFont baseFont, float fontSize, PdfContentByte cb) {
        cb.setFontAndSize(baseFont, fontSize);
        cb.beginText();
        cb.setRGBColorFill(0, 0, 0);
        cb.showTextAligned(align, code, xPt, yPt, 0);
        cb.endText();
    }

    /**
     * 
     * @param d
     * @return
     */
    private float mm2point(float d) {
        return d * (72.0f / 25.4f);
    }
}
