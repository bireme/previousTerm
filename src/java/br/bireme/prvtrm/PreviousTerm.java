/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 * Esta classe retorna um número determinados de chaves do índice do Lucene
 * que são menores (previos) ou maiores (seguintes) que uma determinada
 * chave.
 *
 * @author Heitor Barbieri
 * date: 20121123
 */
public class PreviousTerm {
    private class Tum {
        private final IndexReader reader;
        private final TermsEnum tenum;
        private boolean eof;
        private String cur;

        Tum(final String sdir,
            final String field,
            final String term) throws IOException {
            assert sdir != null;
            assert field != null;
            assert term != null;

            this.reader = getIndexReader(sdir);
            final Terms terms = MultiFields.getTerms(reader, field.trim());

            if (terms == null) {
                throw new IOException("Invalid field: " + field);
            }

            tenum = terms.iterator();

            if (tenum.seekCeil(new BytesRef(term.trim()))
                                                  == TermsEnum.SeekStatus.END) {
                eof = true;
            } else {
                eof = false;
            }
            cur = null;
        }

        void close() throws IOException {
            reader.close();
        }

        boolean hasNext() {
            return !eof;
        }

        String next() throws IOException {
            if (eof) {
                throw new IOException("end of iterator found");
            }
            final String ret = tenum.term().utf8ToString();
            cur = ret;
            eof = (tenum.next() == null);

            return ret;
        }

        String current() throws IOException {
            final String current;

            if (cur == null) {
                current = eof ? null : next();
            } else {
                current = cur;
            }

            return current;
        }
    }

    private class NextTerms {
        final Set<String> fields;
        final Set<Tum> lte;
        String cur;
        boolean first;
        final String max;

        NextTerms(final String sdir,
                  final Set<String> fields,
                  final String term) throws IOException {
            assert sdir != null;
            assert fields != null;
            assert term != null;

            this.fields = fields;
            lte = new HashSet<Tum>();
            cur = term.trim();
            first = true;
            max = new Character(Character.MAX_VALUE).toString();

            for (String fld : fields) {
                lte.add(new Tum(sdir, fld, term));
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
            String min = max;

            for (Tum tum : lte) {
                String tcur = tum.current();

                while (true) {
                    if (tcur == null) {
                        break;
                    } else if (first && tcur.compareTo(cur) >= 0) {
                        break;
                    } else if (tcur.compareTo(cur) > 0) {
                        break;
                    } else if (!tum.hasNext()) {
                        break;
                    }
                    tcur = tum.next();
                }
                if ((tcur != null) && (tcur.compareTo(min) < 0)) {
                    min = tcur;
                }
            }
            cur = min.equals(max) ? null : min;
            first = false;
//System.out.println("==> " + cur);
            return cur;
        }
    }

    private final Map<String,String> info;
    private final int maxSize;
    private HashMap<String,IndexReader> readers;
    private HashMap<String,Set<String>> fields;

    public Map<String,String> getInfo() {
        return new HashMap<String,String>(info);
    }

