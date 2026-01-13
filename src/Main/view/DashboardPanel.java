package Main.view;

import Main.controller.WalletController;
import Main.model.Contact;
import Main.model.Validator;
import Main.model.WalletProfile;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final JLabel addressLabel;
    private final JLabel publicKeyLabel;
    private final JLabel base58Label;
    private final JLabel createdAtLabel;
    private final JTextArea activityArea;
    private final DefaultListModel<String> contactsModel;
    private final DefaultListModel<String> validatorsModel;
    private final JTextField validatorApiField;

    private final JTextField sendToField;
    private final JTextField sendAmountField;
    private final JTextField sendMemoField;
    private final JPasswordField sendPasswordField;

    private final JTextField escrowSellerField;
    private final JTextField escrowMemoField;
    private final JTextField escrowAmountField;
    private final JComboBox<String> escrowTypeField;
    private final JTextField escrowReleaseDateField;
    private final JComboBox<String> escrowReleaseAuthorityField;
    private final JTextField escrowDisputeField;
    private final JTextField escrowRefundWindowField;
    private final JTextArea escrowMilestoneArea;
    private final JPasswordField escrowPasswordField;

    private final JTextField contactNameField;
    private final JTextField contactAddressField;

    public DashboardPanel(WalletController controller) {
        setLayout(new BorderLayout());

        addressLabel = new JLabel("-");
        publicKeyLabel = new JLabel("-");
        base58Label = new JLabel("-");
        createdAtLabel = new JLabel("-");

        activityArea = new JTextArea(12, 40);
        activityArea.setEditable(false);

        contactsModel = new DefaultListModel<>();
        validatorsModel = new DefaultListModel<>();

        validatorApiField = new JTextField(30);

        sendToField = new JTextField(24);
        sendAmountField = new JTextField(10);
        sendMemoField = new JTextField(24);
        sendPasswordField = new JPasswordField(14);

        escrowSellerField = new JTextField(24);
        escrowMemoField = new JTextField(12);
        escrowAmountField = new JTextField(10);
        escrowTypeField = new JComboBox<>(new String[]{
                "Time of Release",
                "Release Authority",
                "Milestone-Based",
                "Dispute-Managed",
                "Partial Release",
                "Refund Window"
        });
        escrowReleaseDateField = new JTextField(12);
        escrowReleaseAuthorityField = new JComboBox<>(new String[]{"Buyer", "Seller", "Joint Approval"});
        escrowDisputeField = new JTextField(18);
        escrowRefundWindowField = new JTextField(6);
        escrowMilestoneArea = new JTextArea(4, 24);
        escrowMilestoneArea.setLineWrap(true);
        escrowMilestoneArea.setWrapStyleWord(true);
        escrowPasswordField = new JPasswordField(14);

        contactNameField = new JTextField(12);
        contactAddressField = new JTextField(22);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Overview", buildOverviewPanel(controller));
        tabs.addTab("Send", buildSendPanel(controller));
        tabs.addTab("Escrow", buildEscrowPanel(controller));
        tabs.addTab("Contacts", buildContactsPanel(controller));
        tabs.addTab("Validators", buildValidatorsPanel(controller));
        tabs.addTab("Activity", buildActivityPanel());

        add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildOverviewPanel(WalletController controller) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Wallet Address"), gbc);
        gbc.gridx = 1;
        panel.add(addressLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Compressed Public Key"), gbc);
        gbc.gridx = 1;
        panel.add(publicKeyLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Base58 Public Key"), gbc);
        gbc.gridx = 1;
        panel.add(base58Label, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Created At"), gbc);
        gbc.gridx = 1;
        panel.add(createdAtLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Balance"), gbc);
        gbc.gridx = 1;
        panel.add(new JLabel("0.0000 M3R (sync with network)"), gbc);

        JButton exportKeyButton = new JButton("Export Private Key");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(exportKeyButton, gbc);

        exportKeyButton.addActionListener(event -> controller.exportPrivateKey());

        return panel;
    }

    private JPanel buildSendPanel(WalletController controller) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Recipient Address"), gbc);
        gbc.gridx = 1;
        panel.add(sendToField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Amount (M3R)"), gbc);
        gbc.gridx = 1;
        panel.add(sendAmountField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Memo"), gbc);
        gbc.gridx = 1;
        panel.add(sendMemoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Password"), gbc);
        gbc.gridx = 1;
        panel.add(sendPasswordField, gbc);

        JButton sendButton = new JButton("Send M3R");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(sendButton, gbc);

        sendButton.addActionListener(event -> controller.submitTransfer(
                sendToField.getText(),
                sendAmountField.getText(),
                sendMemoField.getText(),
                new String(sendPasswordField.getPassword())
        ));

        return panel;
    }

    private JPanel buildEscrowPanel(WalletController controller) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Seller Address"), gbc);
        gbc.gridx = 1;
        panel.add(escrowSellerField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Memo Number"), gbc);
        gbc.gridx = 1;
        panel.add(escrowMemoField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Amount (M3R)"), gbc);
        gbc.gridx = 1;
        panel.add(escrowAmountField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Escrow Type"), gbc);
        gbc.gridx = 1;
        panel.add(escrowTypeField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Release Date"), gbc);
        gbc.gridx = 1;
        panel.add(escrowReleaseDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Release Authority"), gbc);
        gbc.gridx = 1;
        panel.add(escrowReleaseAuthorityField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Dispute Policy"), gbc);
        gbc.gridx = 1;
        panel.add(escrowDisputeField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Refund Window (days)"), gbc);
        gbc.gridx = 1;
        panel.add(escrowRefundWindowField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Milestone Notes"), gbc);
        gbc.gridx = 1;
        panel.add(new JScrollPane(escrowMilestoneArea), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Password"), gbc);
        gbc.gridx = 1;
        panel.add(escrowPasswordField, gbc);

        JButton escrowButton = new JButton("Submit Escrow");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(escrowButton, gbc);

        escrowButton.addActionListener(event -> controller.submitEscrow(
                escrowSellerField.getText(),
                escrowMemoField.getText(),
                escrowAmountField.getText(),
                (String) escrowTypeField.getSelectedItem(),
                escrowReleaseDateField.getText(),
                (String) escrowReleaseAuthorityField.getSelectedItem(),
                escrowDisputeField.getText(),
                escrowRefundWindowField.getText(),
                escrowMilestoneArea.getText(),
                new String(escrowPasswordField.getPassword())
        ));

        return panel;
    }

    private JPanel buildContactsPanel(WalletController controller) {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JList<String> contactsList = new JList<>(contactsModel);
        contactsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(contactsList), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Name"), gbc);
        gbc.gridx = 1;
        form.add(contactNameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        form.add(new JLabel("Address"), gbc);
        gbc.gridx = 1;
        form.add(contactAddressField, gbc);

        JButton addButton = new JButton("Add Contact");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        form.add(addButton, gbc);

        addButton.addActionListener(event -> controller.addContact(
                contactNameField.getText(),
                contactAddressField.getText()
        ));

        panel.add(form, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildValidatorsPanel(WalletController controller) {
        JPanel panel = new JPanel(new BorderLayout(16, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JList<String> list = new JList<>(validatorsModel);
        panel.add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        controls.add(new JLabel("Validator API URL"), gbc);
        gbc.gridx = 1;
        controls.add(validatorApiField, gbc);

        JButton refreshButton = new JButton("Refresh Validators");
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        controls.add(refreshButton, gbc);

        refreshButton.addActionListener(event -> controller.refreshValidators(validatorApiField.getText()));

        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        panel.add(new JScrollPane(activityArea), BorderLayout.CENTER);
        return panel;
    }

    public void updateWallet(WalletProfile profile) {
        if (profile == null) {
            addressLabel.setText("-");
            publicKeyLabel.setText("-");
            base58Label.setText("-");
            createdAtLabel.setText("-");
            return;
        }
        addressLabel.setText(profile.getAddress());
        publicKeyLabel.setText("0x" + Main.Util.ByteToHex.BytesToHex(profile.getPublicKey()));
        base58Label.setText(profile.getBase58PublicKey());
        createdAtLabel.setText(profile.getCreatedAt().toString());
    }

    public void updateContacts(List<Contact> contacts) {
        contactsModel.clear();
        for (Contact contact : contacts) {
            contactsModel.addElement(contact.getName() + " - " + contact.getAddress());
        }
    }

    public void updateValidators(List<Validator> validators, String apiUrl) {
        validatorsModel.clear();
        for (Validator validator : validators) {
            validatorsModel.addElement(validator.toString());
        }
        if (apiUrl != null && !apiUrl.isEmpty()) {
            validatorApiField.setText(apiUrl);
        }
    }

    public void appendActivity(String message) {
        activityArea.append(message + "\n");
    }

    public void clearSendForm() {
        sendToField.setText("");
        sendAmountField.setText("");
        sendMemoField.setText("");
        sendPasswordField.setText("");
    }

    public void clearEscrowForm() {
        escrowSellerField.setText("");
        escrowMemoField.setText("");
        escrowAmountField.setText("");
        escrowReleaseDateField.setText("");
        escrowDisputeField.setText("");
        escrowRefundWindowField.setText("");
        escrowMilestoneArea.setText("");
        escrowPasswordField.setText("");
    }

    public void clearContactForm() {
        contactNameField.setText("");
        contactAddressField.setText("");
    }
}
