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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Heitor Barbieri
 * 20121129
 */
@WebServlet(name = "PreviousTermServlet", urlPatterns = 
                                                       {"/PreviousTermServlet"})
public class PreviousTermServlet extends HttpServlet {

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
        final String sdir = servletConfig.getInitParameter("INDEX_DIR");
        if (sdir == null) {
            throw new ServletException("missing index directory (INDEX_DIR) "
                                                                + "parameter.");
        }
        final String maxTerms = servletConfig.getInitParameter("MAX_TERMS");
        if (maxTerms == null) {
            throw new ServletException("missing maximum number of returned "
                                              + "terms (MAX_TERMS) parameter.");
        }
        final String fields = servletConfig.getInitParameter("DOC_FIELDS");
        if (fields == null) {
            throw new ServletException("missing document fields (DOC_FIELDS) "
                                                                + "parameter.");
        }

        try {
            previous = new PreviousTerm(new File(sdir),
                                        Arrays.asList(fields.split("[,;\\-]")),
                                        Integer.parseInt(maxTerms));
        } catch (Exception ex) {
            throw new ServletException(ex);
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
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, 
                                   HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json; charset=UTF-8");
        final PrintWriter out = response.getWriter();

        try {
            final String init = request.getParameter("init");
            if (init == null) {
                throw new ServletException("missing 'init' parameter");
            }
                                    
            int maxSize = previous.getMaxSize();
            String maxTerms = request.getParameter("maxTerms");
            if (maxTerms != null) {
                maxSize = Integer.parseInt(maxTerms);
            }
            
            List<String> fields = previous.getFields();
            String fldsParam = request.getParameter("fields");
            if (fldsParam != null) {
                fields = Arrays.asList(fldsParam.split("[,;\\-]"));
            }

            final List<String> terms;
            String direction = request.getParameter("direction");
            if ((direction == null) ||
                (direction.compareToIgnoreCase("next") == 0)) {
                terms = previous.getNextTerms(init, fields, maxSize);
                direction = "next";
            } else {
                terms = previous.getPreviousTerms(init, fields, maxSize);
                direction = "previous";
            }

            final JSONObject jobj = new JSONObject();
            final JSONArray jlistTerms = new JSONArray();
            final JSONArray jlistFields = new JSONArray();

            jlistTerms.addAll(terms);
            jlistFields.addAll(fields);
            jobj.put("init", init);
            jobj.put("direction", direction);
            jobj.put("maxTerms", maxSize);
            jobj.put("fields", jlistFields);
            jobj.put("terms", jlistTerms);

            out.println(jobj.toJSONString());
        } finally {
            out.close();
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
