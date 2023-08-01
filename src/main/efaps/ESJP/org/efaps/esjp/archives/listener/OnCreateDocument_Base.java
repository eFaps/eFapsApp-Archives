/*
 * Copyright 2003 - 2015 The eFaps Team
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
 */

package org.efaps.esjp.archives.listener;

import java.util.Properties;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.archives.Archive;
import org.efaps.esjp.archives.util.Archives;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("50e8ae36-1078-4c97-81be-7ec58c85b245")
@EFapsApplication("eFapsApp-Archives")
public abstract class OnCreateDocument_Base
    extends AbstractCommon
    implements IOnCreateDocument
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCreate(final Parameter _parameter,
                            final Instance instance)
        throws EFapsException
    {
        final Parameter parameter = ParameterUtil.clone(_parameter);
        parameter.put(ParameterValues.INSTANCE, instance);

        final String typeName = instance.getType().getName();
        final Properties properties = Archives.OBJ2ARCHCONFIG.get();

        // if one of them has the config
        if (properties.containsKey(typeName + ".CreateRoot")
                        || "true".equalsIgnoreCase(getProperty(parameter, "Archives_CreateRoot"))) {

            final Insert insertRoot = new Insert(CIArchives.ArchiveRoot);
            final String name;
            if (properties.containsKey(typeName + ".RootName")) {
                name = properties.getProperty(typeName + ".RootName");
            } else if (containsProperty(_parameter, "Archives_RootNameDBProperty")) {
                name = DBProperties.getProperty(getProperty(_parameter, "Archives_RootNameDBProperty"));
            } else {
                name = DBProperties.getProperty(OnCreateDocument.class.getName() + ".DefaultRootName");
            }
            insertRoot.add(CIArchives.ArchiveRoot.Name, name);
            insertRoot.add(CIArchives.ArchiveRoot.Status, Status.find(CIArchives.ArchiveNodeStatus.Editable));
            insertRoot.executeWithoutAccessCheck();

            final Type connectType = new Archive().getObject2ArchiveType(parameter);

            // Connect Root to Project
            final Insert insertRoot2Proj = new Insert(connectType);
            if (containsProperty(parameter, "Archives_ConnectParentAttribute")) {
                insertRoot2Proj.add(getProperty(parameter, "Archives_ConnectParentAttribute"),
                                instance);
            } else {
                insertRoot2Proj.add(CIArchives.Document2ArchiveAbstract.FromLinkAbstract, parameter.getInstance());
            }
            insertRoot2Proj.add(CIArchives.Object2ArchiveAbstract.ToLinkAbstract, insertRoot.getInstance());
            insertRoot2Proj.executeWithoutAccessCheck();

            new Archive().addDefaultRole(parameter, instance, insertRoot.getInstance());
            new Archive().addDefaultFolders(_parameter, instance, insertRoot.getInstance());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CharSequence getJavaScript4Doc(final ITypedClass _typeClass,
                                          final Parameter _parameter)
        throws EFapsException
    {
        // not used in this implementation
        return "";
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
