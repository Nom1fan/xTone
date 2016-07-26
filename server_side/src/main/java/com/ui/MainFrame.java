package com.ui;

import com.server.spring.SpringConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.JFrame;

/**
 * Created by Mor on 27/03/2016.
 */
@Component
public class MainFrame extends JFrame {

    @Autowired
    private ControlPanel controlPanel;


    @PostConstruct
    public void init() {
        setContentPane(controlPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setVisible(true);
        pack();
    }

    public static void main(String[] args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
        context.getBean(MainFrame.class);
    }

}
