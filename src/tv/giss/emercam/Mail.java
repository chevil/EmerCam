package tv.giss.emercam;

import java.util.Date; 
import java.util.Properties; 
import jakarta.activation.*; 
import jakarta.mail.*;
import jakarta.mail.internet.*;
 
 
public class Mail extends jakarta.mail.Authenticator { 
        private String _user; 
        private String _pass; 
        private String _reply; 
 
        private String[] _to; 
        private String[] _bcc; 
        private String _from; 
 
        private String _port; 
        private String _sport; 
 
        private String _host; 
 
        private String _subject; 
        private String _body; 
 
        private boolean _auth; 
        private boolean _debuggable; 
 
        private Multipart _multipart; 
 
        public Mail() { 
                _host = ""; // smtp server 
                _port = ""; // smtp port 
                _sport = ""; // socketfactory port 
 
                _user = ""; // username 
                _pass = ""; // password 
                _reply = ""; // reply to
                _from = ""; // email sent from 
                _subject = ""; // email subject 
                _body = ""; // email body 
 
                _debuggable = false; // debug mode on or off - default off 
                _auth = true; // smtp authentication - default on 
 
                _multipart = new MimeMultipart("alternative"); 
 
                // There is something wrong with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added. 
                MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap(); 
                mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html"); 
                mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml"); 
                mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain"); 
                mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed"); 
                mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822"); 
                CommandMap.setDefaultCommandMap(mc); 
        } 
 
        public Mail(String user, String pass) { 
                this(); 
                _user = user; 
                _pass = pass; 
        } 

        private Properties _setProperties() {
                Properties props = new Properties();

                props.put("mail.smtp.host", _host);

                if(_debuggable) {
                        props.put("mail.debug", "true");
                }

                if(_auth) {
                        props.put("mail.smtp.auth", "true");
                }

                props.put("mail.smtp.port", _port);
                props.put("mail.smtp.socketFactory.port", _sport);
                props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.connectiontimeout", Constants.TIMEOUT);
                props.put("mail.smtp.timeout", Constants.TIMEOUT);
                props.put("mail.smtp.writetimeout", Constants.TIMEOUT);
                props.put("mail.smtp.ssl.enable", true);
                props.put("mail.smtp.ssl.checkserveridentity", false);
                props.put("mail.smtp.ssl.trust", "*");
                // props.put("mail.smtp.ssl.protocols", "TLSv1.2");

                return props;
        }
 
        public boolean send() throws Exception { 
                Properties props = _setProperties(); 
 
                if(!_user.equals("") && !_pass.equals("") && ( _to != null && _to.length > 0 )
                   && !_from.equals("") && !_subject.equals("") && !_body.equals("")) { 

                        Session session = Session.getDefaultInstance(props, this); 
                        MimeMessage msg = new MimeMessage(session); 
                        msg.setFrom(new InternetAddress(_from)); 
 
                        if ( _to != null && _to.length > 0 ) {
                           InternetAddress[] addressTo = new InternetAddress[_to.length]; 
                           for (int i = 0; i < _to.length; i++) { 
                              addressTo[i] = new InternetAddress(_to[i]); 
                           } 
                           msg.setRecipients(MimeMessage.RecipientType.TO, addressTo); 
                        }
 
                        if ( _bcc != null && _bcc.length > 0 ) {
                           InternetAddress[] addressBcc = new InternetAddress[_bcc.length]; 
                           for (int i = 0; i < _bcc.length; i++) { 
                               addressBcc[i] = new InternetAddress(_bcc[i]); 
                           } 
                           msg.setRecipients(MimeMessage.RecipientType.BCC, addressBcc); 
                        }
 
                        msg.setSubject(_subject); 
                        msg.setSentDate(new Date()); 
                        msg.setReplyTo(new Address[]{new InternetAddress(_reply)}); 
 
                        // setup message body 
                        BodyPart messageBodyPart = new MimeBodyPart(); 
                        messageBodyPart.setText(_body); 
                        _multipart.addBodyPart(messageBodyPart); 
 
                        // Put parts in message 
                        msg.setContent(_multipart); 
 
                        // send email 
                        Transport.send(msg); 
 
                        return true; 
                } else { 
                        return false; 
                } 
        } 
 
        public void addAttachment(String filename) throws Exception { 
                BodyPart messageBodyPart = new MimeBodyPart(); 
                DataSource source = new FileDataSource(filename); 
                messageBodyPart.setDataHandler(new DataHandler(source)); 
                messageBodyPart.setFileName(filename); 
                _multipart.addBodyPart(messageBodyPart); 
        } 
 
        @Override 
        public PasswordAuthentication getPasswordAuthentication() { 
                return new PasswordAuthentication(_user, _pass); 
        } 
 
        // the getters and setters 
        public String getBody() { 
                return _body; 
        } 
 
        public void setBody(String _body) { 
                this._body = _body; 
        } 
 
        public String[] getTo() {
                return this._to;
        }
 
        public void setTo(String[] toArr) {
                this._to = toArr;
        }
 
        public String[] getBcc() {
                return this._bcc;
        }
 
        public void setBcc(String[] bccArr) {
                this._bcc = bccArr;
        }
 
        public String getFrom() {
                return this._from;
        }
 
        public void setFrom(String string) {
                this._from = string;
        }
 
        public String getHost() {
                return this._host;
        }
 
        public void setHost(String host) {
                this._host = host;
        }
 
        public String getPort() {
                return this._port;
        }
 
        public void setPort(String port) {
                this._port = port;
        }
 
        public String getSPort() {
                return this._sport;
        }
 
        public void setSPort(String sport) {
                this._sport = sport;
        }
 
        public String getSubject() {
                return this._subject;
        }

        public void setSubject(String string) {
                this._subject = string;
        }
 
        public String getReply() {
                return this._reply;
        }

        public void setReply(String reply) {
                this._reply = reply;
        }
 
        // more of the getters and setters �.. 
} 

