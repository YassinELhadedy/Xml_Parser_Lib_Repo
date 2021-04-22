package com.swvl.xmpparserlibrary;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;


class XmlParser2 {
    public static String folderPath = System.getProperty("user.dir") + "/app/src/main/res/";
    public static int Counter = 0;
    final static String LINE_NUMBER_KEY_NAME = "lineNumber";

    public static void listFilesForFolder(final File folder) throws IOException, SAXException {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                if (fileEntry.getName().endsWith(".xml"))
                    parseXMLfile(fileEntry);
                else {
                    //System.out.println(fileEntry.getParent());
                    break;
                }
            }
        }
    }

    public static Document readXML(InputStream is) throws IOException, SAXException {
        final Document doc;
        SAXParser parser;
        try {

            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            DocumentBuilderFactory Factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder Builder = Factory.newDocumentBuilder();
            doc = Builder.newDocument();

        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Can't create SAX parser / DOM builder.", e);
        }

        final Stack<Element> elementStack = new Stack<>();
        final StringBuilder textBuffer = new StringBuilder();

        DefaultHandler handler = new DefaultHandler() {
            private Locator locator;

            @Override
            public void setDocumentLocator(Locator locator) {
                this.locator = locator; //Save the locator, so that it can be used later for line tracking when traversing nodes.
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                addTextIfNeeded();
                Element el = doc.createElement(qName);
                for (int i = 0; i < attributes.getLength(); i++)
                    el.setAttribute(attributes.getQName(i), attributes.getValue(i));
                el.setUserData(LINE_NUMBER_KEY_NAME, String.valueOf(this.locator.getLineNumber()), null);
                elementStack.push(el);
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                addTextIfNeeded();
                Element closedEl = elementStack.pop();
                if (elementStack.isEmpty()) { // Is this the root element?
                    doc.appendChild(closedEl);
                } else {
                    Element parentEl = elementStack.peek();
                    parentEl.appendChild(closedEl);
                }
            }

            @Override
            public void characters(char ch[], int start, int length) {
                textBuffer.append(ch, start, length);
            }

            // Outputs text accumulated under the current node
            private void addTextIfNeeded() {
                if (textBuffer.length() > 0) {
                    Element el = elementStack.peek();
                    Node textNode = doc.createTextNode(textBuffer.toString());
                    el.appendChild(textNode);
                    textBuffer.delete(0, textBuffer.length());
                }
            }
        };
        parser.parse(is, handler);

        return doc;
    }

    public static void parseXMLfile(final File xmlFile) throws IOException, SAXException {
        //String filePath = folderPath + "\\" + xmlFile.getName();

        //Array to save labels in one Activity, to prevent duplicate labels.
        ArrayList hints = new ArrayList();
        ArrayList contents = new ArrayList();
        //For count warnings in one Activity
        int innerCounter = 0;
        /*
         * Get the Document Builder
         * Get Document
         * Normalize the xml structure
         * Get all the element by the tag name
         * */
        InputStream is = new FileInputStream(xmlFile);

        Document document = readXML(is);
        is.close();

        System.out.println("-------------------------------");
        System.out.println("IN FILE: \"" + xmlFile.getName() + "\"");
    /*
        // Get the Document Builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Get Document
        Document document = builder.parse (new File(filePath));
*/
        //Normalize the xml structure
        document.getDocumentElement().normalize();

        //Get all the element by the tag name
        NodeList nodeList = document.getElementsByTagName("*");

        //Loop to start parser.
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node textTag = nodeList.item(i);
            //if the node is an element.
            if (textTag.getNodeType() == Node.ELEMENT_NODE) {
                Element textElement = (Element) textTag;
                //Read text size
                String el_size = textElement.getAttribute("android:textSize");

                //if there is attribute
                if (!el_size.isEmpty()) {
                    String text_size = el_size.substring(0, 2);

//******************/\/\/\/\/\/\/\/\/\/\/\/\..FIRST RULES: TEXT SIZE >= 31sp../\/\/\/\/\/\/\/\/\/\/\

                    if (Integer.parseInt(text_size) < 31) {
                        innerCounter++;
                        Counter++;

                        System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": The text size of <" + textTag.getNodeName() +
                                "> is \"" + el_size + "\", it must be not less than \"31\"..");
                    }
                }//________________________________________________________________________\\

//******************/\/\/\/\/\/\/\/\/\/\/\/\..SECOND RULES: ALL COMPONENT OF THE ACTIVITY MUST HAVE A LABEL../\/\/\
//__________________________________________..text fields must have hint not contentDescription..__________________

                //Read content description of the element
                String el_contentDescription = textElement.getAttribute("android:contentDescription");

                if (textTag.getNodeName().equals("EditText") || textTag.getNodeName().equals("AutoCompleteTextView")
                        || textTag.getNodeName().equals("MultiAutoCompleteTextView")
                        || textTag.getNodeName().equals("com.google.android.material.textfield.TextInputEditText")) {
                    //Read hint of the element
                    String el_hint = textElement.getAttribute("android:hint");

                    //if there is contentDescription
                    if (!el_contentDescription.isEmpty()) {
                        innerCounter++;
                        Counter++;
                        System.out.print("________ Warning in line " + textTag.getUserData("lineNumber") + ": the component <" + textTag.getNodeName());
                        System.out.println("> Input fields should have their speakable text set as “hints”, not “content description”. \n" +
                                "If the content description property is set, the screen reader will read it even when the input field is" +
                                " not empty, which could confuse the user who might not know what part is\n" +
                                "the text in the input field and which part is the content description.");
                    }
                    //if there is hint
                    if (!el_hint.isEmpty()) {

//******************/\/\/\/\/\/\/\/\/\/\/\/\..THIRD RULES: THE LABELS NOT DUPLICATE IN ONE ACTIVITY../\/\/\/\/\/\

                        //Check hint in arraylist, if it exist, there is duplicate, print warning
                        if (hints.contains(el_hint)) {
                            innerCounter++;
                            Counter++;
                            System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": duplicate label \"" + el_hint + "\" in <" + textTag.getNodeName() + ">");
                        } else
                            hints.add(el_hint);
                    } else {
                        innerCounter++;
                        Counter++;
                        System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": Missing \"hint\" to provide instructions on how to fill the data entry field for the component: <" + textTag.getNodeName() + ">");
                    }

