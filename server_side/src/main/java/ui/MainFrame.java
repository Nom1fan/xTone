package ui;

import javax.swing.JFrame;

/**
 * Created by Mor on 27/03/2016.
 */
public class MainFrame extends JFrame {

    private ControlPanel _controlPane;
    //private Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

    public MainFrame() {

        _controlPane = new ControlPanel();
        setContentPane(_controlPane);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setVisible(true);
        pack();
    }

    public static void main(String[] args) {

        new MainFrame();
    }

}
