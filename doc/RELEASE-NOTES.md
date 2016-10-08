# Release Notes for `ukf-members`

## Version 1.4.0 ##

* Schema V1.7 updates:
	* Add mandatory `orgID` attribute to `Grant` and `GrantAll`.
 
## Version 1.3.0 ##

* Schema updates:
    * Participant ID is now required.
    * Remove NonMember element.
* Schema-validate members document.
* Reduce to just one `Members` constructor taking `Document`.
* Allow lookup of members or participants by name.
* Detect duplicate participant names.

## Version 1.2.0 ##

* Move to Shibboleth V3 parent POM and Java 7.
* Added a `doc` folder, a `README.md` and `LICENSE.txt`.
* Added `<DomainOwner>` and `<Grant>` system to schema.
* Take advantage of Java 7 type inference.
* Remove support for use as a Xalan XSLT extension.
* Remove dependency on endorsed Xalan and Xerces.
* Add access to the JAXB tree from the main bean.
* Change pushed scopes API from DOM to a list of Strings.

