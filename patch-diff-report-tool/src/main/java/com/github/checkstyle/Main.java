////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2016 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.xml.stream.XMLStreamException;

import com.github.checkstyle.data.ParsedContent;
import com.github.checkstyle.data.StatisticsHolder;
import com.github.checkstyle.parser.StaxParserProcessor;
import com.github.checkstyle.site.SiteGenerator;
import com.github.checkstyle.site.XrefGenerator;

/**
 * Utility class, contains main function and
 * its auxiliary routines.
 *
 * @author atta_troll
 *
 */
public final class Main {

    /**
     * Link to the help html.
     */
    public static final String HELP_HTML_PATH = "help.html";

    /**
     * Name for the site file.
     */
    public static final Path SITEPATH = Paths.get("site.html");

    /**
     * Message for wrong number of arguments.
     */
    public static final String MSG_WRONG_NUMBER_OF_ARGS =
            "Not enough command line args, "
            + "need at least 2 and no more than 4.";

    /**
     * Message for bad input path.
     */
    public static final String MSG_BAD_PATH =
            "Failed to resolve input paths.";

    /**
     * Message for wrong file existence where it shouldn't be.
     */
    public static final String MSG_EXISTS =
            "Unknown regular file exists with this name: ";

    /**
     * Message when necessary file is absent.
     */
    public static final String MSG_NOT_EXISTS =
            "XML file doesn't exist: ";

    /**
     * Message if both input XML files are the same.
     */
    public static final String MSG_IDENTICAL_INPUT =
            "Both input XML files have the same path.";

    /**
     * Message on failure of the first stage of execution.
     */
    public static final String MSG_PREPARATION_FAILURE =
            "Failed to pass initial integrity checks.";

    /**
     * Message on failure of the second stage of execution.
     */
    public static final String MSG_PARSE_FAILURE =
            "Failed to create parse XML files.";

    /**
     * Message on failure of the third stage of execution.
     */
    public static final String MSG_SITE_FAILURE =
            "Failed to create html site";

    /**
     * Message after successful first stage.
     */
    public static final String MSG_PREPARATION_SUCCESS =
            "Successfull preparation stage.";

    /**
     * Message after successful second stage.
     */
    public static final String MSG_PARSE_SUCCESS =
            "XML files successfully parsed.";

    /**
     * Message after successful third stage.
     */
    public static final String MSG_SITE_SUCCESS =
            "Creation of an html site succeed.";

    /**
     * Help message.
     */
    public static final String MSG_HELP =
            "This program creates symmetric difference "
            + "from two checkstyle-result.xml reports\n"
            + "generated for checkstyle build.\n"
            + "It has from 2 to 4 command line arguments:\n"
            + "First two are the links to directories, each containing "
            + "checkstyle-result.xml, these are obligatory arguments,\n"
            + "Third argument is a link to the folder with the code "
            + "under testing, it is fucultative, it will be used to "
            + "relativize xdoc file structure, but it can be skipped and "
            + "absolute paths will be used.\n"
            + "Forth argument is a destination folder for the result site, "
            + "it is facultative, if skipped, you will find the result site "
            + "in your home directory, if this folder already exists, "
            + "ITS CONTENT WILL BE PURGED.\n";

    /**
     * Minimal number of the cli arguments.
     */
    public static final int MIN_ARGS_NUMBER = 2;

    /**
     * Maximal number of the cli arguments.
     */
    public static final int MAX_ARGS_NUMBER = 4;

    /**
     * Minimal number of arguments when first obligatory argument is present.
     */
    public static final int ARGS_NUMBER_FIRST_OBLIGATORY = 0;

    /**
     * Minimal number of arguments when second obligatory argument is present.
     */
    public static final int ARGS_NUMBER_SECOND_OBLIGATORY = 1;

    /**
     * Minimal number of arguments when first facultative argument is present.
     */
    public static final int ARGS_NUMBER_FIRST_FACULTATIVE = 2;

    /**
     * Minimal number of arguments when second facultative argument is present.
     */
    public static final int ARGS_NUMBER_SECOND_FACULTATIVE = 3;

    /**
     * Number of "file" xml tags parsed at one iteration of parser.
     */
    public static final int XML_PARSE_PORTION_SIZE = 50;

    /**
     * Name for standart checkstyle xml report.
     */
    private static final Path XML_FILEPATH =
            Paths.get("checkstyle-result.xml");

    /**
     * Name for the CSS files folder.
     */
    private static final Path CSS_FILEPATH = Paths.get("css");

    /**
     * Name for the CSS files folder.
     */
    private static final Path XREF_FILEPATH = Paths.get("xref");

    /**
     * Utility class ctor.
     */
    private Main() {

    }

