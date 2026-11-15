package com.lemadane.jax.cli;

import java.io.PrintStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.util.Map;
import java.util.Locale;

import com.lemadane.jax.exceptions.DataMappingException;

/**
* Command-line source generator for JSON and XML samples.
*/
public final class JaxCli {

  JaxCli() {
  }

  public static void main(String[] args) {
    int exitCode = run(args, System.out, System.err);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }

  static int run(String[] args, PrintStream out, PrintStream err) {
    if (args.length != 3 || !"--create".equals(args[0])) {
      err.println("Usage: java -jar jax.jar --create <record|pojo|with-constructor> <input.json|input.xml>");
      return 2;
    }

    JavaSourceGenerator.Style style;
    try {
      style = JavaSourceGenerator.Style.fromArgument(args[1]);
    } catch (IllegalArgumentException e) {
      err.println(e.getMessage());
      return 2;
    }

    try {
      Path input = Path.of(args[2]).toAbsolutePath().normalize();
      String fileName = input.getFileName().toString();
      String extension = extension(fileName);
      if (!extension.equals("json") && !extension.equals("xml")) {
        throw new IllegalArgumentException("Input file must have a .json or .xml extension: " + input);
      }
      String className = className(fileName.substring(0, fileName.lastIndexOf('.')));
      String content = Files.readString(input, StandardCharsets.UTF_8);
      Map<String, String> sources = JavaSourceGenerator.generate(extension.equals("xml") ? null : className, content, extension.equals("xml"), style);
      // Check every destination before creating any file, including referenced types.
      for (String file : sources.keySet()) {
        Path output = input.resolveSibling(file);
        if (Files.exists(output, LinkOption.NOFOLLOW_LINKS)) {
          throw new FileAlreadyExistsException(output.toString());
        }
      }
      for (Map.Entry<String, String> source : sources.entrySet()) {
        Path output = input.resolveSibling(source.getKey());
        Files.writeString(output, source.getValue(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        out.println("Created " + output);
      }
      return 0;
    } catch (IOException | DataMappingException | IllegalArgumentException e) {
      err.println("jax: " + e.getMessage());
      return 1;
    }
  }

  static String extension(String fileName) {
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return "";
    }
    return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  static String className(String name) {
    String encoded = name.isEmpty() ? "Generated" : name;
    StringBuilder result = new StringBuilder();
    boolean capitalize = true;
    for (int i = 0; i < encoded.length(); i++) {
      char ch = encoded.charAt(i);
      if (Character.isJavaIdentifierPart(ch)) {
        if (result.isEmpty() && !Character.isJavaIdentifierStart(ch)) {
          result.append('_');
        }
        result.append(capitalize ? Character.toUpperCase(ch) : ch);
        capitalize = false;
      } else {
        capitalize = true;
      }
    }
    if (result.isEmpty()) {
      return "Generated";
    }
    return result.toString();
  }
}
