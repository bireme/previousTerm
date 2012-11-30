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
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

/**
 * Esta classe retorna um número determinados de chaves do índice do Lucene
 * que são menores (previos) ou maiores (seguintes) que uma determinada
 * chave.
 * @author Heitor Barbieri
 * data 20121123
 */
public class PreviousTerm {
    class Tum {
        TermEnum tenum;
        String cur;

        public Tum(TermEnum tenum) {
            this.tenum = tenum;
            cur = null;
        }
    }

    private final IndexReader reader;
    private final List<String> fields;
    private final int maxSize;

    /**
     * Construtor da classe
     * @param dir caminho para o diretorio onde esta o indice do Lucene
     * @param fields indica a qual campos os termos previos pretencem
     * @param maxSize numero de termos previos a serem retornados
     * @throws IOException
     */
    public PreviousTerm(final File dir,
                         final List<String> fields,
                         final int maxSize) throws IOException {
        if (dir == null) {
            throw new NullPointerException("dir");
        }
        if (fields == null) {
            throw new NullPointerException("fields");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        final Directory sdir = new SimpleFSDirectory(dir);
        reader = IndexReader.open(sdir);
        this.fields = fields;
        this.maxSize = maxSize;
    }

    /**
     * Fecha os recursos abertos
     * @throws IOException
     */
    public void close() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /**
     *
     * @param init temos inicial em relacao ao qual os termos previos serao encontrados
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    public List<String> getPreviousTerms(final String init)
                                                            throws IOException {
        return getPreviousTerms(init, fields, maxSize);
    }

    /**
     *  Retorna os proximos 'maxSize' termos do indice a partir de 'init'
     * @param init termo inicial a partir do qual os outros serao retornados.
     * Pode ser null ou string vazia
     * @return lista ordenada de termos que são os próximos termos a partir de 'init'
     */
    public List<String> getNextTerms(final String init) throws IOException {
        return getNextTerms(init, fields, maxSize);
    }

    /**
     *
     * @param init temos inicial em relacao ao qual os termos previos serao encontrados
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    private List<String> getPreviousTerms(final String init,
                                            final List<String> fields,
                                            final int maxSize)
                                                            throws IOException {
        assert fields != null;
        assert maxSize > 0;

        final String init0 = (init == null) ? "" : init;
        List<String> ret = null;
        String initX = init;

        while (ret == null) {
            final String previousWord = guessPreviousWord(initX, null);
            if (previousWord.isEmpty()) {
                ret = new ArrayList<String>();
            } else {
                final List<String> lst =
                                getNextTerms(previousWord, fields, maxSize + 1);
                if (lst.get(0).equals(init0)) {
                    initX = previousWord;
                } else {
                    ret = getPreviousTermsRange(lst, init0, fields, maxSize);
                }
            }
        }

        return ret;
    }

    /**
     *  Retorna os proximos 'maxSize' termos do indice a partir de 'init'
     * @param init termo inicial a partir do qual os outros serao retornados.
     * Pode ser null ou string vazia
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista ordenada de termos que são os próximos termos a partir de 'init'
     */
    private List<String> getNextTerms(final String init,
                                       final List<String> fields,
                                       final int maxSize) throws IOException {
        assert fields != null;
        assert maxSize > 0;

        final List<String> ret = new ArrayList<String>();
        final String init0 = (init == null) ? "" : init;
        final List<Tum> lte = new ArrayList<Tum>();

        for (String field : fields) {
            lte.add(new Tum(reader.terms(new Term(field, init0))));
        }

        int idx = 0;
        String lastTerm = "";

        while (idx++ < maxSize) {
            lastTerm = getNextTerm(lte, lastTerm);
            if (lastTerm == null) {
                break;
            }
            ret.add(lastTerm);
        }

        for (Tum tum : lte) {
            tum.tenum.close();
        }

        return ret;
    }

    /**
     * Dada uma lista de TermEnum retorna o proximo termo a partit de 'lastTerm'
     * @param ltum lista de TermEnum, uma para cada campo indexado a ter termos recuperados
     * @param lastTerm termo a partir do qual serao recuperados os proximos
     * @return string contendo o proximo termo
     * @throws IOException
     */
    private String getNextTerm(final List<Tum> ltum,
                                final String lastTerm) throws IOException {
        assert ltum != null;

        String min = null;

        for (Tum tum : ltum) {
            if ((tum.cur == null) || (tum.cur.compareTo(lastTerm) <= 0)) {
                if (tum.tenum.next()) {
                    tum.cur = tum.tenum.term().text();
                }
            }
            if ((min == null) || (tum.cur.compareTo(min) < 0)) {
                min = tum.cur;
            }
        }

        return (lastTerm.equals(min)) ? null : min;
    }

    /**
     *
     * @param lstTerms lista de termos canditatos a termos previos
     * @param init temos inicial em relacao ao qual os termos previos serao encontrados
     * @param lstField indica quais campos os termos previos pretencem
     * @param maxSize numero de termos previos a serem retornados
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    private List<String> getPreviousTermsRange(final List<String> lstTerms,
                                                 final String init,
                                                 final List<String> lstField,
                                                 final int maxSize)
                                                            throws IOException {
        assert lstTerms != null;
        assert lstTerms.size() > 0;
        assert init != null;
        assert maxSize > 0;

        final List<String> ret;
        final String last = lstTerms.get(lstTerms.size() - 1);
        final int compare = init.compareTo(last);

        if (compare < 0) { // Temos inicial está no meio dos termos encontrados
            final List<String> lst =
                                lstTerms.subList(0, lstTerms.indexOf(init));
            final int size = lst.size();
            if (size < maxSize) {
                final List<String> prev =
                         getPreviousTerms(lst.get(0), lstField, maxSize - size);
                prev.addAll(lst);
                ret = prev;
            } else {
                ret = lst.subList(size - maxSize, size);
            }
        } else if (compare == 0) { // Achou os n termos previos
            final int size = lstTerms.size();
            ret = (size <= maxSize) ? lstTerms
                                    : lstTerms.subList(size - maxSize, size);
        } else {  // Existem outros termos entre os candidatos e o inicial
            lstTerms.addAll(
                           getNextTerms(last, lstField, Math.max(10, maxSize)));
            ret = getPreviousTermsRange(lstTerms, init, lstField, maxSize);
        }

        return ret;
    }

    /**
     * Retorna o prefixo comum a duas strings
     * @param word1 string 1
     * @param word2 string 2
     * @return o prefixo comum a duas strings
     */
    private String getCommonPrefix(final String word1,
                                     final String word2) {
        assert word1 != null;
        assert word2 != null;

        final int len = Math.min(word1.length(), word2.length());
        int common = 0;

        for (int idx = 0; idx < len; idx++) {
            if (word1.charAt(idx) == word2.charAt(idx)) {
                common++;
            } else {
                break;
            }
        }

        return word1.substring(0, common);
    }

    /**
     * Gera uma string que estaria a uma distância média entre as strings
     * 'current' e 'lastGuess'.
     * @param current
     * @param lastGuess
     * @return uma string anterior menor que a 'current' mas maior que 'lastGuess'
     */
    private String guessPreviousWord(final String current,
                                       final String lastGuess) {
        assert current != null;
        assert (lastGuess == null) ? true : (current.compareTo(lastGuess) > 0);

        final String ret;
        final int clen = current.length();

        if (lastGuess == null) {
            if (clen == 1) {
                final char med = (char)(current.charAt(0)/2);
                ret = new String(new char[] {med});
            } else {
                ret = current.substring(0, clen/2);
            }
        } else {
            ret = getCommonPrefix(current, lastGuess);
        }

        return ret;
    }
}
