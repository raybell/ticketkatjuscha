package the.dancing.company.ticketkatjuscha.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.javatuples.Pair;

import the.dancing.company.ticketkatjuscha.CodeListHandlerFactory;
import the.dancing.company.ticketkatjuscha.ICodeListHandler;
import the.dancing.company.ticketkatjuscha.ITicketProcessFailed;
import the.dancing.company.ticketkatjuscha.PropertyHandler;
import the.dancing.company.ticketkatjuscha.SeatPlanHandler;
import the.dancing.company.ticketkatjuscha.TicketExpert;
import the.dancing.company.ticketkatjuscha.TicketNotifier;
import the.dancing.company.ticketkatjuscha.TicketPaymentHandler;
import the.dancing.company.ticketkatjuscha.data.AdditionalCodeData.ADDITIONAL_DATA;
import the.dancing.company.ticketkatjuscha.data.CodeData;
import the.dancing.company.ticketkatjuscha.util.SeatTokenizer;
import the.dancing.company.ticketkatjuscha.util.Toolbox;

public class TicketOffice extends JFrame implements IToggleFieldParent{

	private static final long serialVersionUID = -7245404133018518793L;

	private JLabel pendingTicketLabel;
	private JCheckBox toggleCheckBox;


	public TicketOffice(){
		super("Ticket Katjuscha");
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		JTabbedPane tabPanel = new JTabbedPane();

		//******** office panel ********
		JPanel filler20 = new JPanel();
		filler20.setPreferredSize(new Dimension(20, 1));

		JPanel filler50 = new JPanel();
		filler50.setPreferredSize(new Dimension(50, 1));
		
		JPanel filler100 = new JPanel();
		filler100.setPreferredSize(new Dimension(100, 1));
		
		JPanel filler500 = new JPanel();
		filler500.setPreferredSize(new Dimension(500, 5));

		JPanel officePanel = new JPanel();
		officePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));

