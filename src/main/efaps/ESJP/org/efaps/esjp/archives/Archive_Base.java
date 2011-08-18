/*
 * Copyright 2003 - 2011 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.archives;

import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.util.EFapsException;

/**
 * TODO description!
 * 
 * @author The eFasp Team
 * @version $Id: TreeViewStructurBrowser_Base.java 5979 2010-12-23 03:37:33Z
 *          jan@moxter.net $
 */
@EFapsUUID("04972bca-38fa-41f3-b160-cafdce4b51cd")
@EFapsRevision("$Rev$")
public abstract class Archive_Base
{
    /**
     * Create a new Archive.
     * 
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create() {

            @Override
            public Instance basicInsert(final Parameter _parameter)
                throws EFapsException
            {
                final Type type = Type.get(Long.parseLong(_parameter.getParameterValue("type")));

                final Insert insert = new Insert(type);
                insert.add(CIArchives.ArchiveNode.Name, _parameter.getParameterValue("name"));
                insert.add(CIArchives.ArchiveNode.ParentLink, _parameter.getInstance().getId());
                insert.add(CIArchives.ArchiveNode.Description, _parameter.getParameterValue("description"));
                insert.add(CIArchives.ArchiveNode.Date, _parameter.getParameterValue("date"));
                insert.execute();
                return insert.getInstance();
            }
        };
        return create.execute(_parameter);
    }

    /**
     * Create a Root Folder.
     * 
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createRoot(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create();
        final Instance instance = create.basicInsert(_parameter);
        create.connect(_parameter, instance);
        return new Return();
    }

    public Return checkAccess2ArchiveRoot(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = (String) properties.get("Type");
        final boolean checked = "true".equals(properties.get("activeAccessCheck")) ? true : false;
        if (!checked) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            if (type != null && !type.isEmpty()) {
                final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
                if (properties.containsKey("AttributeLink")) {
                    queryBldr.addWhereAttrEqValue((String) properties.get("AttributeLink"), _parameter.getInstance().getId());
                }
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.execute();
                if (multi.getInstanceList().size() == 0) {
                    ret.put(ReturnValues.TRUE, true);
                }
            }
        }

        return ret;
    }

    public Return createFromZip(final Parameter _parameter)
        throws EFapsException
    {
        return new Return();
    }
}
