/*
 * Copyright (C) 2013 University of Edinburgh.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import uk.org.ukfederation.members.jaxb.DomainOwnerElement;
import uk.org.ukfederation.members.jaxb.MemberElement;
import uk.org.ukfederation.members.jaxb.MembersElement;
import uk.org.ukfederation.members.jaxb.ParticipantType;
import uk.org.ukfederation.members.jaxb.ScopesElement;

/**
 * Java bean representing the whole of the members.xml file.
 * 
 * This class is responsible for parsing the file into a basic object graph using JAXB, and provides a facade
 * simplifying access to much of the functionality required by client applications.
 * 
 * Author: Ian A. Young, ian@iay.org.uk
 */
public class Members {

    /**
     * The root of the underlying JAXB object graph.
     */
    private final MembersElement membersElement;

    /**
     * Map of participants indexed by name.
     */
    private final Map<String, ParticipantType> participantByName = new HashMap<>();
    
    /**
     * Collections of (non-regex) scopes pushed to each entity.
     * 
     * This is initialised the first time it is needed.
     */
    private Map<String, List<String>> pushedScopes;
    
    /**
     * Internal constructor, called by all public constructors.
     * 
     * @param m The {@link MembersElement} on which to base the new bean.
     * @throws ComponentInitializationException if there is a problem in the members document
     */
    private Members(@Nonnull final MembersElement m) throws ComponentInitializationException {
        membersElement = m;
        
        // Index members.
        for (final MemberElement member : membersElement.getMember()) {
            addParticipant(member);
        }
        
        // Index domain owners.
        for (final DomainOwnerElement domainOwner : membersElement.getDomainOwner()) {
            addParticipant(domainOwner);
        }
    }

    /**
     * Constructs a {@link Members} object from an XML document represented as a stream.
     * 
     * @param stream {@link InputStream} to parse.
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    public Members(@Nonnull final InputStream stream)
            throws ComponentInitializationException, JAXBException, SAXException {
        this(makeUnmarshaller().unmarshal(new StreamSource(stream), MembersElement.class).getValue());
    }

    /**
     * Constructs a {@link Members} object from an XML document represented as a DOM {@link Node} object.
     * 
     * @param doc {@link Document} node to base the {@link Members} object on
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    public Members(@Nonnull final Node doc) throws ComponentInitializationException, JAXBException, SAXException {
        this(makeUnmarshaller().unmarshal(makeDOMSource(doc), MembersElement.class).getValue());
    }

    /**
     * Constructs a {@link Members} object from an XML document referred to as a {@link File}.
     * 
     * @param file File to be converted into a {@link Members} object.
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    public Members(@Nonnull final File file) throws ComponentInitializationException, JAXBException, SAXException {
        this((MembersElement)makeUnmarshaller().unmarshal(file));
    }

    /**
     * Make a schema-validating unmarshaller for MembersElement documents.
     * 
     * @return the constructed {@link Unmarshaller}
     * @throws SAXException if there is a problem parsing the schema document
     * @throws JAXBException if there is a problem constructing the unmarshaller
     */
    private static Unmarshaller makeUnmarshaller() throws SAXException, JAXBException {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI); 
        final Source schemaSource =
                new StreamSource(Members.class.getClassLoader().getResourceAsStream("ukfederation-members.xsd"));
        final Schema schema = sf.newSchema(schemaSource);
        
        final JAXBContext jc = JAXBContext.newInstance(MembersElement.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setSchema(schema);
        return unmarshaller;
    }

    /**
     * Helper method for constructors.
     * 
     * Returns a {@link DOMSource} derived from the provided DOM {@link Node}. If the node is a {@link Document}, dig
     * down to its document element. This sometimes helps JAXB when called from within Xalan.
     * 
     * @param node DOM node to start from.
     * @return An appropriate {@link DOMSource}.
     */
    private static DOMSource makeDOMSource(@Nonnull final Node node) {
        if (node == null) {
            throw new NullPointerException("provided DOM node is null");
        }

        if (node instanceof Document) {
            Document d = (Document) node;
            return new DOMSource(d.getDocumentElement());
        }
        return new DOMSource(node);
    }

    /**
     * Registers a participant (a member or domain owner) from the members document.
     * 
     * @param participant the participant to register
     * @throws ComponentInitializationException if the participant is malformed
     */
    private void addParticipant(@Nonnull final ParticipantType participant) throws ComponentInitializationException {
        final String name = participant.getName();
        if (participantByName.containsKey(name)) {
            throw new ComponentInitializationException("duplicate participant name in members document: " + name);
        } else {
            participantByName.put(name, participant);
        }
    }

    /**
     * Returns the {@link MembersElement} object the bean is based on.
     * 
     * @return the {@link MembersElement} object
     */
    @Nonnull public MembersElement getMembersElement() {
        return membersElement;
    }

    /**
     * Checks for the name of a federation member.
     * 
     * @param s Name to check.
     * @return {@code true} if and only if {@code s} contains a legitimate entity owner name.
     */
    public boolean isOwnerName(@Nonnull final String s) {
        return participantByName.get(s) instanceof MemberElement;
    }
    
    /**
     * Collect all of the scopes mentioned in Scopes elements for different
     * members.  Store these away in a map that allows us to retrieve them
     * by reference to the entityID which they were pushed to.
     */
    private void collectPushedScopes() {
        // initialise the map we store everything in
        pushedScopes = new HashMap<>();
        
        // loop through each Member element
        for (final MemberElement member : membersElement.getMember()) {
            
            // each Member may have multiple Scopes elements
            for (final ScopesElement scopesElement : member.getScopes()) {
                
                // each Scopes may have many Entity elements
                final List<String> entities = scopesElement.getEntity();
                
                // each Scopes may have many Scope elements
                final List<String> scopes = scopesElement.getScope();
                
                // Register each of those scopes for each of those entities
                for (String entityID: entities) {
                    final List<String> scopeList = pushedScopes.get(entityID);
                    if (scopeList == null) {
                        // first mention of this entity
                        pushedScopes.put(entityID, new ArrayList<>(scopes));
                    } else {
                        // entity has a list already, add in the new ones
                        scopeList.addAll(scopes);
                    }
                }
            }
        }
    }

    /**
     * Computes the "pushed" scope list for the named entity.
     * 
     * @param entityID name of the entity
     * @return ordered list of scopes to be added to the entity, or <code>null</code>
     */
    public List<String> scopesForEntity(@Nonnull final String entityID) {
        
        // retrieve the pushed scopes if they have not already been retrieved
        if (pushedScopes == null) {
            collectPushedScopes();
        }
        
        return pushedScopes.get(entityID);
    }

}
