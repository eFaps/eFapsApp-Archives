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


package org.efaps.esjp.archives.util;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("35d00ada-2336-4c3a-bbf9-aed20d3bcee8")
@EFapsApplication("eFapsApp-Archives")
public interface ArchivesSettings
{
    /**
     * OID for a Link.<br/>
     * Default Currency for the Form like Invoice etc..
     */
    String BASE = "org.efaps.archives.";

    /**
     * Boolean (true/false).
     * Activate the possibility to change file to store.
     */
    String ACTIVATE_EDITFILENAME = ArchivesSettings.BASE + "ActivateEditFileName";

    /**
     * Boolean (true/false).
     * Activate versioning of archives
     */
    String ACTIVATE_VERSIONING = ArchivesSettings.BASE + "ActivateVersioning";

    /**
     * Definition for File Structure as properties
     */
    String FILE_STRUCTURE = ArchivesSettings.BASE + "FileStructure";

    /**
     * Properties. Can be concatenated.
     */
    String OBJ2ARCHCONFIG = ArchivesSettings.BASE + "Object2ArchiveConfig";
}
