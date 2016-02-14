package the.dancing.company.ticketkatjuscha.ui;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class LimitedTextField extends JTextField {
	private static final long serialVersionUID = -4950783030374090164L;
	private int limit;

    public LimitedTextField(int columns, int limit) {
        super(columns);
        this.limit = limit;
    }

    @Override
    protected Document createDefaultModel() {
        return new LimitDocument();
    }

    private class LimitDocument extends PlainDocument {
		private static final long serialVersionUID = -4883571864756544454L;

		@Override
        public void insertString( int offset, String  str, AttributeSet attr ) throws BadLocationException {
            if (str == null) return;

            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }       

    }

}