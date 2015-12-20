package the.dancing.company.ticketkatjuscha.ui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import the.dancing.company.ticketkatjuscha.ITicketProcessFailed;
import the.dancing.company.ticketkatjuscha.TicketExpert;

public class TicketOffice extends JFrame{

	private static final long serialVersionUID = -7245404133018518793L;

	public TicketOffice(){
		super("Ticket Office");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 5, 20));
		
		JLabel lTicketAmount = new JLabel("Wieviel?");
		getContentPane().add(lTicketAmount);
		JTextField tfTicketAmount = new JTextField(3);
		getContentPane().add(tfTicketAmount);
		
		JLabel lTicketOwner = new JLabel("Für wen?");
		getContentPane().add(lTicketOwner);
		JTextField tfTicketOwner = new JTextField(7);
		getContentPane().add(tfTicketOwner);
		
		JButton makeTickets = new JButton("Gib's mir");
		getContentPane().add(makeTickets);
		
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

		setSize(400, 100);
		setLocationRelativeTo(null);
		setResizable(false);
		setVisible(true);
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
