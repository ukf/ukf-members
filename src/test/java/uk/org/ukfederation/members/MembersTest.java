/*
 * Copyright (C) 2011 University of Edinburgh.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ukfederation.members;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.custommonkey.xmlunit.Diff;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

/**
 * Tests for the {@link Members} class.
 * 
 * Makes use of TestNG and XMLUnit.
 * 
 * @author iay
 */
public class MembersTest {

    /** Document builder factory used by every test. */
    private DocumentBuilderFactory dbFactory;

    /** Transformer factory used by many tests. */
    private TransformerFactory tFactory;

    /**
     * Acquire a stream for the named test resource.
     * 
     * @param resourceName name of the resource to acquire
     * @return {@InputStream} to the resource
     */
    private InputStream streamResource(String resourceName) {
        return MembersTest.class.getResourceAsStream("/" + resourceName);
    }

    /**
     * Fetch a {@link Members} object corresponding to the named resource.
     * 
     * @param resourceName name of the resource
     * @return {@link Members} object corresponding to the resource.
     */
    private Members fetchMembers(String resourceName) {
        return new Members(streamResource(resourceName));
    }

    /**
     * Fetch an XML {@link Document} from the named resource.
     * 
     * @param resourceName name of the resource to parse
     * @return the resource as a {@link Document}
     * 
     * @throws Exception if anything goes wrong
     */
    private Document fetchDocument(String resourceName) throws Exception {
        DocumentBuilder db = dbFactory.newDocumentBuilder();
        return db.parse(streamResource(resourceName));
    }

    /**
     * Setup performed before any test is run.
     * 
     * @throws Exception is anything goes wrong.
     */
    @BeforeClass
    public void setUp() throws Exception {
        dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        dbFactory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        tFactory = TransformerFactory.newInstance();
    }

    /**
     * Tests {@link Members.isOwnerName} with simple {@link String} parameters.
     * 
     * Also, inter alia, tests the stream constructor.
     */
    @Test
    public void testIsOwnerNameString() {
        Members m = fetchMembers("oneOfEach.xml");
        Assert.assertTrue(m.isOwnerName("Valid Member"));
        Assert.assertTrue(m.isOwnerName("Valid Non Member"));
        Assert.assertFalse(m.isOwnerName("Should not be present"));
    }

    /**
     * Tests construction of a {@link Members} object from a DOM document.
     * 
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testConstructorDocument() throws Exception {
        Members m = new Members(fetchDocument("oneOfEach.xml"));
        Assert.assertTrue(m.isOwnerName("Valid Member"));
        Assert.assertTrue(m.isOwnerName("Valid Non Member"));
        Assert.assertFalse(m.isOwnerName("Should not be present"));
    }

    /**
     * Tests the {@link File} constructor.
     * 
     * @throws Exception is anything goes wrong
     */
    @Test
    public void testConstructorFile() throws Exception {
        URL u = MembersTest.class.getResource("/oneOfEach.xml");
        File f = new File(u.toURI());
        Members m = new Members(f);
        Assert.assertTrue(m.isOwnerName("Valid Member"));
        Assert.assertTrue(m.isOwnerName("Valid Non Member"));
        Assert.assertFalse(m.isOwnerName("Should not be present"));
    }

    /**
     * Tests the {@link isMemberName} predicate used from within XSLT.
     * 
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testIsOwnerNameXSLT() throws Exception {
        Document in = fetchDocument("isMemberNameIn.xml");
        Document tr = fetchDocument("isMemberName.xsl");
        Document ok = fetchDocument("isMemberNameOK.xml");
        Transformer t = tFactory.newTransformer(new DOMSource(tr));
        DOMResult out = new DOMResult();
        t.transform(new DOMSource(in), out);
        Document ou = (Document) (out.getNode());
        Diff diff = new Diff(ok, ou);
        Assert.assertTrue(diff.identical(), "diff comparison should have been identical: " + diff);
    }

}
