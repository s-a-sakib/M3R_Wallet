package Main.controller;

import Main.model.Contact;
import Main.model.EscrowDetails;
import Main.model.TransactionRequest;
import Main.model.TransactionType;
import Main.model.Validator;
import Main.model.WalletProfile;
import Main.service.PasswordService;
import Main.service.CryptoService;
import Main.service.TransactionService;
import Main.service.ValidatorService;
import Main.service.WalletService;
import Main.service.WalletStore;
import Main.view.MainFrame;

import javax.swing.SwingWorker;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WalletController {
    private final WalletService walletService;
    private final WalletStore walletStore;
    private final ValidatorService validatorService;
    private final TransactionService transactionService;
    private MainFrame view;
    private WalletProfile profile;
    private final List<Contact> contacts;
    private List<Validator> validators;
    private String validatorApiUrl;

    public WalletController() {
        this.walletService = new WalletService(new PasswordService());
        this.walletStore = new WalletStore();
        this.validatorService = new ValidatorService();
        this.transactionService = new TransactionService(new CryptoService());
        this.contacts = new ArrayList<>();
        this.validators = new ArrayList<>();
        this.validatorApiUrl = "";
    }

    public void attachView(MainFrame view) {
        this.view = view;
    }

    public void start() {
        try {
            if (walletStore.exists()) {
                profile = walletStore.loadProfile();
                contacts.addAll(walletStore.loadContacts());
                validatorApiUrl = walletStore.loadValidatorApiUrl();
                view.showLogin();
            } else {
                view.showWelcome();
            }
        } catch (IOException e) {
            view.showError("Error", "Unable to load wallet data: " + e.getMessage());
            view.showWelcome();
        }
    }

    public void showWelcome() {
        view.showWelcome();
    }

    public void showCreateWallet() {
        view.showCreateWallet();
    }

    public void showImportWallet() {
        view.showImportWallet();
    }

    public void createWallet(String password, String confirm) {
        if (!validatePassword(password, confirm)) {
            return;
        }
        profile = walletService.createNewWallet(password);
        persistProfile();
        view.resetCreateWalletForm();
        view.updateWallet(profile, contacts, validators, validatorApiUrl);
        view.showDashboard();
        view.showMessage("Wallet Created", "Your wallet is ready. Please back up your private key.");
        view.appendActivity("Wallet created at " + profile.getCreatedAt());
    }

    public void importWallet(String keyMaterial, String password, String confirm) {
        if (!validatePassword(password, confirm)) {
            return;
        }
        if (keyMaterial == null || keyMaterial.trim().isEmpty()) {
            view.showError("Import Error", "Please enter a private key or mnemonic phrase.");
            return;
        }
        try {
            profile = walletService.importWallet(keyMaterial.trim(), password);
        } catch (IllegalArgumentException e) {
            view.showError("Import Error", e.getMessage());
            return;
        }
        persistProfile();
        view.resetImportWalletForm();
        view.updateWallet(profile, contacts, validators, validatorApiUrl);
        view.showDashboard();
        view.showMessage("Wallet Imported", "Wallet imported successfully. Please back up your private key.");
        view.appendActivity("Wallet imported at " + Instant.now());
    }

    public void login(String password) {
        if (profile == null) {
            view.showError("Login Error", "No wallet found. Please create or import a wallet.");
            view.showWelcome();
            return;
        }
        if (!walletService.verifyPassword(profile, password)) {
            view.showError("Login Error", "Incorrect password.");
            return;
        }
        view.resetLoginForm();
        view.updateWallet(profile, contacts, validators, validatorApiUrl);
        view.showDashboard();
        view.appendActivity("Wallet unlocked at " + Instant.now());
    }

    public void exportPrivateKey() {
        if (profile == null) {
            view.showError("Export Error", "No wallet loaded.");
            return;
        }
        String privateKeyHex = walletService.privateKeyHex(profile);
        view.showMessage("Private Key Export", "Store this private key securely:\n" + privateKeyHex);
    }

    public void addContact(String name, String address) {
        if (name == null || name.isBlank() || address == null || address.isBlank()) {
            view.showError("Contact Error", "Please provide both name and address.");
            return;
        }
        contacts.add(new Contact(name.trim(), address.trim()));
        persistProfile();
        view.updateWallet(profile, contacts, validators, validatorApiUrl);
        view.clearContactForm();
        view.appendActivity("Contact added: " + name.trim());
    }

    public void refreshValidators(String apiUrl) {
        String url = apiUrl == null || apiUrl.isBlank() ? validatorApiUrl : apiUrl.trim();
        if (url == null || url.isBlank()) {
            view.showError("Validator Error", "Please provide a validator API URL.");
            return;
        }
        validatorApiUrl = url;
        SwingWorker<List<Validator>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Validator> doInBackground() throws Exception {
                return validatorService.fetchValidators(url);
            }

            @Override
            protected void done() {
                try {
                    validators = get();
                    persistProfile();
                    view.updateWallet(profile, contacts, validators, validatorApiUrl);
                    view.appendActivity("Validators refreshed: " + validators.size());
                } catch (Exception e) {
                    view.showError("Validator Error", "Unable to fetch validators: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    public void submitTransfer(String toAddress, String amount, String memo, String password) {
        if (!validateTransactionInputs(toAddress, amount, password)) {
            return;
        }
        TransactionRequest request = new TransactionRequest(
                TransactionType.TRANSFER,
                profile.getAddress(),
                toAddress.trim(),
                amount.trim(),
                memo == null ? "" : memo.trim(),
                null,
                Instant.now()
        );
        view.clearSendForm();
        sendTransaction(request);
    }

    public void submitEscrow(String sellerAddress,
                             String memoNumber,
                             String amount,
                             String escrowType,
                             String releaseDate,
                             String releaseAuthority,
                             String disputePolicy,
                             String refundWindow,
                             String milestoneNotes,
                             String password) {
        if (!validateTransactionInputs(sellerAddress, amount, password)) {
            return;
        }
        EscrowDetails details = new EscrowDetails(
                sellerAddress.trim(),
                memoNumber == null ? "" : memoNumber.trim(),
                escrowType == null ? "" : escrowType,
                releaseDate == null ? "" : releaseDate.trim(),
                releaseAuthority == null ? "" : releaseAuthority,
                disputePolicy == null ? "" : disputePolicy.trim(),
                refundWindow == null ? "" : refundWindow.trim(),
                milestoneNotes == null ? "" : milestoneNotes.trim()
        );
        TransactionRequest request = new TransactionRequest(
                TransactionType.ESCROW,
                profile.getAddress(),
                sellerAddress.trim(),
                amount.trim(),
                memoNumber == null ? "" : memoNumber.trim(),
                details,
                Instant.now()
        );
        view.clearEscrowForm();
        sendTransaction(request);
    }

    private void sendTransaction(TransactionRequest request) {
        if (validators.isEmpty()) {
            view.showError("Validator Error", "No validators loaded. Refresh the validator list first.");
            return;
        }
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return transactionService.sendToValidators(request, validators);
            }

            @Override
            protected void done() {
                try {
                    List<String> responses = get();
                    for (String response : responses) {
                        view.appendActivity(response);
                    }
                    view.showMessage("Transaction Sent", "Transaction broadcast completed. Check activity for responses.");
                } catch (Exception e) {
                    view.showError("Transaction Error", "Unable to send transaction: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private boolean validatePassword(String password, String confirm) {
        if (password == null || password.isBlank()) {
            view.showError("Password Error", "Password is required.");
            return false;
        }
        if (!password.equals(confirm)) {
            view.showError("Password Error", "Passwords do not match.");
            return false;
        }
        if (password.length() < 8) {
            view.showError("Password Error", "Password must be at least 8 characters.");
            return false;
        }
        return true;
    }

    private boolean validateTransactionInputs(String address, String amount, String password) {
        if (profile == null) {
            view.showError("Wallet Error", "Please login or create a wallet first.");
            return false;
        }
        if (address == null || address.isBlank()) {
            view.showError("Transaction Error", "Recipient address is required.");
            return false;
        }
        if (amount == null || amount.isBlank()) {
            view.showError("Transaction Error", "Amount is required.");
            return false;
        }
        if (!walletService.verifyPassword(profile, password)) {
            view.showError("Authentication Error", "Incorrect password.");
            return false;
        }
        return true;
    }

    private void persistProfile() {
        try {
            walletStore.save(profile, contacts, validatorApiUrl);
        } catch (IOException e) {
            view.showError("Storage Error", "Unable to save wallet data: " + e.getMessage());
        }
    }
}
