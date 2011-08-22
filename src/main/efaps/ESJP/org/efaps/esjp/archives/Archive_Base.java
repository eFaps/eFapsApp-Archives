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
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIArchives;
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
{
    /**
     * access check to edit store files.
     * 
     * @param _parameter Parameter as passed from the eFaps API.
     * @return ret Return.
     * @throws EFapsException on error.
     */
    public Return accessCheck4EditFileName(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        // Archives_Configuration
        final SystemConfiguration sisconf = SystemConfiguration.get(UUID.fromString("18c6c5fb-fa59-4951-8f67-fc6644dccb44"));

        if (sisconf.getAttributeValueAsBoolean("ActivateEditFileName")) {
            ret.put(ReturnValues.TRUE, true);
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
