/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Heitor Barbieri
 * date: 20150827
 */
public class Tools {
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
        final Map<String,String> map = new HashMap<>();
        map.put("index", sdir);

        final PreviousTerm pterm = new PreviousTerm(map, count+1);

        final Set<String> set = new HashSet<>();
        set.add(fldName);
        final List<String> lst = pterm.getNextTerms("index", from, set, count);

        pterm.close();

        return new TreeSet<>(lst);
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
