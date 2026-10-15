package com.oxygenxml.resources.batch.converter.latex;

import java.io.File;
import java.util.List;

import com.oxygenxml.batch.converter.core.ConversionOptionTags;
import com.oxygenxml.resources.batch.converter.UserInputsProvider;

/**
 * Inputs provider given to the HTML to DITA converter when it converts the HTML produced from a LaTeX
 * document. It answers the HTML options with the values of the matching LaTeX options and delegates
 * everything else.
 *
 * @author sorin_carbunaru
 */
public class LatexInputsProvider implements UserInputsProvider {

  /**
   * The inputs provider of the LaTeX to DITA conversion.
   */
  private final UserInputsProvider delegate;

  /**
   * Constructor.
   *
   * @param delegate The inputs provider of the LaTeX to DITA conversion.
   */
  public LatexInputsProvider(UserInputsProvider delegate) {
    this.delegate = delegate;
  }

  /**
   * @see UserInputsProvider#getInputFiles()
   */
  @Override
  public List<File> getInputFiles() {
    return delegate.getInputFiles();
  }

  /**
   * @see UserInputsProvider#getOutputFolder()
   */
  @Override
  public File getOutputFolder() {
    return delegate.getOutputFolder();
  }

  /**
   * The HTML to DITA converter reads its own option for splitting the document into topics, it gets the
   * value chosen for the LaTeX documents.
   *
   * @see UserInputsProvider#getAdditionalOptionValue(String)
   */
  @Override
  public Boolean getAdditionalOptionValue(String additionalOptionId) {
    if (ConversionOptionTags.CREATE_DITA_MAP_FROM_HTML.equals(additionalOptionId)) {
      return delegate.getAdditionalOptionValue(LatexConversionSupport.CREATE_DITA_MAP_FROM_LATEX);
    }
    return delegate.getAdditionalOptionValue(additionalOptionId);
  }

  /**
   * @see UserInputsProvider#getMaxHeadingLevelForCreatingTopics()
   */
  @Override
  public Integer getMaxHeadingLevelForCreatingTopics() {
    return delegate.getMaxHeadingLevelForCreatingTopics();
  }

  /**
   * @see UserInputsProvider#getFormatForSameTypeReferences()
   */
  @Override
  public String getFormatForSameTypeReferences() {
    return delegate.getFormatForSameTypeReferences();
  }

  /**
   * @see UserInputsProvider#getRootInputDirectoryForFile(File)
   */
  @Override
  public File getRootInputDirectoryForFile(File inputFile) {
    return delegate.getRootInputDirectoryForFile(inputFile);
  }

  /**
   * @see UserInputsProvider#mustOpenConvertedFiles()
   */
  @Override
  public boolean mustOpenConvertedFiles() {
    return delegate.mustOpenConvertedFiles();
  }
}
