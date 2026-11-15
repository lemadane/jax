package com.lemadane.jax.xml.internal;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import com.lemadane.jax.exceptions.XmlMappingException;

import static org.assertj.core.api.Assertions.*;

class XmlParserSecurityTest {
  @TempDir
  Path directory;

  @Test
  void rejectsExternalFileEntitiesWithoutExposingContent() throws Exception {
    Path secret = directory.resolve("secret.txt");
    Files.writeString(secret, "private-test-content");
    String xml = "<!DOCTYPE root [<!ENTITY external SYSTEM '" + secret.toUri() + "'>]><root>&external;</root>";
    assertThatThrownBy(() -> XmlRecordReader.parseXmlDocument(xml))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("DOCTYPE")
        .hasMessageNotContaining("private-test-content");
  }

  @Test
  @ResourceLock(Resources.SYSTEM_PROPERTIES)
  void enforcesLibraryDepthLimitWhenJdkLimitIsDisabled() {
    String key = "jdk.xml.maxElementDepth";
    String previous = System.getProperty(key);
    try {
      System.setProperty(key, "0");
      assertThat(XmlRecordReader.parseXmlDocument("<n>".repeat(256) + "</n>".repeat(256)).name()).isEqualTo("n");
      assertThatThrownBy(() -> XmlRecordReader.parseXmlDocument("<n>".repeat(257) + "</n>".repeat(257)))
          .isInstanceOf(XmlMappingException.class).hasMessageContaining("Maximum XML nesting depth of 256");
    } finally {
      if (previous == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, previous);
      }
    }
  }

  @Test
  void reportsClosedModuleConstructorAccess() {
    assertThatThrownBy(() -> XmlObjectReader.createInstance(Void.class, "/root"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("No accessible no-argument constructor");
  }
}
