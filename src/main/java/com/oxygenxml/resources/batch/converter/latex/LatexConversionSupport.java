package com.oxygenxml.resources.batch.converter.latex;

import java.util.Locale;

import com.oxygenxml.batch.converter.core.ConversionFormatUtil;
import com.oxygenxml.batch.converter.core.ConverterTypes;
import com.oxygenxml.batch.converter.core.extensions.ExtensionGetter;

/**
 * The LaTeX to DITA conversion is provided by the add-on, not by the core conversion library.
 * <p>
 * The LaTeX documents are first converted to HTML with Pandoc (https://pandoc.org), then the HTML is
 * converted to DITA by the HTML to DITA converter of the core library. This class holds the converter
 * type, the formats and the extensions of this conversion and falls back to the core library for
 * every other conversion, so that the callers can use it in place of the core helpers.
 *
 * @author sorin_carbunaru
 */
public final class LatexConversionSupport {

  /**
   * The LaTeX to DITA converter type. It follows the naming of the core converter types, as it is
   * also used to build the translation keys, like <code>Menu_Item_Text_Conv_latex.to.dita</code>.
   */
  public static final String LATEX_TO_DITA = "_latex.to.dita";

  /**
   * The converter type used to convert the HTML produced by Pandoc.
   */
  public static final String DELEGATE_CONVERTER_TYPE = ConverterTypes.HTML_TO_DITA;

  /**
   * The name of the LaTeX input format, as given to the command line script and to the AI tool.
   */
  public static final String LATEX_INPUT_FORMAT = "latex";

  /**
   * Another accepted name for the LaTeX input format.
   */
  private static final String TEX_INPUT_FORMAT = "tex";

  /**
   * The name of the DITA output format.
   */
  private static final String DITA_OUTPUT_FORMAT = "dita";

  /**
   * The extensions of the LaTeX documents.
   */
  private static final String[] LATEX_EXTENSIONS = new String[] {"tex", "latex", "ltx"};

  /**
   * Option that splits the LaTeX documents into topics referenced from a DITA map. It is mapped to the
   * matching option of the HTML to DITA converter.
   */
  public static final String CREATE_DITA_MAP_FROM_LATEX = "create.dita.map.from.latex";

  /**
   * Private constructor.
   */
  private LatexConversionSupport() {
    throw new UnsupportedOperationException("Instantiation of this utility class is not allowed!");
  }

  /**
   * Check if the given converter type is the LaTeX to DITA one.
   *
   * @param converterType The converter type.
   *
   * @return <code>true</code> for the LaTeX to DITA converter type.
   */
  public static boolean isLatexToDita(String converterType) {
    return LATEX_TO_DITA.equals(converterType);
  }

  /**
   * Get the converter type for the given formats.
   *
   * @param inputFormat  The input format.
   * @param outputFormat The output format.
   *
   * @return The converter type or <code>null</code> if the conversion is not supported.
   */
  public static String getConverterType(String inputFormat, String outputFormat) {
    if (isLatexFormat(inputFormat) && outputFormat != null
        && DITA_OUTPUT_FORMAT.equals(outputFormat.trim().toLowerCase(Locale.ROOT))) {
      return LATEX_TO_DITA;
    }
    return ConversionFormatUtil.getConverterType(inputFormat, outputFormat);
  }

  /**
   * Check if the given conversion formats are supported.
   *
   * @param inputFormat  The input format.
   * @param outputFormat The output format.
   *
   * @return <code>true</code> if the conversion is supported.
   */
  public static boolean isSupportedConversionFormat(String inputFormat, String outputFormat) {
    return getConverterType(inputFormat, outputFormat) != null;
  }

  /**
   * Get the input extensions of the given converter type.
   *
   * @param converterType The converter type.
   *
   * @return The extensions, or <code>null</code> if none is declared for the converter type.
   */
  public static String[] getInputExtension(String converterType) {
    if (isLatexToDita(converterType)) {
      return LATEX_EXTENSIONS.clone();
    }
    return ExtensionGetter.getInputExtension(converterType);
  }

  /**
   * Get the input extensions of the given conversion formats.
   *
   * @param inputFormat  The input format.
   * @param outputFormat The output format.
   *
   * @return The extensions, or <code>null</code> if none is declared for the conversion formats.
   */
  public static String[] getInputExtension(String inputFormat, String outputFormat) {
    String converterType = getConverterType(inputFormat, outputFormat);
    if (isLatexToDita(converterType)) {
      return LATEX_EXTENSIONS.clone();
    }
    return ExtensionGetter.getInputExtension(inputFormat, outputFormat);
  }

  /**
   * Get the converter type that does the actual conversion work for the given converter type.
   *
   * @param converterType The converter type.
   *
   * @return The HTML to DITA converter type for the LaTeX to DITA conversion, the given type otherwise.
   */
  public static String getEngineConverterType(String converterType) {
    return isLatexToDita(converterType) ? DELEGATE_CONVERTER_TYPE : converterType;
  }

  /**
   * Check if the given input format names LaTeX.
   *
   * @param inputFormat The input format.
   *
   * @return <code>true</code> for "latex" or "tex".
   */
  private static boolean isLatexFormat(String inputFormat) {
    if (inputFormat == null) {
      return false;
    }
    String format = inputFormat.trim().toLowerCase(Locale.ROOT);
    return LATEX_INPUT_FORMAT.equals(format) || TEX_INPUT_FORMAT.equals(format);
  }
}
