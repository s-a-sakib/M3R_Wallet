package Main;

import Main.controller.WalletController;
import Main.view.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            WalletController controller = new WalletController();
            MainFrame frame = new MainFrame(controller);
            controller.attachView(frame);
            controller.start();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Use default
        }
    }
}
