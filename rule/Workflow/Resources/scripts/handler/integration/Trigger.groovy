package handler.integration
import groovy.io.FileType
import groovy.xml.MarkupBuilder
import kz.nextbase.script.*
import kz.nextbase.script.events._DoScheduledHandler
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class Trigger extends _DoScheduledHandler {

    void sendDocuments(_Session ses) {
        def db = ses.getCurrentDatabase()
        //def out_col = db.getCollectionOfDocuments("form = 'out' and pushedToMI = '1' and sentToMI = '0'", false)
        def out_col = db.getCollectionOfDocuments("form = 'out'", false)
        out_col.entries.each {
            def out_doc = ((_ViewEntry)it).document
            sendOutgoingDocument(out_doc, db)
        }
    }

    void sendOutgoingDocument(_Document doc, _Database db) {
        File tmpdir = new File("tmp/" + _Helper.getRandomValue())
        tmpdir.mkdir()
        File file = new File(tmpdir.path + "/document.xml")
        file.createNewFile()
        file.withWriter { writer ->
            def xml = new MarkupBuilder(writer)
            xml.doubleQuotes = true
            xml.mkp.xmlDeclaration('version': "1.0", 'encoding': "UTF-8")
            xml.DocumentXmlInfo('xmlns:xsd' : "http://www.w3.org/2001/XMLSchema", 'xmlns:xsi' : "http://www.w3.org/2001/XMLSchema-instance") {
                //DocumentTypeID('Письмо')
                //   String deltypeXML = someXmlRecords.FormSentDictionaryId?.text();

                def doc_type = doc.getValueString("vid")
                def doc_type_glos = db.getCollectionOfGlossaries("form = 'vid' and docid = ${doc_type}", 0, 1)
                def doc_type_viewtext = ""
                doc_type_glos.entries.each {
                    doc_type_viewtext = it.document.getValueString("viewtext")
                }
                DocumentTypeID(doc_type_viewtext)

                def del_type = doc.getValueString("deliverytype")
                def del_type_glos = db.getCollectionOfGlossaries("form = 'deliverytype' and docid = ${del_type}", 0, 1)
                def del_type_viewtext = ""
                del_type_glos.entries.each {
                    del_type_viewtext = it.document.getValueString("viewtext")
                }
                FormSentDictionaryId(del_type_viewtext)

                DocumentNumber(doc.getValueString("vn"))

                Subject(doc.getValueString("briefcontent"))

                SheetCount(doc.getValueString("np"))
                AppendCount(doc.getValueString("np2"))

                def signer = doc.getValueString("signedby")
                def signer_SN = ""
                if (signer) {
                    def signer_emp = db.baseObject.getStructure().getAppUser(signer)
                    signer_SN = signer_emp.getShortName()
                }
                SignerNameRu(signer_SN)

                def cur_org = db.getCollectionOfGlossaries("form = 'corr' and currentorg = '1'", 0, 1)
                def cur_org_id = ""
                cur_org.entries.each {
                    cur_org_id = it.document.getValueString("corrid")
                }
                IdOrg(cur_org_id)

                def corr = doc.getValueString("corr")
                def corr_glos = db.getCollectionOfGlossaries("form = 'corr' and docid = ${corr}", 0, 1)
                def corr_id = ""
                corr_glos.entries.each {
                    corr_id = it.document.getValueString("corrid")
                }
                To(corr_id)

                doc.getAttachments("rtfcontent", tmpdir.absolutePath)
                Attachments {
                    tmpdir.eachFileRecurse(FileType.FILES) { att ->
                        AttachmentXmlInfo {
                            if (!att.name.contains("document.xml")) {
                                FileName(att.name)
                                MimeFileName(UUID.randomUUID().toString() + "." + FilenameUtils.getExtension(att.name))
                            }
                        }
                    }
                }
                /*  <Attachments>
                        <AttachmentXmlInfo>
                            <FileName>5.png</FileName>
                            <MimeFileName>126f3b6990ad4aeeb87ca96c0291fb7b.png</MimeFileName>
                        </AttachmentXmlInfo>*/

                //таких полей в исходящем документе нет
                /*ExecutorFIO('Кожабаев Е.О.')
                ExecutorNA('CN=Erbolat Kozhabayev/O=KMGRM')*/
            }
        }
        def attachments = []
        tmpdir.eachFileRecurse(FileType.FILES) {
            attachments.add(it)
        }
        sendMessage(attachments)
    }

    void sendMessage(ArrayList<File> attachments) {
        String host = "smtp.mail.ru";
        Properties props = System.getProperties();
        props.put("mail.smtp.auth", true);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.user", "4msworkflow@inbox.ru");
        props.put("mail.smtp.password", "kf,jhfnjhbz,eleotuj");
        props.put("mail.smtp.port", "25");
        props.put("mail.smtp.ssl.enable", "true");

        Session session = Session.getDefaultInstance(props, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("4msworkflow@inbox.ru"));

        InternetAddress toAddress = new InternetAddress("4msworkflow@inbox.ru");

        message.addRecipient(Message.RecipientType.TO, toAddress);

        message.setSubject("Document");
        message.setText("Mail text");

        def textpart = new MimeBodyPart()
        textpart.text = 'This is new message'
        def multi = new MimeMultipart()
        attachments.each {
            def attPart = new MimeBodyPart()
            attPart.attachFile(it)
            multi.addBodyPart(attPart)
        }

        multi.addBodyPart(textpart)
        message.content = multi

        Transport transport = session.getTransport("smtps");

        transport.connect(host, "4msworkflow@inbox.ru", "kf,jhfnjhbz,eleotuj");

        transport.sendMessage(message, message.getAllRecipients());
        transport.close();

    }

    void receiveDocuments(_Session ses) {
        def db = ses.getCurrentDatabase()

        def trust_email = "4msworkflow@inbox.ru"
        def password = "kf,jhfnjhbz,eleotuj"
        def props = new Properties()

        def server = "imap.mail.ru"
        def port = 993
        props.setProperty("mail.host", server)
        props.setProperty("mail.debug", String.valueOf(true))
        props.setProperty("mail.imap.port", port.toString())
        props.setProperty("mail.imap.ssl.enable", String.valueOf(true))

        def session = Session.getInstance(props)// .getDefaultInstance(props)
        def store = session.getStore("imap")

        store.connect(trust_email, password)

        def folder = store.getFolder("INBOX")
        folder.open(Folder.READ_ONLY)
        folder.messages.each { msg ->
            def save = false
            try {
                def content = msg.content
                if (content instanceof Multipart) {
                    def doc = new _Document(ses.getCurrentDatabase());
                    def attach_map = [:]
                    for (i in 0..(content.count - 1)) {
                        BodyPart bodyPart = content.getBodyPart(i)
                        if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.disposition)) {
                            InputStream is = bodyPart.getInputStream();
                            if ("document.xml".equalsIgnoreCase(bodyPart.getFileName())) {

                                def someXmlRecords = new XmlParser().parse(is);
                                println someXmlRecords.get("Subject")

                                //проверить на ошибку при отсутствии тега
                                println someXmlRecords.Attachments.AttachmentXmlInfo.size()
                                someXmlRecords.Attachments.AttachmentXmlInfo.each {
                                    attach_map.put(it.MimeFileName.text(), it.FileName.text())
                                }

                                attach_map.each {
                                    println(it.key + " " + it.value)
                                }

                                String current_org = someXmlRecords.To.text()
                                if (current_org) {
                                    def org_col = db.getCollectionOfGlossaries("form = 'corr' and iscurorg = '1' and corrid = '${current_org}'", 1, 1)
                                    if (org_col?.count > 0) {
                                        doc.setForm("IN");
                                        doc.setValueString("in", someXmlRecords.DocumentNumber.text())
                                        doc.setValueString("vn", "")
                                        doc.setValueNumber("vnnumber", 0)
                                        doc.setValueString("briefcontent", someXmlRecords.Subject.text())
                                        doc.setValueString("np", someXmlRecords.SheetCount.text())
                                        doc.setValueString("unid", someXmlRecords.In.text())
                                        doc.setViewText(someXmlRecords.Subject.text())
                                        doc.addViewText(someXmlRecords.Subject.text())

                                        String corrID = someXmlRecords.IdOrg.text()
                                        String corr = ""
                                        if (corrID) {
                                            def corr_col = db.getCollectionOfGlossaries("form = 'corr' and corrid = '${corrID}'", 0, 0)
                                            corr_col?.entries.each {
                                                doc.addGlossaryField("corr", it.document.getDocID())
                                            }
                                        }

                                        //String execFIO = someXmlRecords.ExecutorFIO.text();
                                        String deltypeXML = someXmlRecords.FormSentDictionaryId?.text();
                                        def deltypeglos = ses.getCurrentDatabase().getCollectionOfGlossaries("form = 'deliverytype' and viewtext ~ '${deltypeXML}'", 1, 1)
                                        if (deltypeglos && deltypeglos.count > 0) {
                                            deltypeglos.entries.each {
                                                doc.addGlossaryField("deliverytype", it.document.getDocID())
                                            }
                                        }

                                        String doctypeXML = someXmlRecords.DocumentTypeId?.text()
                                        def doctype = ses.getCurrentDatabase().getCollectionOfGlossaries("form = 'vid' and viewtext ~ '${doctypeXML}'", 1, 1)
                                        if (doctype && doctype.count > 0) {
                                            doctype.entries.each {
                                                doc.addGlossaryField("vid", it.document.getDocID())
                                            }
                                        }

                                        def project = ses.getCurrentDatabase().getGlossaryDocument("form = 'project'", "viewtext ~ 'Вне проекта'")
                                        if (project) {
                                            doc.addGlossaryField("project", project.getDocID())
                                        }

                                        def category = ses.getCurrentDatabase().getGlossaryDocument("form = 'category'", "viewtext ~ 'Общие вопросы'")
                                        if (category) {
                                            doc.addGlossaryField("category", category.getDocID())
                                        }

                                        String din = someXmlRecords.Date.text()
                                        if (din) {
                                            doc.setValueDate("din", _Helper.convertStringToDate(din.replace("T", " ")))
                                        }
                                        doc.setViewDate(new Date())
                                        String execFIO = someXmlRecords.ExecutorFIO.text();
                                        String[] abb = execFIO.split(" ")
                                        abb = abb.collect { it = StringUtils.removeEnd(it, ".") }

                                        def exec = ses.getStructure().getUserByCondition("fullname like '%" + abb.max {
                                            it.length()
                                        } + "%'")
                                        if (exec) {
                                            doc.addValueToList("recipients", exec.getUserID());
                                        }
                                        save = true
                                    }
                                }


                            } else {
                                def f
                                attach_map.each {
                                    println(it.key + "::" + it.value)
                                }
                                println bodyPart.getFileName()
                                println attach_map.containsKey(bodyPart.getFileName())
                                println attach_map.containsValue(bodyPart.getFileName())

                                if (attach_map.containsKey(bodyPart.getFileName())) {
                                    f = new File(attach_map.get(bodyPart.getFileName()));
                                } else {
                                    f = new File(bodyPart.getFileName());
                                }
                                FileOutputStream fos = new FileOutputStream(f);
                                byte[] buf = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = is.read(buf)) != -1) {
                                    fos.write(buf, 0, bytesRead);
                                }
                                fos.close();

                                //только файл может быть и не zip
                                if (f.getAbsolutePath().endsWith("zip")) {
                                    def ant = new AntBuilder()
                                    ant.unzip(src: f, dest: "tmp", overwrite: "true")
                                    //вкладываем zip а не итоговые файлы
                                    File tmpDir = new File("tmp")
                                    tmpDir.mkdir()
                                    tmpDir.eachFile(FileType.FILES) {
                                        doc.addAttachment("rtfcontent", it)
                                    }
                                } else {
                                    doc.addAttachment("rtfcontent", f)
                                }
                            }
                        }
                    }



                    if (save) {
                        doc.setAuthor("wish")
                        doc.addEditor("wish")
                        doc.addEditor("[wish]")
                        doc.addEditor("[supervisor]")
                        doc.addEditor("supervisor")
                        doc.addReader("[wish]")
                        doc.addReader("wish")
                        doc.addReader("observer")
                        doc.addReader("[observer]")
                        doc.save("[supervisor]")
                    }
                }

            } catch (Exception ignore) {
                ignore.printStackTrace()
                println "error processing message, no big deal, moving on"
            }
            println "-----------done------------"
        }
    }

    @Override
    int doHandler(_Session ses) {
        receiveDocuments(ses);
        sendDocuments(ses)
    }

}

