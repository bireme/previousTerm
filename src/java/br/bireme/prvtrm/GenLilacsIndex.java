/*=========================================================================

    Copyright © 2016 BIREME/PAHO/WHO

    This file is part of PreviousTerm servlet.

    PreviousTerm is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, either version 3 of
    the License, or (at your option) any later version.

    PreviousTerm is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with PreviousTerm. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

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
import org.apache.lucene.store.SimpleFSDirectory;

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
        String abstr = "[empty]";
        
        Field fld = rec.getField(12, 1);
        if (fld != null) {
            title = fld.getContent();
            if ((title == null) || (title.isEmpty())) {
                title = "[empty]";
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
        iwriter.addDocument(doc);                
    }
    
    public static void main(final String[] args) throws IOException, 
                                                                BrumaException {
        /*if (args.length < 2) {
            usage();
        }*/
        
        final String lilPath = "LILACS"; //args[0];
        final String outDir = "lilacs"; //args[1];
        
        final Master mst = MasterFactory.getInstance(lilPath).open();
        //final Reader reader = new FileReader("ALL_StopWords.txt");
        final Analyzer analyzer = new StandardAnalyzer();
                                                                            
        final Directory directory = new SimpleFSDirectory(new File(outDir)
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
