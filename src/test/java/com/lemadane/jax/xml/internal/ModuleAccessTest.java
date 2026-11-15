package com.lemadane.jax.xml.internal;

import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lemadane.jax.Jax;
import com.lemadane.jax.exceptions.JsonMappingException;
import com.lemadane.jax.exceptions.XmlMappingException;
import com.lemadane.jax.json.internal.JsonObjectReader;

import static org.assertj.core.api.Assertions.*;

class ModuleAccessTest {
  @TempDir
  Path directory;

  @Test
  void reportsReflectionDeniedByANonOpenNamedModule() throws Exception {
    Path sources = Files.createDirectories(directory.resolve("sources"));
    Path api = Files.createDirectories(sources.resolve("fixture/api"));
    Path hidden = Files.createDirectories(sources.resolve("fixture/hidden"));
    Path descriptor = sources.resolve("module-info.java");
    Files.writeString(descriptor, "module fixture.closed { exports fixture.api; }");
    Path factory = api.resolve("Factory.java");
    Files.writeString(factory, """
        package fixture.api;
        public class Factory {
          public static Object create() { return new fixture.hidden.Hidden(); }
        }
        """);
    Path bean = hidden.resolve("Hidden.java");
    Files.writeString(bean, """
        package fixture.hidden;
        public class Hidden {
          @com.lemadane.jax.XmlAttribute private String id = "secret";
          public Hidden() {}
          public Hidden(String value) {}
        }
        """);
    Path classes = Files.createDirectories(directory.resolve("classes"));
    assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
        "-parameters", "--add-reads", "fixture.closed=ALL-UNNAMED", "-classpath", System.getProperty("java.class.path"),
        "-d", classes.toString(), descriptor.toString(), factory.toString(), bean.toString())).isZero();
    var configuration = ModuleLayer.boot().configuration().resolve(
        ModuleFinder.of(classes), ModuleFinder.of(), Set.of("fixture.closed"));
    var controller = ModuleLayer.defineModulesWithOneLoader(configuration, java.util.List.of(ModuleLayer.boot()),
        getClass().getClassLoader());
    Module module = controller.layer().findModule("fixture.closed").orElseThrow();
    controller.addReads(module, Jax.class.getModule());
    ClassLoader loader = controller.layer().findLoader("fixture.closed");
    Class<?> type = loader.loadClass("fixture.hidden.Hidden");
    Object instance = loader.loadClass("fixture.api.Factory").getMethod("create").invoke(null);

    assertThatThrownBy(() -> JsonObjectReader.toObject("{\"value\":\"x\"}", type))
        .isInstanceOf(JsonMappingException.class).hasMessageContaining("Cannot access matching constructor");
    assertThatThrownBy(() -> XmlObjectReader.toObject("<Hidden><value>x</value></Hidden>", type))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Cannot access matching constructor");
    assertThatThrownBy(() -> Jax.toXml(instance))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Cannot access annotated XML attribute field");
    var field = type.getDeclaredField("id");
    assertThatThrownBy(() -> XmlObjectReader.setField(field, instance, "changed", type, "/@id"))
        .isInstanceOf(XmlMappingException.class).hasMessageContaining("Cannot access annotated XML attribute field");
  }
}
