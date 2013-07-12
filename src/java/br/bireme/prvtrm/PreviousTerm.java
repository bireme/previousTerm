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
import java.util.LinkedList;
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
        private TermEnum tenum;
        private String cur;
        private boolean eof;
        private boolean first;

        Tum(final String field,
            final String term) throws IOException {
            assert field != null;
            assert term != null;

            cur = term;
            tenum = reader.terms(new Term(field, term));
            first = true;
            getNext();
        }

        void close() throws IOException {
            tenum.close();
        }

        boolean hasNext() {
            return !eof;
        }

        String next() throws IOException {
            final String ret = cur;

            if (hasNext()) {
                getNext();
            } else {
                cur = null;
            }
            return ret;
        }

        String current() {
            return cur;
        }

        private String getNext() throws IOException {
            if (!eof) {
                final Term trm = tenum.term();
                if (trm == null) {
                    eof = true;
                    cur = null;
                } else {
                    eof = false;
                    if (first) {
                        first = false;
                    } else {
                        tenum.next();
                    }
                    cur = trm.text();
                }
            }

            return cur;
        }
    }

    class NextTerms {
        final List<String> fields;
        final List<Tum> lte;
        String cur;
        boolean first;

        NextTerms(final List<String> fields,
                  final String term) throws IOException {
            assert fields != null;
            assert term != null;

            this.fields = fields;
            lte = new ArrayList<Tum>();
            cur = term;
            first = true;

            for (String fld : fields) {
                lte.add(new Tum(fld, term));
            }
        }

        void close() throws IOException {
            for (Tum tum : lte) {
                tum.close();
            }
        }

        boolean hasNext() {
            boolean ret = false;

            for (Tum tum : lte) {
                if (tum.hasNext()) {
                    ret = true;
                    break;
                }
            }

            return ret;
        }

        String next() throws IOException {
            String min = new Character(Character.MAX_VALUE).toString();

            for (Tum tum : lte) {
                while (tum.hasNext()) {
                    if (first && tum.current().compareTo(cur) >= 0) {
                        break;
                    } else if (tum.current().compareTo(cur) > 0) {
                        break;
                    }
                    tum.next();
                }
                if (tum.current().compareTo(min) < 0) {
                    min = tum.current();
                }
            }
            cur = min;
            first = false;

            return cur;
        }
    }

    public class TermElem {
        private String term;
        private int tot;

        public TermElem(String term) {
            this.term = term;
            tot = 0;
        }
        public String getTerm() {
            return term;
        }
        public int getTotal() {
            return tot;
        }
    }

    private final IndexReader reader;
    private final List<String> fields;
    private final int maxSize;

    public List<String> getFields() {
        return fields;
    }

    public int getMaxSize() {
        return maxSize;
    }

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
     * Encontra os termos previos de um indice em relacao ao termo inicial
     * @param init termo inicial em relacao ao qual os termos previos serao encontrados
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    public List<String> getPreviousTerms(final String init) throws IOException {
        if ((init == null) || init.isEmpty()) {
            throw new IOException("invalid init");
        }
        return getPreviousTerms(init, fields, maxSize);
    }

    /**
     * Encontra os termos previos de um indice em relacao ao termo inicial
     * @param init termo inicial em relacao ao qual os termos previos serao encontrados
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    public List<String> getPreviousTerms(final String init,
                                         final List<String> fields,
                                         final int maxSize) throws IOException {
        if ((init == null) || init.isEmpty()) {
            throw new IOException("invalid init");
        }
        if ((fields == null) || fields.isEmpty()) {
            throw new IOException("invalid fields");
        }
        if (maxSize <= 0) {
            throw new IOException("invalid maxSize");
        }

        final LinkedList<String> ret = new LinkedList<String>();
        final List<Tum> lte = new ArrayList<Tum>();
        int mSize = maxSize;
        String initX = init;

        final NextTerms nterms = new NextTerms(fields, initX);
        if (nterms.hasNext() && nterms.next().equals(initX)) {
            ret.add(initX);
            mSize--;
        }
        nterms.close();

        for (int tot = 0; tot < mSize; tot++) {
            final String prev = getPreviousTerm(initX, fields);

            if (prev == null) {
                break;
            }
            ret.add(prev);
            initX = prev;
        }

        return ret;
    }

    /**
     * Encontra o termo previo em relacao ao termo inicial
     * @param init termo inicial em relacao ao qual o termo previo sera encontrado
     * @param fields indica a quais campos os termo deve pertencer
     * @return o termo previo em relacao ao termo inicial
     * @throws IOException
     */
    private String getPreviousTerm(final String init,
                                   final List<String> fields)
                                                            throws IOException {
        assert fields != null;
        assert (init != null) && (!init.isEmpty());

        final int RANGE = 10;
        final int MAXTOTFIRSTPOS = 210;

        String initX = init;
        String lowerBound = null;
        String ret;
        int totFirstPos = 0;

        while (true) {
            final String previousWord = guessPreviousWord(initX, lowerBound);
            if (previousWord == null) {
                ret = null;
                break;
            }
            List<String> nextWords;
            int idx = -1;

            nextWords = getNextTerms(previousWord, fields, RANGE);
            if (nextWords.isEmpty()) {
                ret = null;
                break;
            }
            final String last = nextWords.get(nextWords.size() - 1);
            if (last.compareTo(initX) < 0) {    // init esta em um bloco adiante
                //initX = init;
                lowerBound = last;
                continue;
            }
            for (String word : nextWords) {     // init esta no bloco corrente
                if (word.compareTo(init) >= 0) {
                    break;
                }
                idx++;
            }
            if (idx == -1) {                    // init esta na primeira posicao
                if (totFirstPos++ > MAXTOTFIRSTPOS) {
                    ret = lowerBound;
                    break;
                }
                //lowerBound = null;
                initX = previousWord;
                continue;
            } else {
                ret = nextWords.get(idx);               // achou termo previo
                break;
            }
        }
        return ret;
    }

    //==========================================================================

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
        assert (lastGuess == null) ? true : (current.compareTo(lastGuess) > 0)
                                                    : current + ">" + lastGuess;

        final String ret;
        final int clen = current.length();

        if (lastGuess == null) {
            if (clen == 1) {
                final char first = current.charAt(0);
                final char med = (char)(first/2);
                ret = "" + ((first == med)
                                  ? first + (char)(Character.MAX_VALUE / 2)
                                  : med);
            } else {
                //ret = current.substring(0, clen/2);
                ret = current.substring(0, clen - 1);
            }
        } else {
            final StringBuilder builder = new StringBuilder();
            final int llen = lastGuess.length();
            final int max = Math.max(clen, llen);
            boolean addLetter = false;

            for (int idx = 0; idx < max; idx++) {
                if (idx < clen) {
                    final char cch = current.charAt(idx);
                    if (idx < llen) {
                        final char lch = lastGuess.charAt(idx);
                        if (addLetter) {
                            final char med =
                                       (char) ((Character.MAX_VALUE - lch) / 2);
                            if (med > 0) {
                                builder.append((char)(lch + med));
                                addLetter = false;
                                break;
                            }
                        } else if (cch == lch) {
                            builder.append(cch);
                        } else {
                            final char med = (char)((lch + cch) / 2);
                            if (med == lch) {
                                builder.append(lch);
                                addLetter = true;
                            } else {
                                builder.append(med);
                                addLetter = false;
                                break;
                            }
                        }
                    } else {
                        final char med = (char)(cch / 2);
                        if (med > 0) {
                            builder.append(med);
                            addLetter = false;
                            break;
                        } else {
                            builder.append(cch);
                            addLetter = true;
                        }
                    }
                } else {
                    final char lch = lastGuess.charAt(idx);
                    final char med = (char) ((Character.MAX_VALUE - lch) / 2);

                    if (med > 0) {
                        builder.append((char)(lch + med));
                        addLetter = false;
                        break;
                    } else {
                        builder.append(lch);
                        addLetter = true;
                    }
                }
            }
            if (addLetter) {
                final char med = (char)(Character.MAX_VALUE / 2);
                builder.append(med);
            }

            ret = builder.toString();
        }
        return ret;
    }

    //==========================================================================

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
     *  Retorna os proximos 'maxSize' termos do indice a partir de 'init'
     * @param init termo inicial a partir do qual os outros serao retornados.
     * Pode ser null ou string vazia
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista ordenada de termos que são os próximos termos a partir de 'init'
     */
    public List<String> getNextTerms(final String init,
                                     final List<String> fields,
                                     final int maxSize) throws IOException {
        if ((init == null) || init.isEmpty()) {
            throw new IOException("invalid init");
        }
        if ((fields == null) || fields.isEmpty()) {
            throw new IOException("invalid fields");
        }
        if (maxSize <= 0) {
            throw new IOException("invalid maxSize");
        }

        final List<String> ret = new ArrayList<String>();
        final NextTerms nterms = new NextTerms(fields, init);
        int total = 0;

        while (nterms.hasNext()) {
            if (++total > maxSize) {
                break;
            }
            ret.add(nterms.next());
        }
        nterms.close();

        return ret;
    }
}
