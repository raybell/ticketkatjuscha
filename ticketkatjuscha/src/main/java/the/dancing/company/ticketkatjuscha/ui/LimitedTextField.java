package the.dancing.company.ticketkatjuscha.ui;

import java.util.ArrayList;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class LimitedTextField extends JTextField {
	private static final long serialVersionUID = -4950783030374090164L;
	private int limit;
	private IToggleFieldParent toggleParent;
	private ArrayList<String[]> toggleChars = new ArrayList<>();
	
    public LimitedTextField(int columns, int limit, IToggleFieldParent toggleParent, String[]...toggleChars) {
        super(columns);
        this.limit = limit;
        this.toggleParent = toggleParent;
        for (String[] tc : toggleChars) {
        	if (tc.length != 2){
        		throw new RuntimeException("need two characters to toggle");
        	}
        	this.toggleChars.add(tc);
		}
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

            if (toggleParent.shouldToggle()){
	            for (String[] charArray : toggleChars) {
					for (int i=0;i<2;i++) {
						if (charArray[i].equals(str)){
							str = charArray[Math.abs(i-1)];
							break;
						}
					}
				}
            }
		
            if ((getLength() + str.length()) <= limit) {
                super.insertString(offset, str, attr);
            }
        }       

    }

}