    public Set<String> getFields(final String index) {
        final Set<String> set;

        if (index == null) {
            set = null;
        } else {
            final Set<String> cset = fields.get(index);
            set = (cset == null) ? null: new HashSet<String>(fields.get(index));
        }

        return set;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public Set<String> getIndexes() {
        return new HashSet<String>(info.keySet());
    }

    /**
     * Construtor da classe
     * @param info conjunto de  nomes e caminhos dos indices Lucene a
     * serem utilizados
     * @param maxSize numero de termos previos a serem retornados
     * @throws IOException
     */
    public PreviousTerm(final Map<String,String> info,
                        final int maxSize) throws IOException {
        if (info == null) {
            throw new NullPointerException("fields");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        this.info = info;
        this.maxSize = maxSize;
        this.readers = new HashMap<String,IndexReader>();
        this.fields = new HashMap<String,Set<String>>();

        for (Map.Entry<String,String> entry : info.entrySet()) {
            final Directory sdir = new SimpleFSDirectory(
                                           new File(entry.getValue()).toPath());
            final IndexReader reader = DirectoryReader.open(sdir);
            final String key = entry.getKey();
            final HashSet<String> fset = new HashSet<String>();

            this.readers.put(key, reader);
            this.fields.put(key, fset);

            for (String fname : MultiFields.getFields(reader)) {
                fset.add(fname);
            }
        }
    }

    /**
     * Fecha os recursos abertos
     * @throws IOException
     */
    public void close() throws IOException {
        for (IndexReader reader: readers.values()) {
            reader.close();
        }
    }

    /**
     * Encontra os termos previos de um indice em relacao ao termo inicial
     * @param sdir nome do indice lucene a ser utilizado
     * @param init termo inicial em relacao ao qual os termos previos serao encontrados
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista de termos previos em relacao ao termo inicial
     * @throws IOException
     */
    public List<String> getPreviousTerms(final String sdir,
                                         final String init,
                                         final Set<String> fields,
                                         final int maxSize) throws IOException {
        if ((sdir == null) || sdir.isEmpty()) {
            throw new IOException("empty 'sdir' parameter");
        }
        if ((init == null) || init.isEmpty()) {
            throw new IOException("empty 'init' parameter");
        }
        if ((fields == null) || fields.isEmpty()) {
            throw new IOException("empty 'fields' parameter");
        }
        if (maxSize <= 0) {
            throw new IOException("invalid maxSize [" + maxSize + "]");
        }

        final List<String> ret = new ArrayList<String>();
        int mSize = maxSize;
        String initX = init;

        final NextTerms nterms = new NextTerms(sdir, fields, initX);
        if (nterms.hasNext() && nterms.next().equals(initX)) {
            ret.add(initX);
            mSize--;
        }
        //nterms.close();

        for (int tot = 0; tot < mSize; tot++) {
            final String prev = getPreviousTerm(sdir, initX, fields);

            if (prev == null) {
                break;
            }
            ret.add(prev);
            initX = prev;
        }

        return ret;
    }

    /**
     *
     * @param index nome do diretorio onde esta o indice Lucene a ser lido
     * @return um objeto IndexReader em cache ou cria um novo
     */
    private IndexReader getIndexReader(final String index) throws IOException {
        assert index != null;

        final IndexReader reader = readers.get(index);
        if (reader == null) {
            throw new IOException("invalid index name: " + index);
        }

        return reader;
    }

    /**
     * Encontra o termo previo em relacao ao termo inicial
     * @param sdir nome do indice lucene a ser utilizado
     * @param init termo inicial em relacao ao qual o termo previo sera encontrado
     * @param fields indica a quais campos os termo deve pertencer
     * @return o termo previo em relacao ao termo inicial
     * @throws IOException
     */
    private String getPreviousTerm(final String sdir,
                                   final String init,
                                   final Set<String> fields)
                                                            throws IOException {
        assert sdir != null;
        assert fields != null;
        assert (init != null) && (!init.isEmpty());

        final int RANGE = 10;             // next terms max buffer
        final int MAXTOTFIRSTPOS = 210;   // max tries to guess the previous world

        String initX = init;
        String lowerBound = null;
        String ret;
        int totFirstPos = 0;
        int totGetNext = 0;

        while (true) {
            final String previousWord = guessPreviousWord(initX, lowerBound);
            if (previousWord == null) {
                ret = null;
                break;
            }
            final List<String> nextWords =
                                getNextTerms(sdir, previousWord, fields, RANGE);
            if (nextWords.isEmpty()) {
                if (totGetNext++ > MAXTOTFIRSTPOS) {
                    ret = lowerBound;
                    break;
                }
                initX = previousWord;
            } else {
                final String last = nextWords.get(nextWords.size() - 1);
                if (last.compareTo(initX) < 0) {
                    lowerBound = last;              // init esta em um bloco adiante
                } else {
                    int idx = 0;
                    for (String word : nextWords) {     // init esta no bloco corrente
                        if (word.compareTo(init) >= 0) {
                            break;
                        }
                        idx++;
                    }
                    if (idx == 0) {                    // init esta na primeira posicao
                        if (totFirstPos++ > MAXTOTFIRSTPOS) { // stop guessing previous key
                            ret = lowerBound;
                            break;
                        }
                        initX = previousWord;
                    } else {
                        ret = nextWords.get(idx - 1);  // achou termo previo
                        break;
                    }
                }
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
     * @param sdir nome do indice lucene a ser utilizado
     * @param init termo inicial a partir do qual os outros serao retornados.
     * Pode ser null ou string vazia
     * @param fields indica a quais campos os termos devem pertencer
     * @param maxSize tamanho maximo da lista de termos a ser retornada
     * @return lista ordenada de termos que são os próximos termos a partir de 'init'
     * @throws java.io.IOException
     */
    public List<String> getNextTerms(final String sdir,
                                     final String init,
                                     final Set<String> fields,
                                     final int maxSize) throws IOException {
        if ((sdir == null) || sdir.isEmpty()) {
            throw new IOException("invalid sdir");
        }
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
        final NextTerms nterms = new NextTerms(sdir, fields, init);
        int total = 0;

        while (nterms.hasNext()) {
            if (++total > maxSize) {
                break;
            }
            final String next = nterms.next();
            if (next != null) {
                ret.add(next);
            }
        }
        //nterms.close();

        return ret;
    }
}
