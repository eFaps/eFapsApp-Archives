/*
 * Copyright 2003 - 2014 The eFaps Team
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


package org.efaps.esjp.archives.listener;

import java.util.UUID;

import org.efaps.admin.access.AccessSet;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Role;
import org.efaps.ci.CIAdminAccess;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.listener.ITypedClass;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.erp.listener.IOnCreateDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("50e8ae36-1078-4c97-81be-7ec58c85b245")
@EFapsRevision("$Rev$")
public abstract class OnCreateDocument_Base
    extends AbstractCommon
    implements IOnCreateDocument
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterCreate(final Parameter _parameter,
                            final CreatedDoc _createdDoc)
        throws EFapsException
    {
        if ("true".equalsIgnoreCase(getProperty(_parameter, "Archives_CreateRoot"))) {
            final Insert insertRoot = new Insert(CIArchives.ArchiveRoot);
            String nameKey = getProperty(_parameter, "Archives_RootNameDBProperty");
            if (nameKey == null) {
                nameKey = OnCreateDocument.class.getName() + ".DefaultRootName";
            }
            insertRoot.add(CIArchives.ArchiveRoot.Name,
                            DBProperties.getProperty(nameKey));
            final Status status = Status.find(CIArchives.ArchiveNodeStatus, CIArchives.ArchiveNodeStatus.Editable);
            insertRoot.add(CIArchives.ArchiveRoot.Status, status);
            insertRoot.executeWithoutAccessCheck();

            final String connectype = getProperty(_parameter, "Archives_ConnectType");
            Type connectType;
            if (isUUID(connectype)) {
                connectType = Type.get(UUID.fromString(connectype));
            } else {
                connectType = Type.get(connectype);
            }

            // Connect Root to Project
            final Insert insertRoot2Proj = new Insert(connectType);
            insertRoot2Proj.add(CIArchives.Document2ArchiveAbstract.FromLinkAbstract, _createdDoc.getInstance());
            insertRoot2Proj.add(CIArchives.Object2ArchiveAbstract.ToLinkAbstract, insertRoot.getInstance());
            insertRoot2Proj.executeWithoutAccessCheck();

            if (containsProperty(_parameter, "Archives_Role")) {
                final Role defaultRole;
                final String roleStr = getProperty(_parameter, "Archives_Role");
                if (isUUID(roleStr)) {
                    defaultRole = Role.get(UUID.fromString(roleStr));
                } else {
                    defaultRole = Role.get(roleStr);
                }
                final String accessSet = getProperty(_parameter, "Archives_AccessSet");
                final AccessSet defaultAccessSet;
                if (isUUID(accessSet)) {
                    defaultAccessSet = AccessSet.get(UUID.fromString(accessSet));
                } else {
                    defaultAccessSet = AccessSet.get(accessSet);
                }

                final Insert insert = new Insert(CIAdminAccess.Access4Object);
                insert.add(CIAdminAccess.Access4Object.TypeId, insertRoot.getInstance().getType().getId());
                insert.add(CIAdminAccess.Access4Object.ObjectId, insertRoot.getInstance().getId());
                insert.add(CIAdminAccess.Access4Object.PersonLink, defaultRole.getId());
                insert.add(CIAdminAccess.Access4Object.AccessSetLink, defaultAccessSet.getId());
                insert.executeWithoutAccessCheck();
            }
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
