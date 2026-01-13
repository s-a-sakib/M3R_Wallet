package Main.view;

import Main.controller.WalletController;
import Main.model.Contact;
import Main.model.Validator;
import Main.model.WalletProfile;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.List;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel container;
    private final WelcomePanel welcomePanel;
    private final CreateWalletPanel createWalletPanel;
    private final ImportWalletPanel importWalletPanel;
    private final LoginPanel loginPanel;
    private final DashboardPanel dashboardPanel;

    public MainFrame(WalletController controller) {
        super("M3R Wallet");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));

        cardLayout = new CardLayout();
        container = new JPanel(cardLayout);

        welcomePanel = new WelcomePanel(controller);
        createWalletPanel = new CreateWalletPanel(controller);
        importWalletPanel = new ImportWalletPanel(controller);
        loginPanel = new LoginPanel(controller);
        dashboardPanel = new DashboardPanel(controller);

        container.add(welcomePanel, "welcome");
        container.add(createWalletPanel, "create");
        container.add(importWalletPanel, "import");
        container.add(loginPanel, "login");
        container.add(dashboardPanel, "dashboard");

        setContentPane(container);
    }

    public void showWelcome() {
        cardLayout.show(container, "welcome");
    }

    public void showCreateWallet() {
        cardLayout.show(container, "create");
    }

    public void showImportWallet() {
        cardLayout.show(container, "import");
    }

    public void showLogin() {
        cardLayout.show(container, "login");
    }

    public void showDashboard() {
        cardLayout.show(container, "dashboard");
    }

    public void updateWallet(WalletProfile profile, List<Contact> contacts, List<Validator> validators, String apiUrl) {
        dashboardPanel.updateWallet(profile);
        dashboardPanel.updateContacts(contacts);
        dashboardPanel.updateValidators(validators, apiUrl);
    }

    public void appendActivity(String message) {
        dashboardPanel.appendActivity(message);
    }

    public void showMessage(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String title, String message) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.ERROR_MESSAGE);
    }

    public void resetCreateWalletForm() {
        createWalletPanel.reset();
    }

    public void resetImportWalletForm() {
        importWalletPanel.reset();
    }

    public void resetLoginForm() {
        loginPanel.reset();
    }

    public void clearSendForm() {
        dashboardPanel.clearSendForm();
    }

    public void clearEscrowForm() {
        dashboardPanel.clearEscrowForm();
    }

    public void clearContactForm() {
        dashboardPanel.clearContactForm();
    }
}
