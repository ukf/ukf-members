# `ukf-members`

## UK federation Members Database Access

The UK federation maintains a database of federation members and various properties of those members as an XML file.  This project provides classes to ease read-only access to this database for use in the federation's metadata toolchain, which is based on the [Shibboleth metadata aggregator framework](http://shibboleth.net/products/metadata-aggregator.html).

There are examples of the data contained in the test resources in the `src/test/resources` directory.  Note that these do not represent current operational data, and have been filtered to remove any personal information.

The schema for `members.xml` can be found in `src/main/resources` as `ukfederation-members.xsd`.  This schema is used to generate basic parsing classes for the document using [JAXB](http://en.wikipedia.org/wiki/Java_Architecture_for_XML_Binding "Wikipedia: Java Architecture for XML Binding"); the main functionality of this package is then layered on top.

## Release Process

To release a new version of `ukf-members`:

1. Update `RELEASE-NOTES.md`
2. The following bash commands can be used (remembering to appropriately replace the versions):
3. Create a new release in GitHub https://github.com/ukf/ukf-members/releases/new
```
RELEASE_VERSION="2.0.1"
NEXT_SNAPSHOT_VERSION="2.0.2-SNAPSHOT"

# Version release commit and tag
mvn versions:set -DnewVersion="${RELEASE_VERSION}" -DgenerateBackupPoms=false
git add .
git commit -m "Set version for ${RELEASE_VERSION} release"
git tag -s -m "Tag as version ${RELEASE_VERSION}" "${RELEASE_VERSION}"

# Post-release version commit
mvn versions:set -DnewVersion="${NEXT_SNAPSHOT_VERSION}" -DgenerateBackupPoms=false
git add .
git commit -m "Set snapshot version after release"

# Perform build and release on detached HEAD
git checkout "${RELEASE_VERSION}"
mvn -Prelease clean verify

# test results

# Build it again for real
mvn -Prelease,sign clean deploy

# Commit this release to the ages
git checkout main
git push
git push origin "${RELEASE_VERSION}"
```



In order for the `deploy` step to upload the built artefact to `ukf-packages`, ensure your Maven settings (`settings.xml`) contain the following server declaration with the correct credentials for `ukf-packages`. Credentials are based on GitHub personal access tokens with the `write:packages` authorisation scope.

```
<!--
     GitHub ukf/packages.

     UKf packages repo, scope write:packages

     No expiration.
 -->
<server>
       <id>ukf-packages</id>
       <username>username</username>
       <password>access_token</password>
</server>

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
