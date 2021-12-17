/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 * date: 20150525
 */
public class LuceneIndexInfo {
    final String name;
    final String path;
    final Set<String> fields;

    public LuceneIndexInfo(final String name,
                          final String path,
                          final String... fields) {
        this.name = name;
        this.path = path;
        this.fields = new HashSet<String>(Arrays.asList(fields));
    }
}
