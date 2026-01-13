package Main.view;

import Main.controller.WalletController;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;

public class WelcomePanel extends JPanel {
    public WelcomePanel(WalletController controller) {
        setLayout(new BorderLayout(24, 24));

        JLabel title = new JLabel("Welcome to M3R Wallet", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("<html><div style='text-align:center;'>Securely manage M3R Coin, send payments, and run escrow transactions.</div></html>",
                SwingConstants.CENTER);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        add(subtitle, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 24, 12));
        JButton createButton = new JButton("Create New Wallet");
        JButton importButton = new JButton("Import Existing Wallet");

        createButton.addActionListener(event -> controller.showCreateWallet());
        importButton.addActionListener(event -> controller.showImportWallet());

        buttonPanel.add(createButton);
        buttonPanel.add(importButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }
}
