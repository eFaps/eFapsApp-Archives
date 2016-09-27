/*
 * Copyright 2003 - 2016 The eFaps Team
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

import org.apache.commons.lang3.BooleanUtils;
import org.efaps.admin.access.AccessSet;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
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
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.AbstractCommon;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.util.EFapsException;

/**
 * TODO description!
 *
 * @author The eFasp Team
 */
@EFapsUUID("04972bca-38fa-41f3-b160-cafdce4b51cd")
@EFapsApplication("eFapsApp-Archives")
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
                insert.add(CIArchives.ArchiveNode.ParentLink, _parameter.getInstance());
                insert.add(CIArchives.ArchiveNode.Description, _parameter.getParameterValue("description"));
                insert.add(CIArchives.ArchiveNode.Date, _parameter.getParameterValue("date"));
                Status status = null;
                if (getProperty(_parameter, "StatusGroup") != null && getProperty(_parameter, "Status") != null) {
                    status = Status.find(getProperty(_parameter, "StatusGroup"), getProperty(_parameter, "Status"));
                }
                if (status != null) {
                    insert.add(CIArchives.ArchiveNode.StatusAbstract, status);
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
        if (!containsProperty(_parameter, "ConnectType") && containsProperty(_parameter, "ConnectChildAttribute")) {
            final Type type = getObject2ArchiveType(_parameter);
            if (type != null) {
                ParameterUtil.setProperty(_parameter, "ConnectType", type.getName());
            }
        }

        final Instance instance = create.basicInsert(_parameter);
        create.connect(_parameter, instance);
        addDefaultRole(_parameter,  _parameter.getInstance(), instance);
        addDefaultFolders(_parameter,  _parameter.getInstance(), instance);
        return new Return();
    }

    /**
     * Root name field value.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return rootNameFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            final Properties properties = Archives.OBJ2ARCHCONFIG.get();
            final String typeName = _parameter.getInstance().getType().getName();
            String name = null;
            if (properties.containsKey(typeName + ".RootName")) {
                name = properties.getProperty(typeName + ".RootName");
            } else if (containsProperty(_parameter, "Archives_RootNameDBProperty")) {
                name = DBProperties.getProperty(getProperty(_parameter, "Archives_RootNameDBProperty"));
            }
            if (name != null) {
                ret.put(ReturnValues.VALUES, name);
            }
        }
        return ret;
    }

    /**
     * Adds the default role.
     *
     * @param _parameter the _parameter
     * @param _objectInstance the _object instance
     * @param _rootInstance the _root instance
     * @throws EFapsException the e faps exception
     */
    public void addDefaultRole(final Parameter _parameter,
                               final Instance _objectInstance,
                               final Instance _rootInstance)
        throws EFapsException
    {
        String roleStr = null;
        String accessSetStr = null;

        final Properties properties = Archives.OBJ2ARCHCONFIG.get();
        if (properties.containsKey(_parameter.getInstance().getType().getName() + ".DefaultRole")
                        && properties.containsKey(_parameter.getInstance().getType().getName() + ".DefaultAccessSet")) {
            roleStr = properties.getProperty(_parameter.getInstance().getType().getName() + ".DefaultRole");
            accessSetStr = properties.getProperty(_parameter.getInstance().getType().getName() + ".DefaultAccessSet");
        } else if (containsProperty(_parameter, "DefaultRole") && containsProperty(_parameter, "DefaultAccessSet")) {
            roleStr = getProperty(_parameter, "DefaultRole");
            accessSetStr = getProperty(_parameter, "DefaultAccessSet");
        } else if (containsProperty(_parameter, "Archives_Role")
                        && containsProperty(_parameter, "Archives_AccessSet")) {
            roleStr = getProperty(_parameter, "Archives_Role");
            accessSetStr = getProperty(_parameter, "Archives_AccessSet");
        }
        if (roleStr != null && accessSetStr != null) {
            final Role defaultRole = isUUID(roleStr) ? Role.get(UUID.fromString(roleStr)) : Role.get(roleStr);
            final AccessSet defaultAccessSet = isUUID(accessSetStr) ? AccessSet.get(UUID.fromString(accessSetStr))
                            : AccessSet.get(accessSetStr);

            final Insert insert = new Insert(CIAdminAccess.Access4Object);
            insert.add(CIAdminAccess.Access4Object.TypeId, _rootInstance.getType().getId());
            insert.add(CIAdminAccess.Access4Object.ObjectId, _rootInstance.getId());
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

        final Type type = getObject2ArchiveType(_parameter);
        if (type != null) {
            final boolean inverse = BooleanUtils.toBoolean(getProperty(_parameter, "Inverse"));
            final QueryBuilder queryBldr = new QueryBuilder(type);
            if (getProperty(_parameter, "AttributeLink") != null) {
                queryBldr.addWhereAttrEqValue(getProperty(_parameter, "AttributeLink"), _parameter.getInstance());
            } else {
                queryBldr.addWhereAttrEqValue("FromLink", _parameter.getInstance());
            }
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.execute();
            final boolean access = multi.getInstanceList().size() == 0;

            if (!inverse && access || inverse && !access) {
                ret.put(ReturnValues.TRUE, true);
            }
        }
        return ret;
    }

    /**
     * Check access4 object2 archive.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return checkAccess4Object2Archive(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final boolean inverse = BooleanUtils.toBoolean(getProperty(_parameter, "Inverse"));
        final Type type = getObject2ArchiveType(_parameter);
        final boolean access = type != null;
        if (!inverse && access || inverse && !access) {
            ret.put(ReturnValues.TRUE, true);
        }
        return ret;
    }

    /**
     * Gets the object2 archive type.
     *
     * @param _parameter the _parameter
     * @return the object2 archive type
     * @throws EFapsException the e faps exception
     */
    public Type getObject2ArchiveType(final Parameter _parameter)
        throws EFapsException
    {
        Type ret = null;
        String typeStr = null;
        final Properties properties = Archives.OBJ2ARCHCONFIG.get();
        if (InstanceUtils.isValid(_parameter.getInstance()) && properties.containsKey(_parameter.getInstance().getType()
                        .getName() + ".Object2ArchiveType")) {
            typeStr = properties.getProperty(_parameter.getInstance().getType().getName() + ".Object2ArchiveType");
        } else if (InstanceUtils.isValid(_parameter.getCallInstance()) && properties.containsKey(_parameter
                        .getCallInstance().getType().getName() + ".Object2ArchiveType")) {
            typeStr = properties.getProperty(_parameter.getCallInstance().getType().getName() + ".Object2ArchiveType");
        } else if (containsProperty(_parameter, "Object2ArchiveType")) {
            typeStr = getProperty(_parameter, "Object2ArchiveType");
        } else if (containsProperty(_parameter, "Archives_ConnectType")) {
            typeStr = getProperty(_parameter, "Archives_ConnectType");
        }
        if (typeStr != null) {
            ret = isUUID(typeStr) ? Type.get(UUID.fromString(typeStr)) : Type.get(typeStr);
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
                final List<File> files = new ArrayList<>();
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

    /**
     * Creates the file structure.
     *
     * @param _parameter the _parameter
     * @param _objectInstance the _object instance
     * @param _rootFolderInstance the _root folder instance
     * @throws EFapsException the e faps exception
     */
    public void addDefaultFolders(final Parameter _parameter,
                                  final Instance _objectInstance,
                                  final Instance _rootFolderInstance)
                                      throws EFapsException
    {
        final Properties properties = Archives.OBJ2ARCHCONFIG.get();
        final Map<Integer, String> folders = analyseProperty(_parameter, properties,
                        _objectInstance.getType().getName() + ".Folder");
        if (folders.isEmpty()) {
            folders.putAll(analyseProperty(_parameter, "Archives_Folder"));
        }
        if (!folders.isEmpty()) {
            for (final Entry<Integer, String> folder : folders.entrySet()) {
                insertChildNode(_rootFolderInstance, folder.getValue());
            }
        }
    }

    /**
     * Insert child node.
     *
     * @param _parentNodeInstance the _parent node instance
     * @param _folderDefinition the _folder definition
     * @throws EFapsException the eFaps exception
     */
    protected void insertChildNode(final Instance _parentNodeInstance,
                                   final String _folderDefinition)
        throws EFapsException
    {
        String folderName;
        String subFolderDef;
        if (_folderDefinition.contains("/")) {
            final String[] tmp = _folderDefinition.split("/");
            folderName = tmp[0];
            subFolderDef = _folderDefinition.substring(_folderDefinition.indexOf("/") + 1);
        } else {
            folderName = _folderDefinition;
            subFolderDef = null;
        }
        final QueryBuilder queryBldr = new QueryBuilder(CIArchives.ArchiveNode);
        queryBldr.addWhereAttrEqValue(CIArchives.ArchiveNode.ParentLink, _parentNodeInstance);
        queryBldr.addWhereAttrEqValue(CIArchives.ArchiveNode.Name, folderName);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        final Instance childInstance;
        if (query.next()) {
            childInstance = query.getCurrentValue();
        } else {
            final Insert insert = new Insert(CIArchives.ArchiveNode);
            insert.add(CIArchives.ArchiveNode.ParentLink, _parentNodeInstance);
            insert.add(CIArchives.ArchiveRoot.Status, Status.find(CIArchives.ArchiveNodeStatus.Editable));
            insert.add(CIArchives.ArchiveNode.Name, folderName);
            insert.execute();
            childInstance = insert.getInstance();
        }
        if (subFolderDef != null) {
            insertChildNode(childInstance, subFolderDef);
        }
    }

    /**
     * The Class FileItem.
     */
    public class FileItem
        implements Context.FileParameter
    {
        /**
         * File this item belongs to.
         */
        private final File file;

        /**
         * Instantiates a new file item.
         *
         * @param _file the _file
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
