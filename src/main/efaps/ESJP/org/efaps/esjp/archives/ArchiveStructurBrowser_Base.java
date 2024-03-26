/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.archives;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.ui.structurbrowser.StandartStructurBrowser;
import org.efaps.ui.wicket.models.field.AbstractUIField;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser;
import org.efaps.util.EFapsException;

/**
 * TODO description!
 *
 * @author The eFasp Team
 */
@EFapsUUID("6197af7c-2b19-40e8-8a7c-abaf0b329ac4")
@EFapsApplication("eFapsApp-Archives")
public abstract class ArchiveStructurBrowser_Base
    extends StandartStructurBrowser
{
    /**
     * Add additional Criteria to the QueryBuilder.
     * To be used by implementation.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _queryBldr QueryBuilder the criteria will be added to
     * @throws EFapsException on error
     */
    @Override
    protected void addCriteria(final Parameter _parameter,
                               final QueryBuilder _queryBldr)
        throws EFapsException
    {
        final Type type = new Archive().getObject2ArchiveType(_parameter);
        if (type != null) {
            final QueryBuilder queryBldr = new QueryBuilder(type);
            queryBldr.addWhereAttrEqValue("FromLink", _parameter.getInstance());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selInst = new SelectBuilder().linkto("ToLink").instance();
            multi.addSelect(selInst);
            multi.execute();
            if (multi.getInstanceList().size() == 0) {
                _queryBldr.addWhereAttrEqValue("ID", Long.parseLong("0"));
            } else {
                _queryBldr.setOr(true);
                while (multi.next()) {
                    _queryBldr.addWhereAttrEqValue("ID", multi.<Instance>getSelect(selInst));
                }
            }
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIArchives.Object2ArchiveAbstract);
            _queryBldr.addWhereAttrNotInQuery(CIArchives.ArchiveAbstract.ID,
                            queryBldr.getAttributeQuery(CIArchives.Object2ArchiveAbstract.ToLinkAbstract));
        }
    }

    /**
     * Method to check if an instance allows children. It is used in the tree to
     * determine "folder" or an "item" must be rendered and if the
     * checkForChildren method must be executed.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return with true or false
     * @throws EFapsException on error
     */
    @Override
    protected Return allowChildren(final Parameter _parameter)
    {
        final Return ret = new Return();
        final Instance inst = _parameter.getInstance();
        if (inst != null && inst.isValid()) {
            if (!inst.getType().isKindOf(CIArchives.ArchiveFile.getType())) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    @Override
    protected Return checkHideColumn4Row(final Parameter _parameter)
        throws EFapsException
    {
        final UIStructurBrowser strBrws = (UIStructurBrowser) _parameter.get(ParameterValues.CLASS);
        for (final AbstractUIField uiField : strBrws.getColumns()) {
            if (strBrws.isAllowChildren() && !strBrws.isBrowserField(uiField)
                            && uiField.getFieldConfiguration().getName().equals("checkout")) {
                uiField.setHide(true);
            }
        }
        return new Return();
    }
}
