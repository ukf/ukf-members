/*
 * Simplified access to the UK federation members.xml database.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXB;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import uk.org.ukfederation.members.jaxb.MemberElement;
import uk.org.ukfederation.members.jaxb.MembersElement;
import uk.org.ukfederation.members.jaxb.NonMemberElement;

/*
 * Java bean representing the whole of the members.xml file.
 * 
 * This class is responsible for parsing the file into a basic object graph using JAXB, and
 * provides a facade simplifying access to much of the functionality required by client
 * applications.
 * 
 * Author: Ian A. Young, ian@iay.org.uk
 */
public class Members {
	
	/**
	 * The root of the underlying JAXB object graph.
	 */
	private final MembersElement membersElement;

	/**
	 * Set of all the owner names within the members document.
	 */
	private final Set<String> ownerNames = new HashSet<String>();
	
	/**
	 * Internal constructor, called by all public constructors.
	 * 
	 * @param m The {@link MembersElement} on which to base the new bean.
	 */
	private Members(MembersElement m) {
		this.membersElement = m;

		/*
		 * Collect names of members.
		 */
		for (MemberElement member: membersElement.getMember()) {
			ownerNames.add(member.getName());
		}
		
		/*
		 * Collect names of non-members.
		 */
		for (NonMemberElement nonMember: membersElement.getNonMember()) {
			ownerNames.add(nonMember.getName());
		}
	}
	
	/**
	 * Constructs a {@link Members} object from an XML document represented as a stream.
	 * 
	 * @param stream
	 */
	public Members(InputStream stream) {
		this(JAXB.unmarshal(stream, MembersElement.class));
	}
	
	/**
	 * Returns a {@link DOMSource} derived from the provided DOM {@link Node}.
	 * If the node is a {@link Document}, dig down to its document element.  This
	 * sometimes helps JAXB when called from within Xalan.
	 * 
	 * @param node DOM node to start from.
	 * @return An appropriate {@link DOMSource}.
	 */
	private static DOMSource makeDOMSource(Node node) {
		if (node == null)
			throw new NullPointerException("provided DOM node is null");
		
		if (node instanceof Document) {
			Document d = (Document)node;
			node = d.getDocumentElement();
		}
		return new DOMSource(node);
	}
	
	/**
	 * Constructs a {@link Members} object from an XML document represented as a DOM
	 * {@link Node} object.
	 */
	public Members(Node doc) {
		this(JAXB.unmarshal(makeDOMSource(doc), MembersElement.class));
	}
	
	/**
	 * Constructs a {@link Members} object from an XML document referred to as a
	 * {@link File}.
	 * 
	 * @param file File to be converted into a {@link Members} object.
	 */
	public Members(File file) {
		this(JAXB.unmarshal(file, MembersElement.class));
	}

	/**
	 * Checks for a legitimate entity owner name: either the name of a federation member
	 * or the name of a known non-member entity owner.
	 * 
	 * @param s	Name to check.
	 * @return {@code true} if and only if {@code s} contains a legitimate entity owner name.
	 */
	public boolean isOwnerName(String s) {
		return ownerNames.contains(s);
	}
	
}
