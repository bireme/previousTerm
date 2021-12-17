/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import bruma.BrumaException;
import bruma.master.Field;
import bruma.master.Master;
import bruma.master.MasterFactory;
import bruma.master.Record;
import java.io.File;
import java.io.IOException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Heitor Barbieri
 * date: 20160215
 */
public class GenLilacsIndex {
    private static void usage() {
        System.err.println("usage: GenLilacsIndex <LILPath> <outDir>");
        System.exit(1);
    }

    private static void writeDocument(final IndexWriter iwriter,
                                      final Record rec)
                                            throws BrumaException, IOException {
        String title = "[empty]";
        String title2 = null;
        String abstr = "[empty]";

        Field fld = rec.getField(12, 1);
        if (fld != null) {
            title = fld.getContent();
            if ((title == null) || (title.isEmpty())) {
                title = "[empty]";
            } else {
                final String[] split = title.trim().split(" +", 3);
                if (split.length >= 2) {
                   title2 = split[0] + " " + split[1]; 
                }
            }
        }
        fld = rec.getField(83, 1);
        if (fld != null) {
            abstr = fld.getContent();
            if ((abstr == null) || (abstr.isEmpty())) {
                abstr = "[empty]";
            }
        }

        final Document doc = new Document();
        final org.apache.lucene.document.Field mfnFld = new
                org.apache.lucene.document.StoredField("mfn", rec.getMfn());
        final org.apache.lucene.document.Field titFld = new
                org.apache.lucene.document.TextField("tit", title,
                    org.apache.lucene.document.Field.Store.YES);
        final org.apache.lucene.document.Field absFld = new
                org.apache.lucene.document.TextField("abs", abstr,
                    org.apache.lucene.document.Field.Store.YES);

        doc.add(mfnFld);
        doc.add(titFld);
        doc.add(absFld);
        if (title2 != null) {
            final org.apache.lucene.document.Field titFld2 = new
                org.apache.lucene.document.StringField("tit2", 
                    title2.toLowerCase(),
                    org.apache.lucene.document.Field.Store.YES);
            doc.add(titFld2);

        }
        iwriter.addDocument(doc);
    }

    public static void main(final String[] args) throws IOException,
                                                                BrumaException {
        /*if (args.length < 2) {
            usage();
        }*/

        final String lilPath = "LILACS/lilacs"; //args[0];
        final String outDir = "lilacs"; //args[1];

        final Master mst = MasterFactory.getInstance(lilPath).open();
        //final Reader reader = new FileReader("ALL_StopWords.txt");
        final Analyzer analyzer = new StandardAnalyzer();

        final Directory directory = FSDirectory.open(new File(outDir)
                                                                     .toPath());
        final IndexWriterConfig conf = new IndexWriterConfig(analyzer);
        final IndexWriter iwriter = new IndexWriter(directory, conf);
        int cur = 0;

        for (Record rec : mst) {
            if (rec.isActive()) {
                writeDocument(iwriter, rec);
                if (++cur % 50000 == 0) {
                    System.out.println("+++" + cur);
                }
            }
        }

        iwriter.close();
        directory.close();
        //reader.close();
    }
}
