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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import uk.org.ukfederation.members.jaxb.MemberElement;
import uk.org.ukfederation.members.jaxb.MembersElement;

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

    /**
     * Acquire a stream for the named test resource.
     * 
     * @param resourceName name of the resource to acquire
     * @return {@link InputStream} to the resource
     */
    private InputStream streamResource(String resourceName) {
        return MembersTest.class.getResourceAsStream("/" + resourceName);
    }

    /**
     * Fetch a {@link Members} object corresponding to the named resource.
     * 
     * @param resourceName name of the resource
     * @return {@link Members} object corresponding to the resource.
     * @throws ComponentInitializationException if there is a problem in the members document
     */
    private Members fetchMembers(String resourceName) throws ComponentInitializationException {
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
    }

    /**
     * Tests {@link Members#isOwnerName} with simple {@link String} parameters.
     * 
     * Also, inter alia, tests the stream constructor.
     */
    @Test
    public void testIsOwnerNameString() throws Exception {
        Members m = fetchMembers("oneOfEach.xml");
        Assert.assertTrue(m.isOwnerName("Valid Member"));
        Assert.assertFalse(m.isOwnerName("Should not be present"));
        Assert.assertFalse(m.isOwnerName("Domain Owner"));
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
        Assert.assertFalse(m.isOwnerName("Should not be present"));
    }
    
    /**
     * Test scopes pushed from within the members document to entities.
     * 
     * @throws Exception if anything goes wrong.
     */
    @Test
    public void testPushedScopes() throws Exception {
        final Members m = new Members(fetchDocument("pushedScopes.xml"));
        final List<String> e0 = m.scopesForEntity("entity0");
        Assert.assertNull(e0);

        final List<String> e1 = m.scopesForEntity("entity1");
        Assert.assertEquals(e1.size(), 4);
        Assert.assertTrue(e1.contains("iay.org.uk"));
        Assert.assertTrue(e1.contains("example.com"));
        Assert.assertTrue(e1.contains("ed.ac.uk"));
        Assert.assertTrue(e1.contains("sub.ed.ac.uk"));
        Assert.assertFalse(e1.contains("no"));
        
        final List<String> e2 = m.scopesForEntity("entity2");
        Assert.assertEquals(e2.size(), 2);
        Assert.assertTrue(e2.contains("example.com"));
        Assert.assertTrue(e2.contains("ed.ac.uk"));
        Assert.assertFalse(e2.contains("no"));
    }

    @Test
    public void getMembersElement() throws Exception {
        final Members m = new Members(fetchDocument("oneOfEach.xml"));
        final MembersElement members = m.getMembersElement();
        Assert.assertNotNull(members);
        final List<MemberElement> memberList = members.getMember();
        Assert.assertNotNull(memberList);
        Assert.assertEquals(memberList.size(), 1);
    }

    @Test
    public void testDuplicateMemberName() throws Exception {
        try {
            new Members(fetchDocument("duplicateMember.xml"));
        } catch (ComponentInitializationException e) {
            // expected
            return;
        }
        Assert.fail("expected component initialization exception");
    }

    @Test
    public void testDuplicateParticipantName() throws Exception {
        try {
            new Members(fetchDocument("duplicateParticipant.xml"));
        } catch (ComponentInitializationException e) {
            // expected
            return;
        }
        Assert.fail("expected component initialization exception");
    }
}
