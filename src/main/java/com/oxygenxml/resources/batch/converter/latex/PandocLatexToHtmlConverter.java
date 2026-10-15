package com.oxygenxml.resources.batch.converter.latex;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a LaTeX document to an HTML5 document by running Pandoc (https://pandoc.org).
 * <p>
 * Pandoc must be installed. Its location is the one configured in the add-on preferences page or, when
 * none is configured, the <code>pandoc</code> command found on the <code>PATH</code>.
 *
 * @author sorin_carbunaru
 */
public class PandocLatexToHtmlConverter {

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(PandocLatexToHtmlConverter.class);

  /**
   * The command used when no Pandoc location is configured.
   */
  public static final String DEFAULT_PANDOC_COMMAND = "pandoc";

  /**
   * How long a single conversion may run, in seconds.
   */
  private static final long TIMEOUT_SECONDS = 300;

  /**
   * The largest part of the Pandoc messages kept in an error message.
   */
  private static final int MAX_LOG_LENGTH = 2000;

  /**
   * The extensions tried for images referenced without one, as LaTeX allows.
   */
  private static final String[] IMAGE_EXTENSIONS = {"png", "jpg", "jpeg", "gif", "svg", "pdf", "eps"};

  /**
   * Matches the "src" attribute of the "img" elements written by Pandoc.
   */
  private static final Pattern IMG_SRC_PATTERN = Pattern.compile("(<img\\b[^>]*?\\ssrc=\")([^\"]+)(\")");

  /**
   * Matches a location that starts with a URL scheme, like "http:" or "data:".
   */
  private static final Pattern URL_SCHEME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]+:.*");

  /**
   * The Pandoc command.
   */
  private final String pandocCommand;

  /**
   * Constructor.
   *
   * @param pandocCommand The Pandoc executable. When <code>null</code> or empty, the
   *                      {@link #DEFAULT_PANDOC_COMMAND} is used.
   */
  public PandocLatexToHtmlConverter(String pandocCommand) {
    this.pandocCommand = pandocCommand == null || pandocCommand.trim().isEmpty()
        ? DEFAULT_PANDOC_COMMAND : pandocCommand.trim();
  }

  /**
   * Converts the given LaTeX document to HTML.
   *
   * @param latexFile The LaTeX document.
   * @param htmlFile  The HTML document to write.
   *
   * @throws IOException If Pandoc cannot be started, fails, or runs for too long.
   */
  public void convert(File latexFile, File htmlFile) throws IOException {
    File workingDir = latexFile.getAbsoluteFile().getParentFile();
    File logFile = File.createTempFile("pandoc", ".log");
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(latexFile, htmlFile, workingDir));
      // The LaTeX "\input" and "\include" commands are resolved against the document folder.
      processBuilder.directory(workingDir);
      processBuilder.redirectErrorStream(true);
      processBuilder.redirectOutput(ProcessBuilder.Redirect.to(logFile));

      Process process;
      try {
        process = processBuilder.start();
      } catch (IOException e) {
        throw new IOException("Pandoc could not be started using \"" + pandocCommand + "\". "
            + "The LaTeX to DITA conversion needs Pandoc (https://pandoc.org). Install it, or set its location "
            + "in the Batch Documents Converter preferences page.", e);
      }

      boolean finished;
      try {
        finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        process.destroyForcibly();
        Thread.currentThread().interrupt();
        throw new IOException("The conversion of " + latexFile.getName() + " was interrupted.", e);
      }
      if (!finished) {
        process.destroyForcibly();
        throw new IOException("Pandoc did not finish converting " + latexFile.getName() + " in "
            + TIMEOUT_SECONDS + " seconds.");
      }

      String pandocMessages = readLog(logFile);
      if (process.exitValue() != 0 || !htmlFile.isFile()) {
        throw new IOException("Pandoc failed to convert " + latexFile.getName() + " (exit code "
            + process.exitValue() + ")" + (pandocMessages.isEmpty() ? "." : ": " + pandocMessages));
      }
      if (!pandocMessages.isEmpty() && LOGGER.isDebugEnabled()) {
        LOGGER.debug("Pandoc messages for {}: {}", latexFile, pandocMessages);
      }

      resolveImageReferences(htmlFile, workingDir);
    } finally {
      deleteQuietly(logFile);
    }
  }

  /**
   * Builds the Pandoc command line.
   *
   * @param latexFile  The LaTeX document.
   * @param htmlFile   The HTML document to write.
   * @param workingDir The folder of the LaTeX document.
   *
   * @return The command and its arguments.
   */
  List<String> buildCommand(File latexFile, File htmlFile, File workingDir) {
    List<String> command = new ArrayList<>();
    command.add(pandocCommand);
    command.add("--from=latex");
    command.add("--to=html5");
    command.add("--standalone");
    command.add("--wrap=none");
    // Only used for the "title" of the HTML head, the document title is kept when it has one.
    command.add("--metadata=pagetitle:" + getBaseName(latexFile));
    command.add("--resource-path=" + workingDir.getAbsolutePath());
    command.add("--output=" + htmlFile.getAbsolutePath());
    command.add(latexFile.getAbsolutePath());
    return command;
  }

  /**
   * Rewrites the relative image references from the HTML as absolute file URLs. Pandoc keeps them
   * relative to the LaTeX document, while the DITA documents are written in another folder.
   *
   * @param htmlFile  The HTML document written by Pandoc.
   * @param latexDir  The folder of the LaTeX document.
   *
   * @throws IOException If the HTML document cannot be read or written.
   */
  void resolveImageReferences(File htmlFile, File latexDir) throws IOException {
    String html = new String(Files.readAllBytes(htmlFile.toPath()), StandardCharsets.UTF_8);
    String updatedHtml = resolveImageReferences(html, latexDir);
    if (!updatedHtml.equals(html)) {
      Files.write(htmlFile.toPath(), updatedHtml.getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * Rewrites the relative image references from the given HTML as absolute file URLs.
   *
   * @param html     The HTML content.
   * @param latexDir The folder of the LaTeX document.
   *
   * @return The HTML content with the image references resolved.
   */
  static String resolveImageReferences(String html, File latexDir) {
    Matcher matcher = IMG_SRC_PATTERN.matcher(html);
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String src = matcher.group(2);
      String newSrc = src;
      if (!src.startsWith("/") && !src.startsWith("#") && !URL_SCHEME_PATTERN.matcher(src).matches()) {
        File image = findImage(new File(latexDir, src.replace("&amp;", "&")));
        if (image != null) {
          newSrc = image.toURI().toString();
        }
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + newSrc + matcher.group(3)));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  /**
   * Finds the given image, trying the usual image extensions when it was referenced without one.
   *
   * @param image The image file, as referenced.
   *
   * @return The image file, or <code>null</code> when it is not found.
   */
  private static File findImage(File image) {
    if (image.isFile()) {
      return image;
    }
    if (image.getName().indexOf('.') == -1) {
      for (String extension : IMAGE_EXTENSIONS) {
        File candidate = new File(image.getParentFile(), image.getName() + "." + extension);
        if (candidate.isFile()) {
          return candidate;
        }
      }
    }
    return null;
  }

  /**
   * Reads the Pandoc messages.
   *
   * @param logFile The file holding the messages.
   *
   * @return The messages, shortened when too long.
   */
  private static String readLog(File logFile) {
    String log = "";
    try {
      log = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8).trim();
    } catch (IOException e) {
      LOGGER.debug(e.getMessage(), e);
    }
    if (log.length() > MAX_LOG_LENGTH) {
      log = log.substring(0, MAX_LOG_LENGTH) + "...";
    }
    return log;
  }

  /**
   * Gets the name of the given file, without extension.
   *
   * @param file The file.
   *
   * @return The base name.
   */
  private static String getBaseName(File file) {
    String name = file.getName();
    int dotIndex = name.lastIndexOf('.');
    return dotIndex > 0 ? name.substring(0, dotIndex) : name;
  }

  /**
   * Deletes the given file, ignoring failures.
   *
   * @param file The file.
   */
  static void deleteQuietly(File file) {
    if (file != null) {
      try {
        Files.deleteIfExists(file.toPath());
      } catch (IOException e) {
        LOGGER.debug(e.getMessage(), e);
      }
    }
  }
}
