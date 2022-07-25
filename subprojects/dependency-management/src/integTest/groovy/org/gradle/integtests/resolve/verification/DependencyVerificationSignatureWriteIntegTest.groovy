/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve.verification

import org.gradle.integtests.fixtures.UnsupportedWithConfigurationCache
import org.gradle.security.fixtures.SigningFixtures
import org.gradle.security.internal.Fingerprint
import org.gradle.security.internal.SecuritySupport
import spock.lang.Issue

import static org.gradle.security.fixtures.SigningFixtures.signAsciiArmored

class DependencyVerificationSignatureWriteIntegTest extends AbstractSignatureVerificationIntegrationTest {

    def "can generate trusted PGP keys configuration"() {
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0") {
            withSignature {
                signAsciiArmored(it)
            }
        }
        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }
        """

        when:
        serveValidKey()
        writeVerificationMetadata()
        succeeds ":help"

        then:
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
      <trusted-keys>
         <trusted-key id="${SigningFixtures.validPublicKeyHexString}" group="org" name="foo" version="1.0"/>
      </trusted-keys>
   </configuration>
   <components/>
</verification-metadata>
"""
    }

    def "can generate ignored PGP keys configuration"() {
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0") {
            withSignature {
                signAsciiArmored(it)
            }
        }
        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }
        """

        when:
        writeVerificationMetadata()
        succeeds ":help"

        then:
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
      <ignored-keys>
         <ignored-key id="14f53f0824875d73" reason="Key couldn't be downloaded from any key server"/>
      </ignored-keys>
   </configuration>
   <components>
      <component group="org" name="foo" version="1.0">
         <artifact name="foo-1.0.jar">
            <sha256 value="20ae575ede776e5e06ee6b168652d11ee23069e92de110fdec13fbeaa5cf3bbc" origin="Generated by Gradle because a key couldn't be downloaded"/>
         </artifact>
         <artifact name="foo-1.0.pom">
            <sha256 value="f331cce36f6ce9ea387a2c8719fabaf67dc5a5862227ebaa13368ff84eb69481" origin="Generated by Gradle because a key couldn't be downloaded"/>
         </artifact>
      </component>
   </components>
</verification-metadata>
"""
        and:
        output.contains("""A verification file was generated but some problems were discovered:
   - some keys couldn't be downloaded. They were automatically added as ignored keys but you should review if this is acceptable. Look for entries with the following comment: Key couldn't be downloaded from any key server
""")
    }

    def "warns if a signature failed"() {
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        def module = uncheckedModule("org", "foo", "1.0") {
            withSignature {
                signAsciiArmored(it)
            }
        }
        module.artifactFile.bytes = "corrupted".getBytes("utf-8")

        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }

        """

        when:
        serveValidKey()
        writeVerificationMetadata()
        succeeds ":help"

        then:
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
   </configuration>
   <components>
      <component group="org" name="foo" version="1.0">
         <artifact name="foo-1.0.jar">
            <ignored-keys>
               <ignored-key id="${SigningFixtures.validPublicKeyHexString}" reason="PGP verification failed"/>
            </ignored-keys>
            <sha256 value="3dbb3963d11aa418de8b61f846c3dbd5af43b40d252842adb823f90936fe6920" origin="Generated by Gradle because PGP signature verification failed!"/>
         </artifact>
         <artifact name="foo-1.0.pom">
            <pgp value="${SigningFixtures.validPublicKeyHexString}"/>
         </artifact>
      </component>
   </components>
</verification-metadata>
"""
        and:
        output.contains """A verification file was generated but some problems were discovered:
   - some signature verification failed. Checksums were generated for those artifacts but you MUST check if there's an actual problem. Look for entries with the following comment: PGP verification failed
"""
    }

    // plugins do not publish signatures so we expect the checksums to be present
    def "writes checksums of plugins using plugins block"() {
        given:
        addPlugin()
        settingsFile.text = """
        pluginManagement {
            repositories {
                maven {
                    url '$pluginRepo.uri'
                }
            }
        }
        """ + settingsFile.text
        buildFile << """
          plugins {
             id 'test-plugin' version '1.0'
          }
        """

        when:
        writeVerificationMetadata()
        succeeds ':help'

        then:
        assertMetadataExists()
        hasModules(["test-plugin:test-plugin.gradle.plugin", "com:myplugin"])

    }

    // plugins do not publish signatures so we expect the checksums to be present
    def "writes checksums of plugins using buildscript block"() {
        given:
        addPlugin()
        buildFile << """
          buildscript {
             repositories {
                maven { url "${pluginRepo.uri}" }
             }
             dependencies {
                classpath 'com:myplugin:1.0'
             }
          }
        """

        when:
        writeVerificationMetadata()
        succeeds ':help'

        then:
        assertMetadataExists()
        hasModules(["com:myplugin"])
    }

    def "if signature file is missing, generates a checksum"() {
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0")
        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }
        """

        when:
        writeVerificationMetadata()
        succeeds ":help"

        then:
        hasModules(["org:foo"])
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
   </configuration>
   <components>
      <component group="org" name="foo" version="1.0">
         <artifact name="foo-1.0.jar">
            <sha256 value="20ae575ede776e5e06ee6b168652d11ee23069e92de110fdec13fbeaa5cf3bbc" origin="Generated by Gradle because artifact wasn't signed"/>
         </artifact>
         <artifact name="foo-1.0.pom">
            <sha256 value="f331cce36f6ce9ea387a2c8719fabaf67dc5a5862227ebaa13368ff84eb69481" origin="Generated by Gradle because artifact wasn't signed"/>
         </artifact>
      </component>
   </components>
</verification-metadata>
"""
    }

    def "can export PGP keys"() {
        def keyring = newKeyRing()
        def pkId = Fingerprint.of(keyring.publicKey)
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0") {
            withSignature {
                keyring.sign(it, [(SigningFixtures.validSecretKey): SigningFixtures.validPassword])
            }
        }
        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }
        """

        when:
        serveValidKey()
        keyServerFixture.registerPublicKey(keyring.publicKey)
        writeVerificationMetadata()
        succeeds ":help", "--export-keys"

        then:
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
      <trusted-keys>
         <trusted-key id="${SigningFixtures.validPublicKeyHexString}" group="org" name="foo" version="1.0"/>
         <trusted-key id="$pkId" group="org" name="foo" version="1.0"/>
      </trusted-keys>
   </configuration>
   <components/>
</verification-metadata>
"""
        and:
        def exportedKeyRing = file("gradle/verification-keyring.gpg")
        exportedKeyRing.exists()
        def keyrings = SecuritySupport.loadKeyRingFile(exportedKeyRing)
        keyrings.size() == 2
        keyrings.find { it.publicKey.keyID == SigningFixtures.validPublicKey.keyID }
        keyrings.find { it.publicKey.keyID == keyring.publicKey.keyID }

        and: "also generates an ascii armored keyring file"
        def exportedKeyRingAscii = file("gradle/verification-keyring.keys")
        exportedKeyRingAscii.exists()
        def keyringsAscii = SecuritySupport.loadKeyRingFile(exportedKeyRingAscii)
        keyringsAscii.size() == 2
        keyringsAscii.find { it.publicKey.keyID == SigningFixtures.validPublicKey.keyID }
        keyringsAscii.find { it.publicKey.keyID == keyring.publicKey.keyID }
    }

    @UnsupportedWithConfigurationCache
    def "can generate configuration for dependencies resolved in a buildFinished hook"() {
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0") {
            withSignature {
                signAsciiArmored(it)
            }
        }
        buildFile << """
            println "Adding hook"
            gradle.buildFinished {
               println "Executing hook"
               allprojects {
                   println configurations.detachedConfiguration(dependencies.create("org:foo:1.0")).files
               }
            }
        """

        when:
        serveValidKey()
        writeVerificationMetadata()
        succeeds ":help"

        then:
        assertXmlContents """<?xml version="1.0" encoding="UTF-8"?>
<verification-metadata>
   <configuration>
      <verify-metadata>true</verify-metadata>
      <verify-signatures>true</verify-signatures>
      <key-servers>
         <key-server uri="${keyServerFixture.uri}"/>
      </key-servers>
      <trusted-keys>
         <trusted-key id="${SigningFixtures.validPublicKeyHexString}" group="org" name="foo" version="1.0"/>
      </trusted-keys>
   </configuration>
   <components/>
</verification-metadata>
"""
    }

    @Issue("https://github.com/gradle/gradle/issues/18394")
    def "doesn't fail exporting keys if any has invalid utf-8 char in user id"() {
        String publicKeyResource = "/org/gradle/integtests/resolve/verification/DependencyVerificationSignatureWriteIntegTest/invalid-utf8-public-key.asc"
        String secretKeyResource = "/org/gradle/integtests/resolve/verification/DependencyVerificationSignatureWriteIntegTest/invalid-utf8-secret-key.asc"
        def keyring = newKeyRingFromResource(publicKeyResource, secretKeyResource)
        keyServerFixture.registerPublicKey(keyring.getPublicKey())
        createMetadataFile {
            keyServer(keyServerFixture.uri)
        }

        given:
        javaLibrary()
        uncheckedModule("org", "foo", "1.0") {
            withSignature {
                keyring.sign(it)
            }
        }
        buildFile << """
            dependencies {
                implementation "org:foo:1.0"
            }
        """

        when:
        writeVerificationMetadata()
        succeeds ":help", "--export-keys"

        then:
        outputContains("Exported 1 keys to")
    }

    @Issue("https://github.com/gradle/gradle/issues/20140")
    def "export deduplicated PGP keys"() {
        given:
        testDirectory.file("gradle").mkdir()
        testDirectory.file("gradle/verification-keyring.gpg").newOutputStream().withCloseable {
            for (int i in 1..10) {
                SigningFixtures.validPublicKey.encode(it, true)
            }
        }

        def exportedKeyRing = file("gradle/verification-keyring.gpg")
        def exportedKeyRingAscii = file("gradle/verification-keyring.keys")
        // Check if pre-conditions are alright
        def keyrings = SecuritySupport.loadKeyRingFile(exportedKeyRing)
        assert keyrings.size() == 10

        when:
        // Export the keys...
        writeVerificationMetadata()
        succeeds ":help", "--export-keys"
        keyrings = SecuritySupport.loadKeyRingFile(exportedKeyRing)

        then:
        // Only one key should exists, as keys are deduplicated
        keyrings.size() == 1
        // The expected public key should be the only entry
        keyrings.find { it.publicKey.keyID == SigningFixtures.validPublicKey.keyID }

        // Check the same as above
        def keyringsAscii = SecuritySupport.loadKeyRingFile(exportedKeyRingAscii)
        keyringsAscii.size() == 1
        keyringsAscii.find { it.publicKey.keyID == SigningFixtures.validPublicKey.keyID }
    }
}
