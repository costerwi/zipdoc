import java.io.*;
import java.util.zip.*;

import org.xml.sax.InputSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
 
public class Zipdoc {
    /**
     * Read specified zip document file and output text to stdout.
     *
     * The program takes aa single argument, the name of the file to convert,
     * and produces resulting text on stdout.
     * {@link https://github.com/costerwi/zipdoc}
     */
    public static void main(String argv[]) throws IOException, TransformerException {
        if (1 != argv.length) {
            System.err.println("Usage: Zipdoc infile >textconv.txt");
            System.exit(1);
        }
        ZipInputStream source_zip = new ZipInputStream(new FileInputStream(argv[0]));

        Transformer serializer = SAXTransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        byte[] buffer = new byte[8192];
        ZipEntry entry;
        ByteArrayOutputStream uncomp_bs = new ByteArrayOutputStream();
        CRC32 cksum = new CRC32();
        CheckedOutputStream uncomp_os = new CheckedOutputStream(uncomp_bs, cksum);
        try {
            while ((entry = source_zip.getNextEntry()) != null) {
                uncomp_bs.reset();
                cksum.reset();

                System.out.println("Subfile:\t" + entry);

                // Copy file from source_zip into uncompressed, checksummed output stream
                int len = 0;
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
                    System.out.println("Filesize:\t" + uncomp_bs.size());
                    System.out.println("Checksum:\t" + Long.toHexString(cksum.getValue()));
                }
                System.out.println();
            }
        } finally {
            source_zip.close();
        }
    }
}
