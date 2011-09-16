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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Checkout;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIArchives;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("9800ec60-02ba-405e-a2bd-292e57eff8f4")
@EFapsRevision("$Rev$")
public abstract class Version_Base
{

    /**
     * Upload a Version. Moves the current File to a version and the new File into the current.
     *
     * @param _parameter Parameter as passed by the eFaps API.
     * @return new Return
     * @throws EFapsException on error
     */
    public Return upload(final Parameter _parameter)
        throws EFapsException
    {
        final Context.FileParameter fileItem = Context.getThreadContext().getFileParameters().get("upload");
        if (fileItem != null) {
            try {
                final String name = _parameter.getParameterValue("name");
                final Instance current = _parameter.getCallInstance();
                // create a new Version
                final Insert insert = new Insert(CIArchives.Version);
                insert.add(CIArchives.Version.FileLink, current.getId());
                insert.execute();
                final Instance version = insert.getInstance();
                // get the current file, put it in termporary file and then check it in again
                final Checkout checkout = new Checkout(current);
                final InputStream currentStream = checkout.execute();
                final File temp = new FileUtil().getFile(checkout.getFileName());
                final OutputStream out = new FileOutputStream(temp);
                IOUtils.copy(currentStream, out);
                currentStream.close();
                out.close();
                final FileInputStream in = new  FileInputStream(temp);
                // checkin  the current as version
                final Checkin versionCheckin = new Checkin(version);
                versionCheckin.execute(checkout.getFileName(), in, in.available());
                in.close();
                // override the current with new file
                final Checkin checkin = new Checkin(current);
                checkin.execute(name, fileItem.getInputStream(), (int) fileItem.getSize());
            } catch (final IOException e) {
                throw new EFapsException(this.getClass(), "execute", e, _parameter);
            }
        }
        return new Return();
    }
}
