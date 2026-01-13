package Main.view;

import Main.controller.WalletController;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class CreateWalletPanel extends JPanel {
    private final JPasswordField passwordField;
    private final JPasswordField confirmField;
    private final JTextArea notesArea;

    public CreateWalletPanel(WalletController controller) {
        setLayout(new BorderLayout(16, 16));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Create a new wallet");
        title.setFont(title.getFont().deriveFont(20f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        formPanel.add(new JLabel("Password"), gbc);
        passwordField = new JPasswordField(24);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(new JLabel("Confirm Password"), gbc);
        confirmField = new JPasswordField(24);
        gbc.gridx = 1;
        formPanel.add(confirmField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        notesArea = new JTextArea(6, 24);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setEditable(false);
        notesArea.setText("Security reminders:\n"
                + "• Your password encrypts your wallet on this device.\n"
                + "• You will need it for every transaction.\n"
                + "• Backup your private key after wallet creation.");
        formPanel.add(new JScrollPane(notesArea), gbc);

        JButton createButton = new JButton("Create Wallet");
        gbc.gridy++;
        formPanel.add(createButton, gbc);

        JButton backButton = new JButton("Back");
        gbc.gridy++;
        formPanel.add(backButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        createButton.addActionListener(event -> controller.createWallet(
                new String(passwordField.getPassword()),
                new String(confirmField.getPassword())
        ));

        backButton.addActionListener(event -> controller.showWelcome());
    }

    public void reset() {
        passwordField.setText("");
        confirmField.setText("");
    }
}
