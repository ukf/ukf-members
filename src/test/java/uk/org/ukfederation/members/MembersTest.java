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

import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

import jakarta.xml.bind.UnmarshalException;
import net.shibboleth.shared.component.ComponentInitializationException;
import uk.org.ukfederation.members.jaxb.DomainOwnerElement;
import uk.org.ukfederation.members.jaxb.MemberElement;
import uk.org.ukfederation.members.jaxb.MembersElement;
import uk.org.ukfederation.members.jaxb.ParticipantType;

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
    private Members fetchMembers(String resourceName) throws Exception {
        return new Members(fetchDocument(resourceName));
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
            Assert.assertTrue(e.getMessage().contains("duplicate participant name"));
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
            Assert.assertTrue(e.getMessage().contains("duplicate participant name"));
            return;
        }
        Assert.fail("expected component initialization exception");
    }

    @Test
    public void testDuplicateOrgID() throws Exception {
        try {
            new Members(fetchDocument("duplicateOrg.xml"));
        } catch (UnmarshalException e) {
            // expected
            Assert.assertTrue(e.getLinkedException() instanceof SAXParseException);
            return;
        }
        Assert.fail("expected parsing exception");
    }

    @Test
    public void getParticipantByName() throws Exception {
        final Members m = new Members(fetchDocument("oneOfEach.xml"));
        
        final ParticipantType p1 = m.getParticipantByName("not present");
        Assert.assertNull(p1);
        
        final ParticipantType p2 = m.getParticipantByName("Valid Member");
        Assert.assertNotNull(p2);
        Assert.assertTrue(p2 instanceof MemberElement);
        Assert.assertEquals(p2.getName(), "Valid Member");
        Assert.assertEquals(p2.getID(), "ukforg12345");

        final ParticipantType p3 = m.getParticipantByName("Domain Owner");
        Assert.assertNotNull(p3);
        Assert.assertTrue(p3 instanceof DomainOwnerElement);
        Assert.assertEquals(p3.getName(), "Domain Owner");
        Assert.assertEquals(p3.getID(), "ukforg1234");
    }

    @Test
    public void getMemberByName() throws Exception {
        final Members m = new Members(fetchDocument("oneOfEach.xml"));
        
        final MemberElement m1 = m.getMemberByName("not present");
        Assert.assertNull(m1);

        final MemberElement m2 = m.getMemberByName("Valid Member");
        Assert.assertNotNull(m2);
        Assert.assertEquals(m2.getName(), "Valid Member");
        Assert.assertEquals(m2.getID(), "ukforg12345");

        final MemberElement m3 = m.getMemberByName("Domain Owner");
        Assert.assertNull(m3);
    }

    @Test
    public void testBadGrantTo() throws Exception {
        try {
            new Members(fetchDocument("badGrantTo.xml"));
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("unknown grant to="), "wrong message");
            return;            
        }
        Assert.fail("expected component initialization exception");
    }

    @Test
    public void testBadGrantToNonMember() throws Exception {
        try {
            new Members(fetchDocument("badGrantToNonMember.xml"));
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("not to a member"), "wrong message");
            return;            
        }
        Assert.fail("expected component initialization exception");
    }
    
    @Test
    public void goodGrants() throws Exception {
        final Members members = new Members(fetchDocument("goodGrants.xml"));
        Assert.assertEquals(members.getMembersElement().getMember().size(), 2);
        Assert.assertEquals(members.getMembersElement().getDomainOwner().size(), 1);
        
        final MemberElement m1 = members.getMembersElement().getMember().get(0);
        final MemberElement m2 = members.getMembersElement().getMember().get(1);
        final DomainOwnerElement d1 = members.getMembersElement().getDomainOwner().get(0);
        
        Assert.assertEquals(m1.getGrants().getGrantOrGrantAll().size(), 1);
        Assert.assertSame(m1.getGrants().getGrantOrGrantAll().get(0).getOrgID(), m2);
        
        Assert.assertEquals(m2.getGrants().getGrantOrGrantAll().size(), 1);
        Assert.assertSame(m2.getGrants().getGrantOrGrantAll().get(0).getOrgID(), m1);
                
        Assert.assertEquals(d1.getGrants().getGrantOrGrantAll().size(), 2);
        Assert.assertSame(d1.getGrants().getGrantOrGrantAll().get(0).getOrgID(), m1);
        Assert.assertSame(d1.getGrants().getGrantOrGrantAll().get(1).getOrgID(), m2);
    }
    
    @Test
    public void badGrantOrgID() throws Exception {
        try {
            new Members(fetchDocument("badGrantOrgID.xml"));
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("wrong participant"), "wrong message");
            return;            
        }
        Assert.fail("expected component initialization exception");
    }
    
    @Test
    public void overrideSchema() throws Exception {
        final Document schemaDocument = fetchDocument("overrideSchema.xsd");
        final Document membersDocument = fetchDocument("overrideSchema.xml");
        final Members members = new Members(membersDocument, schemaDocument);
        final MembersElement membersElement = members.getMembersElement();
        membersElement.getMember().get(0);
    }

    @Test
    public void duplicateDomain() throws Exception {
        // straightforward: two Members
        try {
            new Members(fetchDocument("duplicateDomain.xml"));
            Assert.fail("expected component initialization exception");
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("domain"), "wrong message");
            Assert.assertTrue(e.getMessage().contains("appears in multiple participants"), "wrong message");
        }
    }

    @Test
    public void duplicateDomainSingle() throws Exception {
        // twice in the same member
        try {
            new Members(fetchDocument("duplicateDomain2.xml"));
            Assert.fail("expected component initialization exception");
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("domain"), "wrong message");
            Assert.assertTrue(e.getMessage().contains("more than once"), "wrong message");
        }
    }

    @Test
    public void duplicateDomainMix() throws Exception {
        // one Member, one DomainOwner
        try {
            new Members(fetchDocument("duplicateDomain3.xml"));
            Assert.fail("expected component initialization exception");
        } catch (final ComponentInitializationException e) {
            // expected
            Assert.assertTrue(e.getMessage().contains("domain"), "wrong message");
            Assert.assertTrue(e.getMessage().contains("appears in multiple participants"), "wrong message");
        }
    }
}
