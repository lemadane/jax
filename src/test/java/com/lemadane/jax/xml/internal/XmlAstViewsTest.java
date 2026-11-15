package com.lemadane.jax.xml.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class XmlAstViewsTest {
  @Test
  void retainedViewsStayLiveAndReadOnlyAsTheTreeGrows() {
    var root = new XmlElement("root");
    var attributes = root.attributes();
    var children = root.children();
    var content = root.content();
    var child = new XmlElement("child");
    root.addAttribute("id", "7");
    root.addText("before");
    root.addChild(child);
    assertThat(attributes).containsEntry("id", "7");
    assertThat(children).containsExactly(child);
    assertThat(content).containsExactly("before", child);
    assertThatThrownBy(() -> attributes.put("other", "value")).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> children.add(child)).isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> content.add("after")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void textTraversalPreservesMixedContentAndReflectsLaterChildEdits() {
    var root = new XmlElement("root");
    assertThat(root.textContent()).isEmpty();
    root.addText("before");
    assertThat(root.textContent()).isEqualTo("before");
    var child = new XmlElement("child");
    root.addChild(child);
    root.addText("after");
    assertThat(root.textContent()).isEqualTo("beforeafter");
    child.addText("inside");
    var grandchild = new XmlElement("grandchild");
    grandchild.addText("deep");
    child.addChild(grandchild);
    child.addText("end");
    assertThat(root.textContent()).isEqualTo("beforeinsidedeependafter");
    assertThat(root.children()).containsExactly(child);
  }
}
