package com.oxygenxml.resources.batch.converter.latex;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import com.oxygenxml.batch.converter.core.ConversionOptionTags;
import com.oxygenxml.batch.converter.core.ConverterTypes;
import com.oxygenxml.resources.batch.converter.UserInputsProvider;

/**
 * Tests for the LaTeX to DITA conversion support. Pandoc is not needed by these tests.
 *
 * @author sorin_carbunaru
 */
public class LatexConversionSupportTest {

  /**
   * <p><b>Description:</b> The LaTeX formats resolve to the LaTeX to DITA converter type.</p>
   */
  @Test
  public void testConverterType() {
    assertEquals(LatexConversionSupport.LATEX_TO_DITA, LatexConversionSupport.getConverterType("latex", "dita"));
    assertEquals(LatexConversionSupport.LATEX_TO_DITA, LatexConversionSupport.getConverterType(" TeX ", "DITA"));
    assertTrue(LatexConversionSupport.isSupportedConversionFormat("latex", "dita"));
    assertFalse(LatexConversionSupport.isSupportedConversionFormat("latex", "xhtml"));
    // The other conversions are still resolved by the core library.
    assertEquals(ConverterTypes.MD_TO_DITA, LatexConversionSupport.getConverterType("markdown", "dita"));
  }

  /**
   * <p><b>Description:</b> The LaTeX extensions are used and the HTML to DITA converter does the work.</p>
   */
  @Test
  public void testExtensionsAndEngine() {
    assertArrayEquals(new String[] {"tex", "latex", "ltx"},
        LatexConversionSupport.getInputExtension(LatexConversionSupport.LATEX_TO_DITA));
    assertArrayEquals(new String[] {"tex", "latex", "ltx"},
        LatexConversionSupport.getInputExtension("latex", "dita"));
    assertEquals(ConverterTypes.HTML_TO_DITA,
        LatexConversionSupport.getEngineConverterType(LatexConversionSupport.LATEX_TO_DITA));
    assertEquals(ConverterTypes.MD_TO_DITA, LatexConversionSupport.getEngineConverterType(ConverterTypes.MD_TO_DITA));
  }

  /**
   * <p><b>Description:</b> The relative image references are resolved against the LaTeX folder, also when
   * they have no extension. The other references are kept.</p>
   */
  @Test
  public void testResolveImageReferences() throws Exception {
    File latexDir = Files.createTempDirectory("latexTest").toFile();
    File figures = new File(latexDir, "figures");
    figures.mkdirs();
    File image = new File(figures, "diagram.png");
    Files.write(image.toPath(), new byte[] {0});
    try {
      String html = "<p><img src=\"figures/diagram\" alt=\"a\" /></p>"
          + "<p><img src=\"figures/diagram.png\" /></p>"
          + "<p><img src=\"http://example.com/x.png\" /></p>"
          + "<p><img src=\"missing.png\" /></p>";
      String expectedUri = image.toURI().toString();
      String expected = "<p><img src=\"" + expectedUri + "\" alt=\"a\" /></p>"
          + "<p><img src=\"" + expectedUri + "\" /></p>"
          + "<p><img src=\"http://example.com/x.png\" /></p>"
          + "<p><img src=\"missing.png\" /></p>";
      assertEquals(expected, PandocLatexToHtmlConverter.resolveImageReferences(html, latexDir));
    } finally {
      Files.deleteIfExists(image.toPath());
      Files.deleteIfExists(figures.toPath());
      Files.deleteIfExists(latexDir.toPath());
    }
  }

  /**
   * <p><b>Description:</b> The Pandoc command line converts LaTeX to a standalone HTML5 document.</p>
   */
  @Test
  public void testPandocCommand() {
    File latexFile = new File("doc.tex").getAbsoluteFile();
    File htmlFile = new File("out.html").getAbsoluteFile();
    List<String> command = new PandocLatexToHtmlConverter(" ").buildCommand(latexFile, htmlFile,
        latexFile.getParentFile());
    assertEquals(PandocLatexToHtmlConverter.DEFAULT_PANDOC_COMMAND, command.get(0));
    assertTrue(command.contains("--from=latex"));
    assertTrue(command.contains("--to=html5"));
    assertTrue(command.contains("--standalone"));
    assertTrue(command.contains("--metadata=pagetitle:doc"));
    assertTrue(command.contains("--output=" + htmlFile.getAbsolutePath()));
    assertEquals(latexFile.getAbsolutePath(), command.get(command.size() - 1));

    assertEquals("C:/tools/pandoc.exe",
        new PandocLatexToHtmlConverter("C:/tools/pandoc.exe").buildCommand(latexFile, htmlFile,
            latexFile.getParentFile()).get(0));
  }

  /**
   * <p><b>Description:</b> The HTML to DITA converter gets the value of the LaTeX option for splitting
   * the documents into topics.</p>
   */
  @Test
  public void testInputsProviderMapsTheSplitOption() {
    UserInputsProvider delegate = new UserInputsProvider() {
      @Override
      public boolean mustOpenConvertedFiles() {
        return false;
      }

      @Override
      public List<File> getInputFiles() {
        return null;
      }

      @Override
      public File getOutputFolder() {
        return null;
      }

      @Override
      public Boolean getAdditionalOptionValue(String additionalOptionId) {
        if (LatexConversionSupport.CREATE_DITA_MAP_FROM_LATEX.equals(additionalOptionId)) {
          return Boolean.FALSE;
        }
        return Boolean.TRUE;
      }

      @Override
      public File getRootInputDirectoryForFile(File inputFile) {
        return null;
      }
    };
    LatexInputsProvider latexInputsProvider = new LatexInputsProvider(delegate);
    assertEquals(Boolean.FALSE,
        latexInputsProvider.getAdditionalOptionValue(ConversionOptionTags.CREATE_DITA_MAP_FROM_HTML));
    assertEquals(Boolean.TRUE,
        latexInputsProvider.getAdditionalOptionValue(ConversionOptionTags.IGNORE_DIV_ELEMENTS_FROM_HTML));
  }
}
