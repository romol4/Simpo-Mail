import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.mail.*;
import javax.swing.*;

// This class displays the dialog used for creating messages.
public class MessageDialog extends JDialog {

    // Dialog message identifiers.
    public static final int NEW = 0;
    public static final int REPLY = 1;
    public static final int FORWARD = 2;

    // Message from, to and subject text fields.
    private JTextField fromTextField, toTextField;
    private JTextField subjectTextField;

    // Message content text area.
    private JTextArea contentTextArea;
    private String to = "", subject = "", from = "";

    private void switchLang()
    {
        String cmd = "setxkbmap -layout \"ru\"";

        Runtime run = Runtime.getRuntime();
        Process pr = null;

        try
        {
            pr = run.exec(cmd);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        try
        {
            pr.waitFor();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    // Flag specifying whether or not dialog was cancelled.
    private boolean cancelled;

    // Constructor for dialog.
    public MessageDialog(Frame parent, int type, Message message)
            throws Exception {
        // Call super constructor, specifying that dialog is modal.
        super(parent, true);

        //switchLang();

    /* Set dialog title and get message's "to", "subject"
       and "content" values based on message type. */
        String content = "";

        Properties prop = new Properties();
        try {
            //load a properties file
            prop.load(new FileInputStream("config.properties"));
            from = prop.getProperty("user");
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        switch (type) {
            // Reply message.
            case REPLY:
                //setTitle("Reply To Message");
                setTitle("Ответить на письмо");

                // Get message "to" value
                Address[] senders = message.getFrom();
                if (senders != null || senders.length > 0) {
                    to = senders[0].toString();
                }
                to = message.getFrom()[0].toString();

                // Get message subject.
                subject = message.getSubject();
                if (subject != null && subject.length() > 0) {
                    subject = "RE: " + subject;
                } else {
                    subject = "RE:";
                }

                /*
                // Get message content and add "REPLIED TO" notation.
                content = "\n----------------- " +
                        "REPLIED TO MESSAGE" +
                        " -----------------\n" +
                        EmailClient.getMessageContent(message);
                        */
                break;

            // Forward message.
            case FORWARD:
                //setTitle("Forward Message");
                setTitle("Переслать письмо");

                // Get message subject.
                subject = message.getSubject();
                if (subject != null && subject.length() > 0) {
                    subject = "FWD: " + subject;
                } else {
                    subject = "FWD:";
                }

                // Get message content and add "FORWARDED" notation.
                content = "\n----------------- " +
                        "FORWARDED MESSAGE" +
                        " -----------------\n" +
                        EmailClient.getMessageContent(message);
                break;

            // New message.
            default:
                //setTitle("New Message");
                setTitle("Написать письмо");
        }

        // Handle closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionCancel();
            }
        });

        // Setup fields panel.
        JPanel fieldsPanel = new JPanel();
        GridBagConstraints constraints;
        GridBagLayout layout = new GridBagLayout();
        fieldsPanel.setLayout(layout);
        if (from.length()==0)
        {
            //JLabel fromLabel = new JLabel("From:");
            JLabel fromLabel = new JLabel("От:");
            constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets(5, 5, 0, 0);
            layout.setConstraints(fromLabel, constraints);
            fieldsPanel.add(fromLabel);
            fromTextField = new JTextField(from);
            constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(5, 5, 0, 0);
            layout.setConstraints(fromTextField, constraints);
            fieldsPanel.add(fromTextField);
        }

        if (to.length()==0)
        {
            //JLabel toLabel = new JLabel("To:");
            JLabel toLabel = new JLabel("Кому:");
            constraints = new GridBagConstraints();
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = new Insets(5, 5, 0, 0);
            layout.setConstraints(toLabel, constraints);
            fieldsPanel.add(toLabel);
            toTextField = new JTextField(to);
            constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(5, 5, 0, 0);
            constraints.weightx = 1.0D;
            layout.setConstraints(toTextField, constraints);
            fieldsPanel.add(toTextField);
        }

        if (subject.length()==0)
        {
            //JLabel subjectLabel = new JLabel("Subject:");
            JLabel subjectLabel = new JLabel("Тема:");
            constraints = new GridBagConstraints();
            constraints.insets = new Insets(5, 5, 5, 0);
            layout.setConstraints(subjectLabel, constraints);
            fieldsPanel.add(subjectLabel);
            subjectTextField = new JTextField(subject);
            constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(5, 5, 5, 0);
            layout.setConstraints(subjectTextField, constraints);
            fieldsPanel.add(subjectTextField);
        }

        // Setup content panel.
        JScrollPane contentPanel = new JScrollPane();
        contentTextArea = new JTextArea(content, 10, 50);
        contentPanel.setViewportView(contentTextArea);

        // Setup buttons panel.
        JPanel buttonsPanel = new JPanel();
        //JButton sendButton = new JButton("Send");
        JButton sendButton = new JButton("Послать");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSend();
            }
        });
        buttonsPanel.add(sendButton);
        //JButton cancelButton = new JButton("Cancel");
        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        buttonsPanel.add(cancelButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(fieldsPanel, BorderLayout.NORTH);
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

        // Size dialog to components.
        pack();

        // Center dialog over application.
        setLocationRelativeTo(parent);
    }

    // Validate message fields and close dialog.
    private void actionSend() {
        if ((from.length() <1)
                || (to.length() < 1)
                || (subject.length() < 1)
                || contentTextArea.getText().trim().length() < 1) {
            JOptionPane.showMessageDialog(this,
//                    "One or more fields is missing.",
//                    "Missing Field(s)", JOptionPane.ERROR_MESSAGE);
                    "Ничего не написано, пап, надо бы чего-то написать деткам...",
                    "Пропущенные поля", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Close dialog.
        dispose();
    }

    // Cancel creating this message and close dialog.
    private void actionCancel() {
        cancelled = true;

        // Close dialog.
        dispose();
    }

    // Show dialog.
    public boolean display() {
        show();

        // Return whether or not display was successful.
        return !cancelled;
    }

    // Get message's "From" field value.
    public String getFrom() {
        return from;
    }

    // Get message's "To" field value.
    public String getTo() {
        return to;
    }

    // Get message's "Subject" field value.
    public String getSubject() {
        return subject;
    }

    // Get message's "content" field value.
    public String getContent() {
        return contentTextArea.getText();
    }
}