/*
 * Copyright 2015 Carl Osterwisch <costerwi@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.xml.sax.InputSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The program takes a single argument, the name of the file to convert,
 * and produces a more human readable, textual representation of its content on stdout.
 * {@see https://github.com/costerwi/zipdoc}
 */
public class ZipDoc {

    public static void main(final String[] argv) throws IOException, TransformerException {

        if (1 != argv.length) {
            System.err.printf("Usage: %s infile > text_representation.txt\n", ZipDoc.class.getSimpleName());
            System.exit(1);
        }

        transform(argv[0]);
    }

    /**
     * Checks whether a file denotes an XML based file format.
     * @param fileName to be checked
     * @return whether the supplied file name is XML based
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isXml(final String fileName) {
        // TODO Improve this function with a longer list of extensions, or optimally even by inspecting the MIME-type
 		return (fileName.endsWith(".xml") ||  fileName.endsWith(".xhtml"));
    }

    /**
     * Checks whether a file denotes an plain-text file format.
     * @param fileName to be checked
     * @return whether the supplied file name is text based
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isPlainText(final String fileName) {
        // TODO Improve this function with a longer list of extensions, or optimally even by inspecting the MIME-type
        return fileName.endsWith(".txt");
    }

    /**
     * Reads the specified ZIP file and outputs a textual representation of its to stdout.
     * @param zipFilePath the ZIP file to convert to a text
     */
    @SuppressWarnings("WeakerAccess")
    public static void transform(final String zipFilePath) throws IOException, TransformerException {

        try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            transform(zipIn, System.out);
        }
    }

    /**
     * Reads the specified ZIP document and outputs a textual representation of its to the specified output stream.
     * @param zipIn the ZIP document to convert to a text
     * @param output where the text gets written to
     */
    @SuppressWarnings("WeakerAccess")
    public static void transform(final ZipInputStream zipIn, final PrintStream output)
            throws IOException, TransformerException
    {
        final Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        final byte[] buffer = new byte[8192];
        ZipEntry entry;
        final ByteArrayOutputStream uncompressedOutRaw = new ByteArrayOutputStream();
        final CRC32 checkSum = new CRC32();
        final CheckedOutputStream uncompressedOutChecked = new CheckedOutputStream(uncompressedOutRaw, checkSum);
        while ((entry = zipIn.getNextEntry()) != null) {
            uncompressedOutRaw.reset();
            checkSum.reset();

            output.println("Sub-file:\t" + entry);

            // Copy the file from zipIn into the uncompressed, check-summed output stream
            int len;
            while ((len = zipIn.read(buffer)) > 0) {
                uncompressedOutChecked.write(buffer, 0, len);
            }
            zipIn.closeEntry();

            if (isXml(entry.getName())) {
                // XML file: pretty-print the data to stdout
                InputSource in = new InputSource(new ByteArrayInputStream(uncompressedOutRaw.toByteArray()));
                serializer.transform(new SAXSource(in), new StreamResult(output));
            } else if (isPlainText(entry.getName())) {
                // Text file: dump directly to output
                uncompressedOutRaw.writeTo(output);
            } else {
                // Unknown file type: report uncompressed size and CRC32
                output.println("File size:\t" + uncompressedOutRaw.size());
                output.println("Checksum:\t" + Long.toHexString(checkSum.getValue()));
            }
            output.println();
        }
    }
}
