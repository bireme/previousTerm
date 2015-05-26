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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Heitor Barbieri
 * date: 20150525
 */
public class MongoIndexInfo {    
    final String name;
    final String path;
    final Set<String> fields; 
    
    public MongoIndexInfo(final String name, 
                          final String path, 
                          final String... fields) {
        this.name = name;
        this.path = path;
        this.fields = new HashSet<String>(Arrays.asList(fields));
    }
}
