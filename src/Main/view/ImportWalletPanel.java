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

public class ImportWalletPanel extends JPanel {
    private final JTextArea keyArea;
    private final JPasswordField passwordField;
    private final JPasswordField confirmField;

    public ImportWalletPanel(WalletController controller) {
        setLayout(new BorderLayout(16, 16));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Import wallet");
        title.setFont(title.getFont().deriveFont(20f));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;
        formPanel.add(new JLabel("Private Key (hex) or Mnemonic"), gbc);
        keyArea = new JTextArea(4, 24);
        keyArea.setLineWrap(true);
        keyArea.setWrapStyleWord(true);
        gbc.gridx = 1;
        formPanel.add(new JScrollPane(keyArea), gbc);

        gbc.gridx = 0;
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

        JButton importButton = new JButton("Import Wallet");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(importButton, gbc);

        JButton backButton = new JButton("Back");
        gbc.gridy++;
        formPanel.add(backButton, gbc);

        add(formPanel, BorderLayout.CENTER);

        importButton.addActionListener(event -> controller.importWallet(
                keyArea.getText(),
                new String(passwordField.getPassword()),
                new String(confirmField.getPassword())
        ));

        backButton.addActionListener(event -> controller.showWelcome());
    }

    public void reset() {
        keyArea.setText("");
        passwordField.setText("");
        confirmField.setText("");
    }
}
