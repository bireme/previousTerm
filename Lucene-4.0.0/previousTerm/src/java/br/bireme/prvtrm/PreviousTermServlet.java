/*=========================================================================

    previousTerm © Pan American Health Organization, 2018.
    See License at: https://github.com/bireme/previousTerm/blob/master/LICENSE.txt

  ==========================================================================*/

package br.bireme.prvtrm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Heitor Barbieri
 * date: 20121129
 */
@WebServlet(name = "PreviousTermServlet", urlPatterns =
                                                       {"/PreviousTermServlet"})
public class PreviousTermServlet extends HttpServlet {

    private final Logger logger = LogManager.getLogger(PreviousTermServlet.class);
    private String maxTerms;
    private Map<String,String> iinfo;
    private PreviousTerm previous;

    /**
     * INDEX_DIR diretorio contendo o indice Lucene
     * MAX_TERMS numero maximo de termos a serem retornados
     * DOC_FIELDS nomes dos campos cujos termos serao retornados
     * (separados por ',' ';' ou '-' )
     * @param servletConfig
     * @throws ServletException
     */
    @Override
    public void init(final ServletConfig servletConfig)
                                                       throws ServletException {
        try {
            maxTerms = servletConfig.getInitParameter("MAX_TERMS");
            if (maxTerms == null) {
                throw new ServletException("missing maximum number of returned "
                                              + "terms (MAX_TERMS) parameter.");
            }
            final String indexes = servletConfig.getInitParameter("LUCENE_INDEXES");
            if (indexes == null) {
                throw new ServletException(
                             "missing LUCENE_INDEXES configuration parameter.");
            }
            iinfo = getIndexInfo(indexes);
            previous = new PreviousTerm(iinfo, Integer.parseInt(maxTerms));
        } catch (Exception ex) {
            logger.catching(Level.ERROR, ex);
            throw new ServletException(ex);
        }
    }

    // [name="<index name>" path="<index path>"]
    private Map<String,String> getIndexInfo(final String in) {
        assert in != null;

        final Map<String,String> infol = new HashMap<String,String>();
        final Matcher mat = Pattern.compile(
         "\\[\\s*name\\s*=\\s*\"([^\"]+)\"\\s+path\\s*=\\s*\"([^\"]+)\"\\s*\\]")
                                                                   .matcher(in);

        while (mat.find()) {
            infol.put(mat.group(1), mat.group(2));
        }
        return infol;
    }

    private void info(final HttpServletRequest request,
                      final HttpServletResponse response)
                                          throws ServletException, IOException {
        PrintWriter out = null;

        try {
            response.setContentType("text/html; charset=UTF-8");
            out = response.getWriter();

            out.println("<html>");
            out.println("<head><title>PreviousTerm Info</title></head>");
            out.println("<body>");
            out.println("<h1>PreviousTerm Info</h1>");
            out.println("<p>Host:" + request.getServerName() + "</p>");
            out.println("<p>Port:" + request.getServerPort() + "</p>");
            out.println("<p>Indexes:</p>");
            out.println("<table><tr><th>name></th><th>path</th></tr>");
            for (Map.Entry<String,String> entry: iinfo.entrySet()) {
                out.println("<tr><td>" + entry.getKey() + "</td><td>" +
                            entry.getValue() + "</td></tr>");
            }
            out.println("</table>");
            out.println("</body></html>");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws java.io.IOException
     */
    protected void processRequest(final HttpServletRequest request,
                                  final HttpServletResponse response)
                                          throws ServletException, IOException {
        PrintWriter out = null;
        String verbose = null;

        if (request.getParameter("info") != null) {
            info(request, response);
            return;
        }

        try {
            response.setContentType("application/json; charset=UTF-8");
            out = response.getWriter();

            verbose = request.getParameter("verbose");

            final String index = request.getParameter("index");
            if (index == null) {
                throw new ServletException("missing 'index' parameter");
            }

            final String init = request.getParameter("init");
            if (init == null) {
                throw new ServletException("missing 'init' parameter");
            }

            final String maxTermsStr = request.getParameter("maxTerms");
            final int maxSize = (maxTermsStr == null) ? previous.getMaxSize()
                                                : Integer.parseInt(maxTermsStr);

            final String sfields = request.getParameter("fields");
            final Set<String> fields;
            if (sfields == null) {
                throw new ServletException("missing 'fields' parameter");
            } else {
                fields = new HashSet<String>(
                        Arrays.asList(sfields.trim().split(" *[\\,\\;] *")));
            }
            
            final boolean cleanTokens;
            final String sclean = request.getParameter("cleanTokens");
            if ((sclean == null) || (sclean.trim().length() == 0)) cleanTokens = false;
            else cleanTokens = Boolean.getBoolean(sclean);

            final List<String> terms;
            String direction = request.getParameter("direction");
            if ((direction == null) ||
                (direction.compareToIgnoreCase("previous") == 0)) {
                terms = previous.getPreviousTerms(index, init, fields, maxSize,
                                                                   cleanTokens);
                direction = "previous";
            } else if (direction.compareToIgnoreCase("next") == 0) {
                terms = previous.getNextTerms(index, init, fields, maxSize, 
                                                                   cleanTokens);
                direction = "next";
            } else {
                throw new IOException("invalid direction parameter");
            }

            final JSONObject jobj = new JSONObject();
            final JSONArray jlistTerms = new JSONArray();
            final JSONArray jlistFields = new JSONArray();

            jlistTerms.addAll(terms);
            jlistFields.addAll(fields);
            jobj.put("index", index);
            jobj.put("init", init);
            jobj.put("direction", direction);
            jobj.put("maxTerms", maxSize);
            jobj.put("fields", jlistFields);
            jobj.put("terms", jlistTerms);

            out.println(jobj.toJSONString());

        } catch (Exception ex) {
            logger.catching(Level.ERROR, ex);
            if (out != null) {
                if (verbose == null) {
                    out.println("{}");
                } else {
                    out.println("{Exception:\"" + ex.toString() + "\"}");
                }
            }
        } finally {
            if (out != null) {
                out.close();
            }
        }
}

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Retrieves n next/previous terms starting from a term from a "
                +  "Lucene index";
    }// </editor-fold>
}