//*****************/\/\/\/\/\/\/\/\/\/\/\/\..FOURTH RULES: PROVIDE FIELD FILL-IN TIPS TO AVOID INCREASING THE VISUALLY IMPAIRED USER INTERACTION LOAD DUE TO INCORRECT INPUT.

                    //Read fill in tips of the element
                    String el_text = textElement.getAttribute("android:text");
                    if (el_text.isEmpty()) {
                        //Counter++;
                        System.out.println("________ ** Note in line " + textTag.getUserData("lineNumber") + ": Try to write text tips to help user to fill field in component <" + textTag.getNodeName() + ">");
                    }
                }

//******************/\/\/\/\/\/\/\/\/\/\/\/\..FIFTH RULES: WARN IF THE IMAGE CONTAIN TEXT../\/\/\/\/\/\/\/\/\/\/\

                if (textTag.getNodeName().equals("ImageView")) {
                    innerCounter++;
                    Counter++;
                    System.out.print("________ Warning in line " + textTag.getUserData("lineNumber") + ": In the component <" + textTag.getNodeName());
                    System.out.println("> Does the image you inserted contain text? \nIf the answer is \"yes\", this image is not accessible to persons with disabilities.");
                }

//******************/\/\/\/\/\/\/\/\/\/\/\/\..SIXTH RULES: THE BUTTON AND OTHER CLICKABLE ELEMENTS SIZE NOT LESS THAN "57dp" HIGHT AND "57dp" WIDTH.

                if (textTag.getNodeName().equals("Button") || textTag.getNodeName().equals("ImageButton")
                        || textTag.getNodeName().equals("RadioButton") || textTag.getNodeName().equals("CheckBox")
                        || textTag.getNodeName().equals("Switch") || textTag.getNodeName().equals("ToggleButton")
                        || textTag.getNodeName().equals("com.google.android.material.floatingactionbutton.FloatingActionButton")) {
                    //Read width of the element.
                    String el_width = textElement.getAttribute("android:layout_width");
                    //Read hight of the element.
                    String el_height = textElement.getAttribute("android:layout_height");

                    if (!(el_width.equalsIgnoreCase("wrap_content") || el_width.equalsIgnoreCase("match_parent"))) {
                        int index = el_width.indexOf("d");

                        if (Integer.parseInt(el_width.substring(0, index)) < 57) {
                            innerCounter++;
                            Counter++;
                            System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": The width size of <" + textTag.getNodeName() +
                                    "> is \"" + el_width + "\", it must be not less than \"57dp\"..");
                        }
                    }
                    if (!(el_height.equalsIgnoreCase("wrap_content") || el_height.equalsIgnoreCase("match_parent"))) {
                        int index = el_height.indexOf("d");
                        if (Integer.parseInt(el_height.substring(0, index)) < 57) {
                            innerCounter++;
                            Counter++;
                            System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": The height size of <" + textTag.getNodeName() +
                                    "> is \"" + el_height + "\", it must be not less than \"57dp\"..");
                        }
                    }
                    //check if contentDescription missing or duplicated.
                    //if there is contentDescription
                    if (!el_contentDescription.isEmpty()) {

                        //Check contentDescription in arraylist, if it exist, there is duplicate, print warning
                        if (contents.contains(el_contentDescription)) {
                            innerCounter++;
                            Counter++;
                            System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": duplicate label \"" + el_contentDescription + "\" in <" + textTag.getNodeName() + ">");
                        } else
                            contents.add(el_contentDescription);
                    } else {
                        innerCounter++;
                        Counter++;
                        System.out.println("________ Warning in line " + textTag.getUserData("lineNumber") + ": Missing \"contentDescription\" for the component: <" + textTag.getNodeName() + ">");
                    }
                }
            }
        }
        if (innerCounter == 0)
            System.out.println("<..PASS..>, warning " + innerCounter);
    }
}
