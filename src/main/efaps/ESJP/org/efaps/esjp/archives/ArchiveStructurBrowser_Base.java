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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.EventExecution;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser;
import org.efaps.ui.wicket.models.objects.UIStructurBrowser.ExecutionStatus;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO description!
 * 
 * @author The eFasp Team
 * @version $Id: TreeViewStructurBrowser_Base.java 5979 2010-12-23 03:37:33Z
 *          jan@moxter.net $
 */
@EFapsUUID("6197af7c-2b19-40e8-8a7c-abaf0b329ac4")
@EFapsRevision("$Rev$")
public abstract class ArchiveStructurBrowser_Base
    implements EventExecution
{
    /**
     * Logger for this class.
     */
    protected final Logger LOG = LoggerFactory.getLogger(ArchiveStructurBrowser_Base.class);

    /**
     * @param _parameter Parameter
     * @throws EFapsException on error
     * @return Return
     */
    @Override
    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = null;

        final UIStructurBrowser strBro = (UIStructurBrowser) _parameter.get(ParameterValues.CLASS);
        final ExecutionStatus status = strBro.getExecutionStatus();
        if (status.equals(ExecutionStatus.EXECUTE)) {
            ret = internalExecute(_parameter);
        } else if (status.equals(ExecutionStatus.ALLOWSCHILDREN)) {
            ret = allowChildren(_parameter);
        } else if (status.equals(ExecutionStatus.CHECKFORCHILDREN)) {
            ret = checkForChildren(_parameter);
        } else if (status.equals(ExecutionStatus.ADDCHILDREN)) {
            ret = addChildren(_parameter);
        } else if (status.equals(ExecutionStatus.SORT)) {
            ret = sort(_parameter);
        }
        return ret;
    }

    /**
     * Method to get a list of instances the StructurBrowser will be filled
     * with.
     * 
     * @param _parameter as passed from eFaps API.
     * @return Return with instances
     * @throws EFapsException on error
     */
    protected Return internalExecute(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Instance, Boolean> tree = new LinkedHashMap<Instance, Boolean>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String typesStr = (String) properties.get("Types");
        final String[] types = typesStr.split(";");
        for (final String type : types) {
            final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
            final boolean checked = "true".equals(properties.get("checkStructure")) ? true : false;
            if (checked) {
                final String type2Structure = (String) properties.get("Type2Structure");
                final QueryBuilder queryBldr2 = new QueryBuilder(Type.get(type2Structure));
                queryBldr2.addWhereAttrEqValue("FromLink", _parameter.getInstance().getId());
                final MultiPrintQuery multi = queryBldr2.getPrint();
                final SelectBuilder selID = new SelectBuilder().linkto("ToLink").attribute("ID");
                multi.addSelect(selID);
                multi.execute();
                if (multi.getInstanceList().size() == 0) {
                    queryBldr.addWhereAttrEqValue("ID", Long.parseLong("0"));
                } else {
                    queryBldr.setOr(true);
                    while (multi.next()) {
                        queryBldr.addWhereAttrEqValue("ID", multi.<Long>getSelect(selID));
                    }
                }
            }
            final InstanceQuery query = queryBldr.getQuery();
            query.execute();
            while (query.next()) {
                tree.put(query.getCurrentValue(), null);
            }
        }
        ret.put(ReturnValues.VALUES, tree);
        return ret;
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

    /**
     * Method to check if an instance has children. It is used in the tree to
     * determine if a "plus" to open the children must be rendered.
     * 
     * @param _parameter Parameter as passed from the eFaps API
     * @return Return with true or false
     * @throws EFapsException on error
     */
    protected Return checkForChildren(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Instance, Boolean> map = getChildren(_parameter, true);
        if (!map.isEmpty()) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * @param _parameter Parameter as passed from the eFaps API
     * @param _check check only?
     * @return map with instances
     * @throws EFapsException on error
     */
    protected Map<Instance, Boolean> getChildren(final Parameter _parameter,
                                                 final boolean _check)
        throws EFapsException
    {
        final Map<Instance, Boolean> ret = new LinkedHashMap<Instance, Boolean>();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String productTypesStr = (String) properties.get("ArchiveTypes");
        if (productTypesStr != null && !productTypesStr.isEmpty()) {
            final String[] productTypes = productTypesStr.split(";");
            final List<Long> typeIds = new ArrayList<Long>();
            for (final String productType : productTypes) {
                typeIds.add(Type.get(productType).getId());
            }
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIArchives.ArchiveFile);
            attrQueryBldr.addWhereAttrEqValue(CIArchives.ArchiveFile.Type, typeIds.toArray());
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIArchives.ArchiveFile.ID);
            final QueryBuilder queryBldr = new QueryBuilder(CIArchives.ArchiveFile);
            queryBldr.addWhereAttrEqValue(CIArchives.ArchiveFile.ParentLink, _parameter.getInstance().getId());
            queryBldr.addWhereAttrInQuery(CIArchives.ArchiveFile.ArchiveLink, attrQuery);
            final InstanceQuery query = queryBldr.getQuery();
            if (_check) {
                query.setLimit(1);
            }
            query.execute();
            while (query.next()) {
                ret.put(query.getCurrentValue(), null);
            }

            if (!_check || (_check && ret.isEmpty())) {
                final QueryBuilder queryBldr2 = new QueryBuilder(CIArchives.ArchiveNode);
                queryBldr2.addWhereAttrEqValue(CIArchives.ArchiveNode.ParentLink, _parameter.getInstance().getId());
                queryBldr2.addWhereAttrIsNull(CIArchives.ArchiveNode.ArchiveLink);
                final InstanceQuery query2 = queryBldr2.getQuery();
                query2.execute();
                while (query2.next()) {
                    ret.put(query2.getCurrentValue(), null);
                }
            }
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIArchives.ArchiveNode);
            queryBldr.addWhereAttrEqValue(CIArchives.ArchiveNode.ParentLink, _parameter.getInstance().getId());
            final InstanceQuery query = queryBldr.getQuery();
            if (_check) {
                query.setLimit(1);
            }
            query.execute();
            while (query.next()) {
                ret.put(query.getCurrentValue(), null);
            }
        }
        return ret;
    }

    /**
     * Method to add the children to an instance. It is used to expand the
     * children of a node in the tree.
     * 
     * @param _parameter Paraemter as passed from the eFasp API
     * @return Return with instances
     * @throws EFapsException on error
     */
    protected Return addChildren(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<Instance, Boolean> map = getChildren(_parameter, false);
        ret.put(ReturnValues.VALUES, map);
        return ret;
    }

    /**
     * Method to sort the values of the StructurBrowser.
     * 
     * @param _parameter _sructurBrowser to be sorted
     * @return empty Return;
     */
    protected Return sort(final Parameter _parameter)
    {
        final UIStructurBrowser strBro = (UIStructurBrowser) _parameter.get(ParameterValues.CLASS);

        Collections.sort(strBro.getChilds(), new Comparator<UIStructurBrowser>() {

            @Override
            public int compare(final UIStructurBrowser _structurBrowser1,
                               final UIStructurBrowser _structurBrowser2)
            {
                final String value1 = getSortString(_structurBrowser1);
                final String value2 = getSortString(_structurBrowser2);
                return value1.compareTo(value2);
            }

            protected String getSortString(final UIStructurBrowser _structurBrowser)
            {
                final StringBuilder ret = new StringBuilder();
                try {
                    if (_structurBrowser.getInstance() != null) {
                        _structurBrowser.getInstance().getType();
                    }
                } catch (final EFapsException e) {
                    ArchiveStructurBrowser_Base.this.LOG.error("error during sorting", e);
                }
                ret.append(_structurBrowser.getLabel());
                return ret.toString();
            }
        });
        return new Return();
    }
}