//		officePanel.add(filler500);
//		officePanel.add(filler20);

		JLabel lTicketAmount = new JLabel("Wieviel?");
		officePanel.add(lTicketAmount);
		JTextField tfTicketAmount = new JTextField(3);
		officePanel.add(tfTicketAmount);

		JLabel lTicketOwner = new JLabel("Für wen?");
		officePanel.add(lTicketOwner);
		JTextField tfTicketOwner = new JTextField(7);
		officePanel.add(tfTicketOwner);

		JLabel lTicketRecipient = new JLabel("Wohin?");
		officePanel.add(lTicketRecipient);
		JTextField tfTicketRecipient = new JTextField(7);
		officePanel.add(tfTicketRecipient);

		officePanel.add(filler20);

		JLabel lTicketPrice = new JLabel("Preis (leer für Standardpreis " + PropertyHandler.getInstance().getPropertyString(PropertyHandler.PROP_TICKET_PRICE) + "Euro)?");
		officePanel.add(lTicketPrice);
		JTextField tfTTicketPrice = new JTextField(7);
		officePanel.add(tfTTicketPrice);

		officePanel.add(filler20);

		JPanel seatPanel = new JPanel();
		seatPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));

		JLabel lSeats = new JLabel("Sitzplätze (Format: Reihe1.Sitz1,Reihe2.Sitz2)");
		officePanel.add(lSeats);
		JTextField tfSeats = new JTextField(20);
		officePanel.add(tfSeats);

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
				List<Pair<String, String>> seats;
				if (isFilled(tfSeats)){
					//check seats
					try {
						seats = SeatTokenizer.parseSeats(tfSeats.getText());
						if (seats.size() != ticketAmount){
							showErrorDialog("Es gibt nicht genug oder viel zu viele Plätze für deine Gäste. Wir brauchen " + ticketAmount + ", aber du willst mir " + seats.size() + " geben. Das geht so nicht.");
							return;
						}
					} catch (NoSuchElementException e1) {
						showErrorDialog("Das sind aber komische Sitze, ich glaube nicht dass Gäste darauf ihren Platz finden werden.");
						return;
					}
				}else{
					showErrorDialog("Wo sollen sie alle nur sitzen? Oder sollen sie alle stehen? Nein, das können wir ihnen nicht antun!");
					return;
				}
				if (!isFilled(tfTicketOwner)){
					showErrorDialog("Wie heißt der Sack / die Säckin?");
					return;
				}

				int price = -1;
				try {
					price = Integer.parseInt(tfTTicketPrice.getText());
				} catch (NumberFormatException e1) {
					//ignore
				}

				TicketExpert theExpert = new TicketExpert(ticketAmount, tfTicketOwner.getText(), seats, tfTicketRecipient.getText(), price);
				if (theExpert.process(new ITicketProcessFailed() {
					@Override
					public boolean handleFailedState(String message, Exception cause) {
						showErrorDialog("Huiuiuiui sagt die UI, da ging wohl was in die Hose.\nNachricht: " + message + (cause != null ? "\n\nInfos vom Verursacher: " + cause.toString() : "" ) + "\n\nMehr auf der Konsole...");
						return false;
					}
				}, System.out)){
					showInfoDialog("Yeah!!!", "We rocked the office." + (theExpert.hasProcessWarning() ? "\n\nBut we have some warnings: \n" + theExpert.getProcessWarnings() : ""));
				}
			}
		});
		
		JButton seatPlanButton = new JButton("Create initial seatplan");
		officePanel.add(seatPlanButton);
		seatPlanButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					new SeatPlanHandler(System.out).makeNewPlan();
					showInfoDialog("Yippii", "Seat plan generated");
				} catch (IOException e1) {
					showErrorDialog(e1.getMessage());
					e1.printStackTrace();
				}
			}
		});
		
		tabPanel.addTab("Ticket Office", officePanel);

		//************ ticket notification panel *************
		JPanel ticketNotifyPanel = new JPanel();
		ticketNotifyPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		JLabel ticketNotifySeatsLabel = new JLabel("Sitzplätze (Format: Reihe1.Sitz1,Reihe2.Sitz2)");
		ticketNotifyPanel.add(ticketNotifySeatsLabel);
		JTextField ticketNotifySeats = new JTextField(20);
		ticketNotifyPanel.add(ticketNotifySeats);

		JButton sendPaymentNotification = new JButton("Zahlungserinnerung");
		ticketNotifyPanel.add(sendPaymentNotification);

		sendPaymentNotification.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleNotificationClick(ticketNotifySeats, TicketNotifier.NOTIFICATION_TYPE.PAYMENT_NOTIFICATION);
			}
		});

		JButton sendTicketRevocation = new JButton("Ticketschredder");
		ticketNotifyPanel.add(sendTicketRevocation);

		sendTicketRevocation.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleNotificationClick(ticketNotifySeats, TicketNotifier.NOTIFICATION_TYPE.TICKET_REVOCATION);
			}
		});

		tabPanel.addTab("Ticket BackOffice", ticketNotifyPanel);

		//************ payment panel **********
		JPanel paymentPanel = new JPanel();
		paymentPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel bookingNoLabel = new JLabel("Buchungsnummer");
		c.insets = new Insets(10,0,0,0);  //top padding
		c.gridx = 0;
		c.gridy = 0;
		paymentPanel.add(bookingNoLabel, c);
		c.gridx = 1;
		c.gridy = 0;
		JTextField bookingNoField = new JTextField(20);
		paymentPanel.add(bookingNoField, c);
		
		JLabel totalSumLabel = new JLabel("<html>Bezahlte Summe (optional falls<br>abweichend von Verkaufspreis)&nbsp;</html>");
		c.gridx = 0;
		c.gridy = 1;
		paymentPanel.add(totalSumLabel, c);
		JTextField totalSumField = new JTextField(20);
		c.gridx = 1;
		c.gridy = 1;
		paymentPanel.add(totalSumField, c);
		
		JButton payTheMent = new JButton("Set to paid");
		c.gridx = 1;
		c.gridy = 3;
		paymentPanel.add(payTheMent, c);
		
		c.gridx = 1;
		c.gridy = 4;
		c.weighty = 1;
		paymentPanel.add(filler20, c);

		payTheMent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				 try {
					Number totalSum = null;
					if (!Toolbox.isEmpty(totalSumField.getText())) {
						try {
							totalSum = DecimalFormat.getInstance(Locale.GERMAN).parse(totalSumField.getText());
						} catch (ParseException pe) {
							showErrorDialog("Error reading total sum '" + totalSumField.getText() + "': " + pe.getMessage());
							return;
						}
					}
					 
					if (new TicketPaymentHandler(System.out).setToPaid(bookingNoField.getText(), totalSum)) {
						showInfoDialog("Paid", "Booking number '" + bookingNoField.getText() + "' set to paid.");
					}else {
						showErrorDialog("Booking number '" + bookingNoField.getText() + "' wasn't found");
					}
				} catch (Exception e1) {
					showErrorDialog("Problem setting booking number to paid: " + e1.getMessage());
				}
			}
		});
		tabPanel.addTab("Payment Office", paymentPanel);
		
		//************ event panel *************
		JPanel eventPanel = new JPanel();
		eventPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 10));
		JLabel ticketCodeLabel = new JLabel("Ticket code");
		eventPanel.add(ticketCodeLabel);
		LimitedTextField ticketCode = new LimitedTextField(10, calculateMaxCodeLength(), this, new String[]{"y","z"}, new String[]{"Y", "Z"});
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

		toggleCheckBox = new JCheckBox("Toggle y/z (scanner mode)");
		toggleCheckBox.setSelected(true);
		toggleCheckBox.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				ticketCode.grabFocus();
			}
		});
		eventPanel.add(toggleCheckBox);

		JLabel explainLabel = new JLabel("<html>Das Eingabefeld unterstützt zwei Verifizierungsmethoden.<br>Automatisch: Eingabe von Ticketcode + Prüfcode durch Leerzeichen getrennt.<br>Manuell: Eingabe des Ticketcodes + manueller Verifizierung des Prüfcodes.</html>");
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
		tabPanel.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
			}

			@Override
			public void focusGained(FocusEvent e) {
				//always set focus back to input field (to support flawless scanner usage)
				ticketCode.grabFocus();
			}
		});

		add(tabPanel);

		updatePendingTicketsLabel(null);

		setSize(520, 210);
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
	}

	private void handleNotificationClick(JTextField ticketNotifySeats, TicketNotifier.NOTIFICATION_TYPE notificationType){
		List<Pair<String, String>> seats;

		//check seats
		if (isFilled(ticketNotifySeats)){
			seats = SeatTokenizer.parseSeats(ticketNotifySeats.getText());

			TicketNotifier ticketNotifier = new TicketNotifier(new ITicketProcessFailed() {
				@Override
				public boolean handleFailedState(String message, Exception cause) {
					showErrorDialog(message);
					return false;
				}
			}, System.out);

			boolean sendSuccessFull = ticketNotifier.sendNotification(seats, notificationType);
			if (sendSuccessFull){
				showInfoDialog("Geschafft", "Nachricht wurde verschickt.");
			}
		}else{
			showErrorDialog("NoNoNo, so nicht. Du hast da keinen sitzen.");
			return;
		}
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
		
		if (codeData.getAdditionalCodeData().getDataAsBoolean(ADDITIONAL_DATA.TICKET_UNPAID)){
			//ticket valid, but must be paid
			showInfoDialogForUnpaidTicket("Gültiges, aber unbezahltes Ticket ", "Das Ticket mit dem Code " + inputCode + " ist gültig aber muss noch bezahlt werden.");
		}else{
			//ticket valid, go in
			showInfoDialog("Gültiges Ticket", "Das Ticket mit dem Code " + inputCode + " ist gültig und wurde entwertet.");
		}

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

	private void showInfoDialogForUnpaidTicket(String title, String message, String... params){
		setOptionPaneBackground(Color.YELLOW);
		JOptionPane.showMessageDialog(this, makeHTMLMessage(message, params), title, JOptionPane.WARNING_MESSAGE );
		resetOptionPaneBackground();
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

	@Override
	public boolean shouldToggle() {
		return this.toggleCheckBox.isSelected();
	}
}
