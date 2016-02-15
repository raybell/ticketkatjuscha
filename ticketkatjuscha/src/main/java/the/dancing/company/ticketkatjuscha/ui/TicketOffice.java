package the.dancing.company.ticketkatjuscha.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import the.dancing.company.ticketkatjuscha.CodeListHandlerFactory;
import the.dancing.company.ticketkatjuscha.ICodeListHandler;
import the.dancing.company.ticketkatjuscha.ITicketProcessFailed;
import the.dancing.company.ticketkatjuscha.PropertyHandler;
import the.dancing.company.ticketkatjuscha.TicketExpert;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;

public class TicketOffice extends JFrame{

	private static final long serialVersionUID = -7245404133018518793L;

	private JLabel pendingTicketLabel;
	
	public TicketOffice(){
		super("Ticket Katjuscha");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		JTabbedPane tabPanel = new JTabbedPane();
		
		//******** office panel ********
		JPanel officePanel = new JPanel();
		officePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 30));
		
		JLabel lTicketAmount = new JLabel("Wieviel?");
		officePanel.add(lTicketAmount);
		JTextField tfTicketAmount = new JTextField(3);
		officePanel.add(tfTicketAmount);
		
		JLabel lTicketOwner = new JLabel("Für wen?");
		officePanel.add(lTicketOwner);
		JTextField tfTicketOwner = new JTextField(7);
		officePanel.add(tfTicketOwner);
		
		JButton makeTickets = new JButton("Gib's mir");
		officePanel.add(makeTickets);
		
		makeTickets.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int ticketAmount;
				if (!isFilled(tfTicketAmount)){
					showErrorDialog("Wieviel, wieviel, sag mir wieviel willst du haben?");
					return;
				}
				try {
					ticketAmount = Integer.parseInt(tfTicketAmount.getText());
				} catch (NumberFormatException e1) {
					showErrorDialog("Ich glaub du willst mit mir spielen. Gib mir eine Zahl, nicht zu gross, nicht zu klein, irgendwas dazwischen.");
					return;
				}
				if (new TicketExpert(ticketAmount, tfTicketOwner.getText()).process(new ITicketProcessFailed() {
					@Override
					public boolean handleFailedState(Exception cause) {
						showErrorDialog("Huiuiuiui sagt die UI, da ging wohl was in die Hose: \n\n" + cause.toString() + "\n\nMehr auf der Konsole...");
						return false;
					}
				}, System.out)){
					showInfoDialog("Yeah!!!", "We rocked the office.");
				}
			}
		});
		tabPanel.addTab("Ticket Office", officePanel);
		
		//************ event panel *************
		JPanel eventPanel = new JPanel();
		eventPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		JLabel ticketCodeLabel = new JLabel("Ticket code");
		eventPanel.add(ticketCodeLabel);
		LimitedTextField ticketCode = new LimitedTextField(10, calculateMaxCodeLength());
		ticketCode.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER){
					checkTicketCodeHandler(ticketCode);
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		eventPanel.add(ticketCode);
		
		JButton checkCodeButton = new JButton("Check code");
		checkCodeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkTicketCodeHandler(ticketCode);
			}
		});
		eventPanel.add(checkCodeButton);
		
		pendingTicketLabel = new JLabel();
		eventPanel.add(pendingTicketLabel);
		
		JLabel explainLabel = new JLabel("<html>Das Eingabefeld unterstützt zwei Verifizierungsmethoden.<br>Automatisch: Eingabe von Ticketcode + Prüfcode durch Leerzeichen getrennt.<br>Manuell: Eingabe des Ticketcodes + manueller Verifizerung des Prüfcodes.</html>");
		eventPanel.add(explainLabel);
		
		tabPanel.addTab("Event mode", eventPanel);
		tabPanel.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (tabPanel.getSelectedIndex() == 1){
					updatePendingTicketsLabel(null);
					ticketCode.grabFocus();
				}
			}
		});
		
		add(tabPanel);
		
		updatePendingTicketsLabel(null);
		
		setSize(450, 170);
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}
	
	private void updatePendingTicketsLabel(Map<String, CodeData> codeList){
		if (codeList == null){
			ICodeListHandler codeListHandler = CodeListHandlerFactory.produceHandler();
			try {
				codeList = codeListHandler.loadCodeList();
			} catch (IOException e) {
				showErrorDialog(e.getMessage());
				return;
			}
		}
		
		this.pendingTicketLabel.setText(pendingTicketsDisplay(codeList));
	}
	
	private static String pendingTicketsDisplay(Map<String, CodeData> codeList){
		int allValidTickets = 0;
		int invalidateTickets = 0;
		
		for (CodeData codeData : codeList.values()) {
			if (!codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_WITHDRAWED)){
				allValidTickets++;
				if (codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_INVALIDATED)){
					invalidateTickets++;
				}
			}
		}
		return invalidateTickets + "/" + allValidTickets;
	}
	
	private static int calculateMaxCodeLength(){
		int maxCodeLength = PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_MAX_CODE_DIGITS)
							+ PropertyHandler.getInstance().getPropertyInt(PropertyHandler.PROP_MAX_CHECKCODE_DIGITS)
							+ 1;
		return maxCodeLength > 1 ? maxCodeLength : 10;
	}
	
	private void checkTicketCodeHandler(JTextField ticketCode){
		checkTicketCode(ticketCode);
		
		//clear text
		ticketCode.setText("");
		
		//set focus to input field
		ticketCode.grabFocus();
	}
	
	private void checkTicketCode(JTextField ticketCode){
		if (ticketCode.getText() == null || ticketCode.getText().length() == 0){
			showErrorDialog("If you give me a ticket code, i will verify it for you.");
			return;
		}
		
		//load ticket codes
		ICodeListHandler codeListHandler = CodeListHandlerFactory.produceHandler();
		Map<String, CodeData> codeList = null;
		try {
			codeList = codeListHandler.loadCodeList();
		} catch (IOException e) {
			showErrorDialog(e.getMessage());
			return;
		}
		
		//check it
		String inputCode = null;
		String inputCheckcode = null;
		StringTokenizer inputTokenizer = new StringTokenizer(ticketCode.getText(), " ");
		inputCode = inputTokenizer.nextToken();
		if (inputTokenizer.hasMoreTokens()){
			inputCheckcode = inputTokenizer.nextToken();
		}
		
		CodeData codeData = codeList.get(inputCode);
		if (codeData == null){
			showInfoDialogForInvalidTicket("Ungültiger code", "Konnte den Code %s nicht in der Codeliste finden.", inputCode);
			return;
		}
		
		if (codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_WITHDRAWED)){
			showInfoDialogForInvalidTicket("Ungültiges Ticket", "Das Ticket mit dem Code %s ist leider ungültig.", inputCode);
			return;
		}
		
		if (codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_INVALIDATED)){
			showInfoDialogForInvalidTicket("Ungültiges Ticket", "Das Ticket mit dem Code %s wurde bereits entwerted am " + codeData.getAdditionalCodeData().getData(ADDITIONAL_DATA.TICKET_INVALIDATION_TIMESTAMP), inputCode);
			return;
		}
		
		if (inputCheckcode == null || inputCheckcode.trim().length() == 0){
			//no checkcode provided, but code is valid so far
			if (showYesNoDialog("Prüfcode prüfen", "Der Code %s ist gültig. Der dazugehörige Prüfcode lautet: %s<br>Korrekt?", inputCode, codeData.getCheckCode()) == JOptionPane.NO_OPTION){
				//invalid checkcode
				return;
			}else{
				//valid checkcode, proceed
			}
		}else{
			//we have a checkcode, check it
			if (!codeData.getCheckCode().equalsIgnoreCase(inputCheckcode)){
				showInfoDialogForInvalidTicket("Ungültiger Prüfcode", "Der Prüfcode des Tickets %s stimmt nicht mit dem Prüfcode der CodeListe %s überein.", inputCheckcode, codeData.getCheckCode());
				return;
			}
		}
		
		//seems the ticket is valid
		codeData.getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_INVALIDATED, "1");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		codeData.getAdditionalCodeData().setAdditionalData(ADDITIONAL_DATA.TICKET_INVALIDATION_TIMESTAMP, sdf.format(new Date()));
		
		//update code list
		try {
			codeListHandler.saveCodeList(codeList);
		} catch (IOException e) {
			showErrorDialog(e.getMessage());
			return;
		}
		
		showInfoDialog("Gültiges Ticket", "Das Ticket mit dem Code " + inputCode + " ist gültig und wurde entwertet.");
		
		updatePendingTicketsLabel(codeList);
	}
	
	private static boolean isFilled(JTextField textField){
		return textField.getText() != null && textField.getText().trim().length() > 0;
	}
	
	private void showErrorDialog(String message){
		new JOptionPane(breakTheLineIfNotBreakedYet(message)).createDialog(TicketOffice.this, "Da stimmt was nicht").setVisible(true);
	}
	
	private void showInfoDialog(String title, String message){
		new JOptionPane(breakTheLineIfNotBreakedYet(message)).createDialog(TicketOffice.this, title).setVisible(true);
	}
	
	private void showInfoDialogForInvalidTicket(String title, String message, String... params){
		setOptionPaneBackground(Color.RED);
		JOptionPane.showMessageDialog(this, makeHTMLMessage(message, params), title, JOptionPane.WARNING_MESSAGE );
		resetOptionPaneBackground();
	}
	
	private int showYesNoDialog(String title, String message, String... params){
		return JOptionPane.showOptionDialog(this, makeHTMLMessage(message, params), title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
	}
	
	private static String makeHTMLMessage(String message, String... params){
		StringWriter sw = new StringWriter();
		sw.append("<html>");
		
		String[] messageParams = new String[params.length];
		for (int i=0;i<params.length;i++) {
			messageParams[i] = "<span style='font-size:larger;'>" + params[i] + "</span>";
		}
		
		sw.append(String.format(message, messageParams));
		sw.append("</html>");
		return sw.toString();
	}
	
	private static void resetOptionPaneBackground(){
		UIManager.put("OptionPane.background", null);
		UIManager.put("Panel.background", null);
	}
	
	private static void setOptionPaneBackground(Color color){
		UIManager.put("OptionPane.background", color);
		UIManager.put("Panel.background", color);
	}
	
	private static String breakTheLineIfNotBreakedYet(String message){
		int maxLength = 100;
		StringBuilder sb = new StringBuilder(message);
		
		for(int i=maxLength; i<message.length(); i+=maxLength){
			int lb = sb.substring(i-maxLength, i-1).indexOf("\n");
			if (lb > -1){
				i = i - maxLength + lb + 1;
			}else{
				sb.insert(i, "\n");
				i++;
			}
		}
		return sb.toString();
	}
}
