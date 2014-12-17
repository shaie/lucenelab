package com.shaie;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.Attribute;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeSource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public final class XMLParsingTokenizer extends Tokenizer {

    public static final String TYPE_TAG_START = "TAG_START";
    public static final String TYPE_TAG_END = "TAG_END";
    public static final String TYPE_TOKEN = "TOKEN";

    private final XMLInputFactory xmlFactory;
    private XMLStreamReader xmlReader;

    private final Tokenizer textTokenizer;

    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);;

    private boolean consumeText = false;

    /** Make this Tokenizer get attributes from the delegate token stream. */
    private static final AttributeFactory delegatingAttributeFactory(final AttributeSource source) {
        return new AttributeFactory() {
            @Override
            public AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass) {
                return (AttributeImpl) source.addAttribute(attClass);
            }
        };
    }

    public XMLParsingTokenizer(Tokenizer textTokenizer, Reader input) {
        super(delegatingAttributeFactory(textTokenizer), input);
        xmlFactory = XMLInputFactory.newFactory();
        this.textTokenizer = textTokenizer;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (consumeText) {
            if (!textTokenizer.incrementToken()) {
                consumeText = false;
            } else {
                typeAtt.setType(TYPE_TOKEN);
                return true;
            }
        }

        try {
            if (!xmlReader.hasNext()) {
                return false;
            }

            final int event = xmlReader.next();
            switch (event) {
            case XMLStreamConstants.START_ELEMENT:
                typeAtt.setType(TYPE_TAG_START);
                termAtt.setEmpty().append(xmlReader.getLocalName());
                break;
            case XMLStreamConstants.END_ELEMENT:
                typeAtt.setType(TYPE_TAG_END);
                termAtt.setEmpty().append(xmlReader.getLocalName());
                break;
            case XMLStreamConstants.CHARACTERS:
                textTokenizer.setReader(new CharArrayReader(xmlReader.getTextCharacters(), xmlReader.getTextStart(),
                        xmlReader.getTextLength()));
                textTokenizer.reset();
                typeAtt.setType(TYPE_TOKEN);
                consumeText = true;
                return incrementToken();
            case XMLStreamConstants.END_DOCUMENT:
                return false;
            default:
                System.out.println("unhandled event: " + event);
            }
            return true;
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void reset() throws IOException {
        super.reset();
        try {
            xmlReader = xmlFactory.createXMLStreamReader(input);
            textTokenizer.reset();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void end() throws IOException {
        try {
            textTokenizer.end();
        } finally {
            super.end();
        }
    }

    @Override
    public void close() throws IOException {
        try {
            xmlReader.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        } finally {
            try {
                textTokenizer.close();
            } finally {
                super.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        XMLParsingTokenizer tokenizer = new XMLParsingTokenizer(new WhitespaceTokenizer(new StringReader("")),
                new StringReader("<foo>this is the content</foo>"));
        tokenizer.reset();
        TypeAttribute typeAtt = tokenizer.addAttribute(TypeAttribute.class);
        CharTermAttribute termAtt = tokenizer.addAttribute(CharTermAttribute.class);
        PositionIncrementAttribute posIncrAtt = tokenizer.addAttribute(PositionIncrementAttribute.class);
        while (tokenizer.incrementToken()) {
            System.out.println("term=" + termAtt + ", type=" + typeAtt.type() + ", posIncr="
                    + posIncrAtt.getPositionIncrement());
        }
        tokenizer.end();
        tokenizer.close();
    }

}
