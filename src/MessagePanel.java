import java.awt.*;
import javax.mail.internet.MimeMessage;
import javax.swing.*;
import javax.mail.*;
import javax.mail.internet.MimeUtility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class MessagePanel extends JPanel {

    private boolean archiveMessage()
    {
        return file.renameTo(new File(file.getParent().replace("new", "archive")+"//"+file.getName()));
    }
    // Reply to a message.
    private void actionReply() {
        email.sendMessage(MessageDialog.REPLY, current_message);

        if (archiveMessage())
            email.removeFromList(this);
    }

    // Reply to a message.
    private void actionSave() {
        if (archiveMessage())
            email.removeFromList(this);
    }

    private JPanel p;
    private JPanel iconPanel;
    private JLabel l;
    private JButton b;
    private JButton b2;
    private JLabel tl;
    //private JTextArea ta;
    private ArrayList<String> names;
    private EmailClient email;
    private Message current_message;
    private String sender;
    private File file;

    public MessagePanel(Message message_in, Object o, File file_in) {

        email = (EmailClient) o;
        MimeMessage message = (MimeMessage) message_in;
        current_message = message_in;
        file = file_in;
        setBackground(Color.white);

        //p = new JPanel();
        setLayout(new BorderLayout());

//        setBorder(BorderFactory.createTitledBorder("title"));

        try {
            Address[] senders = message.getFrom();
            if (senders != null || senders.length > 0) {
                sender = MimeUtility.decodeText(senders[0].toString());
            }
            else
                sender = "неизвестно кого";
        } catch (Exception e) {
            // Fail silently.
            sender = "неизвестно кого";
        }

        LineBorder roundedLineBorder = new LineBorder(Color.black, 2, true);
        TitledBorder roundedTitledBorder = new TitledBorder(roundedLineBorder, sender, TitledBorder.CENTER, TitledBorder.TOP, new Font("times new roman",Font.PLAIN,16), Color.BLUE);
        setBorder(roundedTitledBorder);

        // icon
        iconPanel = new JPanel(new BorderLayout());
        iconPanel.setBackground(Color.white);

        l = new JLabel("icon"); // <-- this will be an icon instead of a
        l.setFont(new Font("Courier New", Font.PLAIN, 18));
        // text

//        b = new JButton("Reply");
        b = new JButton("Написать Ответ");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionReply();
            }
        });
        b.setEnabled(true);
        b.setFont(new Font("Courier New", Font.ITALIC, 22));

        b2 = new JButton("Сохранить в архив");
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSave();
            }
        });
        b2.setEnabled(true);
        b2.setFont(new Font("Courier New", Font.ITALIC, 22));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.white);

        buttonPanel.setBorder( new EmptyBorder( 8, 8, 8, 8 ) );
        buttonPanel.add(b, BorderLayout.NORTH);

        // add save to archive for new messages only
        String par = file.getParent();
        if (file.getParent().endsWith("new"))
            buttonPanel.add(b2, BorderLayout.SOUTH);

        iconPanel.add(l, BorderLayout.WEST);
        iconPanel.add(buttonPanel, BorderLayout.EAST);
        add(iconPanel, BorderLayout.NORTH);

        File folder = new File("images/");
        File[] listOfFiles = folder.listFiles();

        names = new ArrayList();
        for(int i=0;i<listOfFiles.length;i++){
            if (listOfFiles[i].isFile() && listOfFiles[i].getName().toLowerCase().endsWith(".jpg")) {
                names.add(listOfFiles[i].getName());
            }
        }

        int width = ((EmailClient) o).getWidth()-40;
  //      // this is just to lure the ta's internal sizing mechanism into action
        if (width > 0)
            setSize(width-400, Short.MAX_VALUE);
  //      Dimension dd = this.getSize();
  //      ta.setMaximumSize(getSize());

        try {
            l.setText(getHeaderText(message));
            l.setIcon(getIcon(message));

            String text_message = (String) getText(message);

            // remove tags simply
            if (textIsHtml)
            {
                text_message = text_message.replaceAll("<br/>", "<br>");
                text_message = text_message.replaceAll("<br />", "<br>");
                text_message = text_message.replaceAll("<br>", System.getProperty("line.separator"));
                text_message = text_message.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
            }

            JTextArea ta = new JTextArea();
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setFont(new Font("Serif", Font.PLAIN, 22));

            ta.setText(text_message);
            ta.setEditable(false);
            //ta.setContentType("text/html");
            add(ta, BorderLayout.CENTER);
        }
        catch (Exception e) {
            // Fail silently.
            return;
        }
    }

    public ImageIcon getIcon(MimeMessage message) {
        try {
            String from;

            Address[] senders = message.getFrom();
            if (senders != null || senders.length > 0) {
                from = MimeUtility.decodeText(senders[0].toString());
            }
            else
//                from = "none";
                from = "none";

            Pattern p = Pattern.compile("\\<(.*?)\\>", Pattern.DOTALL);

            Matcher matcher = p.matcher(from);
            if(matcher.find())
                from=matcher.group();


            int i = names.indexOf(from+".jpg");
            if (i!=-1)
                return new ImageIcon("images/"+names.get(i));
            else
                return new ImageIcon("images/none.jpg");

            //return new ImageIcon("images/1.jpg");
        } catch (Exception e) {
            // Fail silently.
            return new ImageIcon("images/none.jpg");
        }
    }

    private boolean textIsHtml = false;
    private String text_type;

    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType("text/html");
            text_type = p.getContentType();
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }

    public String getHeaderText(MimeMessage message) {
        String result;
        result = "<html>";
        try {
            result += "<b><font color=red>"+sender+"</font></b>";

            //case 1: // Subject
            result += "<br>";
            String subject = message.getSubject();
            if (subject != null && subject.length() > 0) {
                result += subject;
            } else {
//                result += "[none]";
                result += "[без темы]";
            }
            //case 2: // Date
            result += "<br>";
            Date date = message.getSentDate();
            if (date != null) {
                result += date.toString();
            } else {
//                result += "[none]";
                result += "[без даты]";
            }
            //result += "<br><br><p>";
            //result += getMessageContent(message);
            result += "</p></html>";
            //}
        } catch (Exception e) {
            // Fail silently.
            return "";
        }

        return result;
    }
}
