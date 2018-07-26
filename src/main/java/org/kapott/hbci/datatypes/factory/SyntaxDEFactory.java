/*  $Id: SyntaxDEFactory.java,v 1.1 2011/05/04 22:38:01 willuhn Exp $

    This file is part of HBCI4Java
    Copyright (C) 2001-2008  Stefan Palme

    HBCI4Java is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    HBCI4Java is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package org.kapott.hbci.datatypes.factory;

import org.kapott.hbci.datatypes.SyntaxDE;
import org.kapott.hbci.exceptions.NoSuchConstructorException;
import org.kapott.hbci.exceptions.NoSuchSyntaxException;
import org.kapott.hbci.exceptions.ParseErrorException;
import org.kapott.hbci.manager.HBCIUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class SyntaxDEFactory {

    public static SyntaxDE createSyntaxDE(String dataType, String path, Object value, int minsize, int maxsize) {
        // laden der klasse, die die syntax des de enthaelt
        try {
            Class c = Class.forName("org.kapott.hbci.datatypes.Syntax" + dataType, false, SyntaxDEFactory.class.getClassLoader());
            Constructor con = c.getConstructor(new Class[]{value.getClass(), int.class, int.class});
            return (SyntaxDE) (con.newInstance(new Object[]{value, new Integer(minsize), new Integer(maxsize)}));
        } catch (ClassNotFoundException e) {
            throw new NoSuchSyntaxException(dataType, path);
        } catch (NoSuchMethodException e) {
            throw new NoSuchConstructorException(dataType);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new ParseErrorException(HBCIUtils.getLocMsg("EXCMSG_PROT_ERRSYNDE", path), (Exception) e.getCause());
        }
    }
}
