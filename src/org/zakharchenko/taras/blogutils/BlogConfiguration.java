package org.zakharchenko.taras.blogutils;
import javax.xml.parsers.*;
import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class BlogConfiguration {
    private String username;
    private String full_name;
    private String email;
    private String uri;
    private String priv_key;

    private void processStream(InputStream input_stream) throws ParserConfigurationException, IOException, SAXException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(input_stream);
        Element root = doc.getDocumentElement();
        username = root.getElementsByTagName("UserName").item(0).getTextContent();
        full_name = root.getElementsByTagName("FullName").item(0).getTextContent();
        email = root.getElementsByTagName("Email").item(0).getTextContent();
        uri = root.getElementsByTagName("RepoURI").item(0).getTextContent();
        priv_key = root.getElementsByTagName("PrivateKey").item(0).getTextContent();
    }

    public BlogConfiguration(InputStream input_stream) throws ParserConfigurationException, IOException, SAXException {
        this.processStream(input_stream);
    }

    public BlogConfiguration(String content) throws ParserConfigurationException, IOException, SAXException {
        ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes("UTF-8"));
        this.processStream(input);
    }

    public BlogConfiguration(FileInputStream file_stream) throws ParserConfigurationException, IOException, SAXException {
        this.processStream(file_stream);
    }

    public String getUserName() {
        return this.username;
    }

    public String getFullName() {
        return this.full_name;
    }

    public String getEmail() {
        return this.email;
    }

    public String getRepoUri() {
        return this.uri;
    }

    public String getPrivateKeyPath() {
        return this.priv_key;
    }
}
