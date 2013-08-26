# `ukf-members`

## UK federation Members Database Access

The UK federation maintains a database of federation members and various properties of those members as an XML file.  This project provides classes to ease read-only access to this database for use in the federation's metadata toolchain, which is based on the [Shibboleth metadata aggregator framework](http://shibboleth.net/products/metadata-aggregator.html).

There are examples of the data contained in `members.xml` in the `src/test/resources` directory.  Note that these do not represent current operational data, and have been filtered to remove any personal information.

The schema for `members.xml` can be found in `src/main/resources` as `ukfederation-members.xsd`.  This schema is used to generate basic parsing classes for the document using [JAXB](http://en.wikipedia.org/wiki/Java_Architecture_for_XML_Binding "Wikipedia: Java Architecture for XML Binding"); the main functionality of this package is then layered on top.

## Copyright and License

The entire package is Copyright (C) 2013, University of Edinburgh.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
