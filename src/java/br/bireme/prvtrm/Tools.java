/*=========================================================================

    Copyright © 2012 BIREME/PAHO/WHO

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.ReaderUtil;

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
        final Directory sfsdir = new SimpleFSDirectory(new File(sdir));
        final IndexReader reader = IndexReader.open(sfsdir);
        FieldInfos infos;
        
        try {
            infos = reader.getFieldInfos();
        } catch(UnsupportedOperationException uop) {
            infos = ReaderUtil.getMergedFieldInfos(reader);
        }
        
        for (FieldInfo info : infos) {
            set.add(info.name);
        }
        reader.close();
        
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
