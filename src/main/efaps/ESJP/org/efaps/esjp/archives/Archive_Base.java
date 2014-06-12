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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.efaps.admin.access.AccessSet;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Role;
import org.efaps.ci.CIAdminAccess;
import org.efaps.ci.CIAdminCommon;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.archives.util.Archives;
import org.efaps.esjp.archives.util.ArchivesSettings;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.file.FileUtil;
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
    extends AbstractCommon
{
    /**
     * The Date value for the table view. On folders an empty string will be
     * presented instead of a default value (current date).
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return getDateFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (!_parameter.getInstance().getType().isKindOf(CIArchives.ArchiveFileAbstract.getType())) {
            ret.put(ReturnValues.SNIPLETT, " ");
        }
        return ret;
    }

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
                if (!type.isKindOf(CIArchives.ArchiveFileAbstract.getType())) {
                    insert.add(CIArchives.ArchiveNode.Name, _parameter.getParameterValue("name"));
                }
                insert.add(CIArchives.ArchiveNode.ParentLink, _parameter.getInstance().getId());
                insert.add(CIArchives.ArchiveNode.Description, _parameter.getParameterValue("description"));
                insert.add(CIArchives.ArchiveNode.Date, _parameter.getParameterValue("date"));
                Status status = null;
                if (getProperty(_parameter, "StatusGroup") != null && getProperty(_parameter, "Status") != null) {
                    status = Status.find(getProperty(_parameter, "StatusGroup"), getProperty(_parameter, "Status"));
                }
                if (status != null) {
                    insert.add(CIArchives.ArchiveNode.StatusAbstract, status.getId());
                }
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
        addDefaultRole(_parameter, instance);
        return new Return();
    }

    protected void addDefaultRole(final Parameter _parameter,
                                  final Instance _instance)
        throws EFapsException
    {
        if (getProperty(_parameter, "DefaultRole") != null && getProperty(_parameter, "DefaultAccessSet") != null) {
            final Role defaultRole = Role.get(getProperty(_parameter, "DefaultRole"));
            final AccessSet defaultAccessSet = AccessSet.get(getProperty(_parameter, "DefaultAccessSet"));

            final Insert insert = new Insert(CIAdminAccess.Access4Object);
            insert.add(CIAdminAccess.Access4Object.TypeId, _instance.getType().getId());
            insert.add(CIAdminAccess.Access4Object.ObjectId, _instance.getId());
            insert.add(CIAdminAccess.Access4Object.PersonLink, defaultRole.getId());
            insert.add(CIAdminAccess.Access4Object.AccessSetLink, defaultAccessSet.getId());
            insert.executeWithoutAccessCheck();
        }
    }

    /**
     * Move files and folders to another folder.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */

    public Return move(final Parameter _parameter)
        throws EFapsException
    {
        String[] oids = new String[0];
        if (Context.getThreadContext().containsSessionAttribute("archiveOID")
                        && Context.getThreadContext().getSessionAttribute("archiveOID") != null) {
            oids = (String[]) Context.getThreadContext().getSessionAttribute("archiveOID");
            Context.getThreadContext().setSessionAttribute("archiveOID", null);
        }
        final String sel = _parameter.getParameterValue("selectedRow");
        if (oids.length > 0 && sel != null && !sel.isEmpty()) {
            for (final String oid : oids) {
                final Instance instance = Instance.get(oid);
                if (instance.getType().isKindOf(CIArchives.ArchiveNode.getType())) {
                    final Update update = new Update(Instance.get(oid));
                    update.add(CIArchives.ArchiveNode.ParentLink, Instance.get(sel).getId());
                    update.execute();
                }
            }
        }
        return new Return();
    }

    /**
     * Create a node.
     *
     * @param _parameter Parameter as passed by the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return createMultiple(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();

        String[] oids = new String[0];
        if (Context.getThreadContext().containsSessionAttribute("archiveOID")
                        && Context.getThreadContext().getSessionAttribute("archiveOID") != null) {
            oids = (String[]) Context.getThreadContext().getSessionAttribute("archiveOID");
            Context.getThreadContext().setSessionAttribute("archiveOID", null);
        }

        if (oids.length > 0) {
            final Instance instance = Instance.get(oids[0]);
            if (CIArchives.ArchiveRoot.getType().isKindOf(instance.getType())
                            || CIArchives.ArchiveNode.getType().isKindOf(instance.getType())) {
                _parameter.put(ParameterValues.INSTANCE, instance);
                if (getProperty(_parameter, "CreateFromType") != null) {
                    if ("Node".equals(getProperty(_parameter, "CreateFromType"))) {
                        ret = new Create().execute(_parameter);
                    } else if ("File".equals(getProperty(_parameter, "CreateFromType"))) {
                        ret = create(_parameter);
                    } else if ("Zip".equals(getProperty(_parameter, "CreateFromType"))) {
                        ret = createFromZip(_parameter);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Edit filename.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return editFileName(final Parameter _parameter)
        throws EFapsException
    {
        final Instance instance = _parameter.getInstance();
        final String name = _parameter.getParameterValue("name");
        if (name != null && !name.isEmpty()) {
            //GeneralStoreAbstract
            final QueryBuilder queryBuilder = new QueryBuilder(UUID.fromString("5248c84b-2724-4c46-afd3-b937959a74d7"));
            queryBuilder.addWhereAttrEqValue(CIAdminCommon.GeneralInstance.InstanceID, instance.getId());
            queryBuilder.addWhereAttrEqValue(CIAdminCommon.GeneralInstance.InstanceTypeID, instance.getType().getId());
            final InstanceQuery query = queryBuilder.getQuery();
            query.execute();
            if (query.next()) {
                final Instance genInst = query.getCurrentValue();
                final Update update = new Update(genInst);
                update.add("FileName", name);
                update.execute();
            }
        }
        return new Return();
    }

    /**
     * Check if access will be granted to the cmd to create a root node.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return checkAccess2ArchiveRoot(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String type = getProperty(_parameter, "Type");
        final boolean checked = "true".equalsIgnoreCase(getProperty(_parameter, "activeAccessCheck"));
        if (!checked) {
            ret.put(ReturnValues.TRUE, true);
        } else {
            if (type != null && !type.isEmpty()) {
                final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
                if (getProperty(_parameter, "AttributeLink") != null) {
                    queryBldr.addWhereAttrEqValue(getProperty(_parameter, "AttributeLink"), _parameter.getInstance());
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

    /**
     * Check if access will be granted to the cmd to create a root node.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return createFromZip(final Parameter _parameter)
        throws EFapsException
    {
        final Context.FileParameter fileItem = Context.getThreadContext().getFileParameters().get("upload");
        if (fileItem != null) {
            try {
                final InputStream input = fileItem.getInputStream();
                final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(input));
                ZipEntry entry;
                final FileUtil fileUtil = new FileUtil();
                final List<File> files = new ArrayList<File>();
                while ((entry = zis.getNextEntry()) != null) {
                    int size;
                    final byte[] buffer = new byte[2048];
                    final File file = fileUtil.getFile(entry.getName());
                    if (!file.isHidden()) {
                        files.add(file);
                        final FileOutputStream fos = new FileOutputStream(file);
                        final BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
                        while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
                            bos.write(buffer, 0, size);
                        }
                        bos.flush();
                        bos.close();
                    }
                }
                for (final File file : files) {
                    if (file.length() > 0) {
                        final Context.FileParameter fileTmp = new FileItem(file);
                        Context.getThreadContext().getFileParameters().put("upload", fileTmp);
                        _parameter.getParameters().put("name", new String[] { fileTmp.getName() });
                        create(_parameter);
                    }
                }
            } catch (final IOException e) {
                throw new EFapsException(Archive_Base.class, "createFromZip", e);
            }
        }
        return new Return();
    }

    public void createFileStructure(final Parameter _parameter,
                                    final Instance _instance)
        throws EFapsException
    {
        addSystemConfiguration(_parameter);
        final Map<Integer, String> folders = analyseProperty(_parameter, "folder");
        if(!folders.isEmpty()) {
            for (final Entry<Integer, String> folder : folders.entrySet()) {
                insertChildNode(_instance, folder.getValue());
            }
        }
    }

    protected void addSystemConfiguration(final Parameter _parameter)
        throws EFapsException
    {
        final Properties props = Archives.getSysConfig().getAttributeValueAsProperties(ArchivesSettings.FILE_STRUCTURE);
        _parameter.put(ParameterValues.PROPERTIES, props);
    }

    protected void insertChildNode(final Instance _parent,
                                   final String _folder)
        throws EFapsException
    {
        final Insert insert = new Insert(CIArchives.ArchiveNode);
        insert.add(CIArchives.ArchiveNode.ParentLink, _parent);
        insert.add(CIArchives.ArchiveRoot.Status, Status.find(CIArchives.ArchiveNodeStatus.Editable));
        if (_folder.contains("/")) {
            final Instance insNode;
            final String[] tmp = _folder.split("/");
            final InstanceQuery insQuery = getQuery4Node(_parent, tmp[0]);
            if (insQuery.next()) {
                insNode = insQuery.getCurrentValue();
            } else {
                insert.add(CIArchives.ArchiveNode.Name, tmp[0]);
                insert.execute();
                insNode = insert.getInstance();
            }
            insertChildNode(insNode, _folder.substring(_folder.indexOf("/") + 1));
        } else {
            if (getQuery4Node(_parent, _folder).getValues().isEmpty()) {
                insert.add(CIArchives.ArchiveNode.Name, _folder);
                insert.execute();
            }
        }
    }

    protected InstanceQuery getQuery4Node(final Instance _instance,
                                          final String _search)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIArchives.ArchiveNode);
        queryBldr.addWhereAttrEqValue(CIArchives.ArchiveNode.ParentLink, _instance);
        queryBldr.addWhereAttrEqValue(CIArchives.ArchiveNode.Name, _search);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        return query;
    }

    public class FileItem
        implements Context.FileParameter
    {
        /**
         * File this item belongs to.
         */
        private final File file;

        /**
         * @param _file
         */
        public FileItem(final File _file)
        {
            this.file = _file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close()
            throws IOException
        {
            // nothing must be done
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream()
            throws IOException
        {
            return new FileInputStream(this.file);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getSize()
        {
            return this.file.length();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getContentType()
        {
            return "";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName()
        {
            return this.file.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getParameterName()
        {
            return "upload";
        }
    }
}
