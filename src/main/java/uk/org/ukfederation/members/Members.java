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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import net.shibboleth.shared.component.ComponentInitializationException;
import uk.org.ukfederation.members.jaxb.BaseGrantType;
import uk.org.ukfederation.members.jaxb.DomainElement;
import uk.org.ukfederation.members.jaxb.DomainOwnerElement;
import uk.org.ukfederation.members.jaxb.DomainsElement;
import uk.org.ukfederation.members.jaxb.GrantsElement;
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

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(Members.class);

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
     * A {@link Map} recording the (single) participant registered as owning a
     * given domain.
     */
    private Map<String, ParticipantType> domainOwners = new HashMap<>();

    /**
     * Constructs a {@link Members} object from an XML document.
     * 
     * This constructor validates against the schema document passed as parameter.
     * 
     * @param document {@link Document} node to base the {@link Members} object on
     * @param schema schema to validate against
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    private Members(@Nonnull final Source document, @Nonnull final Source schema)
            throws JAXBException, SAXException, ComponentInitializationException {
        membersElement = makeUnmarshaller(schema).unmarshal(document, MembersElement.class).getValue();

        // Index members.
        for (final MemberElement member : membersElement.getMember()) {
            addParticipant(member);
            if (member.getDomains() != null) {
                addParticipantDomains(member, member.getDomains());
            }
        }
        
        // Index domain owners.
        for (final DomainOwnerElement domainOwner : membersElement.getDomainOwner()) {
            addParticipant(domainOwner);
            if (domainOwner.getDomains() != null) {
                addParticipantDomains(domainOwner, domainOwner.getDomains());
            }
        }
        
        /*
         * Cross-check Grants elements.
         * 
         * This needs to be done after all participants have been registered so that we can
         * see which participant each Grant is made to. However, each grant must by definition
         * be made to an entity owner, and those must all be members.
         */
        for (final MemberElement member : membersElement.getMember()) {
            checkGrants(member.getGrants(), member.getName());
        }
        for (final DomainOwnerElement domainOwner : membersElement.getDomainOwner()) {
            checkGrants(domainOwner.getGrants(), domainOwner.getName());
        }
    }

    /**
     * Constructs a {@link Members} object from an XML document.
     * 
     * This constructor validates against the schema document passed as parameter.
     * This allows a slightly extended schema to be used without rebuilding this
     * project.
     * 
     * @param document {@link Document} node to base the {@link Members} object on
     * @param schema schema to validate against
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    public Members(@Nonnull final Document document, @Nonnull final Document schema)
            throws JAXBException, SAXException, ComponentInitializationException {
        this(new DOMSource(document.getDocumentElement(), "members.xml"),
                new DOMSource(schema.getDocumentElement(), "ukfederation-members.xsd"));
    }

    /**
     * Constructs a {@link Members} object from an XML document.
     * 
     * This constructor validates against the schema defined in this project.
     * 
     * @param doc {@link Document} node to base the {@link Members} object on
     * @throws ComponentInitializationException if there is a problem in the members document
     * @throws JAXBException if there is a problem unmarshalling the members document
     * @throws SAXException if there is a problem parsing the schema document
     */
    public Members(@Nonnull final Document doc) throws ComponentInitializationException, JAXBException, SAXException {
        this(new DOMSource(doc.getDocumentElement(), "members.xml"),
                new StreamSource(Members.class.getClassLoader().getResourceAsStream("ukfederation-members.xsd")));
    }

    /**
     * Make a schema-validating unmarshaller for MembersElement documents.
     * 
     * @param schemaSource schema to validate against
     * @return the constructed {@link Unmarshaller}
     * @throws SAXException if there is a problem parsing the schema document
     * @throws JAXBException if there is a problem constructing the unmarshaller
     */
    private static Unmarshaller makeUnmarshaller(@Nonnull final Source schemaSource)
            throws SAXException, JAXBException {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        final Schema schema = sf.newSchema(schemaSource);
        
        final JAXBContext jc = JAXBContext.newInstance(MembersElement.class);
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        unmarshaller.setSchema(schema);
        return unmarshaller;
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
     * Records the domains registered to a particular participant.
     *
     * @param participant participant owning at least one domain
     * @param domainsElement the {@link DomainsElement} for the participant
     * @throws ComponentInitializationException if a domain is registered to more than one participant
     */
    private void addParticipantDomains(@Nonnull final ParticipantType participant,
            @Nonnull final DomainsElement domainsElement)
        throws ComponentInitializationException {
        for (final DomainElement domainElement : domainsElement.getDomain()) {
            final String domain = domainElement.getValue();
            final ParticipantType previousParticipant = domainOwners.get(domain);
            if (previousParticipant == null) {
                // not previously registered, OK
                domainOwners.put(domain, participant);
            } else if (previousParticipant == participant) {
                // two registrations in a single participant
                throw new ComponentInitializationException("participant \"" + participant.getName() +
                        "\" registers domain \"" + domain + "\" more than once");
            } else {
                // two different participants
                throw new ComponentInitializationException("domain \"" + domain +
                        "\" appears in multiple participants: " +
                        "\"" + previousParticipant.getName() + "\", " + 
                        "\"" + participant.getName() + "\"");
            }
        }
    }

    /**
     * Check that a series of grants are valid.
     * 
     * @param grants the {@link GrantsElement} to check
     * @param name name of the granting participant
     * @throws ComponentInitializationException if a grant is not valid
     */
    private void checkGrants(@Nullable final GrantsElement grants, @Nonnull final String name)
            throws ComponentInitializationException {
        if (grants == null) {
            return;
        }
        
        for (final BaseGrantType grant : grants.getGrantOrGrantAll()) {
            // Grant must be to a participant we can look up by name
            final ParticipantType to = participantByName.get(grant.getTo());
            if (to == null) {
                final String message = "unknown grant to=\"" + grant.getTo()
                        + "\" in participant \"" + name + "\"";
                log.error(message);
                throw new ComponentInitializationException(message);
            }
            
            // That participant must be a member
            if (!(to instanceof MemberElement)) {
                final String message = "grant to=\"" + grant.getTo()
                        + "\" in participant \"" + name + "\" is not to a member";
                log.error(message);
                throw new ComponentInitializationException(message);
            }
            
            // Make sure that "orgID" and "to" attributes are consistent.
            final Object orgIDObject = grant.getOrgID();
            if (orgIDObject != to) {
                final String target;
                if (orgIDObject instanceof ParticipantType) {
                    target = "wrong participant \"" + ((ParticipantType)orgIDObject).getName() + "\"";
                } else if (orgIDObject == null) {
                    target = "null";
                } else {
                    target = "unknown " + orgIDObject.getClass().getName() + " object";
                }
                final String message = "grant to=\"" + grant.getTo()
                    + "\" in participant \"" + name + " has bad orgID: " + target;
                log.error(message);
                throw new ComponentInitializationException(message);
            }
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
     * Returns the JAXB object for the named participant.
     * 
     * @param name name of participant to look up
     * @return {@link ParticipantType} for the participant, or <code>null</code>
     */
    @Nullable
    public ParticipantType getParticipantByName(@Nonnull final String name) {
        return participantByName.get(name);
    }

    /**
     * Returns the JAXB object for the named member.
     * 
     * Note: looking up a non-member participant will return <code>null</code>.
     * 
     * @param name name of member to look up
     * @return {@link MemberElement} for the member, or <code>null</code>
     */
    @Nullable
    public MemberElement getMemberByName(@Nonnull final String name) {
        final ParticipantType participant = getParticipantByName(name);
        return (participant instanceof MemberElement) ? (MemberElement)participant : null;
    }

    /**
     * Checks for the name of a federation member.
     * 
     * @param name Name to check.
     * @return {@code true} if and only if {@code name} contains a legitimate entity owner name.
     */
    public boolean isOwnerName(@Nonnull final String name) {
        return getMemberByName(name) != null;
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
