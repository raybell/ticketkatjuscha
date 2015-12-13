/**
 * 
 */
package the.dancing.company.ticketkatjuscha;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    private static final String TEMPLATE_FILE_NAME = "template.pdf";
    
    private String dir; 
    
    public PDFTicketGenerator(String ticketGenDir) {
        this.dir = ticketGenDir;
    }

    @Override
    public void generate(String code, String checkCode, String ticketOwnerName, OutputStream output) throws IOException, DocumentException {

        //InputStream is = this.getClass().getClassLoader().getResourceAsStream(TEMPLATE_FILE_NAME);
        InputStream is = new FileInputStream(dir + File.separator + TEMPLATE_FILE_NAME);
        
        PdfReader reader = new PdfReader(is);
        
        int n = reader.getNumberOfPages();
        PdfStamper stamper = new PdfStamper(reader, output);
        for (int i = 1; i <= n; i++) {
            
            PdfContentByte cb = stamper.getOverContent(i);
            Rectangle pageSize = reader.getPageSize(i);
            //float width = pageSize.getWidth();
            float height = pageSize.getHeight();

            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, "UTF-8", true);
            
            // code1 -> (13mm / 131mm)
            drawText(code, mm2point(13.0f), height - mm2point(131.0f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            // code 2 -> (125mm / 131mm)
            drawText(checkCode, mm2point(125.0f), height - mm2point(131.0f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);
            
            // name -> (132mm / 62mm)
            drawText(ticketOwnerName, mm2point(115.0f), height - mm2point(63.6f), 0, PdfContentByte.ALIGN_LEFT, baseFont, 11.0f, cb);

            // horizontal barcode (13mm / 105mm)
            drawBarcode(code + " " + checkCode, mm2point(13.0f), height - mm2point(105.0f), 0, PdfContentByte.ALIGN_LEFT, cb);
            
            // vertical boarcode (25mm / 40mm)
            drawBarcode(code + " " + checkCode, mm2point(25.0f), height - mm2point(40.0f), 90, PdfContentByte.ALIGN_RIGHT, cb);
            
        }
        stamper.close();
        
        reader.close();
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
        bc.setBarHeight(mm2point(8));
        bc.setCode(code);
        
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