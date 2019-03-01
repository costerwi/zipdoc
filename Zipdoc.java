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
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Zipdoc {
    /**
     * Read specified zip document file and output text to stdout.
     *
     * The program takes aa single argument, the name of the file to convert,
     * and produces resulting text on stdout.
     * {@see https://github.com/costerwi/zipdoc}
     */
    public static void main(final String[] argv) throws IOException, TransformerException {
        if (1 != argv.length) {
            System.err.printf("Usage: %s infile > text_representation.txt\n", Zipdoc.class.getSimpleName());
            System.exit(1);
        }

        try (final ZipInputStream source_zip = new ZipInputStream(new FileInputStream(argv[0]))) {
            final Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            final byte[] buffer = new byte[8192];
            ZipEntry entry;
            final ByteArrayOutputStream uncomp_bs = new ByteArrayOutputStream();
            final CRC32 cksum = new CRC32();
            final CheckedOutputStream uncomp_os = new CheckedOutputStream(uncomp_bs, cksum);
            while ((entry = source_zip.getNextEntry()) != null) {
                uncomp_bs.reset();
                cksum.reset();

                System.out.println("Sub-file:\t" + entry);

                // Copy file from source_zip into uncompressed, check-summed output stream
                int len;
                while ((len = source_zip.read(buffer)) > 0) {
                    uncomp_os.write(buffer, 0, len);
                }
                source_zip.closeEntry();

                if (entry.getName().endsWith(".xml")) {
                    // xml file: pretty-print the data to stdout
                    InputSource in = new InputSource(new ByteArrayInputStream(uncomp_bs.toByteArray()));
                    serializer.transform(new SAXSource(in), new StreamResult(System.out));
                } else if (entry.getName().endsWith(".txt")) {
                    // Text file: dump directly to stdout
                    uncomp_bs.writeTo(System.out);
                } else {
                    // Unknown file type: report uncompressed size and CRC32
                    System.out.println("File size:\t" + uncomp_bs.size());
                    System.out.println("Checksum:\t" + Long.toHexString(cksum.getValue()));
                }
                System.out.println();
            }
        }
    }
}