    /**
     * Parses CLI arguments,
     * then passes control to executeStages.
     *
     * @param args
     *        cli arguments.
     */
    public static void main(final String... args) {
        if (args.length >= MIN_ARGS_NUMBER && args.length <= MAX_ARGS_NUMBER) {
            try {
                final Path pathDir1 = Paths
                        .get(args[ARGS_NUMBER_FIRST_OBLIGATORY]);
                final Path pathDir2 = Paths
                        .get(args[ARGS_NUMBER_SECOND_OBLIGATORY]);
                final Path pathTestData;
                if (args.length > ARGS_NUMBER_FIRST_FACULTATIVE) {
                    pathTestData = Paths
                            .get(args[ARGS_NUMBER_FIRST_FACULTATIVE]);
                }
                else {
                    pathTestData = null;
                }
                final Path pathResult;
                if (args.length > ARGS_NUMBER_SECOND_FACULTATIVE) {
                    pathResult = Paths
                            .get(args[ARGS_NUMBER_SECOND_FACULTATIVE]);
                }
                else {
                    pathResult = Paths.get(System.getProperty("user.home"))
                            .resolve("XMLDiffGen_report_"
                                    + new SimpleDateFormat(
                                    "yyyy.MM.dd_HH:mm:ss")
                                    .format(Calendar.getInstance().getTime()));
                }
                final Path pathXml1 = pathDir1.resolve(XML_FILEPATH);
                final Path pathXml2 = pathDir2.resolve(XML_FILEPATH);
                executeStages(pathResult, pathXml1, pathXml2, pathTestData);
            }
            catch (InvalidPathException exception) {
                exception.printStackTrace();
                System.out.print(MSG_HELP);
                System.out.println(MSG_BAD_PATH);
            }
        }
        else {
            System.out.print(MSG_HELP);
            System.out.println(MSG_WRONG_NUMBER_OF_ARGS);
        }
    }

    /**
     * Executes all three stages of this utility process.
     *
     * @param resultPath
     *        path to folder with result site.
     * @param pathXml1
     *        path to first checkstyle-result.xml.
     * @param pathXml2
     *        path to second checkstyle-result.xml.
     * @param pathTestData
     *        path to checkstyle subject data.
     */
    private static void executeStages(Path resultPath, Path pathXml1,
            Path pathXml2, Path pathTestData) {
        //stage 1
        try {
            preparationStage(resultPath, pathXml1, pathXml2, pathTestData);
            System.out.println(MSG_PREPARATION_SUCCESS);
            try {
                //stage 2
                final ParsedContent content = new ParsedContent();
                final StatisticsHolder holder = new StatisticsHolder();
                StaxParserProcessor.parse(content, pathXml1,
                        pathXml2, XML_PARSE_PORTION_SIZE, holder);
                System.out.println(MSG_PARSE_SUCCESS);
                //stage 3
                final XrefGenerator generator =
                        new XrefGenerator(pathTestData, resultPath
                                .resolve(XREF_FILEPATH), resultPath);
                content.getStatistics(holder);
                if (SiteGenerator.writeParsedContentToHtml(content,
                            resultPath.resolve(SITEPATH), holder,
                            generator)
                        && SiteGenerator.writeHtmlHelp(
                                resultPath
                                .resolve(HELP_HTML_PATH))) {
                    System.out.println(MSG_SITE_SUCCESS);
                }
                else {
                    System.out.println(MSG_SITE_FAILURE);
                }
            }
            catch (IOException | XMLStreamException exception) {
                exception.printStackTrace();
                System.out.println(MSG_PARSE_FAILURE);
            }
        }
        catch (IllegalArgumentException | IOException anotherException) {
            anotherException.printStackTrace();
            System.out.print(MSG_HELP);
            System.out.println(MSG_PREPARATION_FAILURE);
        }

    }

    /**
     * Perform preliminary file existence checks,
     * also exports to disc necessary static resources.
     *
     * @param resultPath
     *        path to folder with result site.
     * @param pathXml1
     *        path to first checkstyle-result.xml.
     * @param pathXml2
     *        path to second checkstyle-result.xml.
     * @param pathTestData
     *        path to checkstyle subject data.
     * @throws IOException
     *        thrown on failure to perform checks.
     */
    private static void preparationStage(Path resultPath, Path pathXml1,
            Path pathXml2, Path pathTestData) throws IOException {
        initialVerification(resultPath, pathXml1, pathXml2, pathTestData);
        FilesystemUtils.createOverwriteDirectory(resultPath);
        FilesystemUtils.createOverwriteDirectory(resultPath
                .resolve(CSS_FILEPATH));
        FilesystemUtils.createOverwriteDirectory(resultPath
                .resolve(XREF_FILEPATH));
        FilesystemUtils.exportResource("/maven-theme.css",
                resultPath.resolve(CSS_FILEPATH).resolve("maven-theme.css"));
        FilesystemUtils.exportResource("/maven-base.css",
                resultPath.resolve(CSS_FILEPATH).resolve("maven-base.css"));
    }

    /**
     * Performs file existence checks.
     *
     * @param resultPath
     *        path to folder with result site.
     * @param pathXml1
     *        path to first checkstyle-result.xml.
     * @param pathXml2
     *        path to second checkstyle-result.xml.
     * @param pathTestData
     *        path to checkstyle subject data.
     * @throws IllegalArgumentException
     *         on failure of any check.
     */
    private static void initialVerification(Path resultPath,
            Path pathXml1, Path pathXml2, Path pathTestData)
                    throws IllegalArgumentException {
        if (!Files.isRegularFile(pathXml1)) {
            throw new IllegalArgumentException(MSG_NOT_EXISTS + pathXml1);
        }
        if (!Files.isRegularFile(pathXml2)) {
            throw new IllegalArgumentException(MSG_NOT_EXISTS + pathXml2);
        }
        if (pathXml1.equals(pathXml2)) {
            throw new IllegalArgumentException(MSG_IDENTICAL_INPUT);
        }
        if (Files.isRegularFile(resultPath)) {
            throw new IllegalArgumentException(MSG_EXISTS + resultPath);
        }
        if (pathTestData != null && !Files.isDirectory(pathTestData)) {
            throw new IllegalArgumentException(MSG_NOT_EXISTS + pathTestData);
        }
    }
}
