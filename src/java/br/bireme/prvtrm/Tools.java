/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 *
 * @author Heitor Barbieri
 * date: 20150827
 */
public class Tools {
    public static Set<String> getDocFields(final String sdir)
                                                            throws IOException {
        if (sdir == null) {
            throw new NullPointerException("sdir");
        }
        final Set<String> set = new TreeSet<String>();
        final Directory sfsdir = new SimpleFSDirectory(new File(sdir).toPath());
        final IndexReader reader = DirectoryReader.open(sfsdir);
        final Fields fields = MultiFields.getFields(reader);

        for (String name : fields) {
            set.add(name);
        }

        return set;
    }

    public static Set<String> getNextTerms(final String sdir,
                                           final String fldName,
                                           final String from,
                                           final int count) throws IOException {
        if (sdir == null) {
            throw new NullPointerException("sdir");
        }
        if (fldName == null) {
            throw new NullPointerException("fldName");
        }
        if (from == null) {
            throw new NullPointerException("from");
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count <= 0");
        }
        final Map<String,String> map = new HashMap<String,String>();
        map.put("index", sdir);

        final PreviousTerm pterm = new PreviousTerm(map, count+1);

        final Set<String> set = new HashSet<String>();
        set.add(fldName);
        final List<String> lst = pterm.getNextTerms("index", from, set, count);

        pterm.close();

        return new TreeSet<String>(lst);
    }

    private static void usage() {
        System.err.println("usage: Tools <dir> " +
                                          "[-show=<fieldName>,<from>,<count>]");
        System.exit(1);
    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 1) {
            usage();
        }

        for (String field : getDocFields(args[0])) {
            System.out.println("field: [" + field + "]");
        }
        System.out.println(args.length);
        if (args.length > 1) {
            System.out.println();

            if (args[1].startsWith("-show=")) {
                final String[] split = args[1].substring(6).split(" *, *");
                if (split.length != 3) {
                    usage();
                }
                final Set<String> set = getNextTerms(args[0],split[0],split[1],
                                                     Integer.parseInt(split[2]));
                for (String key : set) {
                    System.out.println("key: [" + key + "]");
                }
            }
        }
    }
}
